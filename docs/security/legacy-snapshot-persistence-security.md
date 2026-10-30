# Legacy file-snapshot security and provenance

**Status: Draft design; issue #99, stacked PR #102. Not a production-security certification.**

## Trust model

- The interactive console administrator menu is **not** the security boundary. The app-specific persistence port, save/load commands, context adapter, and context data-store methods must all receive the **same currently verified administrator actor**; access is rechecked before I/O and before live-state activation.
- The previous no-actor `CarDealership.loadData()`/`saveData()`, `ConsoleCommandHandler.loadData()`/`saveData()` and command `execute(filename)` paths are **deliberately denied**. Call `loadData(User)` or `saveData(User)` only with a credential-authenticated actor attached to this context.
- An identity record, role field, or copied account UUID alone is **not authentication**. The previously verified session must still match a persisted account generation (see #98), including after role/credential changes.
- `ContextPersistenceAdapter` and `DealershipApplicationContext` both enforce `MANAGE_PERSISTENCE` and accept only the legacy `CarDealer.dat` path. The low-level `DAOService` is a trusted local file-format component, not a public application service; the former public `getDataStore()` escape hatch fails closed.
- The JDBC-connected runtime continues to prohibit file snapshots even to the admin role; such systems persist their state through database transactions. No live DB migration is part of this PR.

## Legacy serialization compatibility and limits

`DAOService` preserves the old serialized snapshot classes and serialVersionUIDs, but Java `ObjectInputStream` must be treated as a **potentially unsafe legacy compatibility format**. The reader now installs an **ObjectInputFilter** before `readObject()`, with an explicit snapshot/domain/JDK collection-time allowlist and object-graph limits: depth 48, 20,000 references, 100,000 array elements and 8 MiB serialized size. The reader checks for bounded, regular, nonsymlink source files before opening the stream. Unknown serializable objects fail closed before being accepted as `ApplicationStateSnapshot`.

**Important:** filtering is risk reduction, **not a guarantee that arbitrary untrusted serialized bytes are safe**. Serialized legacy files must be sourced from trusted archival evidence. Future migration should use a separately approved offline transformer from historical Java serialization to a signed, versioned, data-only format with an explicit schema (e.g., validated JSON/CBOR) and provenance checks. Do not rewrite archival originals.

## Security and recovery acceptance

1. Deny unauthenticated, customer, forged admin, deleted/demoted admin, null actor, legacy no-actor entry point and direct adapter calls **before filesystem access or live-state replacement**.
2. Allow genuine administrator to save/load a verified local file at the authorized name; verify no unauthenticated caller can access arbitrary paths.
3. Preserve the existing genuine snapshot round-trip; reject unknown Java type, oversized input, symlink and malformed content while leaving the active state unchanged.
4. Review path race/TOCTOU, file owner/mode, trusted working directory, archive backup/versioning, atomic rename, write durability, authenticated integrity signature, and complete recovery behavior. These are **not yet proven** by the bounded serialization patch alone.
5. Run full Java 11/17 tests and PostgreSQL 16 security tests on exact PR head, and independently review bypass routes through public context/services, Java reflection, file-system privileges and direct DBA access. In-process arbitrary code execution remains out of scope.
6. Keep snapshot evidence and initial legacy source immutable; use new draft branches only. Do not deploy, merge, rotate production credentials or change production databases based on this documentation.

References: Oracle JDK 11 ObjectInputFilter and serialization filtering docs; OWASP Web Security Testing Guide, Insecure Deserialization; issue #99 and integration issue #85.
