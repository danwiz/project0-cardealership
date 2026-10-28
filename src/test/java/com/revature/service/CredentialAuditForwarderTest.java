package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class CredentialAuditForwarderTest {
    private static final String STREAM = "fixture-stream";

    @Test
    void forwardsOnlyNewEventsAndAllowsNormalSequenceGaps() {
        Harness fixture = new Harness(List.of(event(1), event(3)));
        assertEquals(2, fixture.forwarder.forwardNextBatch());
        assertEquals(3, fixture.ack.get());
        assertEquals(List.of(STREAM + ":1", STREAM + ":3"),
                new ArrayList<>(fixture.archive.keySet()));
        assertEquals(0, fixture.forwarder.forwardNextBatch());
        assertTrue(fixture.alerts.isEmpty());
    }

    @Test
    void failedAppendAlertsAndRetriesWithoutSkippingTheEvent() {
        Harness fixture = new Harness(List.of(event(1), event(2)));
        fixture.failAppendId = 2;
        assertThrows(IllegalStateException.class, fixture.forwarder::forwardNextBatch);
        assertEquals(1, fixture.ack.get());
        assertEquals(1, fixture.archive.size());
        assertEquals(List.of("append:2"), fixture.alerts);

        assertEquals(1, fixture.forwarder.forwardNextBatch());
        assertEquals(2, fixture.ack.get());
        assertEquals(2, fixture.archive.size());
    }

    @Test
    void checkpointFailureRetriesTheSameIdempotentAppend() {
        Harness fixture = new Harness(List.of(event(1)));
        fixture.failCheckpointId = 1;
        assertThrows(IllegalStateException.class, fixture.forwarder::forwardNextBatch);
        assertEquals(0, fixture.ack.get());
        assertEquals(1, fixture.archive.size());
        assertEquals(List.of("checkpoint-write:1"), fixture.alerts);

        assertEquals(1, fixture.forwarder.forwardNextBatch());
        assertEquals(1, fixture.ack.get());
        assertEquals(1, fixture.archive.size());
        assertEquals(2, fixture.appendAttempts);
    }

    @Test
    void wrongStreamOrReorderedBatchFailsBeforeAnyDelivery() {
        Harness wrongStream = new Harness(List.of(
                new CredentialAuditEvent("other-stream", 1, 1,
                        "alice", "writer", Instant.EPOCH)));
        assertThrows(SecurityException.class, wrongStream.forwarder::forwardNextBatch);
        assertTrue(wrongStream.archive.isEmpty());
        assertEquals(List.of("source-read-or-order:0"), wrongStream.alerts);

        Harness reversed = new Harness(List.of(event(2), event(1)));
        assertThrows(SecurityException.class, reversed.forwarder::forwardNextBatch);
        assertTrue(reversed.archive.isEmpty());
        assertEquals(0, reversed.ack.get());
    }

    @Test
    void commitOrderCursorCanAdvanceWhenDatabaseAuditIdsArriveOutOfOrder() {
        // Commit-order CDC may emit audit ID 11 before late-committing ID 10.
        Harness fixture = new Harness(List.of(
                new CredentialAuditEvent(STREAM, 1, 11, "alice", "writer", Instant.EPOCH),
                new CredentialAuditEvent(STREAM, 2, 10, "bob", "writer", Instant.EPOCH)));
        assertEquals(2, fixture.forwarder.forwardNextBatch());
        assertEquals(2, fixture.ack.get());
        assertEquals(List.of(STREAM + ":11", STREAM + ":10"),
                new ArrayList<>(fixture.archive.keySet()));
        assertEquals(0, fixture.forwarder.forwardNextBatch());
    }

    @Test
    void alertTransportFailureCannotMaskOriginalDeliveryFailure() {
        Harness fixture = new Harness(List.of(event(1)));
        fixture.failAppendId = 1;
        fixture.failAlerts = true;
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.forwarder::forwardNextBatch);
        assertEquals("fixture append unavailable", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(0, fixture.ack.get());
    }

    private static CredentialAuditEvent event(long id) {
        return new CredentialAuditEvent(STREAM, id, id, "alice", "writer", Instant.EPOCH);
    }

    private static final class Harness {
        private final AtomicLong ack = new AtomicLong();
        private final Map<String, CredentialAuditEvent> archive = new LinkedHashMap<>();
        private final List<String> alerts = new ArrayList<>();
        private final CredentialAuditForwarder forwarder;
        private long failAppendId;
        private long failCheckpointId;
        private boolean failAlerts;
        private int appendAttempts;

        private Harness(List<CredentialAuditEvent> records) {
            CredentialAuditForwarder.AuditSource source = (after, limit) -> records.stream()
                    .filter(event -> event.sourceCursor() > after)
                    .limit(limit)
                    .collect(Collectors.toList());
            CredentialAuditForwarder.AppendOnlySink sink = event -> {
                appendAttempts++;
                if (failAppendId == event.auditId()) {
                    failAppendId = 0;
                    throw new IllegalStateException("fixture append unavailable");
                }
                archive.putIfAbsent(event.eventKey(), event);
            };
            CredentialAuditForwarder.CheckpointStore checkpoint =
                    new CredentialAuditForwarder.CheckpointStore() {
                @Override public long lastAcknowledgedCursor() { return ack.get(); }
                @Override public void markAcknowledged(long sourceCursor) {
                    if (failCheckpointId == sourceCursor) {
                        failCheckpointId = 0;
                        throw new IllegalStateException("fixture checkpoint unavailable");
                    }
                    if (sourceCursor <= ack.get()) throw new SecurityException("checkpoint rollback");
                    ack.set(sourceCursor);
                }
            };
            CredentialAuditForwarder.AlertSink notifications = (phase, stream, id) -> {
                if (failAlerts) throw new IllegalStateException("fixture alert unavailable");
                alerts.add(phase + ":" + id);
            };
            forwarder = new CredentialAuditForwarder(STREAM, source, sink,
                    checkpoint, notifications, 10);
        }
    }
}
