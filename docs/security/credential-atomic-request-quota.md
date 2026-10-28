# Credential request quota — shared atomic provider seam, not deployed

**Draft increment after #56.** This adds a provider-neutral Java contract and
test-only fake. No cache/DB/identity provider, managed gateway, HTTP listener,
new credential, network call, production rate limit or deployment is installed.
Production gate **G02 remains NOT RUN / NO-GO** in the
[release evidence register](credential-security-release-evidence-register.md).

## What changed

- `TrustedCredentialRequestHandler` now accepts `CredentialRequestQuota` for
  both the **pre-verification ingress** and **verified-subject** decisions.
  Existing `CredentialRequestRateLimiter` implements the interface and remains
  an explicitly non-distributed, in-memory fixture.
- `AtomicCredentialRequestQuota` delegates to an `AtomicQuotaStore` interface.
  The store must atomically decide and charge an allowed attempt. `false`
  denies access. Exceptions deny with a generic unavailable error; there is
  no local bypass, permissive fallback or retry on ambiguous outcomes.
- The caller supplies only keys established by **trusted gateway transport**
  or a **validated principal**. This class does not validate a forwarded
  header, authenticate an OAuth token, or generate trusted keys by itself.
  The incoming bearer must **never** become a quota key or appear in logs.

## Provider adapter requirements

1. Authenticate the isolated writer service to the backend on a private,
   protected channel; grant only atomic charge privileges for its own
   independently named namespaces. Use distinct namespaces for ingress and
   principal throttles; do not mix keys from different trust levels.
2. Make `tryAcquire(namespace, trustedKey, maxAttempts, windowMillis)` a
   **single linearizable decision-and-increment** for each key across all
   service replicas. No client-side read/modify/write, eventual-consistency
   counter, duplicate local cache or `GET` then `INCR` sequence is sufficient.
3. Use an authoritative backend clock, stable window semantics across
   restarts and bounded memory/cardinality. Document exact burst limits at
   window edges and protect the backend itself from key-exhaustion attacks.
4. Preserve charged quotas across writer restarts and failover within the
   configured window. On backend outage, timeout, overload, unknown namespace,
   conflicting policy or indeterminate write outcome, deny new rotations.
5. Bind the pre-authentication key at the actual trusted ingress from a
   documented gateway/proxy trust boundary. Untrusted request headers and
   freely chosen client identifiers must not select or reset that key.
   Apply independent edge-wide network controls before this Java gate.
6. After identity verification, key subject quota by an immutable verified
   subject/account mapping, not a mutable username supplied by a request.
   Apply explicit cross-replica and network-abuse policy, safe exception
   messages, monitoring and approved quota dimensions without exposing PII.
7. Record de-identified denial/outage metrics and make backend availability
   independently observable. Define incident response and recovery policies;
   do **not** disable a failed limiter to restore availability.

The contract deliberately has no backend-specific client library. A real
atomic-store adapter and gateway identity resolver require separate deployment
and threat review. Passing this PR's shared **in-process fake** cannot prove
real distributed durability, persistence, cross-region behavior, identity
authenticity or application/network integration.

## Non-production verification and remaining acceptance

Unit tests cover shared budget across two adapter objects and recreation,
independent ingress/subject namespaces, concurrent charges, backend rejection,
outage sanitization, input validation and two request-handler replicas.

Before G02 could pass, execute
[LIMIT-01](credential-security-production-integration-plan.md) with the chosen
real provider in isolated staging: burst and sustained attempts across
replicas, restart, forged proxy headers, backend failure, unknown keys,
cardinality exhaustion, window boundaries, clock/failover effects and
event/alert traces. Capture independently witnessed evidence and configuration
digest in the register. Authentication gate **G01** separately requires a real
issuer/signature/audience/expiry/revocation/assurance verifier; test verifier
strings remain fakes, and no production identity is trusted by this PR.

References:
[OWASP REST Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html),
[OWASP REST Assessment](https://cheatsheetseries.owasp.org/cheatsheets/REST_Assessment_Cheat_Sheet.html),
[RFC 7662 token introspection](https://www.rfc-editor.org/rfc/rfc7662.html).

**Keep security issue #29 open and this and preceding PRs draft/unmerged.**
No production credentials, DB grants, replication configuration, deployment,
paid GitHub entitlement or cloud service changes belong to this increment.
