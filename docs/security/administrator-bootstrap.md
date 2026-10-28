# One-time administrator bootstrap (JDBC deployments)

Status: **draft security design — no production deployment is authorized**.

## Trust boundary

The normal dealership menu supports only unauthenticated **CUSTOMER** signup.
Employee/admin provisioning and deletion require a previously authenticated
administrator. The first administrator is provisioned through a **separate,
offline, operator-only Java CLI**; it is not reachable from the customer UI,
public application registration command, or network services.

A privileged operating-system operator must control the database credentials,
host filesystem permissions and terminal session. Do not grant untrusted
programs direct write access to the JDBC `accounts` table.

## Preparation

1. Stop the application and take a tested database backup.
2. Use durable JDBC storage (e.g. access-restricted H2 file database or a
   managed database with equivalent transaction and unique-key semantics).
   Never use `jdbc:h2:mem:` for the bootstrap CLI.
3. Ensure the bootstrap operator has an interactive local terminal.
4. Configure `dealership.persistence=jdbc`, `dealership.jdbc.url`,
   `dealership.jdbc.user`, and `dealership.jdbc.password` using your
   controlled runtime configuration. **Do not put database secrets or the
   initial administrator password into source code, repository files,
   command arguments, tickets, or CI logs.** The existing application
   configuration consumes Java system properties; arrange secure delivery
   of those properties in the deployment environment.
5. Compile and build the application with Java 11+ and the existing Maven
   project. Launch the dedicated
   `com.revature.application.AdministratorBootstrapCli` main class with
   `--bootstrap-admin` as its **only** argument and a classpath that includes
   the built classes and H2 JDBC dependency. Do not invoke the ordinary
   `CarDealership` application menu for this operation.
6. Enter a unique admin username and a strong password (minimum 12
   characters) when prompted, then confirm it. The CLI does not echo
   passwords and clears the mutable input arrays.

## One-time enforcement

Schema migration **v4** creates `administrator_bootstrap_state` with a
primary-keyed singleton row. The JDBC bootstrap service:

- Rejects weak or missing credentials, a previously completed bootstrap,
  and any existing administrator.
- Hashes the password with the application's PBKDF2 password hasher.
- Inserts the administrator into `accounts` and the singleton audit marker
  **inside one transaction**. A unique key prevents two competing successful
  initializations.
- Rolls back both inserts when either step fails.
- Refuses a second bootstrap after restart **even if the original
  administrator account was subsequently removed**.

If the last administrator becomes unavailable, follow a separately approved
database recovery/change-control procedure. Do **not** delete the audit marker,
invent default credentials or expose an online "reset admin" route.

## Verification and release hold

CI tests cover file-backed reopen, hashed credential authentication, duplicate
attempts, invalid input, conflict rollback, concurrent initialization, and
persisted completion after account deletion. The CLI intentionally requires
`System.console()` and cannot perform the bootstrap in headless CI.

This increment does **not** certify the full authorization architecture.
The subsequent draft verified-principal authorization work (PR #32) rejects
forged and mutated `User` identities at the application command/query
boundary, but in-process direct repository capabilities and actual database
role separation remain under review. Actual production/operator execution,
database backups, rollout/rollback rehearsal, and security sign-off remain
outstanding. Keep the stacked pull requests draft.
