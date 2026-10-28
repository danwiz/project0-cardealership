# Verified principal authorization boundary

Status: **draft, stacked on administrator-bootstrap PR #31; not deployed**.

## Threat and defense

A mutable `User` record is a data transfer object, **not** a credential or an
authorization capability. Prior application commands called role checks on
arbitrary `User` instances; a fabricated `User` with `ADMINISTRATOR` role
could pass without a successful login.

The application context now configures `RoleAuthorizationService` in
verified-session mode:

1. The identity adapter checks the supplied credentials against the
   repository's persisted PBKDF2 hash and checks the requested role.
2. On successful authentication, the authorization service independently
   verifies credentials and registers the **exact returned account object**
   in a private identity-based session registry.
3. Every protected command/query checks that registry and re-reads the
   current account's persisted username, role, and credential hash.
4. Mutating or copying the account DTO cannot grant a new permission.
   Removing an account or rotating its password invalidates old sessions.
5. Explicit logout/failed authentication and snapshot replacement revoke
   the active session registry.

The public no-arg authorization service remains a **role policy evaluator**
for isolated legacy code and tests. Do not inject that policy-only instance
into a production application adapter outside the secured composition root.

## Boundaries and remaining work

- The application is a trusted in-process **console** architecture, not an
  Internet-facing auth service; do **not** expose direct repository getters,
  the public `User` DTO, or internal adapter methods through network APIs.
- This is a transitional identity-reference implementation. For a multi-user
  remote service, replace it with explicitly scoped opaque sessions/tokens,
  expiration/revocation, immutable principals, centralized auth middleware,
  audit trails, rate-limits and transactional DB authorization checks.
- The direct `ConfiguredDealershipApplication.getInventory()` and similar
  lower-level repository getters are intentionally trusted infrastructure
  capabilities. They must not be accessible to untrusted callers.
- Role-only standalone injected test ports are separately outside the real
  context's authentication guarantee.
- Acquisition contract lookups verify an authenticated session before checking whether a contract identifier exists.
- Secure first-admin deployment, operator/DB permissions, and broader
  session lifecycle review still require signoff. The original issue #29
  remains open pending these and post-merge validation.
