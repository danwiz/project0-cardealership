# JDBC signed-receipt pin floor with a monotonic witness

**Status: provider-neutral, disposable H2 CI implementation only.** No external
resource, cloud account, production role, deployed SQL migration or paid
entitlement is created.

`JdbcSignedReceiptPinRevisionFloor` implements
`SignedReceiptPinIndex.TrustedRevisionFloor`. It binds every operation to the
canonical proof-derived receipt key for the configured namespace and evidence
role. A separately provisioned and independently administered row is required
for each permitted key; the adapter never creates, inserts, resets, deletes,
or seeds a missing floor. An example *operator-owned staging* schema is:

```sql
CREATE TABLE signed_receipt_pin_floors (
    canonical_key VARCHAR(256) PRIMARY KEY,
    minimum_revision BIGINT NOT NULL CHECK (minimum_revision > 0)
);
```

The actual production schema, owner, RLS/grants, backups and operational
retention require independent design and approval. A read-only principal
requires only `SELECT`. The separately authorized pin publisher requires
restricted `UPDATE` of the minimum revision, and must **first authenticate a
new signed pin record**. No runtime process should hold a table-owner, INSERT,
DELETE or DDL-capable principal.

The [authenticated signed-pin publisher](authenticated-signed-pin-publisher.md)
provides a two-signature authorization gate before this adapter's publisher
method. The JDBC `advance` method itself has no identity verification; it
must only be exposed as a privileged capability within that trusted service.

## Why SQL alone is insufficient

An atomic SQL compare-and-swap (CAS) prevents ordinary concurrent clients
from lowering a row. It **cannot** stop an authorized database owner or a
backup restore from replacing the whole database with an older snapshot.
Therefore a production implementation must inject a separate
`MonotonicWitness` that provides:

- `minimumRevision(key)`: authenticated, strongly consistent, durable
  monotonic minimum for the *same* exact canonical key.
- `reserveAtLeast(key, revision)`: atomic durable max-reservation, returning
  the effective lower bound. It must never move backward, report success
  before durable acknowledgment or silently recreate an unknown key.

On every read, the adapter loads the SQL value and witness value, and **denies
any missing, nonpositive, malformed or SQL-below-witness result**. It never
uses the highest value as an optimistic substitute for an inconsistent store.
The witness can be provider-neutral and separately implemented, but a CI
`AtomicLong` is **not** independent persistent storage.

## Publisher-only advance protocol

1. Derive and validate the canonical proof/key and require `next > expected > 0`.
2. Confirm the current SQL row still equals the expected revision. A stale
   caller returns false without attempting to raise the witness.
3. Reserve `next` durably with the independent monotonic witness **before**
   modifying SQL. If the witness reports any other effective revision, deny.
4. Execute `UPDATE ... SET minimum_revision = next WHERE canonical_key = key
   AND minimum_revision = expected AND minimum_revision < next` inside an
   explicit SQL transaction. Require exactly one affected row and a committed
   result. CAS conflict returns false; outage/unknown commit fails closed.

The ordering deliberately favors *security over availability*: a crash or
failed SQL commit after a successful witness reservation leaves SQL below
the witness. Readers **stop**, and a separately authorized repair must
advance SQL from its observed value to the witnessed bound. A racing higher
reservation may similarly require catch-up. Do not lower the witness to
restore availability. If SQL has advanced without a durable witness reservation,
the implementation is misconfigured and must not be released.

The adapter assumes both DB and witness are trustworthy, strongly consistent
on their respective operations; it cannot create an atomic read/commit across
two independent systems. Witness issuer policy, pinned identity, key rotation,
equivocation control, transaction isolation, region failover, latency budgets
and recovery need an architecture review and fault-injection evidence before
production deployment.

## CI tests and release implications

Disposable H2 plus a simulated monotonic in-memory witness test restart
persistence, stale/replayed CAS, concurrent publishers, out-of-band SQL
rollback detection, witness outage, missing row, failed/uncertain SQL commit,
conservative catch-up, and rejection of a formerly valid signed document after
a durable floor advance. No production or shared database is touched.

This is an incremental implementation aid, **not proof of real WORM,
independent custody, external witness durability, source coverage or staging
release-gate acceptance**. Security issue #29 remains open; G03–G09,
ARCH-01/02, CKPT-01 and SECRET-01 remain **NOT RUN / NO-GO**.
