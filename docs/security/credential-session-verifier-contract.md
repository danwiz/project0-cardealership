# Authenticated-session and account-binding policy — provider-neutral seam

**Status: STAGED ONLY / NOT A TOKEN VERIFIER / NOT DEPLOYED.** This increment
extends the isolated credential-service fixture; it does not create an IdP,
OAuth/OIDC client registration, HTTP endpoint, session cookie, key store,
network listener, production identity, new account permission or deployment.
Production **G01 and AUTH-01 remain NOT RUN / NO-GO**.

## Separate trust responsibilities

1. `TrustedCredentialRequestHandler` first charges the separately configured
   **trusted ingress quota**, even for absent/invalid bearer values. A gateway
   must authenticate the transport identity separately; client-submitted
   forwarded headers and usernames are not valid identity sources.
2. `TrustedSessionPrincipalVerifier.ValidatedSessionSource` must obtain the
   bearer credential over an authenticated channel and, **before returning any
   evidence**, validate its authenticity. For a JWT access token, this means
   a profile-appropriate token type, permitted algorithm, trusted keys/signature,
   matching issuer, intended resource audience, valid times and revocation
   policy. For opaque tokens, use authenticated authoritative introspection,
   confirm token activity and enforce issuer/audience/subject binding. Fail
   closed on network/key/introspection uncertainty or stale cached evidence.
3. The new `TrustedSessionPrincipalVerifier` then enforces **additional Java
   policy** on the returned metadata: active flag, configured exact issuer and
   audience, allowlisted assurance context, `nbf`, strict `exp`, valid
   `auth_time` and maximum authentication age. These are defense-in-depth
   checks on **already validated** evidence, not signature verification.
4. `SubjectAccountBinding.resolveCurrentUsername(issuer,subject)` must perform
   a fresh authoritative lookup of the immutable authenticated subject to a
   uniquely bound, enabled account. Fail on missing, disabled, ambiguous or
   stale bindings. **Never** take the write target username from a bearer
   claim, user input, display name, request header or caller-controlled role.
5. Only after these gates pass does the handler charge the **verified-subject
   quota** and re-check the persisted current password before the isolated
   writer attempts a compare-and-swap hash rotation.

## Important boundaries

- `SessionEvidence` is a Java metadata holder. A caller can construct it in
  tests; its existence does not prove an authentic bearer, revocation check,
  source integrity or provider uptime. Only a separately reviewed, trusted
  implementation of `ValidatedSessionSource` may supply it in deployment.
- Accepted assurance-context values have provider-specific meaning; they
  must be explicitly reviewed, tied to real MFA/step-up claims and bounded
  to a recent authentication before provisioning. A string such as
  `step-up` in a unit test is **not** proof of MFA.
- The session source must define token substitution, replay, proof-of-possession
  (if used), logout/revocation propagation, key rotation, discovery metadata,
  introspection authentication, cache TTL and token binding. Authentication
  freshness alone does not stop a stolen valid bearer; the current-password
  check and credential recovery policy are separate defenses.
- The account binding source must not be supplied by the ordinary dealership
  application or an account-mutable field. Require least-privilege reads,
  an explicit account-disabled policy and a migration for stable subject IDs.
  Do not implicitly create users or grant staff/admin roles from token claims.
- Failures return generic errors. Never log or export raw bearer credentials,
  passwords, signing keys, introspection secrets, provider error details or
  personal account identifiers in GitHub CI logs.

## Fixture tests and independent acceptance

Seven tests simulate accepted, inactive, wrong-issuer, wrong-audience,
insufficient assurance, expired/not-yet-valid/stale/future authentication,
missing account binding, source/binding/clock outage, invalid config and
handler-to-database account targeting. The `ValidatedSessionSource` in the
tests is a **fake function**, not a JWS/JWT implementation or trusted IdP.

Production acceptance requires an independent reviewer to run
[AUTH-01](credential-security-production-integration-plan.md) against a
**chosen** staging identity provider with real signed or introspected tokens,
revoked sessions, account-disable and mapping changes, key rotation,
clock skew, MFA freshness, deliberately substituted token types/audiences,
malicious client-supplied usernames and network/introspection failure.
Record exact artifact/provider/configuration digest, redacted results,
reviewer and timestamp in the
[release evidence register](credential-security-release-evidence-register.md).
Fixture CI must not be marked as production G01 PASS.

Authoritative references:
[RFC 9068 JWT access-token profile](https://www.rfc-editor.org/rfc/rfc9068.html),
[RFC 7662 token introspection](https://www.rfc-editor.org/rfc/rfc7662.html),
[OpenID Connect Core authentication context and time](https://openid.net/specs/openid-connect-core-1_0.html).

Keep issue #29 **open** and all staged security PRs **draft/unmerged**.
No paid GitHub Code Security entitlement, production DB operation, credential,
cloud resource, network routing or deployment is authorized here.
