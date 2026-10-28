# Immutable pin publication: provider-specific adapter blueprints

**Design snapshot: 2026-10-28. Status: DESIGN ONLY / NO-GO for all four
providers.** These are implementation and test plans, not live adapters,
verified retention, provisioned infrastructure, or production approval.
No account, external storage, identity, secret, paid service, or policy has
been created.

## 1. The common contract and evidence boundary

Implement the repository's
[`ImmutableDocumentStore`](../security/immutable-signed-pin-document-publication.md)
without changing the public two-method contract:

- `putIfAbsent(canonicalKey, revision, signedDocument)` performs an **atomic,
  durable create only**. A same-revision collision never changes the
  historical winner. A request timeout means **unknown commit**, not failure.
- `readExact(canonicalKey, revision)` authenticates the provider endpoint and
  returns the retained, complete signed document for that revision, or fails
  closed. No unqualified `latest`, listing-derived guess, eventual replica,
  proxy cache, or fabricated provider acknowledgment is accepted.
- Read-back verifies the **issuer signature, proof-bound canonical key,
  revision, exact canonical transcript and signature bytes**. Object ETags
  and provider checksums are transport hints, **not** substitute proofs of
  signed-content integrity, history, or append-only custody.
- A successful exact read establishes the *currently reachable* bytes. It
  does **not** prove a different valid document was never present earlier.
  An independently administered, monotonic, append-only witness and
  independently retained provider/audit receipts remain mandatory.

Use a versioned namespace containing a non-ambiguous encoding of `(tenant,
document-kind, proof-bound canonical key, revision)`; require length limits,
Unicode normalization policy, scoped issuer/tenant authorization, fixed
byte-size limits and no user-controlled path segments. Treat bytes returned
by the SDK as untrusted until signed verification succeeds. The adapter
must never grant the ordinary application or credential-writer SQL identity
object-writer, retention-administration or witness privileges.

### Reference adapter sequence (pseudocode, **not runnable cloud code**)

1. Authorize tenant/key and validate signed input *before* calling the
   provider; resolve the deterministic immutable name for `(key, revision)`.
2. Issue a documented **conditional create**, not a plain overwrite. Deny
   any request configuration or library retry that drops the precondition.
3. Preserve the service-returned object version/generation identifier and
   correlated request ID in a private redacted receipt. A timeout could
   follow a committed write.
4. Independently query the *exact revision*, resolving and verifying the
   retained version/generation through a trusted receipt/index. If the
   identity cannot be established, deny acknowledgment, including after an
   apparently successful PUT.
5. Compare the returned complete signed bytes to the submitted signed
   document. Identical retry is idempotent; a differing signed document is
   an immutable collision; absent, invalid or ambiguous history fails closed.
6. Only the higher-level publisher may then reconcile a separately fenced
   monotonic witness and the SQL revision floor; never lower either.

An adapter may keep a *hint* index for version IDs but that index must not
become a single mutable authority. Require an independently operated,
append-only receipt stream and periodic reconciliation against all stored
versions and the external witness. No provider primitive alone establishes
the full contract.

## 2. Candidate-provider mapping

| Provider | Create-if-absent candidate | Exact read identity | Anti-rollback / immutability condition | Verdict |
| --- | --- | --- | --- | --- |
| **AWS S3** | `PutObject` with `If-None-Match: *`; enforce conditional PUT policy where supported | Explicit `versionId`, not current-object GET | Versioning + Object Lock protect selected **versions**, but a delete marker or newly created version can change current-key visibility. Deny deletes/marker creation, block new-version bypass, independently retain receipt/history | **DESIGN / NOT RUN** |
| **Azure Blob Storage** | `Put Blob` with `If-None-Match: *` and collision status handling | Retained `x-ms-version-id` when versioning enabled | Version-scope immutable policies/holds are account-feature dependent; another version may still be created. Validate conditional writes, policy immutability, deletion rights and historical receipts | **DESIGN / NOT RUN** |
| **Google Cloud Storage** | Object insert with `ifGenerationMatch=0` | Explicit generation number (`generation`) | Precondition zero means **no live object**, not that no noncurrent generation ever existed. Prevent deletion/visibility reset, lock retention as appropriate and witness all historical generations | **DESIGN / NOT RUN** |
| **Ceph RGW** | **Unproven.** Do not assume S3 `PutObject If-None-Match` is supported or atomic by the selected RGW release | Version ID only if independently verified for deployed release/zone configuration | Object lock/hold and RGW versioning are not evidence of the AWS PUT semantics; privileged cluster operators and multisite behavior expand threat model | **BLOCKED** until atomic create and rollback-resistant custody are demonstrated |

