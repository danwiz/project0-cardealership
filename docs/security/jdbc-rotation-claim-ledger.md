# Durable rotation claim — SQL-backed provider-neutral rehearsal

**Status: CI-ONLY / NOT A PRODUCTION FENCE OR RECONCILIATION AUTHORITY.**

`JdbcRotationClaimLedger` supplies a JDBC transaction with `SELECT ... FOR
UPDATE` on an independently provisioned `writer_rotation_floors` row. A
competing worker can advance an expected generation and rotation epoch only
once; after the winning commit, all losing workers see the updated floor and
are rejected. A newly constructed adapter reads that floor from SQL instead
of resetting it to zero. An absent stream row, denied database permission,
SQL outage or contradictory proof fails closed.

The table belongs to the **separate rotation/reconciliation authority**. The
ordinary application and credential writer roles must not read, update,
create or reset it. No runtime DDL or seed-from-zero fallback is provided.
The exact schema exercised by the isolated fixtures is:

```sql
CREATE TABLE writer_rotation_floors (
    stream_id VARCHAR(100) PRIMARY KEY,
    credential_generation BIGINT NOT NULL,
    rotation_epoch BIGINT NOT NULL,
    cursor_position BIGINT NOT NULL,
    cursor_ordinal INTEGER NOT NULL
);
```

An independent migration, privilege review, high-availability and recovery
design must approve the actual schema before deployment. The cursor's commit
position is stored as its signed 64-bit bit pattern and compared in Java
using the source adapter's unsigned ordering; SQL ordering over this column
cannot substitute for source cursor semantics.

## Integration boundary

1. The independent authority proves source quiescence/completeness and **external
   immutable archive receipts** for the precise stream/checkpoint and epoch.
2. Only after verification, it calls `claim(proof)` atomically through its
   exclusively authorized SQL principal. A failed or duplicate claim is a
   denial, not a cue to bump the epoch and retry silently.
3. `ReconciledJdbcWriterGate` consumes the authority result and separately
   attests the new credential generation and checks that old local sessions
   drained. Its next process instance must receive the validated `load()`
   recovery floor (plus trusted archive/source verification).

The fixture uses `ledger::claim` directly as `ReconciliationAuthority` **only
to exercise a race**. This intentionally does not prove item 1 and must not be
copied into a deployed composition root. A claim committed before the local
credential attestor rejects can consume an epoch without reopening; fail
closed and recover by a new independently reviewed rotation ceremony.

**Crucial limitation:** a SQL claim on its own cannot stop a previously active
replica from committing a database transaction. A real deployment needs a
trusted lease/fence whose loss is enforced at every write transaction, as
well as revocation of old database sessions and reconciliation of commits
that happened before the fence. This PR does not implement that distributed
database enforcement, nor signed source generation identity or a WORM archive.

## Verification scope

- H2: persisted recovery floor, old-generation/replayed-epoch/rollback denial,
  two independent `ReconciledJdbcWriterGate` instances racing for one claim,
  missing row, inaccessible authority and sanitized errors.
- Disposable PostgreSQL 16: separate SQL connections contend for the same
  locked row; new client reads the winner; independent ordinary-app SQL role
  cannot read or claim the protected floor.
- CI is limited to Java 11 and 17, using a strictly identified disposable
  database. No staging or production secret, SQL table or cloud resource is
  touched.

G08, CKPT-01, SECRET-01 and production G03–G09 remain **NOT RUN / NO-GO**.
Require a chosen provider, separate admin domains, independent review and a
staging write-fence failure rehearsal before any release decision.
