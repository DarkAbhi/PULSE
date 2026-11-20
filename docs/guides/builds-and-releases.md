# Builds and releases

`make deploy` builds once and then starts the stack. Keep the same Docker builder
between deployments: Go's compiler cache, Rust's existing Cargo caches, npm's
download cache, and Next.js's build cache survive source changes. The first build
or a dependency/toolchain change still costs more. Pruning build caches trades
disk space for slower future builds.

The web image contains Next.js standalone output, static assets, and public files;
it does not install dependencies a second time or ship the Next.js build cache.
Go and Rust release binaries strip debug symbols to reduce runtime image size.
Native debugging and Rust backtrace symbol names are consequently limited.

Measure on the production host before and after a deployment:

```sh
time make deploy
docker image ls --filter 'reference=life-prod-*'
docker system df
docker buildx du
```

Image storage, build caches, container writable layers, and persistent volumes
are separate. A smaller runtime image does not remove old images or compiler
caches. Avoid routine build-cache pruning if deployment speed matters, and do
not delete volumes to reclaim build space: the monitoring volumes store history.

If Rust compilation remains slow with a warm cache, inspect CPU, available RAM,
swap activity, and disk I/O during the build. Concurrent Go, Rust, and frontend
builds can compete for a small host's resources. The larger next step is to build
images on a faster machine or CI runner for the Homelab's architecture, publish
them to a registry, and deploy by pulling those images instead of compiling on
the Homelab.

## Shared release metadata

Update [`release.json`](../../release.json) once per platform release and rebuild the services together. It supplies the shared version and optional release timestamp to the web app, Go and Rust version endpoints, and MCP metadata.

For signed Android APKs and Telegram delivery, see [Android releases](android-release.md).
