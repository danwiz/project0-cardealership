# Trusted first-administrator bootstrap — security design (issue #80)

Status: **design only; not implemented; release gate open**.

## Threat model

Public account registration is untrusted and must create only CUSTOMER accounts. A caller-provided role, mutable User object, environment variable alone, or hard-coded password cannot confer administrative authority. The current in-memory account repository loses users at restart; it is not a durable identity store. The first administrator cannot safely be created by ordinary application menus or by treating an empty in-memory repository as proof of a fresh deployment.

## Proposed bootstrap protocol

1. Keep bootstrap **disabled by default**. Require a dedicated operator-only command, outside the interactive customer menu, running in a trusted local execution environment with restrictive OS permissions.
2. Require an explicit, short-lived operator authorization token from an out-of-band secret channel. Read secrets without printing them or placing them in command-line arguments, URLs, repository files, or workflow logs. Apply minimum password policy and PBKDF2 password hashing before storage.
3. Use a **durable, transactional** identity store with a singleton bootstrap marker and unique username constraint. In one database transaction, lock the bootstrap marker, verify no prior administrator bootstrap, create exactly one administrator, and commit the consumed marker. Fail closed if the store is unavailable, marker is ambiguous, or concurrency conflicts occur.
4. Produce a non-secret audit event recording operator identity, timestamp, target username, operation outcome, and credential-rotation requirement. Never log plaintext credentials or token material.
5. Disable the bootstrap entry point after success; subsequent staff provisioning requires an authenticated, persisted administrator with MANAGE_USERS. Password rotation and administrator revocation must not reactivate first-run bootstrap.
6. For the current in-memory demo, **do not expose a production bootstrap path**. Use test fixtures for administrator accounts; document that a restart loses identity state. No default administrator credentials.

## Acceptance tests before implementation is eligible to merge

- Public CUSTOMER signup cannot request or forge EMPLOYEE/ADMINISTRATOR and cannot invoke bootstrap.
- Bootstrap is inaccessible when disabled; invalid or expired operator authorization fails without creating an account.
- Two simultaneous bootstrap attempts create at most one administrator; a second attempt after success is rejected.
- Restart does not reset the durable consumed marker; revocation or deletion does not silently permit another first-admin bootstrap.
- Storage failure rolls back both account and marker atomically; no partial privileged identity remains.
- Administrator creation, role elevation, revocation and password rotation are audited without secrets.
- Java 11/17 unit and integration checks pass; deployed configuration, file permissions and operational recovery are reviewed separately.

## Release decision

Do not claim production administrator onboarding for PR #78/#82. Implement this as a separately reviewed change once a durable identity-store architecture and operator trust boundary are selected. This design intentionally creates **no** administrator account and changes **no** registration permissions.