### AWS S3 — non-provisioning blueprint

1. Candidate SDK operation: `PutObject` carrying `If-None-Match: *`,
   consistent signed payload and tenant-specific key. Preserve all errors,
   especially 412 precondition failures, conflicting 409 operations and
   indeterminate transport outcomes, without leaking provider internals.
2. Configure **only in a later approved staging environment**: bucket
   versioning, Object Lock retention scope/mode, policy enforcement of
   conditional writes, narrowly scoped writer/reader principals, a
   separately controlled audit/export destination and independent history
   witness. Do not assert that Object Lock bars new versions.
3. Test a current-object collision; a delete marker hiding an older locked
   version; delete attempts with each service/admin role; a new-version PUT
   under an existing key; and a failure after commit before acknowledgment.
4. Read explicitly by retained `versionId` and compare signed bytes. If
   version history, deletion state or witness disagrees, **stop**; do not
   recover by reading the current key and pretending it is the original.

Primary: [S3 conditional writes](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html),
[enforcing conditional writes](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes-enforce.html),
[Object Lock](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html).

### Microsoft Azure Blob — non-provisioning blueprint

1. Candidate SDK operation: create with `If-None-Match: *`, capture server
   ETag and, where supported, version ID. An ETag alone cannot attest
   historical immutability.
2. Later staging must verify account kind, blob versioning, policy scope,
   version-level time-based retention/legal-hold compatibility, container
   policy lock behavior and identity/RBAC enforcement for the exact SKU.
3. Negative tests: duplicate conditional PUT, attempted new version of
   protected blob, unauthorized deletion or shortened retention, explicit
   prior-version retrieval, stale/cross-region read and lost acknowledgment.
4. Reject any configuration where a data-plane or control-plane operator can
   rewrite the effective revision history without an independent witness and
   durable alert receipt. Do not confuse immutable *version content* with a
   unique immutable *logical revision*.

