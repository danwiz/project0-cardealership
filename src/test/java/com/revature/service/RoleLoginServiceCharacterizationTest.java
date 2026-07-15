package com.revature.service;

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

@Tag("LEGACY-BEHAVIOR")
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
    @Tag("KNOWN-DEFECT")
    @Tag("SECURITY")
    void customerAuthenticationAlwaysReturnsTrueForUnregisteredUser() {
        CustomerLoginService service = new CustomerLoginService();
        User unregistered = user("unknown", "wrong");
        System.setOut(new PrintStream(new ByteArrayOutputStream()));

        assertTrue(service.authenticateUser(unregistered));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    @Tag("SECURITY")
    void employeeAuthenticationAlwaysReturnsTrueForUnregisteredUser() {
        EmployeeLoginService service = new EmployeeLoginService();

        assertTrue(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    @Tag("SECURITY")
    void administratorAuthenticationAlwaysReturnsTrueForUnregisteredUser() {
        AdminLoginService service = new AdminLoginService();

        assertTrue(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    void customerAuthenticationPrintsSubmittedUsername() {
        CustomerLoginService service = new CustomerLoginService();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        service.authenticateUser(user("dane", "secret"));

        assertTrue(output.toString().contains("dane"));
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
