# Monitoring and logs

## Grafana over Tailscale

Grafana binds to `127.0.0.1` by default, so it cannot be reached through the
Docker host's Tailscale IP. Pulse's web port binds to all interfaces, which is
why the web app can be reached through Tailscale without additional settings.

On the production Docker host, set `GRAFANA_BIND_ADDRESS` in `.env` to that
host's Tailscale IPv4 address (from `tailscale ip -4`). For example:

```dotenv
GRAFANA_BIND_ADDRESS=100.115.134.90
```

Recreate just Grafana to apply the port binding; a container restart alone
does not apply changed port mappings:

```sh
docker compose --env-file .env --project-name life-prod -f docker-compose.yml --profile prod up -d --no-deps grafana
```

Open `http://100.115.134.90:3101/` from a device on the same tailnet and sign
in with the existing Grafana account. This binds Grafana only to the specified
Tailscale IP; the localhost URL and SSH forwarding above apply when using the
default binding. Tailscale must be running with that IP before Grafana starts,
and tailnet access rules must allow TCP port 3101.

Development keeps its existing localhost binding on port 3103;
`GRAFANA_BIND_ADDRESS` applies only to production.

## Backend and resource metrics

Under **Dashboards → PULSE → PULSE Backends — Go & Rust**,
select **Go backend**, **Rust backend**, or both. Request rate, error
rate, latency, in-flight requests, and PostgreSQL pool connections are shown
separately for each backend. Go runtime and pool acquisition wait panels remain
Go-specific: Rust has no Go runtime, and SQLx does not expose the equivalent
cumulative pool wait counters. Rust HTTP latency measures handler completion;
streaming body transfer time is not included. Health checks are excluded from
Rust HTTP metrics.

Rust exports Prometheus metrics on container port `9091`, separate from its
public API and Cloudflare tunnel. This port has no host publication. Prometheus
uses the `rust-backend-api` network alias in both development and production,
independent of `RUST_PORT`.

The separate **PULSE System Resources** dashboard shows CPU cores used,
working-set memory, disk read/write throughput, and network traffic for all containers in the matching Compose
project, including Go, Rust, Next.js, Hermes (production), tunnels, and monitoring.
These totals include all processes inside a container. Host panels show overall
CPU, memory, filesystem capacity, and disk throughput, including activity outside
PULSE. Disk throughput is distinct from filesystem space; Docker logs, images,
and named volumes consume host disk space too. Docker Desktop host measurements
cover its Linux VM, not the full macOS or Windows host; filesystem visibility
can differ from a native Linux deployment.

Alloy uses its built-in cAdvisor and Unix exporters, sending metrics to the
local Prometheus remote-write receiver every 15 seconds. Container series are
filtered by Compose project. Alloy now runs privileged with host PID/cgroup
access and read-only host filesystem mounts so it can inspect resources; treat
it as a trusted host-level collector. No additional collector ports are published.
The alerts include each backend independently and sustained high host CPU/memory,
low filesystem space, and resource exporter scrape failures. Alerts are visible
in Prometheus; external alert delivery is not configured.

To apply changes to an existing stack without running migrations or rebuilding
unrelated services:

```sh
# Development
docker compose --env-file .env.dev --project-name life-dev -f docker-compose.yml -f docker-compose.dev.yml --profile dev up -d --build --force-recreate --no-deps rust-backend-dev prometheus alloy grafana
# Production, on the production host
docker compose --env-file .env --project-name life-prod -f docker-compose.yml --profile prod up -d --build --force-recreate --no-deps rust-backend prometheus alloy grafana
```

Grafana provisions both dashboards in the **PULSE** folder. Recreate Grafana to
load the updated provisioning folder. The backend dashboard retains its existing
UID so saved dashboard links continue to work. The provider identity, Compose
project names (`life-dev`/`life-prod`), network aliases, and `life_*` metric names
remain stable to avoid replacing stored data or breaking application routing.
Prometheus now labels new samples as **Go backend** (`job="go-backend"`) and
**Rust backend** (`job="rust-backend"`). Older samples keep their original labels;
the renamed backend selector shows samples collected after this update.

Allow a few minutes of traffic for rate and percentile panels to fill. Host
resources cover the entire Ubuntu host; container resources identify which PULSE
service consumes them. An idle container still exports memory metrics, so **No
data** for container memory is a collection failure, not zero usage. The System
Resources dashboard shows the reporting container count and raises an alert
when it stays at zero even though the cAdvisor scrape succeeds.

