# Pre-commit checks

Install [pre-commit](https://pre-commit.com/#install), Go, Rust, Node.js 24,
Python 3, and Docker, then install the hooks from the repository root:

```sh
pre-commit install
```

Android formatting also requires the [Pulse Android setup](../../pulse-android/README.md#setup).

Each commit scans staged changes with Gitleaks, formats Android Kotlin sources
and Gradle scripts with `ktlintFormat`, and runs the Go, Rust, Next.js, Hermes MCP,
and seed exporter test suites. If formatting changes files, review and stage them,
then retry the commit. To run the checks without committing:

```sh
pre-commit run --all-files
```

Go and Rust database tests need a running Docker daemon. Rust always starts
disposable PostgreSQL containers for its database tests. See [Testing](testing.md).

The complete hook definitions are in [`.pre-commit-config.yaml`](../../.pre-commit-config.yaml). Hooks use Bash, Make and `python3`; on Windows use a compatible development shell. The Go hook runs `make -C go-backend test`, Rust runs `cargo test`, frontend runs `node --test`, and the seed hook checks the exporter only. Android formatting runs on every commit. The full CI and combined-report suites are broader; see [Testing](testing.md).
