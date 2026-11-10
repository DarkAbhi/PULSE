# PULSE documentation

Start with the [root README](../README.md) for project structure, development/production setup, application URLs and command tables.

## Operating and contributing guides

| Guide | Topic |
| --- | --- |
| [Configuration and access](guides/configuration.md) | PostgreSQL, sessions, network access, optional Gemini/S3/Cloudflare/MCP settings and startup troubleshooting. |
| [Monitoring and logs](guides/monitoring.md) | Grafana dashboards, Tailscale, metrics, Loki/Alloy and troubleshooting. |
| [Testing and CI](guides/testing.md) | Local suites, reports, CI coverage, disposable databases and code generation. |
| [Pre-commit checks](guides/pre-commit.md) | Installation, formatting, secret scanning and commit-time tests. |
| [Development seed data](guides/development-seed.md) | Demo records, export, restore and verification. |
| [Builds and releases](guides/builds-and-releases.md) | Build caches, disk usage and shared version metadata. |
| [Android releases](guides/android-release.md) | Signing, GitHub release workflow and Telegram delivery. |
| [Maintenance commands](guides/maintenance-commands.md) | Catalog generation and CI device helpers. |

## Development records

The [development-history folder](development-history/README.md) consolidates the earlier shared and Android documentation, screenshots, plans, review reports and design evidence. Browse its [timeline](development-history/timeline.md) in date order or its category index by topic. Historical test counts and plans describe the recorded work; current source and the operating guides take precedence.

## Documentation conventions

- Keep checkout-to-running instructions, URLs and the command inventory in the root README.
- Put reusable operating/contributor instructions in `guides/`, one subject per file.
- Put implementation rationale, plans, AI-assisted build notes, audits and captured evidence in `development-history/`; keep images alongside their record and add a category/timeline entry.
- Record the date, scope, decision, verification and remaining uncertainty in new history entries. Preserve earlier findings rather than rewriting them as current guarantees.
- Use repository-relative Markdown links and update inbound links whenever moving a document.

## README organization references

The refactor draws on [Immich's README](https://github.com/immich-app/immich/blob/main/README.md) and its separate [developer setup](https://docs.immich.app/developer/setup/), plus the explicit getting-started and prerequisite sections in [Cal.com's community edition README](https://github.com/calcom/cal.diy/blob/main/README.md). PULSE keeps its complete setup and root command inventory on the front page, with detailed operations and development records linked here. These are organization references, not dependencies or copied project instructions.
