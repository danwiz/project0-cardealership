# Renewable writer secret lease policy — provider-neutral, non-deployed

**Status: STAGED POLICY ONLY.** This introduces no real secret manager,
workload identity, database role, production credential, network call or
connection pool. The separate writer and release gate **G04 remain NOT RUN /
NO-GO**. Do not confuse a test-supplied metadata snapshot with a valid
secret-manager attestation.

## Connection-time policy

`JdbcDatabase(url, JdbcCredentialSource)` already requests a new credential
before **each** new JDBC connection, with no cached fallback, and zeroes the
mutable supplied buffer in a `CredentialLease` even on driver failure.

The new `RenewableJdbcCredentialSource` implements that existing interface.
Each acquisition calls a `SecretLeaseProvider` and requires:

1. The expected **exact, allowlisted writer SQL role** (never a migrated owner
   or arbitrary elevated username) and a nonempty owned password array.
2. A non-revoked lease, positive generation, start time no later than current
   time, and strict expiry **after** current time.
3. A bounded *remaining* lifetime, using a trusted clock. Expired, far-future,
   not-yet-valid or revoked material is rejected.
4. A generation at least as recent as the last observed one. An optional
   **initial floor** lets an independently recovered version protect restart,
   but an in-memory floor **does not** persist or coordinate across replicas.
5. Fail-closed behavior on provider outage, null response, unexpected role,
   bad metadata, clock outage or ambiguous lease status. No previously
   successful secret is retained or silently reused.

Rejected material is zeroed. Accepted material is transferred to the JDBC
credential lease, which zeroes the original character array on close. JDBC
drivers require an unerasable Java `String` copy, so this narrows exposure
without promising full secret-memory erasure. The source serializes acquisition
per JVM to fence local generation rollback. It neither restarts live
connections nor automatically revokes a database login.

## Provider adapter responsibilities (still missing)

- Independently authenticate the isolated writer workload using an approved
  workload identity and a protected channel. An adapter must obtain the
  current active version and revocation state from an authoritative provider,
  not forge “active” metadata from a JSON field controlled by the application.
- Guarantee a **durable, cross-instance monotonic generation floor** and
  prevent rollback during provider failover, restoration or concurrent
  rotation. Never reset the floor to zero on restart without independent
  reconciliation. Do not permit a provider to accept older leased secrets
  simply because local Java counters were reset.
- Deliver a **fresh, exclusively owned** `char[]` per request. Never reuse
  arrays, expose the secret in logs/traces, or allow unrelated application
  roles or CI builds to read or rotate the writer credential.
- Coordinate database login creation/rotation and new-connection validation
  against lease publication. Track existing pooled connections and drain them
  under an approved grace/revocation window; changing the password does not
  automatically guarantee established connections have been terminated.
- Require audit records, owner/on-call alarms, revocation evidence and
  recovery procedures for source outage, lease expiry, bad database login,
  too-old generations, provider rollback and post-rotation connection storm.
  Do not restore an old shared application SQL login to recover availability.
- Define approved maximum lifetime, clock-skew policy, service account
  binding, staging isolation, provider cost and infrastructure approvals.
  This PR picks none of them.

## Fixture evidence versus production acceptance

Six unit tests exercise successful transfer/zeroing, generation rollback,
revoked/expired/future/oversized/role-swapped material, backend outage,
sanitized exceptions and invalid configuration. These **do not** prove a
secret provider's currentness, cross-region availability, persistent version
fencing or safe draining of existing connections. The prior disposable
PostgreSQL integration fixture already checks old-password rejection after
role rotation and new-connection success with a refreshed source.

Complete [SECRET-01](credential-security-production-integration-plan.md)
in isolated protected staging with the selected real provider, validated
workload identity, two live writer replicas, revoked/rotated DB roles,
lease/clock failures, forced provider rollback and live-connection draining.
Record independent approvals and redacted artifacts in the
[release evidence register](credential-security-release-evidence-register.md).
The release remains **NO-GO** until every relevant production gate closes.

Reference: [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html).

Issue #29 stays **open** and all security PRs remain **draft/unmerged**. No
production secret rotation, SQL grant change, cloud resource or paid GitHub
Code Security feature is part of this change.
