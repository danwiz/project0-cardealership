package com.revature.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.Car;
import com.revature.cardealer.PaymentTransaction;
import com.revature.cardealer.User;

class RecordPaymentCommandTest {

    @Test
    void jdbcCustomerPaymentUpdatesPlanAndLedgerDurably() {
        String url = "jdbc:h2:mem:record_payment_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
        InfrastructureConfiguration configuration = InfrastructureConfiguration.jdbc(url);
        ConfiguredDealershipApplication application = DealershipCompositionRoot.create(configuration);
        application.getAccounts().save(user("alice", AccountRole.CUSTOMER));
        application.ownershipFor("alice").add(new Car("Honda", "Accord", 2020), 1200, 12);

        PaymentTransaction transaction = new RecordPaymentCommand(application)
                .execute(user("alice", AccountRole.CUSTOMER), "alice", 0, 200);

        assertEquals(200, transaction.getAmount());
        assertEquals(1000, transaction.getRemainingBalance());
        assertEquals(200, application.ownershipFor("alice").findAll().get(0)
                .getPaymentPlan().getAmountPaid());
        assertEquals(1, application.paymentsFor("alice").findAll().size());

        ConfiguredDealershipApplication restarted = DealershipCompositionRoot.create(configuration);
        assertEquals(200, restarted.ownershipFor("alice").findAll().get(0)
                .getPaymentPlan().getAmountPaid());
        assertEquals(1000, restarted.paymentsFor("alice").ledger().getBalance());
    }

    @Test
    void employeeCanRecordCustomerPaymentButCustomerCannotPayAnotherAccount() {
        ConfiguredDealershipApplication application = DealershipCompositionRoot.create(
                InfrastructureConfiguration.inMemory());
        application.getAccounts().save(user("alice", AccountRole.CUSTOMER));
        application.getAccounts().save(user("employee", AccountRole.EMPLOYEE));
        application.ownershipFor("alice").add(new Car("Toyota", "Corolla", 2019), 600, 6);
        RecordPaymentCommand command = new RecordPaymentCommand(application);

        command.execute(user("employee", AccountRole.EMPLOYEE), "alice", 0, 100);
        assertEquals(100, application.paymentsFor("alice").ledger().getTotalPaid());
        assertThrows(SecurityException.class,
                () -> command.execute(user("bob", AccountRole.CUSTOMER), "alice", 0, 100));
    }

    @Test
    void jdbcOverpaymentRollsBackPlanAndLedgerTogether() {
        String url = "jdbc:h2:mem:payment_rollback_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
        ConfiguredDealershipApplication application = DealershipCompositionRoot.create(
                InfrastructureConfiguration.jdbc(url));
        application.getAccounts().save(user("alice", AccountRole.CUSTOMER));
        application.ownershipFor("alice").add(new Car("BMW", "4Series", 2018), 500, 5);

        assertThrows(IllegalArgumentException.class,
                () -> new RecordPaymentCommand(application).execute(
                        user("alice", AccountRole.CUSTOMER), "alice", 0, 501));
        assertEquals(0, application.ownershipFor("alice").findAll().get(0)
                .getPaymentPlan().getAmountPaid());
        assertEquals(0, application.paymentsFor("alice").findAll().size());
    }

    private static User user(String username, AccountRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-credential");
        user.setRole(role);
        return user;
    }
}
