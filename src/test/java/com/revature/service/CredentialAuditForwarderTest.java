package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class CredentialAuditForwarderTest {
    private static final String STREAM = "fixture-stream";

    @Test
    void forwardsOnlyNewEventsAndAllowsNormalSequenceGaps() {
        Harness fixture = new Harness(List.of(event(1), event(3)));
        assertEquals(2, fixture.forwarder.forwardNextBatch());
        assertEquals(3, fixture.ack.get().sourceCursor());
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
        assertEquals(1, fixture.ack.get().sourceCursor());
        assertEquals(1, fixture.archive.size());
        assertEquals(List.of("append:2:2"), fixture.alerts);

        assertEquals(1, fixture.forwarder.forwardNextBatch());
        assertEquals(2, fixture.ack.get().sourceCursor());
        assertEquals(2, fixture.archive.size());
    }

    @Test
    void checkpointFailureRetriesTheSameIdempotentAppend() {
        Harness fixture = new Harness(List.of(event(1)));
        fixture.failCheckpointId = 1;
        assertThrows(IllegalStateException.class, fixture.forwarder::forwardNextBatch);
        assertEquals(0, fixture.ack.get().sourceCursor());
        assertEquals(1, fixture.archive.size());
        assertEquals(List.of("checkpoint-write:1:1"), fixture.alerts);

        fixture.restart(); // Simulated process crash after durable append, before checkpoint.
        assertEquals(1, fixture.forwarder.forwardNextBatch());
        assertEquals(1, fixture.ack.get().sourceCursor());
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
        assertEquals(List.of("source-read-or-order:0:0"), wrongStream.alerts);

        Harness reversed = new Harness(List.of(event(2), event(1)));
        assertThrows(SecurityException.class, reversed.forwarder::forwardNextBatch);
        assertTrue(reversed.archive.isEmpty());
        assertEquals(0, reversed.ack.get().sourceCursor());
    }

    @Test
    void commitOrderCursorCanAdvanceWhenDatabaseAuditIdsArriveOutOfOrder() {
        // Commit-order CDC may emit audit ID 11 before late-committing ID 10.
        Harness fixture = new Harness(List.of(
                new CredentialAuditEvent(STREAM, 1, 11, "alice", "writer", Instant.EPOCH),
                new CredentialAuditEvent(STREAM, 2, 10, "bob", "writer", Instant.EPOCH)));
        assertEquals(2, fixture.forwarder.forwardNextBatch());
        assertEquals(2, fixture.ack.get().sourceCursor());
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
        assertEquals(0, fixture.ack.get().sourceCursor());
    }

    @Test
    void checkpointCannotBeReusedForAnotherSourceGeneration() {
        Harness fixture = new Harness(List.of(event(1)));
        fixture.ack.set(new AuditCheckpoint("restored-different-stream", 0));
        assertThrows(SecurityException.class, fixture.forwarder::forwardNextBatch);
        assertEquals(List.of("checkpoint-read:0:0"), fixture.alerts);
        assertTrue(fixture.archive.isEmpty());
    }

    @Test
    void staleCheckpointCompareAndSetFailsClosedAndReplaysIdempotently() {
        Harness fixture = new Harness(List.of(event(1)));
        fixture.rejectNextCheckpoint = true;
        assertThrows(SecurityException.class, fixture.forwarder::forwardNextBatch);
        assertEquals(0, fixture.ack.get().sourceCursor());
        assertEquals(1, fixture.archive.size());
        assertEquals(List.of("checkpoint-write:1:1"), fixture.alerts);

        fixture.restart();
        assertEquals(1, fixture.forwarder.forwardNextBatch());
        assertEquals(1, fixture.ack.get().sourceCursor());
        assertEquals(1, fixture.archive.size());
        assertEquals(2, fixture.appendAttempts);
    }

    @Test
    void lostReplayHistoryFailsClosedInsteadOfReportingEmptyBatch() {
        Harness fixture = new Harness(List.of(event(1)));
        fixture.historyUnavailable = true;
        assertThrows(CredentialAuditForwarder.AuditHistoryUnavailableException.class,
                fixture.forwarder::forwardNextBatch);
        assertEquals(List.of("source-history-unavailable:0:0"), fixture.alerts);
        assertEquals(0, fixture.ack.get().sourceCursor());
        assertTrue(fixture.archive.isEmpty());

        fixture.historyUnavailable = false;
        assertEquals(1, fixture.forwarder.forwardNextBatch());
        assertEquals(1, fixture.ack.get().sourceCursor());
    }

    @Test
    void transientSourceOutageRetainsCheckpointUntilRecovery() {
        Harness fixture = new Harness(List.of(event(1), event(2)));
        fixture.sourceUnavailable = true;
        assertThrows(IllegalStateException.class, fixture.forwarder::forwardNextBatch);
        assertEquals(List.of("source-read-or-order:0:0"), fixture.alerts);
        assertEquals(0, fixture.ack.get().sourceCursor());
        fixture.sourceUnavailable = false;
        assertEquals(2, fixture.forwarder.forwardNextBatch());
        assertEquals(2, fixture.ack.get().sourceCursor());
    }

    private static CredentialAuditEvent event(long id) {
        return new CredentialAuditEvent(STREAM, id, id, "alice", "writer", Instant.EPOCH);
    }

    private static final class Harness {
        private final AtomicReference<AuditCheckpoint> ack =
                new AtomicReference<>(new AuditCheckpoint(STREAM, 0));
        private final Map<String, CredentialAuditEvent> archive = new LinkedHashMap<>();
        private final List<String> alerts = new ArrayList<>();
        private CredentialAuditForwarder forwarder;
        private final CredentialAuditForwarder.AuditSource source;
        private final CredentialAuditForwarder.AppendOnlySink sink;
        private final CredentialAuditForwarder.CheckpointStore checkpoint;
        private final CredentialAuditForwarder.AlertSink notifications;
        private long failAppendId;
        private long failCheckpointId;
        private boolean failAlerts;
        private boolean rejectNextCheckpoint;
        private boolean historyUnavailable;
        private boolean sourceUnavailable;
        private int appendAttempts;

        private Harness(List<CredentialAuditEvent> records) {
            source = (after, limit) -> {
                if (historyUnavailable) {
                    throw new CredentialAuditForwarder.AuditHistoryUnavailableException();
                }
                if (sourceUnavailable) throw new IllegalStateException("fixture source offline");
                return records.stream()
                    .filter(event -> event.sourceCursor() > after)
                    .limit(limit)
                    .collect(Collectors.toList());
            };
            sink = event -> {
                appendAttempts++;
                if (failAppendId == event.auditId()) {
                    failAppendId = 0;
                    throw new IllegalStateException("fixture append unavailable");
                }
                archive.putIfAbsent(event.eventKey(), event);
            };
            checkpoint =
                    new CredentialAuditForwarder.CheckpointStore() {
                @Override public AuditCheckpoint load() { return ack.get(); }
                @Override public boolean advanceIfCurrent(
                        AuditCheckpoint expected, long sourceCursor) {
                    if (failCheckpointId == sourceCursor) {
                        failCheckpointId = 0;
                        throw new IllegalStateException("fixture checkpoint unavailable");
                    }
                    if (rejectNextCheckpoint) {
                        rejectNextCheckpoint = false;
                        return false;
                    }
                    if (sourceCursor <= expected.sourceCursor()) {
                        throw new SecurityException("checkpoint rollback");
                    }
                    return ack.compareAndSet(expected,
                            new AuditCheckpoint(expected.streamId(), sourceCursor));
                }
            };
            notifications = (phase, stream, cursor, id) -> {
                if (failAlerts) throw new IllegalStateException("fixture alert unavailable");
                alerts.add(phase + ":" + cursor + ":" + id);
            };
            restart();
        }

        private void restart() {
            forwarder = new CredentialAuditForwarder(STREAM, source, sink,
                    checkpoint, notifications, 10);
        }
    }
}
