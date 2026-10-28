# Production security release evidence register — blank template

**Purpose:** Trace each provider-neutral integration gate to independently
verified evidence. This template is **not an approval**, nor evidence that any
production integration test has run. Source: [integration plan](credential-security-production-integration-plan.md).

## Environment and decision metadata

| Field | Required value |
| --- | --- |
| Repository and reviewed commit | **UNSET** — immutable SHA, not branch name alone |
| Environment ID / isolation proof | **UNSET** — no shared production database or slots for fault injection |
| Change request / approval reference | **UNSET** |
| Application / writer / CDC / archive / checkpoint / outbox / alert owners | **UNSET** |
| Provider and region decisions | **UNSET** — provider-neutral until approved |
| Data classification and retention schedule | **UNSET** — legal/data-owner approval required |
| Recovery targets / backlog / alert SLOs | **UNSET** — do not invent thresholds |
| Independent security reviewer | **UNSET** |
| Test and evidence capture dates | **UNSET** |

## Gate decisions

Valid state values are **NOT RUN**, **PASS**, **FAIL**, **BLOCKED** or
**NOT APPLICABLE** (requires written approval explaining why). PASS requires
evidence linked to exact environment/version, reviewer and timestamp. A CI
fixture may be cited as design context but must **not** be marked as a real
provider PASS.

| Gate | State | Test references | Independent reviewer / evidence link |
| --- | --- | --- | --- |
| G01 Authentication | NOT RUN | AUTH-01 | UNSET |
| G02 Rate limits | NOT RUN | LIMIT-01 | UNSET |
| G03 Writer isolation | NOT RUN | WRITER-01 | UNSET |
| G04 Writer secret lifecycle | NOT RUN | SECRET-01 | UNSET |
| G05 Database audit | NOT RUN | TRIGGER-01 | UNSET |
| G06 CDC completeness | NOT RUN | CDC-01, CDC-02, CDC-03 | UNSET |
| G07 WORM archive | NOT RUN | ARCH-01, ARCH-02 | UNSET |
| G08 Checkpoint/fencing | NOT RUN | CKPT-01 | UNSET |
| G09 Outbox/alerts | NOT RUN | ALERT-01, ALERT-02 | UNSET |
| G10 Privacy/retention | NOT RUN | PRIV-01 | UNSET |
| G11 Availability/DR | NOT RUN | DR-01 | UNSET |
| G12 CI/supply chain | NOT RUN | CI/review provenance | UNSET |
| G13 Threat model | NOT RUN | signed independent threat review | UNSET |
| G14 Release authorization | NOT RUN | signed change decision | UNSET |

## One test execution record (copy per test)

| Field | Value |
| --- | --- |
| Test ID and revision | UNSET |
| Preconditions and isolated environment | UNSET |
| Exact tested artifact SHA and configuration digest | UNSET |
| Fault injection, test commands and operator | UNSET |
| Expected observable assertions | UNSET |
| Actual counters/events/checkpoint and result | UNSET |
| Redacted immutable evidence URI + digest | UNSET |
| Start/end timestamps and system clock checks | UNSET |
| Observed privacy/credential leak check | UNSET |
| Independent reviewer, date and decision | UNSET |
| Open defect/risk waiver reference | UNSET |

## Go / no-go decision

**Default: NO-GO.** A release is blocked by any `NOT RUN`, `FAIL`, `BLOCKED`,
unreviewed `NOT APPLICABLE`, missing independent reviewer, unresolved high-risk
finding, unknown audit coverage or absent rollback/alerting plan.

Final approval must record the named operations owner, data owner, independent
security reviewer, application release owner, artifact SHA, approved window,
rollback/fail-closed strategy, immutable evidence manifest and incident
contacts. Do not include live credentials or customer identifiers in this
register or GitHub issue comments. Changes to any code or configuration after
approval invalidate affected evidence until re-tested and re-approved.

**All fields intentionally remain unset. No production provider has been
selected, deployed or tested.**
