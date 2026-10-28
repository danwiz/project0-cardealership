# Independently signed receipt pins — provider-neutral CI adapter

**Status: synthetic in-memory signed documents and revision floors, no provider
deployment or external resources.**

`SignedReceiptPinIndex` implements the `TrustedReceiptIndex` seam consumed by
`VersionPinnedSignedReceiptTransport`. Two separately injected authorities
must supply:

* `DocumentFetcher`: a document signed by a **pinned, independent** RSA 2048+
  public-key authority (source and archive pin keys should be distinct from
  their respective receipt-signing keys, and from each other).
* `TrustedRevisionFloor`: the **authenticated monotonic minimum revision**
  for the exact proof-bound canonical receipt key. An unknown, missing or
  nonpositive floor is never defaulted to revision zero. This value must not
  be obtained from a mutable object response or a self-declared document.

Each immutable `SignedPinDocument` contains the receipt role, issuer, exact
canonical key, positive revision, issue/expiry timestamps, expected version
ID, opaque conditional ETag, byte length, SHA-256 digest and signature. The
signature covers a versioned, domain-separated, length-delimited transcript
of **all fields except the signature**. The canonical key itself is derived
from the trusted namespace, role, stream, generation, epoch and commit cursor.

The reader rejects a wrong role, issuer or key; invalid signature; expired,
not-yet-valid or longer-than-five-minute document; and any revision below the
independently supplied monotonic floor. Only then does the version-pinned
transport issue its conditional object read and verify content bytes. Any
provider or metadata error returns a generic denial without leaking secrets
or backend diagnostics.

## Synthetic integration coverage

Three JUnit/H2 cases exercise a full chain: **four generated, distinct RSA
signers** (source/archive receipt authorities and source/archive pin
authorities), two versioned receipt maps, signed pin documents, independent
revision-floor lookups and one durable JDBC claim. Positive evidence claims
one epoch and denies replay. Negative cases mutate kind, issuer, key, pin
digest and signature; lower the signed revision, raise or remove the trusted
floor; expire/overextend the signed document; or simulate a provider outage.
Denials do not reach the archive object reader or advance the SQL floor.

## What is *not* established

- **No actual issuer custody or independence**: generated CI keys and in-memory
  maps simulate those boundaries. Production requires separated operators,
  independent identity, hardware/key custody or equally reviewed controls,
  revocation, key rotation and audit trails.
- **No monotonic storage implementation**: the floor interface is a trust
  assumption. A production implementation must ensure authenticated,
  durable, rollback-resistant, race-safe floor reads and revisions that
  cannot be reused or equivocally issued for different pins. A replayed
  valid document *at* the current floor is admissible until its expiry;
  its signed object version must still exist.
- **No live object-store guarantees**: real conditional version reads,
  immutability, retention, deletion/overwrite races, bounded streaming,
  replica consistency and disaster recovery require provider-specific
  staging evidence and independent witnessing.

This is compatible with AWS, Azure, Google Cloud or self-hosted backends
without selecting one. No credentials, cloud SDKs, paid entitlements, data
plane accounts, deployments, merges or production database writes occur.
Security issue #29 and production G03–G09 / ARCH-01/02 / CKPT-01 /
SECRET-01 remain **open / NOT RUN / NO-GO**.
