package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.Offer;
import com.revature.cardealer.User;

class AuthorizedInventoryServiceTest {
    private static User credentials(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }

    private static void saveAccount(UserAccountRepository repository, String username,
            String password, AccountRole role) {
        User stored = credentials(username, new Pbkdf2PasswordHasher().hash(password));
        stored.setRole(role);
        repository.save(stored);
    }

    @Test
    @Tag("security")
    void authenticatedEmployeeCanAddVehicle() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        saveAccount(repository, "staff", "secret", AccountRole.EMPLOYEE);
        AuthorizedInventoryService service = new AuthorizedInventoryService(repository, new Offer());
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> service.addVehicle(credentials("staff", "secret"),
                        "Honda", "Accord", 2017, 15000, "Yes", 1));
    }

    @Test
    @Tag("security")
    void customerCannotModifyInventory() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        saveAccount(repository, "customer", "secret", AccountRole.CUSTOMER);
        AuthorizedInventoryService service = new AuthorizedInventoryService(repository, new Offer());
        assertThrows(SecurityException.class,
                () -> service.addVehicle(credentials("customer", "secret"),
                        "Honda", "Accord", 2017, 15000, "Yes", 1));
    }

    @Test
    @Tag("security")
    void forgedRoleAndInvalidPasswordCannotModifyInventory() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        saveAccount(repository, "customer", "secret", AccountRole.CUSTOMER);
        AuthorizedInventoryService service = new AuthorizedInventoryService(repository, new Offer());
        User forged = credentials("customer", "secret");
        forged.setRole(AccountRole.ADMINISTRATOR);
        assertThrows(SecurityException.class,
                () -> service.removeVehicle(forged, 0));
        assertThrows(SecurityException.class,
                () -> service.removeVehicle(credentials("customer", "wrong"), 0));
    }

    @Test
    @Tag("security")
    void revokedEmployeeCannotModifyInventory() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        saveAccount(repository, "employee", "secret", AccountRole.EMPLOYEE);
        AuthorizedInventoryService service = new AuthorizedInventoryService(repository, new Offer());
        repository.removeByUsername("employee");
        assertThrows(SecurityException.class,
                () -> service.removeVehicle(credentials("employee", "secret"), 0));
    }

    @Test
    @Tag("security")
    void demotedEmployeeCannotModifyInventory() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        saveAccount(repository, "employee", "secret", AccountRole.EMPLOYEE);
        AuthorizedInventoryService service = new AuthorizedInventoryService(repository, new Offer());
        // Recreate fixture state explicitly: repository.save is create-only.
        org.junit.jupiter.api.Assertions.assertTrue(repository.removeByUsername("employee"));
        saveAccount(repository, "employee", "secret", AccountRole.CUSTOMER);
        assertThrows(SecurityException.class,
                () -> service.removeVehicle(credentials("employee", "secret"), 0));
    }
}
