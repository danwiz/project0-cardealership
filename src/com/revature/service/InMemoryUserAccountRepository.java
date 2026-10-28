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

    @Override
    public User save(User user) {
        User stored = copy(Objects.requireNonNull(user, "user"));
        usersByUsername.put(stored.getUsername(), stored);
        return copy(stored);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username)).map(InMemoryUserAccountRepository::copy);
    }

    @Override
    public boolean removeByUsername(String username) {
        return usersByUsername.remove(username) != null;
    }

    @Override
    public List<User> findAll() {
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
