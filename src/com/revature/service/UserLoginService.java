package com.revature.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.revature.cardealer.User;

public class UserLoginService {

    private final UserAccountRepository userRepository;

    public UserLoginService() {
        this(new InMemoryUserAccountRepository());
    }

    public UserLoginService(UserAccountRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    public User registerUser(String username, String password) {
        User newUser = new User();
        newUser.setPassword(password);
        newUser.setUsername(username);
        return userRepository.save(newUser);
    }

    public void removeUser(User user) {
        if (user != null) {
            userRepository.removeByUsername(user.getUsername());
        }
    }

    public boolean authenticateUser(User user) {
        if (user == null) {
            return false;
        }

        Optional<User> registeredUser = userRepository.findByUsername(user.getUsername());
        return registeredUser
                .map(account -> Objects.equals(account.getPassword(), user.getPassword()))
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
}
