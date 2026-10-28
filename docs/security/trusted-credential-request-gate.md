# Trusted credential request gate and rate limit — non-deployed prototype

**Draft PR #45 (2026-10-28), stacked on #44.** Implements a code-level entry-point seam for a separately trusted credential service; it **does not** provide an HTTP endpoint, a real signed token verifier, distributed throttle, production secrets or deployment.

## Request flow

1. A trusted `PrincipalVerifier` implementation must authenticate an opaque bearer credential and return a stable subject plus the bound account username. The production adapter must independently validate the issuer, signature/keys, audience, expiry, token revocation and required assurance level/MFA; **no such adapter is included in this PR**.
2. `TrustedCredentialRequestHandler.rotate` takes only a bearer credential, current password and replacement password. It intentionally **has no target username parameter**; the target comes only from the verified identity.
3. Before PBKDF2 work, `CredentialRequestRateLimiter` consumes a quota keyed to the stable verified subject, regardless of success or failure. The limiter is synchronized, uses a fixed window, limits the number of tracked subjects, fails closed at capacity, and does not reset on a backward wall-clock jump.
4. `TrustedCredentialRotationService` reauthenticates against the persisted hash and submits a compare-and-swap update using the separately held writer repository. PR #44's PostgreSQL audit-trigger fixture covers the DB writer's committed updates.

## Follow-up: pre-verification ingress quota (staged, not deployed)

`TrustedCredentialRequestHandler` now requires a separately injected `TrustedIngressIdentityResolver` and a second, independent `CredentialRequestRateLimiter`. Each attempted rotation consumes an ingress quota **before** its bearer token is inspected, including blank, invalid and forged tokens. If ingress identity cannot be established, it fails closed without calling the verifier or writer. The existing verified-subject quota remains in place after token verification. There is deliberately no legacy constructor omitting ingress enforcement.

The resolver's key is **trusted server-side transport context**, never a client-supplied username, arbitrary `X-Forwarded-For` header or unverified token field. Tests use a fixture resolver. No proxy trust configuration, network listener, session binding, distributed atomic limiter or real ingress identity implementation is delivered here. A hostile network client must never be able to choose or rotate their own rate-limit key. Both in-memory limiters remain restartable, single-process prototypes; shared-state and gateway-wide protection are release blockers.

## Test coverage

- Verified caller identity determines the account being changed. The client has no way to substitute another username.
- Invalid or missing tokens fail before any writer invocation (using a **test-only fake verifier**, not a signed token).
- Wrong passwords consume attempts and reaching the quota blocks even a correct password until the time window expires.
- Cardinality is bounded; expired subjects can be evicted and backwards time does not open new quota.
- Concurrent threads cannot exceed the per-subject admission budget.
- Blank/forged bearer attempts consume a separate ingress budget before verification, and exhaustion denies an otherwise valid bearer. Missing trusted ingress identity prevents verification.

## Release blockers / limitations

- The supplied verifier in tests is a stub mapping strings to principals. **Passing tests do not validate OAuth/OIDC, JWT signatures, key rotation, revocation, audience, device assurance, MFA or replay defense.** Do not expose this class on a live network until a reviewed implementation exists.
- This Java limiter stores counters in-process. They disappear on restart and are not shared across replicas. Fixed windows allow bursts at boundaries, the bounded subject map may deny legitimate accounts under cardinality pressure, and invalid tokens are not subject to this post-verification limiter. Production needs atomic shared-state account/actor throttling and ingress-level IP/device/session abuse protection, with DoS safeguards.
- A real isolated service needs mutual TLS or private authenticated transport, CSRF/session hardening as applicable, credential-writer secrets from a managed secret store, allowlisted network paths, audit export, alerts, password-reset/recovery and emergency response.
- Java Strings containing plaintext password/token cannot be reliably zeroed; do not log them or persist them. Assess char-array and secret-binding APIs during production hardening.
- The existing dealership application continues to use its shared JDBC identity and its old credential-change path. This PR **does not enable or replace** runtime routes.

References: [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html) and [Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

Do not merge/deploy this prototype, change real DB grants or activate paid private-ERM GitHub Code Security. Keep issue #29 open.
