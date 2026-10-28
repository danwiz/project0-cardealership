# Credential audit forwarding and alert contract — non-deployed prototype

This staged component reads the **CI-only** `credential_rotation_audit` table using the existing SELECT-only auditor SQL identity. It does **not** install a production table, start a background job, implement a network connector or create a real immutable archive.

## Data and delivery flow

1. `JdbcCredentialAuditSource` reads a bounded, ascending batch of `audit_id`, `account_username`, `database_actor` and `changed_at` using parameterized SQL. It neither reads password hashes nor has a write capability. The audit stream ID is a trusted configuration value identifying a particular database/source generation.
2. `CredentialAuditForwarder` starts after an externally maintained last-acknowledged ID. It validates source stream/order and delivers each event to an injected `AppendOnlySink` using a stable key (`streamId:auditId`). PostgreSQL sequence gaps are allowed because rolled-back transactions can consume sequence values.
3. A checkpoint is advanced **only after** the sink reports a durable append/idempotent acknowledgement. If append fails, the event is retried. If append succeeded but checkpoint persistence fails, the same event can be retried, so the real sink **must deduplicate** by stable event key without changing an existing record.
4. Read, validation, append and checkpoint errors call a structured `AlertSink` with phase, stream ID and audit ID; the alert API intentionally receives **no plaintext credential, encoded hash, account name or raw exception message**. An alert failure is attached to the original exception and never permits checkpoint advance.
5. The PostgreSQL integration test passes a fake external sink/checkpoint and proves the restricted auditor can read the committed service and direct-SQL credential mutations. Unit tests cover ordered forwarding, natural ID gaps, append failure/retry, checkpoint failure/replay and alert failure isolation.

## Critical limits / review gates

- An interface named `AppendOnlySink` does **not** make storage immutable. Production must provide an independently controlled append-only/WORM destination with encryption, retention/lock, deduplication, access controls and verified acknowledgement. Likewise `CheckpointStore` and `AlertSink` are unimplemented contracts; the tests use in-memory fakes.
- One JVM's `synchronized` method is **not** a distributed lease. Multiple workers require leader fencing/atomic checkpoint compare-and-swap, and operators need backlog/lag SLOs, dead-letter handling, retries with bounds, alert delivery monitoring and outage recovery.
- The source database owner/superuser can still tamper with audit rows before forwarding. External archiving reduces post-forward tampering but does not prove that missing events were never committed; an independently authenticated hash-chain/attestation and source coverage/threat review remain separate design tasks.
- A source ID must not be recycled across reset/restore of audit IDs. Audit checkpoints and sink credentials must be held outside the ordinary dealership and credential-writer processes. Account identifiers are sensitive metadata; minimize access and retention accordingly.
- There is **no production migration, timer, real secret, cloud entitlement, write to an external destination, live alert or deployment** in this PR. Independent security review and alert/retention destination selection are required before integration. Keep issue #29 and all security PRs draft/open.