If host graphs work but all container graphs are empty, inspect these on the
Ubuntu host from the project folder:

```sh
docker version --format '{{.Server.Version}}'
docker info --format 'Storage driver: {{.Driver}}; Docker root: {{.DockerRootDir}}'
docker compose --env-file .env --project-name life-prod -f docker-compose.yml --profile prod logs --tail=100 alloy
```

On Ubuntu with Docker 29's containerd image store, cAdvisor's Docker factory
also needs access to containerd. The exporter uses
`/rootfs/run/containerd/containerd.sock` through the existing read-only host
filesystem mount. The generic containerd namespace stays at its default so
Docker discovery retains the Compose labels. This fixes the observed factory
registration failure when Alloy's own `/run/containerd/containerd.sock` is
missing. Recreate Alloy after changing its bind-mounted config; do not rely on
a container restart to pick up a replaced file.

If container metrics remain missing, look for cAdvisor Docker API errors,
failed container creation or layer lookup, or missing Compose labels. Collection
and filtering depend on cAdvisor finding Docker metadata: metrics without the
matching project label are deliberately excluded. A successful scrape alone
does not prove that container metrics exist. [Alloy issue #6308](https://github.com/grafana/alloy/issues/6308)
documents further compatibility issues with Docker 29's containerd image store.
For a Docker installation using a different containerd socket, adjust
`containerd_host` to that socket under `/rootfs`. Do not disable project filtering
or change Docker storage drivers to make panels appear.

## Browser logs

Run `make dev` locally or `make up` on the production host. Open the matching
Grafana URL above and sign in as `admin` with that environment's
`GRAFANA_ADMIN_PASSWORD`. Under **Dashboards → PULSE → PULSE Service Logs**,
select one or more services, enter optional search text, and choose a time
range. The dashboard refreshes every five seconds.

When adding logging to an already-running stack, restart Grafana once after
starting the stack so it loads the new data source: use the matching Compose
command from the Makefile followed by `restart grafana`.

For live streaming, open **Explore**, select **Loki**, use the query builder
to select a `service` (or open the logs panel's menu and select **Explore**),
then click **Live**. Pause and resume without leaving the browser. Explore
also lets you download query results as TXT, JSON, or CSV; exports are limited
to the returned query results.

Alloy collects Docker stdout/stderr, including Go, Rust, production Hermes, Next.js
`npm run dev` output, production Next.js output, and monitoring/tunnel logs.
It does not collect browser console logs or application files outside Docker's
log stream. Hermes runs only in production. Development tunnel logs come from
`cloudflared-dev`, which creates the temporary random URL; production tunnel
logs come from `cloudflared`.

Development (`life-dev`) and production (`life-prod`) have separate Loki and
Alloy containers, networks, and project-scoped named volumes. Each collector
filters by its Compose project, so logs stay separate even on the same Docker
host. The upstream Loki/Alloy images are pinned and shared; their running
instances and data are separate. Keep using the Make commands (or their exact
`--project-name` equivalents) to preserve this separation.

Loki keeps collected logs for seven days, with background deletion after
expiry. Named volumes preserve history across container replacement and
`make down`/`make dev-down`; `docker compose down -v` deletes that history.
Old logs already removed by Docker cannot be recovered. Filesystem storage
is intended for this single-host setup; monitor disk space as log volume grows.
Loki retention does not change Docker's own log rotation settings.

Loki and Alloy publish no host ports. Grafana defaults to localhost and
uses its existing login. On a laptop, access production through
`ssh -N -L 3101:127.0.0.1:3101 <homelab-host>`, then open
`http://localhost:3101`, or use the Tailscale binding described above.
No public tunnel is required. If exposing Grafana
through Cloudflare later, protect it with Cloudflare Access and keep Grafana
authentication enabled.

Docker Desktop on macOS and Windows must use Linux containers; Alloy reads
the Docker API socket inside its Linux VM, rather than host log-file paths.
The socket mount is marked read-only, but this does **not** restrict Docker
API permissions: treat Alloy as a trusted privileged collector. Logs may
contain sensitive data, so restrict Grafana access and avoid logging secrets.

See [service URLs](../../README.md#service-urls) for development and production ports.
