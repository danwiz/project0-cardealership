package com.revature.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.revature.service.CredentialAuditEvent;
import com.revature.service.CredentialAuditForwarder;

/** Read-only polling adapter for the CI-proven credential_rotation_audit schema. */
public final class JdbcCredentialAuditSource implements CredentialAuditForwarder.AuditSource {
    private final JdbcDatabase auditorDatabase;
    private final String streamId;

    public JdbcCredentialAuditSource(JdbcDatabase auditorDatabase, String streamId) {
        this.auditorDatabase = Objects.requireNonNull(auditorDatabase, "auditorDatabase");
        if (streamId == null || streamId.trim().isEmpty()) {
            throw new IllegalArgumentException("audit stream identity required");
        }
        this.streamId = streamId;
    }

    @Override
    public List<CredentialAuditEvent> readAfter(long lastAcknowledgedId, int limit) {
        if (lastAcknowledgedId < 0 || limit <= 0 || limit > 1000) {
            throw new IllegalArgumentException("invalid audit cursor or limit");
        }
        List<CredentialAuditEvent> events = new ArrayList<>();
        try (Connection connection = auditorDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT audit_id, account_username, database_actor, changed_at "
                        + "FROM credential_rotation_audit WHERE audit_id > ? "
                        + "ORDER BY audit_id LIMIT ?")) {
            statement.setLong(1, lastAcknowledgedId);
            statement.setInt(2, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    OffsetDateTime timestamp = result.getObject(4, OffsetDateTime.class);
                    events.add(new CredentialAuditEvent(streamId, result.getLong(1),
                            result.getString(2), result.getString(3),
                            Objects.requireNonNull(timestamp, "audit timestamp").toInstant()));
                }
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("audit source query failed", failure);
        }
        return events;
    }
}
