# Version-pinned signed receipt read-back — provider-neutral CI contract

**Status:** synthetic map-only integration seam; no external accounts, storage,
paid services, real credentials or production writes.

`VersionPinnedSignedReceiptTransport` composes with the existing
`SignedEvidenceArtifactAdapter`, canonical receipt locator, two independently
signed evidence providers, and durable JDBC epoch-claim ledger. It adds a
necessary *read-consistency* check before signature verification.

The subsequent [independently signed pin index](independently-signed-receipt-pin-index.md)
implements the injected trusted pin lookup with domain-separated signatures,
bounded validity and an independent minimum-revision interface. Both remain
CI-only; the floor and signer custody must be independently secured in staging.

For each source/archive `ReopenProof`:

1. Derive the authorization-scoped, canonical receipt key using the fixed
   namespace, evidence role, stream, generations, epoch and source cursor.
2. Obtain a trusted `Pin(versionId, etag, length, sha256)` for that **exact**
   proof and key. The index is an injected, **independently authenticated**
   authority, not metadata taken from the object being fetched. An absent or
   invalid pin is a denial.
3. Pass the canonical key, opaque expected version and `If-Match` ETag to an
   injected `ConditionalObjectReader`. A production reader **must enforce**
   the version qualifier and ETag precondition server-side; falling back to
   latest/unversioned reads is forbidden.
4. Require the returned version and ETag to match the pin, then verify exact
   length and SHA-256 of returned bytes. Only after these checks does the
   downstream adapter decode the 4,096-byte-max signed envelope and the
   authority verify distinct issuers/signatures, manifest equality and
   single-winner durable epoch claim.

ETags are **opaque conditional-read tokens**, not assumed cryptographic hashes.
Neither an ETag nor a content digest replaces a version-qualified server-side
read. Metadata constructors defensively copy buffers; malformed/empty tokens,
oversized objects and invalid digest lengths are denied. Provider exceptions
are sanitized; callers never receive object identifiers or SDK diagnostics.

## CI negative/positive matrix

| Case | Required behavior |
| --- | --- |
| Two separately pinned, valid source/archive objects | One epoch claim accepted; replay rejected |
| Latest object substituted for requested version | Reject before signed evidence verification/claim |
| Stale ETag, modified receipt bytes | Reject, no floor advancement |
| Wrong pinned length or SHA-256 | Reject, no floor advancement |
| Missing independently trusted pin, metadata error, provider outage | Reject, no floor advancement |
| Cross-tenant/malformed canonical namespace | Reject before object access |

Fixtures are in-memory maps, synthetic RSA keys and disposable H2 SQL floors.
They demonstrate the **contract**, not the trustworthiness of a provider, WORM
storage, a real independent manifest authority or endpoint authentication.

## Production acceptance still required

* Define an independent pin publisher, authentication, authorization, expiry,
  anti-rollback policy and retention. Bind manifest entries to the full proof;
  prevent self-referential acceptance of mutable object metadata.
* Implement a provider-specific, fail-closed, version-qualified conditional
  read with bounded streaming, timeout, retries and cancellation. Reject
  unexpected response metadata and redirects. Confirm the provider's exact
  version/precondition behavior with its official API documentation and a
  disposable integration test before trusting it.
* Independently verify storage immutability/retention policy, signer key
  custody/revocation, source completeness, archived payload durability and
  administration separation. A valid signed receipt alone is not evidence
  that those controls actually operated.
* Exercise missing/deleted versions, object overwrite races, revoked
  identity, delayed replication, conflicting ETags, temporary outage and
  disaster recovery in approved staging, witnessed and logged.

This increment **does not** run G03–G09, ARCH-01/02, CKPT-01 or SECRET-01 in
production; all production release gates remain **NO-GO**. The architecture
and tests require no choice between AWS, Azure, Google Cloud and self-hosted
implementations.
