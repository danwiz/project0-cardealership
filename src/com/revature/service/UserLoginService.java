package com.revature.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public User registerUser(String username, String password) {
        validateCredential("username", username);
        validateCredential("password", password);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username is already registered");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordHasher.hash(password));
        return userRepository.save(newUser);
    }

    public void removeUser(User user) {
        if (user != null && user.getUsername() != null) {
            userRepository.removeByUsername(user.getUsername());
        }
    }

    public boolean authenticateUser(User user) {
        if (user == null || isBlank(user.getUsername()) || user.getPassword() == null) {
            return false;
        }

        Optional<User> registeredUser = userRepository.findByUsername(user.getUsername());
        return registeredUser
                .map(account -> passwordHasher.matches(user.getPassword(), account.getPassword()))
                .orElse(false);
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
