package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.application.ConfiguredDealershipApplication;
import com.revature.application.ConfiguredDomainAdapter;
import com.revature.application.DealershipCompositionRoot;
import com.revature.application.InfrastructureConfiguration;
import com.revature.application.ManageInventoryCommand;
import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;
import com.revature.support.AuthenticatedAccounts;
import com.revature.support.TestAccounts;

/**
 * Negative tests transferred by behavior from the short legacy-console branch.
 * These checks prove immediate denial after persisted role demotion; they do
 * NOT prove irreversible invalidation after demote-then-restore (issue #98).
 */
@Tag("SECURITY")
class SessionDemotionInventoryRegressionTest {

    @Test
    void demotionDeniesExistingEmployeeInventorySessionInMemory() {
        assertDemotionDeniesExistingSession(
                DealershipCompositionRoot.create(InfrastructureConfiguration.inMemory()));
    }

    @Test
    void demotionDeniesExistingEmployeeInventorySessionJdbc() {
        String url = "jdbc:h2:mem:inventory_role_revocation_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
        assertDemotionDeniesExistingSession(
                DealershipCompositionRoot.create(InfrastructureConfiguration.jdbc(url)));
    }

    private static void assertDemotionDeniesExistingSession(
            ConfiguredDealershipApplication application) {
        UserAccountRepository repository = TestAccounts.trustedRepository(application);
        TestAccounts.seed(repository, AccountRole.EMPLOYEE, "staff", "TestPass123!");

        User authenticated = AuthenticatedAccounts.login(application,
                AccountRole.EMPLOYEE, "staff", "TestPass123!");
        ConfiguredDomainAdapter adapter = new ConfiguredDomainAdapter(application);
        ManageInventoryCommand command = new ManageInventoryCommand(adapter, adapter);

        int initial = application.getInventory().listingCount();
        command.add(authenticated, "Honda", "Accord", 2020, 15000, 2);
        assertEquals(initial + 1, application.getInventory().listingCount());

        // A trusted fixture changes persisted authority after the actor logged in.
        assertTrue(repository.updateRoleIfCurrent(
                "staff", AccountRole.EMPLOYEE, AccountRole.CUSTOMER));

        assertThrows(SecurityException.class,
                () -> command.add(authenticated, "Toyota", "Camry", 2021, 18000, 1));
        assertThrows(SecurityException.class,
                () -> command.remove(authenticated, 0));
        assertEquals(initial + 1, application.getInventory().listingCount());
        assertEquals(AccountRole.CUSTOMER,
                repository.findByUsername("staff").orElseThrow(AssertionError::new).getRole());
    }
}
