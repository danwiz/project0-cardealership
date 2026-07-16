package com.revature.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.revature.cardealer.Car;
import com.revature.cardealer.OwnedVehicle;
import com.revature.cardealer.PaymentPlan;
import com.revature.repository.OwnershipRepository;

/** JDBC ownership repository scoped to one customer account. */
public final class JdbcOwnershipRepository implements OwnershipRepository {
    private final JdbcDatabase database;
    private final String ownerUsername;

    public JdbcOwnershipRepository(JdbcDatabase database, String ownerUsername) {
        this.database = Objects.requireNonNull(database, "database");
        this.ownerUsername = requireText(ownerUsername, "owner username");
        this.database.migrate();
        requireOwnerExists();
    }

    @Override
    public List<OwnedVehicle> findAll() {
        String sql = "SELECT ov.make, ov.model, ov.vehicle_year, pp.purchase_price, "
                + "pp.payment_months, pp.amount_paid FROM owned_vehicles ov "
                + "JOIN payment_plans pp ON pp.ownership_id = ov.ownership_id "
                + "WHERE ov.owner_username = ? ORDER BY ov.ownership_id";
        List<OwnedVehicle> vehicles = new ArrayList<>();
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUsername);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    PaymentPlan plan = new PaymentPlan(result.getInt("purchase_price"),
                            result.getInt("payment_months"));
                    int amountPaid = result.getInt("amount_paid");
                    if (amountPaid > 0) plan.recordPayment(amountPaid);
                    vehicles.add(new OwnedVehicle(
                            new Car(result.getString("make"), result.getString("model"),
                                    result.getInt("vehicle_year")),
                            plan));
                }
            }
            return Collections.unmodifiableList(vehicles);
        } catch (SQLException exception) {
            throw databaseFailure("could not read owned vehicles", exception);
        }
    }

    @Override
    public void add(Car car, int purchasePrice, int paymentMonths) {
        Objects.requireNonNull(car, "car");
        validatePlan(purchasePrice, paymentMonths, 0);
        inTransaction(connection -> {
            insertOwnedVehicle(connection, car, new PaymentPlan(purchasePrice, paymentMonths));
            return null;
        });
    }

    @Override
    public void replaceAll(List<OwnedVehicle> vehicles) {
        List<OwnedVehicle> replacement = new ArrayList<>(Objects.requireNonNull(vehicles, "vehicles"));
        for (OwnedVehicle vehicle : replacement) validateVehicle(vehicle);
        inTransaction(connection -> {
            deleteCurrentOwnership(connection);
            for (OwnedVehicle vehicle : replacement) {
                insertOwnedVehicle(connection, vehicle.getVehicle(), vehicle.getPaymentPlan());
            }
            return null;
        });
    }

    private void insertOwnedVehicle(Connection connection, Car car, PaymentPlan plan) throws SQLException {
        validateVehicle(new OwnedVehicle(car, plan));
        long ownershipId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO owned_vehicles(owner_username, make, model, vehicle_year) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, ownerUsername);
            statement.setString(2, requireText(car.getCarMake(), "make"));
            statement.setString(3, requireText(car.getCarModel(), "model"));
            statement.setInt(4, car.getCarYear());
            if (statement.executeUpdate() != 1) throw new IllegalStateException("owned vehicle insert failed");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("ownership identifier was not generated");
                ownershipId = keys.getLong(1);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO payment_plans(ownership_id, purchase_price, payment_months, monthly_payment, amount_paid, balance) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, ownershipId);
            statement.setInt(2, plan.getPurchasePrice());
            statement.setInt(3, plan.getTermMonths());
            statement.setInt(4, plan.getMonthlyPayment());
            statement.setInt(5, plan.getAmountPaid());
            statement.setInt(6, plan.getRemainingBalance());
            statement.executeUpdate();
        }
    }

    private void deleteCurrentOwnership(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM payment_plans WHERE ownership_id IN "
                        + "(SELECT ownership_id FROM owned_vehicles WHERE owner_username = ?)")) {
            statement.setString(1, ownerUsername);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM owned_vehicles WHERE owner_username = ?")) {
            statement.setString(1, ownerUsername);
            statement.executeUpdate();
        }
    }

    private void requireOwnerExists() {
        try (Connection connection = database.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT role FROM accounts WHERE username = ?")) {
            statement.setString(1, ownerUsername);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("unknown owner account: " + ownerUsername);
                if (!"CUSTOMER".equals(result.getString("role"))) {
                    throw new IllegalArgumentException("owner account must have CUSTOMER role");
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure("could not validate owner account", exception);
        }
    }

    private static void validateVehicle(OwnedVehicle vehicle) {
        Objects.requireNonNull(vehicle, "owned vehicle");
        Car car = Objects.requireNonNull(vehicle.getVehicle(), "vehicle");
        requireText(car.getCarMake(), "make");
        requireText(car.getCarModel(), "model");
        PaymentPlan plan = Objects.requireNonNull(vehicle.getPaymentPlan(), "payment plan");
        validatePlan(plan.getPurchasePrice(), plan.getTermMonths(), plan.getAmountPaid());
    }

    private static void validatePlan(int purchasePrice, int paymentMonths, int amountPaid) {
        if (purchasePrice < 0) throw new IllegalArgumentException("purchase price must not be negative");
        if (paymentMonths <= 0) throw new IllegalArgumentException("payment months must be positive");
        if (amountPaid < 0 || amountPaid > purchasePrice) {
            throw new IllegalArgumentException("amount paid must be between zero and purchase price");
        }
    }

    private <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                T value = work.execute(connection);
                connection.commit();
                return value;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("ownership transaction failed", exception);
        }
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    private static IllegalStateException databaseFailure(String message, SQLException exception) {
        return new IllegalStateException(message, exception);
    }

    private interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
}
