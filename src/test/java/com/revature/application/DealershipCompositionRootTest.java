package com.revature.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.Car;
import com.revature.cardealer.User;
import com.revature.repository.OfferInventoryRepository;
import com.revature.repository.jdbc.JdbcInventoryRepository;
import com.revature.repository.jdbc.JdbcOwnershipRepository;
import com.revature.repository.jdbc.JdbcPaymentTransactionRepository;
import com.revature.repository.jdbc.JdbcUserAccountRepository;
import com.revature.service.InMemoryUserAccountRepository;

class DealershipCompositionRootTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty(InfrastructureConfiguration.MODE_PROPERTY);
        System.clearProperty(InfrastructureConfiguration.JDBC_URL_PROPERTY);
        System.clearProperty(InfrastructureConfiguration.JDBC_USER_PROPERTY);
        System.clearProperty(InfrastructureConfiguration.JDBC_PASSWORD_PROPERTY);
    }

    @Test
    void defaultsToInMemoryInfrastructure() {
        ConfiguredDealershipApplication application = DealershipCompositionRoot.createDefault();

        assertEquals(InfrastructureConfiguration.Mode.IN_MEMORY,
                application.getConfiguration().getMode());
        assertTrue(application.getAccounts() instanceof InMemoryUserAccountRepository);
        assertTrue(application.getInventory() instanceof OfferInventoryRepository);
        assertTrue(application.getContext().getAccountRepository() == application.getAccounts());
    }

    @Test
    void rejectsUnknownModeAndMissingJdbcUrl() {
        System.setProperty(InfrastructureConfiguration.MODE_PROPERTY, "unknown");
        assertThrows(IllegalArgumentException.class, DealershipCompositionRoot::createDefault);

        System.setProperty(InfrastructureConfiguration.MODE_PROPERTY, "jdbc");
        assertThrows(IllegalArgumentException.class, DealershipCompositionRoot::createDefault);
    }

    @Test
    void composesDurableJdbcRepositoriesFromProperties() {
        String url = jdbcUrl("composition");
        System.setProperty(InfrastructureConfiguration.MODE_PROPERTY, "jdbc");
        System.setProperty(InfrastructureConfiguration.JDBC_URL_PROPERTY, url);

        ConfiguredDealershipApplication first = DealershipCompositionRoot.createDefault();
        assertTrue(first.getAccounts() instanceof JdbcUserAccountRepository);
        assertTrue(first.getInventory() instanceof JdbcInventoryRepository);
        assertTrue(first.getContext().getAccountRepository() == first.getAccounts());

        first.getAccounts().save(user("alice", AccountRole.CUSTOMER));
        first.getInventory().addListing("Honda", "Civic", 2024, 30000, 2);
        first.ownershipFor("alice").add(new Car("Honda", "Civic", 2024), 30000, 60);
        ((JdbcPaymentTransactionRepository) first.paymentsFor("alice")).recordPayment(500, 30000);

        ConfiguredDealershipApplication second = DealershipCompositionRoot.create(
                InfrastructureConfiguration.jdbc(url));
        assertNotSame(first, second);
        assertTrue(second.getAccounts().findByUsername("alice").isPresent());
        assertEquals(1, second.getInventory().listings().size());
        assertEquals(1, second.ownershipFor("alice").findAll().size());
        assertEquals(1, second.paymentsFor("alice").findAll().size());
        assertTrue(second.ownershipFor("alice") instanceof JdbcOwnershipRepository);
        assertTrue(second.paymentsFor("alice") instanceof JdbcPaymentTransactionRepository);
    }

    @Test
    void jdbcFactoriesRemainCustomerScoped() {
        ConfiguredDealershipApplication application = DealershipCompositionRoot.create(
                InfrastructureConfiguration.jdbc(jdbcUrl("scoped")));
        application.getAccounts().save(user("alice", AccountRole.CUSTOMER));
        application.getAccounts().save(user("bob", AccountRole.CUSTOMER));

        application.ownershipFor("alice").add(new Car("A", "One", 2020), 1000, 10);
        ((JdbcPaymentTransactionRepository) application.paymentsFor("alice")).recordPayment(100, 1000);

        assertEquals(1, application.ownershipFor("alice").findAll().size());
        assertEquals(0, application.ownershipFor("bob").findAll().size());
        assertEquals(1, application.paymentsFor("alice").findAll().size());
        assertEquals(0, application.paymentsFor("bob").findAll().size());
        assertFalse(application.getAccounts().findByUsername("missing").isPresent());
    }

    private static User user(String username, AccountRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-credential");
        user.setRole(role);
        return user;
    }

    private static String jdbcUrl(String name) {
        return "jdbc:h2:mem:" + name + "_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    }
}