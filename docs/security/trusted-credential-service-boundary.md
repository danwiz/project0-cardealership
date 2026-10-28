# Trusted credential-writer boundary — staged service design

**Draft PR #43 / 2026-10-28, stacked on PR #42.** This is a narrow PostgreSQL privilege + Java service proof. The service is **not** connected to the normal application, an HTTP endpoint, real operator account or external deployment.

## Target architecture

The ordinary application SQL login has only `SELECT` access to the account and schema-history tables in the isolated test, and is explicitly denied direct credential UPDATE. A separate protected service owns a different SQL login granted `SELECT` plus **only** `UPDATE(encoded_credential)` on accounts. It gets no SQL rights for INSERT/DELETE, role changes, schema modifications or bootstrap-state changes.

`TrustedCredentialRotationService` uses two different repository objects:

1. A read-only identity repository to load the stored PBKDF2 hash and verify the caller's supplied current password.
2. A separate credential writer repository to compare-and-swap only the `encoded_credential` column to a newly salted hash.
3. Unknown accounts and wrong current passwords both fail with the same authentication-required error; stale credentials fail without changing the hash.

The real security boundary is the **separately deployed, trusted service process holding the writer SQL credential**. Instantiating this class with separate repositories in the same compromised application JVM does **not** protect that secret. No API, JWT/OIDC verifier, trusted session facility, service deployment, retries, rate limit, MFA, event audit, password reset or incident workflow is implemented here.

## CI privilege proof

A generated isolated schema in PostgreSQL 16 contains two customer records and different database logins:

- `application_reader`: SELECT schema_history/accounts; no INSERT, UPDATE or DELETE.
- `credential_writer`: SELECT schema_history/accounts plus UPDATE of **encoded_credential only**; no INSERT, DELETE, UPDATE(role), DDL or bootstrap-table modification.

Names and secrets are actual CI-only randomized fixture roles rather than the illustrative names above. The test checks SQLSTATE `42501` for unauthorized writes. It also verifies legitimate and invalid password rotations through the restricted writer.

## Critical residual risk and release decision

A person/process that obtains the credential-writer login can still execute direct SQL to reset **any** account's password hash, bypassing the Java service's prior-password check. Column grants limit damage to hashes but cannot provide customer ownership when one privileged writer identity serves all customers. Use a hardened trusted process, private network reachability, least-privilege secrets, audited approvals and rotation/revocation; consider stored routines with validated end-user assertions only if their proof cannot be forged by the same SQL login.

Security review must decide between this trusted process and PR #42's database-per-customer model, including authenticated identity handoff, non-replayable credentials/session, MFA/high-risk changes, rate limiting, audit, TLS, secrets rotation, emergency recovery, credential hashing policy and availability. Do **not** activate live grants or change deployment composition from this fixture.

The ordinary application currently has a shared JDBC writer and a `UserLoginService.changePassword` path. This PR does **not** claim that path is now isolated or safe against a compromised existing runtime SQL login; follow-up integration must remove that privileged capability from the normal application's database role only once a working trusted credential service is deployed and independently reviewed.

Reference: [PostgreSQL GRANT](https://www.postgresql.org/docs/16/sql-grant.html), [PostgreSQL privilege system](https://www.postgresql.org/docs/16/ddl-priv.html).

Keep PR #43 draft, issue #29 open, all production grants unchanged and private ERM paid GitHub Code Security pending.
