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
    @Tag("TARGET-BEHAVIOR")
    @Tag("SECURITY")
    void customerAuthenticationRejectsUnregisteredUser() {
        CustomerLoginService service = new CustomerLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    @Tag("SECURITY")
    void employeeAuthenticationRejectsUnregisteredUser() {
        EmployeeLoginService service = new EmployeeLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    @Tag("SECURITY")
    void administratorAuthenticationRejectsUnregisteredUser() {
        AdminLoginService service = new AdminLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    void roleServicesAuthenticateRegisteredCredentials() {
        CustomerLoginService customerService = new CustomerLoginService();
        customerService.registerUser("customer", "secret");
        assertTrue(customerService.authenticateUser(user("customer", "secret")));

        EmployeeLoginService employeeService = new EmployeeLoginService();
        employeeService.registerUser("employee", "secret");
        assertTrue(employeeService.authenticateUser(user("employee", "secret")));

        AdminLoginService adminService = new AdminLoginService();
        adminService.registerUser("admin", "secret");
        assertTrue(adminService.authenticateUser(user("admin", "secret")));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    @Tag("SECURITY")
    void customerAuthenticationDoesNotPrintSubmittedUsername() {
        CustomerLoginService service = new CustomerLoginService();
        service.registerUser("dane", "secret");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        service.authenticateUser(user("dane", "secret"));

        assertFalse(output.toString().contains("dane"));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void customerOwnedCarStorageFailsAtFixedArrayBoundary() {
        CustomerLoginService service = new CustomerLoginService();
        for (int i = 0; i < 20; i++) {
            service.setCarsOwned(new Car("Make" + i, "Model" + i, 2000 + i), 10000 + i, 12);
        }

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> service.setCarsOwned(new Car("Overflow", "Vehicle", 2020), 20000, 12));
    }

    @Test
    @Tag("KNOWN-DEFECT")
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
