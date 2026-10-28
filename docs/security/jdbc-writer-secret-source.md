# Rotating JDBC writer credential source — non-deployed proof

This staged code supports a **separately isolated** credential-writer process. It is not a production secret manager, network service, deployment configuration or replacement for existing application JDBC credentials.

## Contract

`JdbcDatabase(url, JdbcCredentialSource)` acquires a new `CredentialLease` before **each new connection**. The credential source must return a fresh mutable password array and is responsible for obtaining the correct username and secret from a trusted secret store. A null source result or an unavailable secret fails closed; there is no cached fallback or environment-property substitution. The lease zeroes its password array when connection acquisition finishes, even if JDBC authentication fails.

The JDBC driver API requires a Java `String` password, which **cannot be reliably erased from JVM memory**. Zeroing the original array narrows exposure, not guarantees zero residual copies. Existing `JdbcDatabase(url, username, password)` callers are left unchanged for legacy compatibility; they retain a fixed String credential and must **not** be used as the real credential-writer service configuration.

## CI-only rotation check

The disposable PostgreSQL fixture creates an isolated SQL login. It proves that:

1. A connection succeeds using an initial secret, and the supplied mutable buffer is cleared.
2. After the database owner rotates the role password, the old credential fails authentication (`28P01`) and its buffer is still cleared.
3. Updating the injected source makes the **next** connection succeed without rebuilding `JdbcDatabase`.
4. If the source is unavailable, connection acquisition fails closed, rather than using the former password.

All test identities and secrets are CI-only; the test is skipped outside the exact disposable PostgreSQL environment.

## Production blockers

- Choose and review a managed secret store, authentication mechanism (prefer short-lived workload identity), rotation automation, version/lease strategy and secret availability policy. **No store adapter is supplied here.**
- Deploy the credential writer in its own protected process or service, with restricted outbound DB access, mTLS/private transport and independent identity verification. Merely injecting a source into the existing dealership JVM does not isolate its writer privilege.
- Plan coordinated PostgreSQL credential rollover, existing pooled connections, draining/expiry, emergency revocation, retries/backoff, audit/alerts and outage recovery. This implementation opens individual connections, not a production connection pool.
- Do not put production credentials in GitHub workflow variables, repository files or logs. This proof does not create/rotate production users, change database grants, deploy or enable paid security products.

Keep draft PR and security issue #29 open until independent review and integration are completed.
