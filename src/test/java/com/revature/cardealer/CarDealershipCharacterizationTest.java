package com.revature.cardealer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.revature.service.Pbkdf2PasswordHasher;
import com.revature.service.UserAccountRepository;
import com.revature.service.UserLoginService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CarDealershipCharacterizationTest {

    private final PrintStream originalOut = System.out;

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    @Test
    @Tag("target-behavior")
    void customerRegistrationUsesSharedServiceWithoutUnsafeDowncast() throws Throwable {
        String username = uniqueUsername("customer");
        ByteArrayOutputStream output = captureOutput();
        replaceScanner("1\n" + username + "\nsecret\n");

        invokePerformUserAction("1");

        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Customer registered"));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void unregisteredEmployeeCannotReachInventoryManagementFlow() throws Throwable {
        ByteArrayOutputStream output = captureOutput();
        replaceScanner(uniqueUsername("unknown") + "\npassword\n");

        invokePerformUserAction("3");

        String rendered = output.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("failure"));
        assertFalse(rendered.contains("Employee View"));
        assertFalse(rendered.contains("Add Car to Lot"));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void customerCredentialsCannotEnterEmployeeMenu() throws Throwable {
        String username = uniqueUsername("customer-role");
        ByteArrayOutputStream output = captureOutput();

        replaceScanner("1\n" + username + "\nsecret\n");
        invokePerformUserAction("1");

        replaceScanner(username + "\nsecret\n");
        invokePerformUserAction("3");

        String rendered = output.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("access denied"));
        assertFalse(rendered.contains("Employee View"));
    }

    @Test
    @Tag("target-behavior")
    void registeredEmployeeReachesOnlyAuthorizedEmployeeMenu() throws Throwable {
        String username = uniqueUsername("employee");
        ByteArrayOutputStream output = captureOutput();

        provisionStaff(username, "secret", AccountRole.EMPLOYEE);

        replaceScanner(username + "\nsecret\n3\n");
        invokePerformUserAction("3");

        String rendered = output.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("Employee View"));
        assertTrue(rendered.contains("View Customer Payments"));
        assertEquals(username, CarDealership.getCurrentAccount().getUsername());
        assertEquals(AccountRole.EMPLOYEE, CarDealership.getCurrentAccount().getRole());
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void administratorFeaturesRequireAuthenticatedAdministratorRole() throws Throwable {
        String username = uniqueUsername("administrator");
        ByteArrayOutputStream output = captureOutput();

        provisionStaff(username, "secret", AccountRole.ADMINISTRATOR);

        replaceScanner(username + "\nsecret\n9\n");
        invokePerformUserAction("5");

        String rendered = output.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("You are now an Admin"));
        assertEquals(AccountRole.ADMINISTRATOR, CarDealership.getCurrentAccount().getRole());
    }

    @Test
    @Tag("security")
    void viewingInventoryAsCustomerDoesNotAddVehicles() throws Throwable {
        String username = uniqueUsername("viewer");
        replaceScanner("1\n" + username + "\nsecret\n");
        invokePerformUserAction("1");

        Field inventoryField = CarDealership.class.getDeclaredField("Carlot");
        inventoryField.setAccessible(true);
        Offer inventory = (Offer) inventoryField.get(null);
        Field indexField = Offer.class.getDeclaredField("oIndex");
        indexField.setAccessible(true);
        int before = indexField.getInt(inventory);

        replaceScanner(username + "\nsecret\n1\n0\n");
        invokePerformUserAction("2");

        assertEquals(before, indexField.getInt(inventory));
    }

    @Test
    @Tag("security")
    void switchingToPublicActionClearsPreviousPrivilegedSession() throws Throwable {
        String username = uniqueUsername("session-admin");
        provisionStaff(username, "secret", AccountRole.ADMINISTRATOR);
        replaceScanner(username + "\nsecret\n9\n");
        invokePerformUserAction("5");
        assertEquals(AccountRole.ADMINISTRATOR, CarDealership.getCurrentAccount().getRole());

        replaceScanner("9\n");
        invokePerformUserAction("1");
        org.junit.jupiter.api.Assertions.assertNull(CarDealership.getCurrentAccount());
    }

    @Test
    @Tag("security")
    void publicRegistrationRefusesEmployeeAndAdministratorCreation() throws Throwable {
        ByteArrayOutputStream output = captureOutput();
        replaceScanner("2\n");
        invokePerformUserAction("1");
        replaceScanner("3\n");
        invokePerformUserAction("1");

        String rendered = output.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("access denied: privileged registration requires administrator"));
        assertFalse(rendered.contains("Employee registered"));
        assertFalse(rendered.contains("Administrator registered"));
    }

    private static void provisionStaff(String username, String password, AccountRole role) throws Exception {
        Field repositoryField = CarDealership.class.getDeclaredField("USER_REPOSITORY");
        repositoryField.setAccessible(true);
        UserAccountRepository repository = (UserAccountRepository) repositoryField.get(null);

        // Test-only trusted bootstrap. The production bootstrap flow is a separate release gate.
        String bootstrapName = uniqueUsername("bootstrap-admin");
        User admin = credentials(bootstrapName, new Pbkdf2PasswordHasher().hash("bootstrap-secret"));
        admin.setRole(AccountRole.ADMINISTRATOR);
        repository.save(admin);

        new UserLoginService(repository).provisionUser(
                credentials(bootstrapName, "bootstrap-secret"),
                username, password, role);
    }

    private static User credentials(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }

    @Test
    @Tag("security")
    void loadDataDeniesUnauthenticatedCallsBeforeLegacyFileHandling() {
        CarDealership application = new CarDealership();

        assertThrows(SecurityException.class, application::loadData);
    }

    private static ByteArrayOutputStream captureOutput() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        return output;
    }

    private static String uniqueUsername(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    private static void replaceScanner(String input) throws Exception {
        Field scannerField = CarDealership.class.getDeclaredField("scan");
        scannerField.setAccessible(true);
        scannerField.set(null,
                new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
    }

    private static void invokePerformUserAction(String option) throws Throwable {
        Method method = CarDealership.class.getDeclaredMethod("performUserAction", String.class);
        method.setAccessible(true);
        try {
            method.invoke(null, option);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
