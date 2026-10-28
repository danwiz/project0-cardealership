# Immutable pin-document adapter acceptance matrix — CI fault injection

**Status: provider-neutral CI fixture only — NOT real WORM, staging or
production acceptance.** No external service, cloud credentials, retention
setting, managed storage, or paid entitlement is created or selected.

This matrix extends the
[immutable signed-pin publication protocol](immutable-signed-pin-document-publication.md)
with deterministic adversarial scenarios for `ImmutableDocumentStore`.
`ImmutableSignedPinDocumentAdapterAcceptanceTest` runs against a disposable
`ConcurrentHashMap` whose compound key is an unambiguous `(canonicalKey,
revision)` tuple. It can simulate forbidden administrator mutation precisely
because **the map is not immutable storage**.

| CI test | Simulated action | Pass/stop condition |
| --- | --- | --- |
| PIN-ADP-01 — race | Two independently valid, different signed documents concurrently attempt the same revision | Atomic first writer wins; exactly one publisher acknowledges; loser denied; winner unchanged; identical replay idempotent |
| PIN-ADP-02 — lost ACK / restart | Append succeeds, acknowledgment is lost; instantiate a new publisher over the same fixture state | Exact signed revision read-back permits retry without duplicating or replacing the record |
| PIN-ADP-03 — failed write | Provider fails *before* append and no revision can be read | Generic denial; no false acknowledgment; recovery permits a later authorized retry |
| PIN-ADP-04 — inconsistent read | Exact revision read is misrouted to a different but valid signed revision | Deny both publication and direct read; a correct exact read permits subsequent idempotent recovery |
| PIN-ADP-05 — outage/tamper | Read times out or fails; then simulated operator deletes/replaces records or injects forged signature | Deny absent/unverifiable records, reject conflicting replay, disclose no provider diagnostics; never treat tampering as proof of durability |
| PIN-ADP-06 — isolation | Same revision on different proof-bound keys plus consecutive revisions on one key | Independent records; no cross-key/tenant proof substitution; exact revision isolation |

These cases supplement the earlier publisher and disposable SQL tests: they
do **not** independently prove persistent writes across a *real* process or
storage restart, cloud object versioning, durable first-writer-wins semantics,
retention lock, tenant IAM policy, storage audit completeness, independent
administration, or rollback resistance. A recreated Java publisher sharing
the same in-memory map is a logical restart simulation, **not a crash-recovery
test of the storage provider**.

## Required provider adapter acceptance (NOT RUN)

For AWS, Azure, Google Cloud or self-hosted implementations, the selected
operator and independent reviewer must repeat the same cases against an
isolated real datastore, with evidence for:

1. **Atomicity:** two independent processes and nodes racing the same key,
   including retained object/version identity, exactly one winner and
   failed collision without accidental overwrite. A provider-specific
   conditional request must have documented atomic semantics.
2. **Commit ambiguity:** terminate a client after the durable write but
   before its reply, then independently read back the exact revision.
   Retry with the original signed bytes, reject a different valid document.
3. **Persistence and consistency:** abruptly restart writers and storage
   nodes, exercise stale replicas, regional failover, transient network loss,
   and read-after-write consistency. Define maximum retries, deadlines and
   terminal fail-closed behavior; do not silently use a latest-object alias.
4. **Administrative containment:** verify actual retention/hold enforcement,
   separation of owners, blocked overwrite/delete/shorten-retention attempts,
   immutable access logs and external alerts using least-privileged accounts.
   A Java interface cannot establish these properties.
5. **Identity and routing:** exact `(tenant, kind, proof, revision)` mapping,
   authenticated provider endpoint, bounded object/metadata size, encryption,
   no path aliasing or cross-tenant override, and private network policy.
6. **Recovery authorization:** reconcile immutable archive state, external
   monotonic witness and SQL floor after uncertain commits. Preserve original
   receipts and failure evidence; never lower/reset the witness or pretend
   an absent/replaced document was historically immutable.

**Critical limitation:** a separate read can validate a *current* issuer-
signed document, but cannot prove that a different valid issuer-signed document
never previously occupied the same revision. Durable append-only storage,
independent controls and an externally witnessed audit history are required
to prevent or detect that historic replacement.

Track actual provider execution in the production
[security integration plan](credential-security-production-integration-plan.md)
and evidence register; until independently reviewed real tests pass, issue
[#29](https://github.com/danwiz/project0-cardealership/issues/29) stays open
and security PRs stay draft/unmerged. No CI pass changes G03–G14, ARCH-01/02,
CKPT-01, or SECRET-01 from **NOT RUN / NO-GO**.
