package com.revature.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;
import com.revature.service.UserAccountRepository;

/** JDBC-backed account repository using the migrated accounts table. */
public final class JdbcUserAccountRepository implements UserAccountRepository {
    private final JdbcDatabase database;

    public JdbcUserAccountRepository(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.database.migrate();
    }

    @Override
    public User save(User user) {
        validate(user);
        String sql = "MERGE INTO accounts (username, encoded_credential, role) KEY(username) VALUES (?, ?, ?)";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getRole().name());
            statement.executeUpdate();
            return copy(user);
        } catch (SQLException exception) {
            throw new IllegalStateException("could not save account", exception);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        String sql = "SELECT username, encoded_credential, role FROM accounts WHERE username = ?";
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("could not find account", exception);
        }
    }

    @Override
    public boolean removeByUsername(String username) {
        if (username == null) return false;
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM accounts WHERE username = ?")) {
            statement.setString(1, username);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("could not remove account", exception);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> accounts = new ArrayList<>();
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT username, encoded_credential, role FROM accounts ORDER BY username");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) accounts.add(map(result));
            return Collections.unmodifiableList(accounts);
        } catch (SQLException exception) {
            throw new IllegalStateException("could not list accounts", exception);
        }
    }

    private static User map(ResultSet result) throws SQLException {
        User user = new User();
        user.setUsername(result.getString("username"));
        user.setPassword(result.getString("encoded_credential"));
        user.setRole(AccountRole.valueOf(result.getString("role")));
        return user;
    }

    private static User copy(User source) {
        User copy = new User();
        copy.setUsername(source.getUsername());
        copy.setPassword(source.getPassword());
        copy.setRole(source.getRole());
        return copy;
    }

    private static void validate(User user) {
        Objects.requireNonNull(user, "user");
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("encoded credential must not be blank");
        }
        Objects.requireNonNull(user.getRole(), "role");
    }
}
