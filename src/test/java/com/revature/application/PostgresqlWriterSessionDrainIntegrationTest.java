package com.revature.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.repository.jdbc.FencedJdbcConnectionGate;
import com.revature.repository.jdbc.JdbcCredentialSource;
import com.revature.repository.jdbc.JdbcDatabase;

/**
 * CI-owned PostgreSQL 16 sessions only. These tests intentionally change and
 * terminate disposable SQL principals; they must never run against staging or
 * production databases via copied environment variables.
 */
@Tag("POSTGRESQL")
@Tag("SECURITY")
class PostgresqlWriterSessionDrainIntegrationTest {
    private static final String INITIAL = "ci-only-drain-initial-secret";
    private static final String REPLACEMENT = "ci-only-drain-replacement-secret";
    private static final String ORIGINAL = "ci-original-credential";

    @Test
    void localDrainRollsBackUncommittedChangeAndReopensOnlyAfterFreshLogin()
            throws Exception {
        Fixture fixture = fixture();
        AtomicReference<String> secret = new AtomicReference<>(INITIAL);
        AtomicBoolean sourceAvailable = new AtomicBoolean(true);
        AtomicLong attestedGeneration = new AtomicLong(1);

        JdbcDatabase writer = new JdbcDatabase(fixture.schemaUrl, () -> {
            if (!sourceAvailable.get()) throw new SecurityException("ci lease unavailable");
            return new JdbcCredentialSource.CredentialLease(
                    fixture.writerRole, secret.get().toCharArray());
        });
        FencedJdbcConnectionGate gate = new FencedJdbcConnectionGate(
                writer::openConnection, generation -> {
                    if (!sourceAvailable.get() || generation != attestedGeneration.get()) {
                        return false;
                    }
                    try (Connection check = writer.openConnection();
                            Statement statement = check.createStatement();
                            ResultSet user = statement.executeQuery("SELECT session_user")) {
                        return user.next() && fixture.writerRole.equals(user.getString(1));
                    } catch (SQLException | SecurityException unavailable) {
                        return false;
                    }
                });

        assertThrows(SecurityException.class, () -> gate.withConnection(c -> true));
        gate.activate(1);

        CountDownLatch updateStarted = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> pending = worker.submit(() -> gate.withConnection(connection -> {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    assertEquals(1, statement.executeUpdate(
                            "UPDATE credential_state SET encoded_credential = 'uncommitted' "
                            + "WHERE id = 1"));
                }
                updateStarted.countDown();
                try {
                    if (!releaseCallback.await(15, TimeUnit.SECONDS)) {
                        throw new SQLException("CI synchronization timed out");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("CI synchronization interrupted", interrupted);
                }
                // The gate must fence this result, even if the callback returns.
                return true;
            }));

            assertTrue(updateStarted.await(15, TimeUnit.SECONDS));
            assertEquals(ORIGINAL, credential(fixture));
            assertEquals(0, auditCount(fixture));

            gate.pauseAndDrain(1);
            assertThrows(SecurityException.class, () -> gate.withConnection(c -> true));
            releaseCallback.countDown();
            ExecutionException rejected = assertThrows(ExecutionException.class,
                    () -> pending.get(15, TimeUnit.SECONDS));
            assertTrue(rejected.getCause() instanceof SecurityException);

            assertEquals(ORIGINAL, credential(fixture));
            assertEquals(0, auditCount(fixture));

            changeRolePassword(fixture, REPLACEMENT);
            assertEquals("28P01", assertThrows(SQLException.class,
                    () -> new JdbcDatabase(fixture.schemaUrl,
                            fixture.writerRole, INITIAL).openConnection()).getSQLState());

            attestedGeneration.set(2);
            secret.set(REPLACEMENT);
            sourceAvailable.set(false);
            assertThrows(SecurityException.class, () -> gate.activate(2));
            assertThrows(SecurityException.class, () -> gate.withConnection(c -> true));
            sourceAvailable.set(true);
            gate.activate(2);
            assertEquals(Boolean.TRUE, gate.withConnection(connection -> {
                try (Statement statement = connection.createStatement()) {
                    assertEquals(1, statement.executeUpdate(
                            "UPDATE credential_state SET encoded_credential = 'committed' "
                            + "WHERE id = 1"));
                }
                return true;
            }));
            assertEquals("committed", credential(fixture));
            assertEquals(1, auditCount(fixture));
        } finally {
            releaseCallback.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @Test
    void passwordChangeDoesNotRevokeExistingSessionAndTerminationRollsBack()
            throws Exception {
        Fixture fixture = fixture();
        JdbcDatabase oldLogin = new JdbcDatabase(
                fixture.schemaUrl, fixture.writerRole, INITIAL);
        try (Connection existing = oldLogin.openConnection()) {
            existing.setAutoCommit(false);
            int pid;
            try (Statement statement = existing.createStatement()) {
                try (ResultSet result = statement.executeQuery("SELECT pg_backend_pid()")) {
                    assertTrue(result.next());
                    pid = result.getInt(1);
                }
                assertEquals(1, statement.executeUpdate(
                        "UPDATE credential_state SET encoded_credential = 'never-committed' "
                        + "WHERE id = 1"));
            }

            changeRolePassword(fixture, REPLACEMENT);
            assertEquals("28P01", assertThrows(SQLException.class,
                    oldLogin::openConnection).getSQLState());

            // Changing authentication data does NOT terminate an existing
            // authenticated backend or automatically roll back its transaction.
            try (Statement statement = existing.createStatement();
                    ResultSet stillConnected = statement.executeQuery("SELECT 1")) {
                assertTrue(stillConnected.next());
                assertEquals(1, stillConnected.getInt(1));
            }
            assertEquals(ORIGINAL, credential(fixture));
            assertEquals(0, auditCount(fixture));

            // This PID was obtained from the test-owned writer connection.
            // Confirm its SQL identity independently before terminating it.
            try (Connection owner = fixture.owner.openConnection();
                    PreparedStatement identity = owner.prepareStatement(
                            "SELECT usename FROM pg_stat_activity WHERE pid = ?")) {
                identity.setInt(1, pid);
                try (ResultSet result = identity.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(fixture.writerRole, result.getString(1));
                    assertFalse(result.next());
                }
                try (PreparedStatement terminate = owner.prepareStatement(
                        "SELECT pg_terminate_backend(?, 5000)")) {
                    terminate.setInt(1, pid);
                    try (ResultSet result = terminate.executeQuery()) {
                        assertTrue(result.next());
                        assertTrue(result.getBoolean(1),
                                "test-owned PostgreSQL session must be terminated");
                    }
                }
            }

            assertThrows(SQLException.class, existing::commit);
            assertEquals(ORIGINAL, credential(fixture));
            assertEquals(0, auditCount(fixture));
            try (Connection fresh = new JdbcDatabase(fixture.schemaUrl,
                    fixture.writerRole, REPLACEMENT).openConnection();
                    Statement statement = fresh.createStatement();
                    ResultSet identity = statement.executeQuery("SELECT session_user")) {
                assertTrue(identity.next());
                assertEquals(fixture.writerRole, identity.getString(1));
            }
        }
    }

    private static Fixture fixture() throws SQLException {
        String baseUrl = System.getenv("DEALERSHIP_PG_TEST_URL");
        String ownerName = System.getenv("DEALERSHIP_PG_TEST_USER");
        String ownerPassword = System.getenv("DEALERSHIP_PG_TEST_PASSWORD");
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("CI"))
                        && "jdbc:postgresql://localhost:5432/dealership_ci".equals(baseUrl)
                        && "dealership_ci_owner".equals(ownerName)
                        && "ci-only-postgres-fixture".equals(ownerPassword),
                "Session termination tests require exact disposable CI PostgreSQL fixture");

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String schema = "drain_" + suffix;
        String writer = "drain_writer_" + suffix;
        JdbcDatabase owner = new JdbcDatabase(baseUrl, ownerName, ownerPassword);
        try (Connection connection = owner.openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("CREATE ROLE " + writer + " LOGIN PASSWORD '" + INITIAL + "'");
            statement.execute("CREATE TABLE " + schema + ".credential_state ("
                    + "id INTEGER PRIMARY KEY, encoded_credential TEXT NOT NULL)");
            statement.execute("INSERT INTO " + schema + ".credential_state VALUES (1, '"
                    + ORIGINAL + "')");
            statement.execute("CREATE TABLE " + schema + ".rotation_audit ("
                    + "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, actor TEXT NOT NULL)");
            statement.execute("CREATE FUNCTION " + schema + ".audit_credential_change() "
                    + "RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER "
                    + "SET search_path = pg_catalog AS $audit$ BEGIN "
                    + "IF OLD.encoded_credential IS DISTINCT FROM NEW.encoded_credential THEN "
                    + "INSERT INTO " + schema + ".rotation_audit(actor) VALUES (session_user); "
                    + "END IF; RETURN NEW; END; $audit$");
            statement.execute("REVOKE ALL ON FUNCTION " + schema
                    + ".audit_credential_change() FROM PUBLIC");
            statement.execute("CREATE TRIGGER credential_audit AFTER UPDATE OF "
                    + "encoded_credential ON " + schema + ".credential_state "
                    + "FOR EACH ROW EXECUTE FUNCTION " + schema + ".audit_credential_change()");
            statement.execute("GRANT USAGE ON SCHEMA " + schema + " TO " + writer);
            statement.execute("GRANT SELECT ON " + schema + ".credential_state TO " + writer);
            statement.execute("GRANT UPDATE (encoded_credential) ON " + schema
                    + ".credential_state TO " + writer);
        }
        return new Fixture(owner, baseUrl + "?currentSchema=" + schema, writer, schema);
    }

    private static void changeRolePassword(Fixture fixture, String password)
            throws SQLException {
        try (Connection connection = fixture.owner.openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER ROLE " + fixture.writerRole + " PASSWORD '" + password + "'");
        }
    }

    private static String credential(Fixture fixture) throws SQLException {
        try (Connection connection = fixture.owner.openConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT encoded_credential FROM "
                        + fixture.schema + ".credential_state WHERE id = 1")) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private static int auditCount(Fixture fixture) throws SQLException {
        try (Connection connection = fixture.owner.openConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT count(*) FROM "
                        + fixture.schema + ".rotation_audit")) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static final class Fixture {
        private final JdbcDatabase owner;
        private final String schemaUrl;
        private final String writerRole;
        private final String schema;

        private Fixture(JdbcDatabase owner, String schemaUrl, String writerRole,
                String schema) {
            this.owner = owner;
            this.schemaUrl = schemaUrl;
            this.writerRole = writerRole;
            this.schema = schema;
        }
    }
}
