# Reconciled writer activation barrier — provider-neutral, non-deployed seam

**Status: STAGED CONTRACT / NO EXTERNAL RECONCILIATION PROVIDER.**
`ReconciledJdbcWriterGate` wraps and owns a
`FencedJdbcConnectionGate`; no caller can access the wrapped gate's
unconditional `activate(generation)` method. The wrapper requires two
**separately attested** preconditions to activate a writer generation:

1. A strictly newer credential generation, validated by the underlying
   trusted `GenerationAttestor`.
2. An independently authenticated reconciliation proof for this rotation
   epoch, asserting that an authoritative, complete source watermark was
   durably acknowledged in an external append-only archive/checkpoint.

This does not implement a secret manager, trustworthy archive, PostgreSQL
replication source, shared fencing lease or network control plane. A test
lambda returning `true` is **never** a production attestor.

The [disposable SQL rotation claim rehearsal](jdbc-rotation-claim-ledger.md)
demonstrates a cross-process, monotonic epoch claim and restart floor. A
fixture wiring `ledger::claim` directly into this wrapper is not independent
source/archive verification and does not fence old replicas' DB transactions.
The [two-signer verification seam](signed-rotation-reconciliation-evidence.md)
adds cryptographic binding of source and archive assertions before claiming,
but its signers remain synthetic and cannot establish real CDC/WORM coverage.

## Reopen proof fields and local validation

| Field | Local requirement | Provider's independent obligation |
| --- | --- | --- |
| `previousGeneration` | Equals the last accepted generation | Bind to durable generation floor from a separate trusted authority |
| `nextGeneration` | Strictly greater and positive | Authenticate current secret and actual database login/revocation state |
| `rotationEpoch` | Greater than the last accepted epoch | Persist a monotonic cross-replica rotation epoch, detect replay after restart |
| `sourceHighWater` | Same immutable stream ID as recovered checkpoint | Prove complete committed-source coverage and quiescence through this watermark |
| `archivedCheckpoint` | Exactly equals `sourceHighWater`, at or beyond the recovered archive cursor | Authenticate external, **durably acknowledged** append receipts and independent checkpoint CAS |

The constructor requires an initial archive checkpoint, generation floor and
rotation epoch. **A production constructor caller must load all three from
an independently secured and durable recovery authority.** Setting origin
and zero as a default after restart can allow replay and is not an approved
recovery strategy. A lost source generation, slot, stream ID, checkpoint or
recovery proof is a **NO-GO**, not permission to start a new stream silently.

The `ReconciliationAuthority.verify(proof)` callback must independently
authenticate the entire proof's source identity, rotation epoch, durable
archive acknowledgement, absence of skipped committed audit events, and
quiescent cut. The wrapper cannot infer those facts by merely comparing
caller-supplied numbers; an authority accepting arbitrary proofs voids the
control. Metadata is credential-free but is **not a cryptographic signature**.

## Failure behavior

- Starts blocked. Rejects null/mismatched/stale/rollback proof before calling
  the authority, and sanitizes authority errors without exposing its diagnostics.
- If the authority is unavailable, evidence is rejected or the secret
  generation cannot be attested, opening remains blocked.
- `pauseAndDrain(expectedGeneration)` fences new work and drains tracked
  connections; a failed close remains fail-closed inside the owned gate.
- An in-flight callback from the prior generation prevents reopening until
  it exits, even when a newer valid reconciliation proof has arrived.
- A successful activation does **not** continuously re-check archive health
  during every query. Production must separately trip the fence on archive,
  CDC, lease or checkpoint health loss, and enforce distributed fencing before
  the writer accepts further changes.
- A commit may already have succeeded even though the request was fenced.
  Reconciliation must include committed events from failed/unknown responses;
  use [the PostgreSQL commit/fence CI rehearsal](credential-writer-commit-fence-reconciliation.md)
  for the demonstrated race, not API response counts as authority.

Five isolated H2/JUnit tests cover initial deny, mismatched stream and lagging
archive, stale epochs/generation replay, authority outage/secret-generation
failure, and active-callback drain fencing. They do not establish durability,
real provider cryptographic identity, database/session termination, independent
archive WORM retention or real two-replica behavior.

Before independent staging acceptance, use the
[release evidence register](credential-security-release-evidence-register.md)
to record signed source/archival checkpoint receipts, stream-generation
identity, immutable archive read-back, two-process fence race, interrupted
commit reconciliation, provider outage, restart and stale proof rejection.
Keep G03–G09 and SECRET-01 **NOT RUN / NO-GO** without those proofs and
independent review. This PR remains draft/unmerged; issue #29 remains open.
