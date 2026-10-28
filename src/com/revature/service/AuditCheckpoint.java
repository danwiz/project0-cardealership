package com.revature.service;

import java.util.Objects;

/**
 * Externally persisted identity and last durably acknowledged source cursor.
 * A stream identity must change when the source is restored or reinitialized.
 */
public final class AuditCheckpoint {
    private final String streamId;
    private final long sourceCursor;

    public AuditCheckpoint(String streamId, long sourceCursor) {
        if (streamId == null || streamId.trim().isEmpty()) {
            throw new IllegalArgumentException("checkpoint stream identity required");
        }
        if (sourceCursor < 0) {
            throw new IllegalArgumentException("checkpoint cursor must be nonnegative");
        }
        this.streamId = streamId;
        this.sourceCursor = sourceCursor;
    }

    public String streamId() { return streamId; }
    public long sourceCursor() { return sourceCursor; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AuditCheckpoint)) return false;
        AuditCheckpoint checkpoint = (AuditCheckpoint) other;
        return sourceCursor == checkpoint.sourceCursor
                && streamId.equals(checkpoint.streamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamId, sourceCursor);
    }
}
