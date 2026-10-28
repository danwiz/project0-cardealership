# Account administration channels and recovery invariants

**Draft design / test checkpoint — 2026-10-28.** Tracks dealership security issue #29 and stacked PRs #37–#40. This is **not** approval to deploy to a real database.

## Authority separation

| Execution boundary | Identity/account access | Privileged account mutation |
|---|---|---|
| Normal composed JDBC runtime | Authenticate users, allow customer-only registration and verified self-service password rotation through ordinary account repository | **No administrative repository wired**: deny staff/admin provisioning, role reassignment, and account deletion |
| Separately authorized account operator | Verify credentials against current persisted account state | PR #39 adds a separate offline interactive CLI that injects distinct identity-reader and administrative-writer JDBC repositories and requires persisted ADMINISTRATOR permission and external approval |
| Standalone in-memory characterization/testing | Local fixture account repository | Legacy in-memory admin capability remains available to test workflows |
| Raw infrastructure with DB credentials | Can directly open JDBC connections | Still a trusted boundary; **not** protected solely by these Java service checks |

The normal JDBC application path (`DealershipCompositionRoot.createJdbc`) intentionally passes `null` for administrative repository access to the context. The context passes that fail-closed configuration to every login-service variant (user/customer/employee/admin). A snapshot replacement cannot silently re-enable privileged operations for a JDBC context.

**PR #39 operator prototype:** `AccountAdministrationCli` requires a local console, distinct identity-reader/operator principal names, password prompts, an external change-ticket reference, and exact deletion confirmation. Its H2-file test exercises different SQL grants. Production operator privileges, secure secret custody, immutable auditing, PostgreSQL integration and approved operating procedures **remain release tasks**. A separate Java process alone is not database-level isolation; elevated credentials must never enter the normal application runtime.

## Service safeguards

1. `UserLoginService.provisionUser` verifies a current persisted administrator and uses only the explicit administrative repository for privileged user creation.
2. `changeRole` requires a current administrator and uses compare-and-update; an administrator cannot demote their own account.
3. `removeUser` requires a current administrator and a privileged repository; an administrator cannot delete their own account.
4. `changePassword` remains available for ordinary users only after reauthentication and compare-and-update, with no privileged repository required.
5. **PR #38:** JDBC role reduction and account deletion lock administrator rows in `SERIALIZABLE` transactions and refuse removing the persisted final administrator through supported JDBC repository calls. This does **not** protect direct SQL or other repositories.
6. The repository layer still exposes write primitives to code holding its reference and database credentials. The application service gate is defense in depth; it is not isolation against arbitrary trusted in-process code.

## Operational gates (unresolved)

- Validate separate migration principal, ordinary runtime principal, and account-administration principal with actual target-database grants.
- Prevent ordinary runtime credentials from updating account `role`, deleting administrator accounts or altering the bootstrap marker; decide how customer INSERT and password rotation should be constrained at database level (e.g., dedicated audited database routines).
- Verify the PR #39 CLI against the target database, design secure credentials custody and immutable auditing, and approve the operator workflow, emergency access and rotation; H2 tests are not production certification.
- Verify the JDBC repository's last-admin transaction guard against **real PostgreSQL concurrency and grants**, then rehearse lost-credential, partial-outage and backup/restore recovery. PR #38's H2 tests and row locks do **not** guarantee safety against privileged raw SQL, direct table writes, or alternate repository implementations.
- Test real PostgreSQL dialect, rollback behavior, restricted SQL errors and backup/restore; H2 compatibility mode is not sufficient.
- Rehearse merge order of stacked drafts, post-merge master CI and release rollback before any production change.

See `docs/security/account-operator-cli.md` and `docs/security/postgresql-ci-grant-verification.md` for operator privilege tests and remaining release gates.

**No live database grants, production accounts, credentials, deployment or GitHub billing entitlement were changed by this PR.**
