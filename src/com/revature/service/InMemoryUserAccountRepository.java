package com.revature.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.revature.cardealer.User;

/**
 * Deterministic in-memory repository that preserves registration order.
 */
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, User> usersByUsername = new LinkedHashMap<>();

    @Override
    public User save(User user) {
        usersByUsername.put(user.getUsername(), user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public boolean removeByUsername(String username) {
        return usersByUsername.remove(username) != null;
    }

    @Override
    public List<User> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(usersByUsername.values()));
    }
}
