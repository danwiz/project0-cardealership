# Credential-writer commit/fence reconciliation — two-gate PostgreSQL CI rehearsal

**Status: DISPOSABLE CI ONLY / NOT AN INDEPENDENT ARCHIVE / NO-GO.**
This exercise is an addition to the [PostgreSQL writer-session revocation rehearsal](postgresql-writer-session-revocation-rehearsal.md).
It uses the existing GitHub Actions PostgreSQL 16 service, synthetic account
data, random test-only roles and schemas, and two separately instantiated
`FencedJdbcConnectionGate` objects. It never contacts a production database,
real secret store, production pool or external audit destination.

## Why a fence is not a transaction rollback

An application request may commit successfully in PostgreSQL **before** an
operator starts the local connection drain. The callback can then be fenced
and report an error, but the committed update and its trigger-generated audit
row remain durable in the database. Conversely, an open uncommitted update
can roll back when the gate closes its connection. **Application response
status is not authoritative evidence of commit or rollback.**

`PostgresqlWriterSessionDrainIntegrationTest` exercises these two cases in
one deterministic, latch-controlled test:

| Simulated writer replica | Before the drain | Expected result after drain |
| --- | --- | --- |
| Gate A | Updates a synthetic credential, explicitly commits, then blocks before returning from the callback | Caller is fenced, but the committed new value and exactly one audit row remain |
| Gate B | Updates the same credential in a new uncommitted transaction, then blocks before returning | Caller is fenced, its transaction is rolled back, and no second audit row appears |

The test also queries the audit table as a **separate SELECT-only PostgreSQL
principal**. That principal sees the committed event's synthetic subject ID,
database actor and timestamp, and is denied access to the credential-state
table and attempts to delete the audit ledger. This isolates the audit reader
from the writer and helps prevent accidental reliance on client response status.

## Evidence and limitations

- The two gates are in one JVM. They represent independent *local* replicas,
  not two deployed processes, cluster-wide fencing or a shared generation
  checkpoint.
- The audit trigger is owner-held and transactionally coupled to the mutation.
  The audit read is **database-local**; it is not an independently administered
  WORM archive, a CDC stream or an external immutable receipt.
- One audit row for one committed mutation is a fixture invariant. It does not
  prove that all changes were captured across a production transaction stream.
- Audit sequence IDs are **not commit-order cursors**. Do not use the simple
  diagnostic `JdbcCredentialAuditSource` polling adapter as a lossless
  production CDC adapter when transactions overlap.
- A real rotation must reconcile committed database audit rows against
  commit-ordered CDC, independently durable archive receipts and a durable
  checkpoint, including requests that returned errors after a commit.
- Concurrent `Connection.close()` is driver-specific. The test does not prove
  PostgreSQL backend termination, quiescence across replicas, or recovery after
  an interrupted network commit; those require independently witnessed tests.

The test runs under the exact disposable `CI=true` PostgreSQL fixture guard
documented in the [revocation rehearsal](postgresql-writer-session-revocation-rehearsal.md).
Java 11/17 push and pull-request checks are automated, but their success must
not be recorded as production G03/G04/G05/G06/G07 acceptance.

For independent staging, capture commit identifiers or authoritative WAL
positions, server-side session/transaction inventory, trigger ledger entries,
CDC acknowledgement, immutable archive receipts, checkpoint position and a
reviewer-signed reconciliation table. Include both a commit-before-fence and
an unknown-commit-result network-failure case. A discrepancy **fails closed**
until an independent reviewer reconciles the source and archive.

The next [reconciled writer activation barrier](reconciled-writer-reopen-barrier.md)
requires independently verified, matching source and external archive
checkpoint evidence before any newer credential generation can activate;
its current verifier is a fixture seam, not an external service.

Official references: PostgreSQL 16 [transaction isolation](https://www.postgresql.org/docs/16/transaction-iso.html)
and [trigger behavior](https://www.postgresql.org/docs/16/trigger-definition.html).

Security issue #29 remains open. This and its upstream PRs remain draft and
unmerged. No deployment, production data migration, paid GitHub service or
external provider integration is authorized by the rehearsal.
