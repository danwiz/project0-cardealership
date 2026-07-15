package com.revature.service;

import java.util.List;
import java.util.Optional;

import com.revature.cardealer.User;

/**
 * Persistence boundary for user accounts used by authentication services.
 */
public interface UserAccountRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    boolean removeByUsername(String username);

    List<User> findAll();
}
