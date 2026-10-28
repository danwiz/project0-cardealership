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

    /** Public self-registration always creates a CUSTOMER. */
    public User registerUser(String username, String password) {
        return registerWithRole(username, password, AccountRole.CUSTOMER);
    }

    /**
     * Privileged role assignment is available only to a previously authenticated
     * administrator. First-admin bootstrap requires a separate trusted process.
     */
    public User provisionUser(User operatorCredentials, String username,
            String password, AccountRole role) {
        User operator = authenticate(operatorCredentials)
                .orElseThrow(() -> new SecurityException("administrator authentication required"));
        new RoleAuthorizationService().requireAuthorized(operator, Permission.MANAGE_USERS);
        return registerWithRole(username, password, role);
    }

    /**
     * A user may change their own password only by supplying their current
     * plaintext credentials. Compare-and-swap prevents stale updates.
     */
    public boolean changePassword(User currentCredentials, String replacementPassword) {
        validateCredential("replacementPassword", replacementPassword);
        User existing = authenticate(currentCredentials)
                .orElseThrow(() -> new SecurityException("current account authentication required"));
        return userRepository.updateCredentialIfCurrent(existing.getUsername(),
                existing.getPassword(), passwordHasher.hash(replacementPassword));
    }

    /**
     * Only a currently authenticated persisted administrator may reassign roles.
     * The expected role is checked atomically against the repository.
     */
    public boolean changeRole(User administratorCredentials, String targetUsername,
            AccountRole expectedRole, AccountRole replacementRole) {
        validateCredential("targetUsername", targetUsername);
        Objects.requireNonNull(expectedRole, "expectedRole");
        Objects.requireNonNull(replacementRole, "replacementRole");
        User administrator = authenticate(administratorCredentials)
                .orElseThrow(() -> new SecurityException("administrator authentication required"));
        new RoleAuthorizationService().requireAuthorized(administrator, Permission.MANAGE_USERS);
        return userRepository.updateRoleIfCurrent(targetUsername, expectedRole, replacementRole);
    }

    private User registerWithRole(String username, String password, AccountRole role) {
        validateCredential("username", username);
        validateCredential("password", password);
        Objects.requireNonNull(role, "role");
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username is already registered");
        }
        User account = new User();
        account.setUsername(username);
        account.setPassword(passwordHasher.hash(password));
        account.setRole(role);
        return userRepository.save(account);
    }

    public boolean removeUser(User operatorCredentials, String targetUsername) {
        validateCredential("targetUsername", targetUsername);
        User operator = authenticate(operatorCredentials)
                .orElseThrow(() -> new SecurityException("administrator authentication required"));
        new RoleAuthorizationService().requireAuthorized(operator, Permission.MANAGE_USERS);
        return userRepository.removeByUsername(targetUsername);
    }

    /** The legacy unauthenticated deletion API fails closed. */
    @Deprecated
    public void removeUser(User user) {
        throw new SecurityException("administrator authentication required for account deletion");
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

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public String[] getUserNames() {
        List<User> users = getUsers();
        String[] usernames = new String[users.size()];
        for (int i = 0; i < users.size(); i++) usernames[i] = users.get(i).getUsername();
        return usernames;
    }

    private static void validateCredential(String fieldName, String value) {
        if (value == null) throw new IllegalArgumentException(fieldName + " must not be null");
        if (isBlank(value)) throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
