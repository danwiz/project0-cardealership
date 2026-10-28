# Audit archive, checkpoint and alert durability gates — provider-neutral draft

**Non-deployed test-backed contracts only.** This increment supplies no external
archive, WORM retention lock, cloud storage account, persistent checkpoint
service, alerting endpoint, secrets, production database changes or deployment.

## Independent persistence boundaries

| Boundary | Receipt contract | Production responsibility |
| --- | --- | --- |
| `VerifiedAuditAppendOnlySink` | Backend `ArchiveStore.appendIfAbsent` returns `AuditDurableReceipt(eventKey, SHA-256 canonical event digest)` matching the exact submitted immutable event | Atomically insert or compare full existing content; reject conflicting key without overwriting; acknowledge only after durable replicated/WORM append under independent access and retention controls |
| `VerifiedAuditCheckpointStore` | Backend CAS returns the exact persisted `AuditCheckpoint(streamId, nextCursor)`, or null for stale expected state | Commit cross-process compare-and-set outside audited database; enforce stream-generation identity, monotonic progress and fencing; never signal success before durable storage commit |
| `VerifiedAuditAlertSink` | Backend `AlertOutbox.store` returns a matching `AuditDurableReceipt(alertKey, alert digest)` | Persist metadata-only notice before acknowledgement; separately drain/retry/monitor outbox to a monitored external alert transport, with dead-letter handling and an independent outage alarm |

The follow-on [provider-neutral alert relay contract](credential-audit-alert-outbox-relay.md)
defines bounded claim leases, per-claim fencing tokens, matching downstream
receipts and durable conditional delivery acknowledgement. It exercises
at-least-once retry and stale-worker cases with in-memory fakes only; it does
not send a real alert or close the independent monitoring gate.

The forwarder continues to **append, verify acknowledgement, then checkpoint**.
An error or mismatched archive receipt halts without checkpoint progress.
An archive write that succeeded while the checkpoint write failed is retried
after restart; **identical event key and content** are idempotent. A repeated
event key with different immutable content is an **integrity violation**, even
if the key was first stored on a different page or in a previous process.

## Canonical digest specification (v1)

The archive digest uses SHA-256 over ordered, length-delimited, big-endian
fields: domain `credential-audit-archive/v1`, stream ID (UTF-8), unsigned
commit-position bit pattern (64-bit), event ordinal (32-bit), audit ID
(64-bit), username (UTF-8), database actor (UTF-8), event UTC epoch seconds
(64-bit) and nanoseconds (32-bit). Each string includes a 32-bit byte count.
No delimiter ambiguity or platform-default encoding is permitted. The digest
is stable across process restarts and Java versions.

The alert digest uses the distinct domain `credential-audit-alert/v1`, a new
UUID per observed failure, a fixed failure phase, stream ID, source cursor and
audit ID. **No username, exception text, password or encoded credential enters
the alert API.** Repeated failures get distinct notice IDs. The real outbox
must retain each notice until externally delivered and acknowledged.

**Important:** An unkeyed SHA-256 digest only checks that a receipt refers to
the submitted bytes. It is **not** a signature or tamper-evident attestation;
the backend could lie about durability, and a database owner could suppress
changes before capture. To establish authenticity and non-repudiation, choose
an independently controlled, authenticated, append-only destination, key
management / signed attestations where appropriate, source coverage verification
and an independent security review. The provider must verify immutable content
on replay, not blindly return a matching digest supplied by the caller.

## Failure/recovery acceptance tests

- Valid archive receipt is accepted; missing, wrong-key or wrong-digest receipt
  raises a security exception and cannot advance the checkpoint.
- Fake immutable backend accepts exact replay but rejects the same key with
  changed metadata without mutating its stored original.
- Two fake process instances share a checkpoint backend and prove stale CAS
  rejection. A backend acknowledging the **wrong checkpoint value** fails.
- Simulated checkpoint failure after append, one-event page, and process
  restart replays the same event, deduplicates and completes in order.
- Fake alert outbox persists strictly metadata-only notices. A mismatched
  alert receipt fails; the forwarder preserves the original error and attaches
  the alert failure as suppressed, so out-of-band outbox health monitoring is
  mandatory.

These are **in-memory fixture tests**. They do **not** prove power-loss
durability, real object immutability/retention, replication quorum, independent
administrative ownership, actual external delivery, encryption, time sync,
retention policy, alert SLAs or failover. Those must be validated with the
selected provider and separately reviewed before merging/deploying.

NIST's [Guide to Computer Security Log Management (SP 800-92)](https://csrc.nist.gov/pubs/sp/800/92/final)
provides broader log-management context; this draft does not assert compliance
with any framework.
