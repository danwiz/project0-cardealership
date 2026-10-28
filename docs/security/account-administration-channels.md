# Account administration channels and recovery invariants

**Draft design / test checkpoint — 2026-10-28.** Tracks dealership security issue #29 and stacked PR #37. This is **not** approval to deploy to a real database.

## Authority separation

| Execution boundary | Identity/account access | Privileged account mutation |
|---|---|---|
| Normal composed JDBC runtime | Authenticate users, allow customer-only registration and verified self-service password rotation through ordinary account repository | **No administrative repository wired**: deny staff/admin provisioning, role reassignment, and account deletion |
| Separately authorized account operator | Verify credentials against current persisted account state | Explicitly inject a distinct administrative repository into `UserLoginService(identityRepository, administrativeRepository)`; require persisted ADMINISTRATOR permission |
| Standalone in-memory characterization/testing | Local fixture account repository | Legacy in-memory admin capability remains available to test workflows |
| Raw infrastructure with DB credentials | Can directly open JDBC connections | Still a trusted boundary; **not** protected solely by these Java service checks |

The normal JDBC application path (`DealershipCompositionRoot.createJdbc`) intentionally passes `null` for administrative repository access to the context. The context passes that fail-closed configuration to every login-service variant (user/customer/employee/admin). A snapshot replacement cannot silently re-enable privileged operations for a JDBC context.

**Operator integration remains a design task:** the production administration runner must obtain its own identity and dedicated privileged database connection through approved secret delivery and audited operator authentication; it must **not** load elevated credentials into the ordinary runtime. A manually constructed operator service in unit tests demonstrates injection, not real database-level separation.

## Service safeguards

1. `UserLoginService.provisionUser` verifies a current persisted administrator and uses only the explicit administrative repository for privileged user creation.
2. `changeRole` requires a current administrator and uses compare-and-update; an administrator cannot demote their own account.
3. `removeUser` requires a current administrator and a privileged repository; an administrator cannot delete their own account.
4. `changePassword` remains available for ordinary users only after reauthentication and compare-and-update, with no privileged repository required.
5. The repository layer still exposes write primitives to code holding its reference and database credentials. The application service gate is defense in depth; it is not isolation against arbitrary trusted in-process code.

## Operational gates (unresolved)

- Validate separate migration principal, ordinary runtime principal, and account-administration principal with actual target-database grants.
- Prevent ordinary runtime credentials from updating account `role`, deleting administrator accounts or altering the bootstrap marker; decide how customer INSERT and password rotation should be constrained at database level (e.g., dedicated audited database routines).
- Design a privileged operator CLI/service, credentials custody, audit trail, approval workflow and emergency access/rotation.
- Prove the first-admin/last-admin recovery rule under concurrent actions, lost credentials, partial outages and backup restores. Self-delete/self-demotion prevention does **not** guarantee safety against another privileged operator or raw database mutation.
- Test real PostgreSQL dialect, rollback behavior, restricted SQL errors and backup/restore; H2 compatibility mode is not sufficient.
- Rehearse merge order of stacked drafts, post-merge master CI and release rollback before any production change.

**No database privileges, accounts, credentials, deployment or GitHub billing entitlement were changed by this PR.**
