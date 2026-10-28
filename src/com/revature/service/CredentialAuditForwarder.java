package com.revature.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral, single-instance audit forwarding seam (not a deployed job).
 * The real destination must durably append/idempotently acknowledge eventKey,
 * and the checkpoint must live outside the database being audited.
 * The sourceCursor is distinct from the database's audit ID. Production MUST
 * use a commit-ordered source cursor or a proven reconciliation scan: PostgreSQL
 * sequence IDs alone can skip late-committing rows.
 */
public final class CredentialAuditForwarder {
    private final String streamId;
    private final AuditSource source;
    private final AppendOnlySink sink;
    private final CheckpointStore checkpoint;
    private final AlertSink alerts;
    private final int batchLimit;

    public CredentialAuditForwarder(String streamId, AuditSource source,
            AppendOnlySink sink, CheckpointStore checkpoint, AlertSink alerts,
            int batchLimit) {
        if (streamId == null || streamId.trim().isEmpty()) {
            throw new IllegalArgumentException("audit stream identity required");
        }
        if (batchLimit <= 0 || batchLimit > 1000) {
            throw new IllegalArgumentException("audit batch limit must be 1..1000");
        }
        this.streamId = streamId;
        this.source = Objects.requireNonNull(source, "source");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.checkpoint = Objects.requireNonNull(checkpoint, "checkpoint");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.batchLimit = batchLimit;
    }

    /**
     * Preserve strict SOURCE-CURSOR order, allow arbitrary audit-ID order/gaps,
     * and never advance the checkpoint on a failed durable append.
     * Synchronization covers one JVM only; production needs an external lease.
     */
    public synchronized int forwardNextBatch() {
        long cursor;
        try {
            cursor = checkpoint.lastAcknowledgedCursor();
            if (cursor < 0) throw new SecurityException("invalid audit checkpoint");
        } catch (RuntimeException failure) {
            alert("checkpoint-read", 0, failure);
            throw failure;
        }

        List<CredentialAuditEvent> events;
        try {
            List<CredentialAuditEvent> fetched = source.readAfter(cursor, batchLimit);
            if (fetched == null || fetched.size() > batchLimit) {
                throw new SecurityException("invalid audit source batch");
            }
            events = new ArrayList<>(fetched);
            long previous = cursor;
            for (CredentialAuditEvent event : events) {
                if (event == null || !streamId.equals(event.streamId())
                        || event.sourceCursor() <= previous) {
                    throw new SecurityException("audit source stream/order violation");
                }
                previous = event.sourceCursor();
            }
        } catch (RuntimeException failure) {
            alert("source-read-or-order", cursor, failure);
            throw failure;
        }

        int delivered = 0;
        for (CredentialAuditEvent event : events) {
            try {
                // Must return only after durable idempotent acknowledgement.
                sink.appendIfAbsent(event);
            } catch (RuntimeException failure) {
                alert("append", event.auditId(), failure);
                throw failure;
            }
            try {
                checkpoint.markAcknowledged(event.sourceCursor());
            } catch (RuntimeException failure) {
                // If append succeeded, retries MUST be deduplicated by eventKey.
                alert("checkpoint-write", event.auditId(), failure);
                throw failure;
            }
            delivered++;
        }
        return delivered;
    }

    private void alert(String phase, long auditId, RuntimeException original) {
        try {
            // No password, hash, username or exception message crosses this API.
            alerts.forwardingFailed(phase, streamId, auditId);
        } catch (RuntimeException alertFailure) {
            original.addSuppressed(alertFailure);
        }
    }

    @FunctionalInterface
    public interface AuditSource {
        /** Must return events in stable commit-cursor order, not audit-ID order. */
        List<CredentialAuditEvent> readAfter(long lastAcknowledgedCursor, int limit);
    }

    @FunctionalInterface
    public interface AppendOnlySink {
        /** Must implement durable, append-only, idempotent acknowledgement. */
        void appendIfAbsent(CredentialAuditEvent event);
    }

    public interface CheckpointStore {
        long lastAcknowledgedCursor();
        void markAcknowledged(long sourceCursor);
    }

    @FunctionalInterface
    public interface AlertSink {
        /** Alert metadata only; real adapter must reliably deliver/retry alerts. */
        void forwardingFailed(String phase, String streamId, long auditId);
    }
}
