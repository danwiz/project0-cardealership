# Canonical signed receipt location — provider-neutral CI contract

**Status: synthetic in-memory object stores; no external cloud or self-hosted
object service provisioned.**

`CanonicalSignedReceiptTransport` composes with the bounded
`SignedEvidenceArtifactAdapter`. It derives a **path-safe, deterministic**
object key from a pinned lowercase namespace, the expected independent source
or archive role, the immutable stream ID, prior/next credential generation,
rotation epoch and commit-order cursor (position plus ordinal).

For each candidate proof, a versioned, length-delimited binary transcript is
hashed with SHA-256. The derived path has this form:

```text
tenant-a/source_complete/v1/<64-lowercase-hex-digest>.receipt
tenant-a/archive_durable/v1/<64-lowercase-hex-digest>.receipt
```

The original stream ID is **never inserted as a path component**: even if it
contains slashes, `../`, Unicode or sensitive identifiers, only the digest
appears in the key. Trusted namespaces must match `[a-z][a-z0-9-]{0,62}`.
The source high-water and archive checkpoint must agree; invalid generation,
epoch or stream-length inputs fail closed before any storage fetch.

The `ReadOnlyObjectStore.read(key)` interface is deliberately small, with no
cloud SDK, endpoint, identity or credential defaults. Each source and archive
transport requires **separately administered storage and signing identities**.
The locator itself does not authenticate an object: the downstream
`SignedRotationEvidenceAuthority` independently verifies two pinned RSA
signatures, manifest digest equality, role, issuer, time bounds and the durable
rotation claim. A hashed path is not equivalent to a WORM lock or an
authenticity signature.

## Disposable CI assertions

- Exact reproducibility for the same tenant/role/stream/generation/epoch/cursor.
- Different keys for each altered tenant, kind, stream, generation, epoch or
  cursor; no path traversal or raw stream leakage.
- Invalid namespace and structurally inconsistent proof rejection.
- Separate source/archive in-memory object maps returning bounded signed
  envelopes through the existing adapter and two independent synthetic keys.
- Wrong stream/epoch/tenant lookup fails, the genuine proof claims exactly
  once, and replay after commit fails.

## Deferred provider integration obligations

The future AWS S3, Azure Blob, Google Cloud Storage or self-hosted Ceph RGW
adapter must use an independently scoped read-only principal, exact object
locator, explicit bucket/container ownership and authenticated endpoint.
It must enforce transfer byte/time bounds **before** allocation, reject
cross-tenant authorization, verify object version/ETag and signed envelope,
and independently read back retention-protected append receipts. An object
store denying delete only through the same privileged administrator as the
database writer does not meet the independent WORM threat model.

No production G03–G09, CKPT-01 or SECRET-01 acceptance test has run. PRs
remain draft/unmerged; issue #29 stays open; external provisioning requires
a separate approved staging provider and operational/security review.
