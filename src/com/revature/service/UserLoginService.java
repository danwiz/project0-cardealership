package com.revature.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;

public class UserLoginService {

    private final UserAccountRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserLoginService() {
        this(new InMemoryUserAccountRepository(), new Pbkdf2PasswordHasher());
    }

    public UserLoginService(UserAccountRepository userRepository) {
        this(userRepository, new Pbkdf2PasswordHasher());
    }

    public UserLoginService(UserAccountRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    /**
     * Unauthenticated self-service registration always creates a customer.
     */
    public User registerUser(String username, String password) {
        return registerWithRole(username, password, AccountRole.CUSTOMER);
    }

    /**
     * Privileged provisioning requires a verified account with MANAGE_USERS.
     * The initial administrator must be installed through a separate trusted
     * bootstrap process; this API does not create one without authentication.
     */
    public User provisionUser(User administratorCredentials, String username,
            String password, AccountRole role) {
        User verifiedOperator = authenticate(administratorCredentials)
                .orElseThrow(() -> new SecurityException("administrator authentication required"));
        new RoleAuthorizationService().requireAuthorized(verifiedOperator, Permission.MANAGE_USERS);
        return registerWithRole(username, password, role);
    }

    private User registerWithRole(String username, String password, AccountRole role) {
        validateCredential("username", username);
        validateCredential("password", password);
        Objects.requireNonNull(role, "role");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username is already registered");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordHasher.hash(password));
        newUser.setRole(role);
        return userRepository.save(newUser);
    }

    public void removeUser(User user) {
        if (user != null && user.getUsername() != null) {
            userRepository.removeByUsername(user.getUsername());
        }
    }

    public Optional<User> authenticate(User credentials) {
        if (credentials == null || isBlank(credentials.getUsername()) || credentials.getPassword() == null) {
            return Optional.empty();
        }

        return userRepository.findByUsername(credentials.getUsername())
                .filter(account -> passwordHasher.matches(credentials.getPassword(), account.getPassword()));
    }

    public boolean authenticateUser(User credentials) {
        return authenticate(credentials).isPresent();
    }

    public String[] getUserNames() {
        List<User> users = userRepository.findAll();
        String[] usernames = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            usernames[i] = users.get(i).getUsername();
        }
        return usernames;
    }

    private static void validateCredential(String fieldName, String value) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
