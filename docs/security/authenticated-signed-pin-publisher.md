# Authenticated signed-pin publisher — provider-neutral CI boundary

**Status: synthetic RSA keys, H2 principals and in-memory witness only.**
No live identities, credentials, secret manager, external storage, cloud SDK,
production schema changes or deployment.

`AuthenticatedSignedPinPublisher` is the intended service entry point for
**publisher-only** revision advancement. It requires two independent trust
anchors before invoking `JdbcSignedReceiptPinRevisionFloor.advance`:

1. A pin-issuer RSA 2048+ signature authenticates the new `SignedPinDocument`,
   checked with `SignedReceiptPinIndex.verifyDocument` for the exact canonical
   namespace/role/proof-derived key, expected issuer, bounded lifetime and all
   pinned version/ETag/length/digest fields.
2. A *different*, separately administered publisher RSA 2048+ signature
   authenticates a short-lived `PublishCommand` (at most 60 seconds). The
   versioned transcript binds publisher issuer, canonical key, expected and
   next revision, validity timestamps, **the complete pin-issuer signing
   transcript and the pin document's signature**. An opaque caller-supplied
   issuer string is never trusted as identity by itself.

Only after both signatures pass does the coordinator attempt the durable
monotonic witness-first SQL CAS. A replay or stale `expectedRevision` returns
false. Missing, expired, tampered or mismatched signatures and provider
outages fail closed with a generic error, without echoing provider diagnostics.
The coordinator contains **public** trust anchors only; test signers live in
disposable fixtures. The coordinator itself neither publishes the signed
document to object storage nor delivers/rotates private keys.

It also requires an independent, **revision-qualified**
`DurableDocumentReadback` before floor advancement: the provider must return
the **exact requested signed revision**, matching both the canonical signing
transcript and issuer signature. Missing, altered, mismatched or unavailable
read-back denies publication. The separate
[immutable document publication contract](immutable-signed-pin-document-publication.md)
models atomic put-if-absent, idempotent retry after uncertain commit, collision
denial and exact revision read-back; none of these CI fixtures prove real WORM
durability or administrative independence.

## Privilege and deployment boundary

* **Receipt readers:** SQL `SELECT` on the trusted revision floor; no
  publisher-signing credential, witness reservation permission, or SQL UPDATE.
* **Publisher service:** pinned authorized publisher identity; distinct
  publisher-only SQL UPDATE capability and witness reservation permission.
  No general owner, INSERT, DELETE, DDL, credential-writer or audit-tamper
  privilege. Call `advance` **only** through the authenticated coordinator.
* **External pin issuer:** signs immutable pin documents, isolated from both
  publisher authorization and read-only receipt retrieval.
* **Independent witness administrator:** owns durable monotonic evidence,
  separate from SQL and pin issuer; protects against restore/rollback.

The H2 fixture grants separate reader `SELECT` and publisher `SELECT, UPDATE`
and verifies that the reader cannot write and publisher cannot insert/delete
or perform DDL. **H2 table-level UPDATE grants are not equivalent to a
production column-level/row-scoped PostgreSQL privilege model**. A compromised
publisher SQL credential could bypass the Java CAS and lower a row directly;
the witness should detect such a rollback, but production database-side
enforcement, restricted columns, auditing and signer/key custody still need
dedicated PostgreSQL staging integration and independent review.

## Recovery and limitation

If the external monotonic witness acknowledges a higher revision but the
publisher SQL commit fails or has unknown outcome, read-back stops. An
authorized publisher can retry a still-valid signed command with the SQL
row's expected revision, or a new signed command/doc must be issued for
the same or a higher witnessed revision. **Never reset or lower the witness**.

The signed pin document must be durably available to its independent reader
before the floor is advanced; otherwise a premature advance can cause
availability loss. There is no distributed atomic commit spanning pin
issuance, witness and SQL. Design outage/replay/key-revocation procedures,
durable document publication acknowledgments and regional isolation before
using a real provider.

A [disposable PostgreSQL rehearsal](postgresql-signed-pin-publication-ci.md)
tests real PostgreSQL column-level revision grants, a database-side monotonic
trigger and read-back-before-floor ordering. Its read-back and witness remain
synthetic, and it is not staging acceptance.

The H2 CI integration tests use only synthetic RSA signers, H2 role grants
and an in-memory `AtomicLong` witness. They are **not** production control
acceptance evidence. Issue #29 remains open and G03–G09 / ARCH-01/02 /
CKPT-01 / SECRET-01 remain **NOT RUN / NO-GO**.
