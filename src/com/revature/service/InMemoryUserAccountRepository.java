package com.revature.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

import com.revature.cardealer.User;

/**
 * Deterministic in-memory repository that preserves registration order.
 */
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, User> usersByUsername = new LinkedHashMap<>();

    /**
     * Create-only persistence boundary. A prior findByUsername check is
     * advisory; the duplicate check and insertion must be atomic here.
     * Account updates require a separate, explicitly authorized operation.
     */
    @Override
    public synchronized User save(User user) {
        User stored = copy(Objects.requireNonNull(user, "user"));
        String username = stored.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (usersByUsername.containsKey(username)) {
            throw new IllegalArgumentException("username is already registered");
        }
        usersByUsername.put(username, stored);
        return copy(stored);
    }

    @Override
    public synchronized Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username)).map(InMemoryUserAccountRepository::copy);
    }

    @Override
    public synchronized boolean removeByUsername(String username) {
        return usersByUsername.remove(username) != null;
    }

    @Override
    public synchronized List<User> findAll() {
        List<User> copies = new ArrayList<>();
        for (User user : usersByUsername.values()) {
            copies.add(copy(user));
        }
        return Collections.unmodifiableList(copies);
    }

    private static User copy(User source) {
        User result = new User();
        result.setUsername(source.getUsername());
        result.setPassword(source.getPassword());
        result.setRole(source.getRole());
        return result;
    }
}
