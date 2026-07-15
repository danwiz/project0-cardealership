package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.User;

@Tag("LEGACY-BEHAVIOR")
class UserLoginServiceCharacterizationTest {

    private PrintStream originalOut;

    @BeforeEach
    void captureOutput() {
        originalOut = System.out;
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    @Test
    void registeredUserAuthenticatesWithMatchingPassword() {
        UserLoginService service = new UserLoginService();
        User registered = service.registerUser("dane", "secret");

        assertTrue(service.authenticateUser(registered));
    }

    @Test
    void authenticationFailsWhenPasswordDoesNotMatch() {
        UserLoginService service = new UserLoginService();
        service.registerUser("dane", "secret");

        User attempt = new User();
        attempt.setUsername("dane");
        attempt.setPassword("wrong");

        assertFalse(service.authenticateUser(attempt));
    }

    @Test
    @Tag("SECURITY")
    @Tag("TARGET-BEHAVIOR")
    void authenticationDoesNotPrintStoredPasswordToStandardOutput() {
        UserLoginService service = new UserLoginService();
        User registered = service.registerUser("dane", "secret");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        service.authenticateUser(registered);

        assertFalse(output.toString().contains("secret"));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void constructingAnotherServiceResetsSharedStaticUserDatabase() {
        UserLoginService first = new UserLoginService();
        User registered = first.registerUser("dane", "secret");

        new UserLoginService();

        assertFalse(first.authenticateUser(registered));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void registeringBeyondFixedCapacityThrowsArrayIndexException() {
        UserLoginService service = new UserLoginService();
        for (int i = 0; i < 10; i++) {
            service.registerUser("user" + i, "pw" + i);
        }

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> service.registerUser("overflow", "pw"));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    void getUserNamesFailsWhenDatabaseIsNotCompletelyPopulated() {
        UserLoginService service = new UserLoginService();
        service.registerUser("dane", "secret");

        assertThrows(NullPointerException.class, service::getUserNames);
    }

    @Test
    void removeUserMakesPreviouslyRegisteredUserUnavailable() {
        UserLoginService service = new UserLoginService();
        User registered = service.registerUser("dane", "secret");

        service.removeUser(registered);

        assertFalse(service.authenticateUser(registered));
    }

    @Test
    void registrationPreservesExactCredentialStrings() {
        UserLoginService service = new UserLoginService();

        User registered = service.registerUser(" Dane ", " Secret ");

        assertEquals(" Dane ", registered.getUsername());
        assertEquals(" Secret ", registered.getPassword());
    }
}