Primary: [Put Blob](https://learn.microsoft.com/en-us/rest/api/storageservices/put-blob),
[conditional headers](https://learn.microsoft.com/en-us/rest/api/storageservices/specifying-conditional-headers-for-blob-service-operations),
[blob versioning](https://learn.microsoft.com/en-us/azure/storage/blobs/versioning-overview),
[version-scope immutable storage](https://learn.microsoft.com/en-us/azure/storage/blobs/immutable-policy-configure-version-scope).

### Google Cloud Storage — non-provisioning blueprint

1. Candidate request: object insert with `ifGenerationMatch=0`, record its
   returned **generation** and validate the full signed payload.
2. During later approved staging, explicitly choose/version-test retention
   and lock controls, service-account permissions, audit export and external
   receipt/witness administration.
3. Reproduce the documented corner case: a noncurrent historical generation
   exists but there is **no live version**. Verify whether a zero-generation
   precondition admits a new object. Treat any such outcome as a failure of
   the higher-level append-only contract, even if the API request succeeded.
4. Read by exact generation. Deny recovery based on listing, current object
   alias, lifecycle deletion, bucket recreation or generation reset.

Primary: [request preconditions](https://docs.cloud.google.com/storage/docs/request-preconditions),
[object versioning](https://docs.cloud.google.com/storage/docs/object-versioning),
[using versioned objects](https://docs.cloud.google.com/storage/docs/using-versioned-objects),
[retention controls](https://docs.cloud.google.com/storage/docs/control-data-lifecycles).

### Self-hosted Ceph RGW — **conditional-PUT capability not established**

1. **Do not implement an optimistic `PutObject` then `HeadObject`**: that
   introduces a replace race and violates first-writer-wins. The published
   RGW object-operation documentation inspected here describes conditional
   `GET`/`HEAD`; it does not establish an atomic `If-None-Match` PUT contract.
2. First record the exact Ceph release, RGW zone/replication topology,
   object-lock settings and support matrix. Obtain primary documentation or
   independently reproduce concurrent distinct-object PUT races across
   processes/nodes. Until then the provider adapter returns **unsupported**.
3. If a release cannot satisfy atomic create, propose a separately managed
   transactional append-only ledger only if its own concurrency, anti-delete,
   privileged-admin isolation and witness obligations can pass the complete
   acceptance matrix. This is a *new architecture review*, not a silent
   compatibility shim.
4. Independently test root/cluster-admin access, backup restore to an older
   namespace, multisite conflict, Object Lock bypass, read consistency,
   missing encryption key and out-of-band audit alarms.

Primary: [Ceph RGW object operations](https://docs.ceph.com/en/latest/radosgw/s3/objectops/),
[S3 compatibility matrix](https://docs.ceph.com/en/latest/radosgw/s3/),
[bucket operations](https://docs.ceph.com/en/latest/radosgw/s3/bucketops/).

## 3. Test catalog and evidence packet — all **NOT RUN** on providers

Run only in a **separately authorized, isolated and disposable** environment.
The present in-memory Java tests exercise control flow, not cloud durability.

| Test ID | Provider-specific adversarial stimulus | Required independent evidence |
| --- | --- | --- |
| PIN-ADP-01 | Race two different signed documents, across nodes and retries, into one logical revision | Exactly one winner across retained versions; independent exact read; no historical override |
| PIN-ADP-02 | Kill writer after provider commit but before ACK; restart writer and retry both identical and conflicting payloads | Original version/generation survives; identical bytes acknowledged only after verification; conflict rejected |
| PIN-ADP-03 | Inject pre-commit failure, rate limit, auth revoke, outage and bounded deadline | No false success; generic denial; preserved diagnostic evidence privately |
| PIN-ADP-04 | Route exact read to wrong revision, stale replica, latest alias or wrong provider identity | Provider/revision mismatch rejected and witnessed |
| PIN-ADP-05 | Delete current name, create marker, restore old snapshot, create a second valid version, revoke KMS key | Original history remains independently verifiable or production gate fails closed with critical alert |
| PIN-ADP-06 | Cross-tenant and proof-bound key mixups, ambiguous encodings and consecutive revisions | No aliasing, privilege escalation, witness reuse or cross-tenant leakage |
| PIN-ADP-07 | AWS delete marker/new version; GCS noncurrent-only create; Azure version re-create | New live content cannot be mistaken for the original logical revision |
| PIN-ADP-08 | Ceph conditional-PUT missing/ignored, multisite split-brain, RGW root override | Explicit unsupported result until cross-node atomicity and administrative isolation are proven |

The evidence packet for **each** provider: provider/release/region, account
feature and IAM policy export (redacted), precise requests and error/status
semantics, original/successful object identities, all versions/generations,
signed-byte digests and witnessed receipts, observed timings, allowed
administrative actions, retained alarms, reviewer identity, test date and
repeatable cleanup. Do not expose live credentials, customer IDs or payloads.
Record a failed test as **NO-GO**, never as a skipped success.

## 4. Approval gates and recommended next build

1. Keep provider-neutral Java 11/17 CI with synthetic signers and disposable
   stores as the implementation baseline.
2. Add non-network **protocol assertion fixtures** for provider-specific
   request preconditions/version-selection and deliberate invalid-response
   behavior before bringing in any production SDK.
3. An independent reviewer must approve threat model, admin/retention
   authority, transport, audit/witness and disaster recovery *before*
   resources or test accounts are provisioned. Ceph needs an explicit
   compatibility decision first.
4. Compare security and costs using the
   [normalized workload model](normalized-cloud-workload-cost-thresholds-2026.md);
   do not choose a cloud provider because a free tier covers a small demo.

No tests here satisfy G03–G14, ARCH-01/02, CKPT-01, SECRET-01, or the
[production security integration plan](../security/credential-security-production-integration-plan.md).
Issue [#29](https://github.com/danwiz/project0-cardealership/issues/29)
remains open; security PRs stay draft and unmerged.
