# Call Allocation Service

An HTTP service that registers call nodes per region and places calls on the least-loaded node with free capacity. Built with Kotlin and http4k. Node and call state is held in memory, so it is lost on restart.

## Prerequisites

| Tool | Needed for | Notes |
|------|------------|-------|
| JDK 21 | Building and running | The Gradle toolchain asks for Java 21 and does not download it, so it must be installed. |
| Docker | Building the image | Must be running. |
| [kind](https://kind.sigs.k8s.io/), kubectl, make | Running on a local Kubernetes cluster | |
| curl, jq | The smoke test script | |

The Gradle wrapper (`./gradlew`) downloads Gradle itself, so no separate install is needed.

## Run it without Kubernetes

```sh
./gradlew :app:test   # run the tests
./gradlew :app:run    # start the service on http://localhost:8080
```

The port comes from `app/src/main/resources/application.yaml`. To use another one, for example when 8080 is taken, build the distribution and override it:

```sh
./gradlew :app:installDist
JAVA_OPTS="-Dconfig.override.server.port=18080" app/build/install/app/bin/app
```

## Run it on a local kind cluster

Everything runs from the `deploy` folder; `make help` lists the targets.

```sh
cd deploy
make up             # create the cluster, build and load the image, deploy, roll the pod
make status         # nodes, deployment, pod and service
make port-forward   # expose the service on localhost:8080 (leave it running)
make seed           # in a second terminal: run the smoke test
make down           # delete the cluster
```

`make up` builds the image `call-allocation-service:v1.0.0` into the local Docker daemon, loads it into a two-node kind cluster named `call-cluster` (`deploy/k8s/cluster.yaml`), and deploys one replica to the `call-allocation` namespace. The pod is pinned to the worker node.

After changing code, run `make up` again: it rebuilds the image and restarts the pod. `make logs` follows the pod's logs.

## Smoke test

`scripts/seed-and-report.sh` registers nodes, allocates and drops calls, checks the error responses of every route, and prints a report of node load and call status. It exits non-zero if any check fails. Each run uses its own ids, so it can be repeated against the same instance.

```sh
scripts/seed-and-report.sh                                  # against http://localhost:8080
BASE_URL=http://localhost:18080 scripts/seed-and-report.sh  # against another address
```

## API

| Method | Path | Body | Purpose |
|--------|------|------|---------|
| `GET` | `/health` | | Liveness check |
| `PUT` | `/nodes` | `{"id", "region", "capacity"}` | Register a node or update its capacity |
| `GET` | `/nodes/{region}/{id}` | | Node capacity and current call count |
| `POST` | `/calls` | `{"callId", "region"}` | Allocate a call to a node in the region |
| `GET` | `/calls/{id}` | | Call status and node |
| `DELETE` | `/calls/{id}` | | Finish a running call |

Errors come back as `{"message": "..."}`: 400 for invalid input, 404 for unknown nodes, calls or regions, 409 for finished or conflicting calls, and 503 when every node in the region is full.

## Troubleshooting

- **The pod fails with `exec format error`.** The image is built for `linux/arm64` only (set in `buildSrc/src/main/kotlin/no/tta/plugins/JibBasePlugin.kt`). On an x86 machine, change `architecture = "arm64"` to `"amd64"` there.
- **`make port-forward` or `./gradlew :app:run` fails because port 8080 is in use.** Forward to another port with `kubectl --context kind-call-cluster -n call-allocation port-forward svc/allocator 18080:80`, or run the app on another port as shown above. Then point the smoke test at it with `BASE_URL`.
- **Gradle says it cannot find a Java 21 toolchain.** Install JDK 21, or point Gradle at an existing install with `org.gradle.java.installations.paths` in `~/.gradle/gradle.properties`.
