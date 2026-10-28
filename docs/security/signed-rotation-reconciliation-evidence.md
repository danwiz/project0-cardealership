# Independent signed rotation evidence — provider-neutral verification seam

**Status: SYNTHETIC CI SIGNERS ONLY / NOT PRODUCTION ATTESTATION.**

`SignedRotationEvidenceAuthority` implements the existing
`ReconciledJdbcWriterGate.ReconciliationAuthority` contract with two separately
pinned public-key identities. An independent *source* provider must attest
complete, quiescent committed-source coverage; an independent *archive*
provider must attest durable externally immutable retention through the **same
event-manifest digest**. Neither attestation can substitute for the other.

The Java 11-compatible verification profile is `SHA256withRSA`, with at least
2048-bit distinct pinned RSA keys and distinct issuer IDs. This is a portable
cryptographic protocol choice, not a commitment to a secret manager, cloud
provider or certificate service. Signatures must be made by separately secured
provider workloads. Private keys are **never** accepted or stored by the
verification class; only public trust anchors and immutable receipts enter it.

## Exact binding and replay controls

Each independently signed, length-delimited, versioned binary transcript
contains:

| Field | Security requirement |
| --- | --- |
| Protocol version | Domain-separate from all other messages/signatures |
| Statement kind | `SOURCE_COMPLETE` or `ARCHIVE_DURABLE`, never interchangeable |
| Pinned issuer | Distinct trusted source/archive identities and public keys |
| Stream ID | Bind to the immutable source-generation identity |
| Previous/next credential generation | Bind the specific writer handoff, prevent transposition |
| Rotation epoch | Bind the receipt to the exact recovery ceremony |
| Commit position and event ordinal | Bind both attestations to the same source high-water mark |
| 32-byte manifest digest | Bind both independent authorities to *identical event contents* |
| Issued/expiry times | Reject future issuance, expiry, and lifetimes longer than 5 minutes |

The source and archive checkpoints in `ReopenProof` must match before receipt
loading. The verifier refuses missing, invalid, stale, swapped-role, wrongly
signed, different-digest or proof-tampered receipts. It next calls the shared
`JdbcRotationClaimLedger.claim(proof)` **only if both signatures verify**.
This final SQL claim atomically rejects replay after a winner commits, even
when multiple replicas received the same otherwise valid evidence.

The signed transcript is produced by
`SignedRotationEvidenceAuthority.signingPayload(proof, evidence)`: four-byte
big-endian length plus UTF-8 bytes for each string, fixed-width big-endian
numeric fields and the exact 32 digest bytes. No raw credentials or access
tokens appear in these fields. Providers must independently implement and
cross-test this canonical encoding; no improvised JSON/string concatenation
is equivalent.

The [bounded transport-envelope adapter](signed-evidence-artifact-transport.md)
now exercises independently retrieved synthetic source/archive artifacts in
disposable CI. It does not implement a real provider SDK or attest the truth
of source/archive assertions.

## Trust requirements still unmet

A valid signature says **which key signed this statement**, not whether a
provider's assertion is truthful. A production source signer must verify
relevant commit-order coverage, CDC retention, transaction quiescence, source
generation/timeline and crash/failover identity. The archive signer must
independently verify the append-only/WORM acknowledgement, exact event manifest
digest, receipt provenance and actual read-back. Merely signing the same
caller-provided hash on both sides voids the intended separation.

Production also requires workload identity, pinned-key issuance/revocation,
key rotation and overlap policy, trusted clock/skew management, endpoint
authentication, bounded delivery, out-of-band signer audit, incident response
and an independent operating domain for the SQL claimant. The fixture uses
two ephemeral RSA key pairs generated inside tests and a synthetic manifest;
it proves **verification logic**, not any of those provider obligations.

The durable claim may commit before local credential attestation completes;
an attestor failure then consumes the epoch and stays blocked. A new recovery
ceremony, never a reset or silent replay, is required. The
[database transaction fence rehearsal](postgresql-writer-epoch-transaction-fence.md)
demonstrates how the claimed row can also fence SQL changes, but the signer
fixture is not connected to a deployed CDC source, archive or writer service.

Four isolated H2/JUnit tests cover accepted independent signatures and claim,
wrong digest/role/signer, tampered stream checkpoint and generation, expired
and future evidence, verifier outage, same-key rejection, and two competing
replicas with one durable claim winner.

**Release stop:** G03–G09, G08/CKPT-01 and SECRET-01 remain **NOT RUN / NO-GO**
without real provider receipts, staged failure drills and independent review.
No production trust anchor, secret, archive, DB schema or deployment is created.
