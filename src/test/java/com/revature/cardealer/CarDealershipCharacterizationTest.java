package com.revature.cardealer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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
    @Tag("KNOWN-DEFECT")
    void customerRegistrationDowncastsBaseLoginServiceAndFails() throws Exception {
        replaceScanner("1\nalice\nsecret\n");

        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> invokePerformUserAction("1"));

        // The exact message is JVM-specific; the exception type captures the unsafe cast.
        assertFalse(exception.getMessage() == null && exception.getCause() != null);
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void adminMenuUsesReferenceEqualityForChoiceStrings() throws Throwable {
        replaceScanner(new String("1") + "\n");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        invokePerformUserAction("5");

        String rendered = output.toString(StandardCharsets.UTF_8);
        assertFalse(rendered.contains("You better be sure"),
                "A dynamically read string with value 1 is not matched by the legacy == comparison");
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    @Tag("SECURITY")
    void unregisteredEmployeeCannotReachInventoryManagementFlow() throws Throwable {
        replaceScanner("employee\npassword\n");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        invokePerformUserAction("3");

        String rendered = output.toString(StandardCharsets.UTF_8);
        assertFalse(rendered.contains("Employee View"));
        assertFalse(rendered.contains("Enter -->  Make"));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void loadDataAddsASecondDatExtensionThenDereferencesNull() {
        CarDealership application = new CarDealership();

        assertThrows(NullPointerException.class, application::loadData);
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
