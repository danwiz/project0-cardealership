package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.User;

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

        User attempt = user("dane", "wrong");

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
    @Tag("TARGET-BEHAVIOR")
    void constructingAnotherServiceDoesNotResetExistingServiceState() {
        UserLoginService first = new UserLoginService();
        User registered = first.registerUser("dane", "secret");

        new UserLoginService();

        assertTrue(first.authenticateUser(registered));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    void registrationIsNotLimitedToTenAccounts() {
        UserLoginService service = new UserLoginService();
        for (int i = 0; i < 25; i++) {
            service.registerUser("user" + i, "pw" + i);
        }

        assertTrue(service.authenticateUser(user("user24", "pw24")));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    void getUserNamesReturnsOnlyRegisteredUsersInRegistrationOrder() {
        UserLoginService service = new UserLoginService();
        service.registerUser("dane", "secret");
        service.registerUser("alex", "pw");

        assertArrayEquals(new String[] {"dane", "alex"}, service.getUserNames());
    }

    @Test
    void removeUserMakesPreviouslyRegisteredUserUnavailable() {
        UserLoginService service = new UserLoginService();
        User registered = service.registerUser("dane", "secret");

        service.removeUser(registered);

        assertFalse(service.authenticateUser(registered));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    void servicesCanShareAnExplicitRepository() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        UserLoginService registrationService = new UserLoginService(repository);
        UserLoginService authenticationService = new UserLoginService(repository);

        registrationService.registerUser("dane", "secret");

        assertTrue(authenticationService.authenticateUser(user("dane", "secret")));
    }

    @Test
    void registrationPreservesExactCredentialStrings() {
        UserLoginService service = new UserLoginService();

        User registered = service.registerUser(" Dane ", " Secret ");

        assertEquals(" Dane ", registered.getUsername());
        assertEquals(" Secret ", registered.getPassword());
    }

    private static User user(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
