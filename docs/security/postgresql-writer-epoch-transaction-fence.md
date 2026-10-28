# Database-enforced writer epoch fence — disposable PostgreSQL rehearsal

**Status: CI-ONLY / NON-DEPLOYED.** This rehearsal composes the
provider-neutral [durable JDBC rotation claim](jdbc-rotation-claim-ledger.md)
with an independently owned **database transaction fence**. The two controls
operate on the **same persisted `writer_rotation_floors` row**; a successful
atomic claim changes the epoch the database accepts for credential writes.

The example adapter is PostgreSQL 16 because its row-lock behavior and SQL
principals can be verified in disposable CI. A production implementation can
use another database/provider only if it supplies equivalent, independently
reviewed transactional semantics. A Java preflight or local process lock is
not equivalent.

## Mandatory invariants

1. The identity for authorization is the *authenticated database principal*
   (`session_user` in this PostgreSQL fixture), not a caller-supplied header,
   Java value or freely writable session variable.
2. The table holding the active rotation epoch and the trusted mapping of
   database principals to approved epochs are **owned by a separate principal**.
   The ordinary application and writer cannot update them, disable the trigger,
   edit grants or create a compatible privileged identity.
3. Every privileged credential mutation hits a database-enforced check **in
   the same transaction**. The check acquires a shared row lock on the epoch
   row that remains held until transaction completion. The rotation claim
   obtains an exclusive row lock, so it waits for earlier writes to commit or
   roll back before it can advance the epoch.
4. After the claim commits, an old SQL principal still connected with its
   original password is rejected at its next write. A new principal is denied
   before the claim and admitted afterward. Unknown principals, missing epoch
   rows and mismatched epochs are denied.
5. Committed changes *before* the fence remain in the audit ledger even when
   their application callback reports an error. Failed writes *after* the
   fence produce no committed credential change or audit entry. This must be
   reconciled using authoritative commit/source evidence, not HTTP responses.

In the fixture, the database owner creates `writer_rotation_floors`, an
ungranted `writer_role_epoch` table, a restricted `credential_state` table,
and a `SECURITY DEFINER` trigger whose qualified SQL names run under a fixed
system search path. The trigger reads the persisted 
`writer_rotation_floors.rotation_epoch ... FOR SHARE`, compares that epoch
to the trusted mapping for `session_user`, and rejects mismatches with SQLSTATE
`42501`. The separately privileged claim role advances the same floor using
`JdbcRotationClaimLedger` and a transaction-scoped `SELECT ... FOR UPDATE`.
The business writer roles receive only `SELECT` and column-limited `UPDATE`
on the credential table—not grants on the guard, role mapping, or audit ledger.

## CI cases and limitations

The disposable PostgreSQL integration tests establish:

- A pending old-writer transaction holding the guard lock forces the claim to
  **wait** (confirmed via PostgreSQL lock wait diagnostics). The old commit and
  audit event become visible before the claim returns.
- After claim commit, the **same still-authenticated old connection** can read
  but cannot update. Spoofing a custom epoch session setting, directly editing
  the guard or disabling the trigger is rejected. The new identity can write,
  and the audit ledger lists both accepted actors in commit order.
- A transaction begun before cutover that has not yet acquired its write fence
  cannot mutate the credential row once the epoch has changed.
- The rotation claim itself rejects replay after the transaction fence moves.

These tests use a *fixture-generated* proof and synthetic fixed secrets. They
do not prove immutable external archive receipts, true CDC completeness,
source failover identity, distributed secret-manager revocation, selected
provider HA/recovery, or owner compromise resistance. A legitimate database
owner can modify the guard and bypass the fixture trigger; production must
review its administrative separation and audit those changes independently.
All DDL, login creation and fault injection are guarded by an exact disposable
CI database identity and must **not** be run against a shared system.

**Release decision: G03–G09, G08/CKPT-01, SECRET-01 and independent staging
acceptance remain NOT RUN / NO-GO.** Do not merge or deploy this contract from
the passing fixture tests alone.
