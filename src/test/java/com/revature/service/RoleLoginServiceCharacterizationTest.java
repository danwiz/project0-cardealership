package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.Car;
import com.revature.cardealer.User;

class RoleLoginServiceCharacterizationTest {

    private PrintStream originalOut;

    @BeforeEach
    void rememberOutput() {
        originalOut = System.out;
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void customerAuthenticationRejectsUnregisteredUser() {
        CustomerLoginService service = new CustomerLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void employeeAuthenticationRejectsUnregisteredUser() {
        EmployeeLoginService service = new EmployeeLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void administratorAuthenticationRejectsUnregisteredUser() {
        AdminLoginService service = new AdminLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("target-behavior")
    void roleServicesAuthenticateRegisteredCredentials() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        CustomerLoginService customerService = new CustomerLoginService(repository);
        customerService.registerUser("customer", "secret");
        assertTrue(customerService.authenticateUser(user("customer", "secret")));

        User bootstrapAdmin = user("bootstrap-admin", new Pbkdf2PasswordHasher().hash("bootstrap-secret"));
        bootstrapAdmin.setRole(com.revature.cardealer.AccountRole.ADMINISTRATOR);
        repository.save(bootstrapAdmin);
        UserLoginService provisioner = new UserLoginService(repository);
        provisioner.provisionUser(user("bootstrap-admin", "bootstrap-secret"),
                "employee", "secret", com.revature.cardealer.AccountRole.EMPLOYEE);
        provisioner.provisionUser(user("bootstrap-admin", "bootstrap-secret"),
                "admin", "secret", com.revature.cardealer.AccountRole.ADMINISTRATOR);

        assertTrue(new EmployeeLoginService(repository).authenticateUser(user("employee", "secret")));
        assertTrue(new AdminLoginService(repository).authenticateUser(user("admin", "secret")));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void customerAuthenticationDoesNotPrintSubmittedUsername() {
        CustomerLoginService service = new CustomerLoginService();
        service.registerUser("dane", "secret");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        service.authenticateUser(user("dane", "secret"));

        assertFalse(output.toString().contains("dane"));
    }

    @Test
    @Tag("known-defect")
    void customerOwnedCarStorageFailsAtFixedArrayBoundary() {
        CustomerLoginService service = new CustomerLoginService();
        for (int i = 0; i < 20; i++) {
            service.setCarsOwned(new Car("Make" + i, "Model" + i, 2000 + i), 10000 + i, 12);
        }

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> service.setCarsOwned(new Car("Overflow", "Vehicle", 2020), 20000, 12));
    }

    @Test
    @Tag("known-defect")
    void viewingOwnedCarsFailsOnFirstNullSlot() {
        CustomerLoginService service = new CustomerLoginService();
        service.setCarsOwned(new Car("Toyota", "Corolla", 2010), 10000, 12);
        System.setOut(new PrintStream(new ByteArrayOutputStream()));

        assertThrows(NullPointerException.class, service::getCarsOwned);
    }

    @Test
    void administratorDeleteAllUsersOnlyEmitsWarningText() {
        AdminLoginService service = new AdminLoginService();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        service.deleteAllUsers();

        assertTrue(output.toString().contains("You better be sure"));
    }

    private static User user(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
