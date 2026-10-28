# Immutable signed-pin document publication — provider-neutral protocol

**Status: contract and CI fixtures only; not production WORM or staging approval.**
No object-store account, cloud SDK, external administrator, provider secret,
network resource, deployment, retention rule or paid entitlement is involved.

`ImmutableSignedPinDocumentPublisher` supplies the missing publication and
revision-qualified read-back seam for `AuthenticatedSignedPinPublisher`. It
uses the independently pinned issuer key from `SignedReceiptPinIndex` to
authenticate the complete signed pin document before any storage attempt. The
provider-specific `ImmutableDocumentStore` must implement two operations:

1. **Atomic durable `putIfAbsent(canonicalKey, revision, signedDocument)`**.
   The compound key is immutable: exactly one signed value may occupy a
   revision. A collision must never overwrite the first value, even across
   concurrent writers or process restarts. A false return means the revision
   already exists; a thrown exception can mean an uncertain commit.
2. **Independent `readExact(canonicalKey, revision)`**. No latest-object
   shortcut, redirect to a different revision, eventual or stale replica,
   caller-supplied acknowledgment, or fabricated successful write response
   may satisfy the check. The read must verify authentic provider identity
   and return durable, complete bytes for the exact requested revision.

## Acknowledgment and recovery

The protocol deliberately treats successful writes, pre-existing revisions,
and lost write acknowledgments the same way: **read the exact revision back,
validate its issuer signature and proof-bound canonical key, and compare its
canonical signed transcript *and* signature byte-for-byte with the caller's
document**. Only exact equality acknowledges publication. A same-document
retry is idempotent. A differing same-revision document, missing/unreadable
object, bad signature or uncertain provider condition denies publication with
the generic `immutable pin publication unavailable` error. No replacement,
delete, mutable upsert, or best-effort floor advancement is permitted.

`AuthenticatedSignedPinPublisher.publish` independently verifies both the
pin-issuer and privileged publisher signatures, performs a second
**revision-qualified** `DurableDocumentReadback`, and only then attempts the
external monotonic-witness reservation and SQL revision CAS. The read-back
contract cannot atomically commit the object, witness and database together:
uncertain witness or SQL commits must be reconciled independently. Do not
decrease the witness or reset the pin revision to restore availability.

## Provider adapter obligations

- Canonical `(key, revision)` names must be injective and safely encoded;
  reject ambiguous separators/escaping, unbounded names and cross-tenant
  path aliases. Reject out-of-scope identities before both read and write.
- Use a storage primitive with **enforced conditional create** and durable
  exact-version reads; an ordinary object PUT followed by a GET is not an
  implementation of this contract. If a service does not provide these
  semantics, use an independently guarded append-only ledger or reject it.
- Establish independent storage/retention administration, retention lock,
  anti-delete/anti-replace permissions, encryption, audit logs and alerting.
  A compromised storage administrator can invalidate this seam unless those
  controls resist that threat. Ordinary application and credential-writer
  identities must have no pin publication or retention-override privilege.
- On network timeout, retry reads with a **finite deadline and explicit
  fail-closed outcome**; never advance the revision floor because the write
  RPC appeared to complete. Operators must reconcile a missing document
  before reissuing a new authorized pin revision.
- Authenticate real provider responses, verify durable read consistency and
  test collision races, process death after commit, outages, restore and
  administrative deletion in isolated staging under an independent reviewer.

## CI evidence boundary

`ImmutableSignedPinDocumentPublisherTest` uses synthetic RSA signers, an
in-process `ConcurrentHashMap` with first-writer-wins, and disposable H2 SQL.
It exercises exact-revision reads, idempotent publication, lost acknowledgement
after an actual append, collision denial, forged signatures, wrong proofs,
and floor advance only after publication. **These fixtures do not establish
real durable or immutable storage, provider read consistency, independent
administration, retention lock or deployment readiness.**

Continue to hold issue [#29](https://github.com/danwiz/project0-cardealership/issues/29)
open and every security PR draft/unmerged. G03–G14, especially real archive
immutability and the provider-neutral publication integration rehearsal,
remain subject to independently witnessed acceptance before production.
