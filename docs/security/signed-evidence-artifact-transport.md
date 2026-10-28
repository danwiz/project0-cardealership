# Signed evidence artifact transport — provider-neutral CI seam

**Status: synthetic local-file transports, no external services provisioned.**

`SignedEvidenceArtifactAdapter` implements the existing
`SignedRotationEvidenceAuthority.EvidenceProvider` contract. It accepts an
injected `ArtifactTransport` that returns the exact signed source or archive
receipt bytes for a requested `ReopenProof`. The adapter decodes one small,
versioned envelope and enforces a required `SOURCE_COMPLETE` or
`ARCHIVE_DURABLE` kind. Its caller verifies distinct pinned public-key
signatures, manifest digest equality, time bounds and the shared SQL claim.

The binary envelope has a four-byte `DRE1` magic, length-prefixed strict UTF-8
kind and issuer, length-prefixed 32-byte manifest digest, signed 64-bit issue
and expiry millisecond timestamps, and length-prefixed signature bytes.
Integer lengths and timestamps are big-endian. The complete envelope is capped
at **4,096 bytes**, issuer UTF-8 at 256 bytes, and signature at 1–1,024 bytes;
unknown kinds, truncated fields, malformed UTF-8, extra trailing fields and
absent/wrong-role receipts are rejected. This *transport envelope* is separate
from the canonical `signingPayload` covered by `SHA256withRSA` signatures.

The two CI tests use **separate directories** to stand in for independent
source and archive artifact reads. They test a corrupted archived receipt,
successful signed read-back and one durable epoch winner, then deny replay.
Additional parsing tests deny truncation, oversized and malformed artifacts,
wrong-kind receipt swapping and missing file/provider errors. Local files are
not immutable and cannot satisfy the production audit release gate.

The subsequent [canonical locator contract](canonical-signed-receipt-location.md)
adds path-safe, content-addressed lookup keys tied to the exact tenant,
stream, role, generation, epoch and cursor. Its backing object maps remain
synthetic and do not prove independent storage administration.

The further [version-pinned read-back contract](version-pinned-signed-receipt-readback.md)
adds an independently trusted version/ETag/length/SHA-256 pin and a required
version-qualified conditional read before this adapter accepts receipt bytes.
Its in-memory versioned-object maps are also synthetic; no provider retention
or immutable storage guarantee is demonstrated.

## Provider implementations deferred

| Transport candidate | Read-only adapter requirement | Independently administered destination requirement |
| --- | --- | --- |
| AWS | Fetch exact signed object via an identity-bound SDK; cap size/time; check version, digest and pinned key | S3 Object Lock compliance configuration, access isolation and read-back evidence |
| Azure | Fetch exact signed Blob through a managed identity with bounded response | Locked immutable Blob retention and separate admin/control path |
| Google Cloud | Fetch exact signed Cloud Storage object using workload identity | Locked Bucket retention and independently auditable receipts |
| Self-hosted | Fetch exact signed object from isolated Ceph RGW or offline archive over authenticated transport | Different storage administrators, hardened WORM/retention and offsite recovery evidence |

These are **research options**, not integrated SDKs. Future adapters must:

1. Authenticate the endpoint/workload identity and pin issuer signing keys;
   never trust the caller's claimed organization, URL or issuer.
2. Enforce transfer limits **before allocating** a response buffer, plus bounded
   timeout/retry/backoff and cancellation. A 4 KiB post-download envelope
   check is a second guard, not the first.
3. Use an authorization-safe, canonical object locator bound to stream and
   epoch; reject path traversal, cross-tenant key substitution and stale
   ETags/versions. Signer key revocation must be independently managed.
4. Verify real source completeness/commit order and actual durable archive
   retention/read-back; signed metadata alone is insufficient.
5. Preserve immutable events and unknown commit outcomes during retries; do
   not silently fast-forward checkpoints after provider outage.

These adapters deliberately expose **no cloud credentials, endpoints,
provider-specific SDK, default account or paid resource**. The separate
private cloud/self-hosted business-case report lists free-first evaluation
choices and cost/risk gates. Production G03–G09,
CKPT-01 and SECRET-01 remain **NOT RUN / NO-GO**.
