# Calculator DevOps — CI/CD Pipeline for a Calculator REST API

**CSE 816 Software Production Engineering — Mini-Project**
**Kanav Bhardwaj (IMT2023024)** — Integrated M.Tech CSE, Term I, 2026-27

A Spring Boot Calculator REST API with a complete, automated pipeline.
The pipeline uses Git/GitHub, Jenkins, Docker, and Docker Compose.

A push to GitHub starts the pipeline. The pipeline builds, tests, packages,
containerizes, and deploys the application. No manual step is necessary.

---

## 1. Architecture

```
 Developer                GitHub                 Jenkins
   |  git push  ------->  repository  --webhook-->  declarative pipeline
                                                        |
        +-----------------------------------------------+
        |            |            |            |        |
     Build        Test      Docker Build     Push     Deploy
   (mvn package) (mvn test)  (image)     (Docker Hub) (compose up -d)
                                                        |
                                                        v
                                          +---------------------------+
                                          |   Docker Compose stack    |
                                          |                           |
                                          |  Nginx  -->  calculator   |
                                          |  :8081       (2 replicas) |
                                          +---------------------------+
```

Nginx is the only entry point. It sends each request to one of the two API
replicas. Docker DNS gives the replica addresses to Nginx.

---

## 2. Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 (Spring Web, Actuator) |
| Build | Maven 3.9 |
| Tests | JUnit 5, Spring MockMvc, JaCoCo |
| Container | Docker, multi-stage build, Alpine JRE |
| Orchestration | Docker Compose (API + Nginx reverse proxy) |
| CI/CD | Jenkins declarative pipeline |
| Registry | Docker Hub |

---

## 3. API reference

The base URL of the deployed stack is `http://localhost:8081`.
Port 8080 stays free, because Jenkins uses it.
A local run with `java -jar` listens on port 8080.

| Operation | Endpoint | Result |
|---|---|---|
| Add | `GET /api/v1/add?a=&b=` | `a + b` |
| Subtract | `GET /api/v1/subtract?a=&b=` | `a - b` |
| Multiply | `GET /api/v1/multiply?a=&b=` | `a * b` |
| Divide | `GET /api/v1/divide?a=&b=` | `a / b`, or HTTP 400 if `b` is 0 |
| Health | `GET /actuator/health` | Actuator health probe |
| Info | `GET /actuator/info` | Application metadata |

### Success response

```bash
curl "http://localhost:8081/api/v1/add?a=2&b=3"
```

```json
{ "operation": "add", "a": 2.0, "b": 3.0, "result": 5.0 }
```

### Error response

The API returns HTTP 400 and a structured body for these cases:

- The divisor `b` is zero.
- A parameter is absent.
- A parameter is not a number.
- A parameter or a result is not a finite number.

```bash
curl -i "http://localhost:8081/api/v1/divide?a=10&b=0"
```

```json
{
  "timestamp": "2026-10-26T09:15:42.317Z",
  "status": 400,
  "error": "Division by zero",
  "message": "Division by zero is not allowed. Give a non-zero value for 'b'.",
  "path": "/api/v1/divide"
}
```

---

## 4. HOW TO RUN EVERYTHING

This section gives every command in order. Follow the parts in sequence.

### Prerequisites

| Tool | Version | Check with |
|---|---|---|
| JDK | 21 or later | `java -version` |
| Maven | 3.9 or later | `mvn -v` |
| Docker Desktop | 24 or later | `docker --version` |
| Docker Compose | v2 | `docker compose version` |
| Git | 2.30 or later | `git --version` |

Start Docker Desktop before Part B. Wait until the whale icon stops moving.

---

### Part A — Run and test the application with Maven

Open a terminal in the project folder.

1. Run the tests.

   ```bash
   mvn clean test
   ```

   The result must show `Tests run: 38, Failures: 0, Errors: 0`.
   Maven writes the JUnit XML reports to `target/surefire-reports/`.
   JaCoCo writes the coverage report to `target/site/jacoco/index.html`.

2. Build the executable JAR.

   ```bash
   mvn clean package
   ```

   Maven writes the JAR to `target/calculator-devops.jar`.

3. Start the application.

   ```bash
   java -jar target/calculator-devops.jar
   ```

4. Call the API in a second terminal.

   ```bash
   curl "http://localhost:8080/api/v1/add?a=2&b=3"
   curl "http://localhost:8080/actuator/health"
   ```

5. Press `Ctrl+C` in the first terminal to stop the application.

---

### Part B — Run one container with Docker

1. Build the image.

   ```bash
   docker build -t paradox73/calculator-app:1.0.0 .
   ```

2. Start the container. The host port is 8081, because Jenkins uses 8080.

   ```bash
   docker run -d -p 8081:8080 --name calculator paradox73/calculator-app:1.0.0
   ```

3. Test the container.

   ```bash
   docker ps
   curl "http://localhost:8081/api/v1/multiply?a=6&b=7"
   ```

4. Stop and remove the container.

   ```bash
   docker rm -f calculator
   ```

---

### Part C — Run the full stack with Docker Compose

The stack has 3 containers: 2 API replicas and 1 Nginx proxy.

1. Start the stack. The `--build` option builds the image first.

   ```bash
   docker compose up -d --build
   ```

2. Wait for the healthy state, then show the containers.

   ```bash
   docker compose ps
   ```

   Each API replica must show `(healthy)`.

3. Test the four operations through the proxy.

   ```bash
   curl "http://localhost:8081/api/v1/add?a=2&b=3"
   curl "http://localhost:8081/api/v1/subtract?a=5&b=3"
   curl "http://localhost:8081/api/v1/multiply?a=4&b=2.5"
   curl "http://localhost:8081/api/v1/divide?a=7&b=2"
   ```

4. Test the error cases. Each command must return HTTP 400.

   ```bash
   curl -i "http://localhost:8081/api/v1/divide?a=10&b=0"
   curl -i "http://localhost:8081/api/v1/add?a=2"
   curl -i "http://localhost:8081/api/v1/add?a=abc&b=3"
   ```

5. Test the health probes.

   ```bash
   curl "http://localhost:8081/actuator/health"
   curl "http://localhost:8081/nginx-health"
   ```

6. Show the balance of the load over the two replicas.

   ```bash
   docker compose logs nginx | grep -o "upstream=[0-9.:]*" | sort | uniq -c
   ```

   Two different addresses prove that Nginx uses both replicas.

7. Stop the stack.

   ```bash
   docker compose down
   ```

---

### Part D — Run Jenkins

The folder `jenkins/` holds a ready Jenkins setup. It runs Jenkins in a
container and configures everything from code (Jenkins Configuration as Code,
`jenkins/casc.yaml`): the admin user, the plugins, the `JDK21` and `Maven3`
tools, the Docker Hub credential, the Gmail settings and the
`calculator-devops` pipeline job.

1. Copy the secrets template and fill it in. `jenkins/.env` is gitignored.

   ```bash
   cp jenkins/.env.example jenkins/.env
   ```

   | Variable | Value |
   |---|---|
   | `JENKINS_ADMIN_PASSWORD` | any password for the Jenkins login |
   | `DOCKERHUB_TOKEN` | Docker Hub **Account settings > Personal access tokens**, read & write |
   | `SMTP_USER` | your Gmail address |
   | `SMTP_PASSWORD` | a Google app password: <https://myaccount.google.com/apppasswords> |

2. Start Jenkins.

   ```bash
   docker compose -f jenkins/docker-compose.yml up -d --build jenkins
   ```

3. Open <http://localhost:8080> and log in as `admin`.
4. Open the job `calculator-devops` and click **Build Now**. The first run
   registers the triggers.

The Jenkins container runs `docker` through the host's Docker socket, so the
images and the deployed stack live in the host's Docker. The pipeline reaches
the deployed stack at `host.docker.internal:8081`.

| Stage | Action |
|---|---|
| Checkout | Pull the latest commit from GitHub |
| Build | Run `mvn clean package -DskipTests`, then archive the JAR |
| Test | Run `mvn test`, publish the JUnit results, fail on a test failure or on coverage below 85% |
| Docker Build | Build the image, tag it with the build number and with `latest` |
| Push | Log in with the Jenkins credentials, push both tags to Docker Hub |
| Deploy | Rolling update: replace `calculator-a`, wait until healthy, pause 10 s, replace `calculator-b`, then start or update Nginx |
| Verify Health | Poll `/actuator/health`, then check that `2 + 3` returns `5.0` through the proxy |

A failed Deploy or Verify Health stage redeploys the last green build.
Every result (success, failure, unstable) sends an email.

---

### Part E — Connect the automatic trigger

The `Jenkinsfile` declares two triggers: the GitHub webhook (preferred) and
`pollSCM('H/5 * * * *')`, a check every 5 minutes that needs no public address.

#### E.1 The webhook

GitHub must reach Jenkins. The `tunnel` service in `jenkins/docker-compose.yml`
runs a Cloudflare quick tunnel, which gives Jenkins a temporary public HTTPS
address. The address changes on every start, so update the webhook after each
start:

```bash
docker compose -f jenkins/docker-compose.yml up -d tunnel
sh jenkins/update-webhook.sh
```

The script reads the tunnel address and creates or updates the GitHub webhook
`<address>/github-webhook/` with the GitHub CLI.

#### E.2 Test the automatic trigger

```bash
git commit --allow-empty -m "Trigger the pipeline"
git push
```

Jenkins starts a new build without a manual click.

---

### Part F — Push the code to GitHub

```bash
git init
git add .
git commit -m "feat: add the Spring Boot calculator API"
git branch -M main
git remote add origin https://github.com/Paradox-73/Calculator_REST_API.git
git push -u origin main
```

Make progressive commits. Use a feature branch and a pull request:

```bash
git checkout -b feature/docker-compose
# make a change
git add .
git commit -m "feat: add the Nginx reverse proxy"
git push -u origin feature/docker-compose
# open a pull request on GitHub, then merge it
```

> A repository with one commit loses marks under the Git practices criterion.

---

## 5. Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| `open //./pipe/dockerDesktopLinuxEngine` | Docker Desktop is not running | Start Docker Desktop. Wait 30 seconds. |
| Port 8080 gives a Jenkins login page | Jenkins uses port 8080 | Use port 8081 for the stack. |
| `bind: address already in use` | Another program uses port 8081 | Set `HOST_PORT` to a free port in `.env`. |
| The container stays `unhealthy` | The application needs more start time | Increase `start_period` in `docker-compose.yml`. |
| Push fails with `password is empty` | `DOCKERHUB_TOKEN` is empty | Fill `jenkins/.env`, then `docker compose -f jenkins/docker-compose.yml up -d jenkins`. |
| The email does not arrive | The SMTP values are wrong | Use a Google app password in `jenkins/.env`, not your normal password. |
| The webhook shows a red cross | The tunnel address changed | Run `sh jenkins/update-webhook.sh`. |
| The first build is slow | Maven downloads the dependencies | This is normal. The next build is faster. |

---

## 6. Docker details

The `Dockerfile` uses two stages:

1. **Build stage** (`maven:3.9-eclipse-temurin-21`) — it downloads the
   dependencies, then compiles and packages the JAR.
2. **Runtime stage** (`eclipse-temurin:21-jre-alpine`) — it copies only the JAR.

Best practices in the file:

- The runtime image is a slim JRE image. It does not contain Maven or the sources.
- The image copies `pom.xml` before the sources. Docker then caches the
  dependency layer, and a source change does not download the dependencies again.
- The application runs as the non-root user `spring`.
- `EXPOSE 8080` declares the port.
- `HEALTHCHECK` calls the Actuator health endpoint.
- `.dockerignore` keeps the build context small.
- Both base images are pinned to a `sha256` digest. The same commit therefore
  always builds from the same base bytes.

---

## 7. Docker Compose details

`docker-compose.yml` starts two components on one bridge network:

| Service | Role | Port |
|---|---|---|
| `calculator-a`, `calculator-b` | The Spring Boot API, 2 replicas | internal 8080 |
| `nginx` | The reverse proxy and load balancer | host 8081 |

The `nginx` image is built from `nginx/Dockerfile`, which copies
`nginx/nginx.conf` into the image. No host file is mounted, so the stack also
deploys when Jenkins runs inside a container. Nginx keeps both replicas in one
`upstream` group and re-reads their addresses from Docker DNS (`resolve`), so
it balances the load and retries a failed request on the other replica.

The two replicas are two services with the same settings (a YAML anchor),
not `replicas: 2`. The pipeline can then replace them one at a time (a rolling
update), so the API stays reachable during a deploy. A measured redeploy with
a request every 0.2 s had 0 failed requests out of 98.

The file also defines a health check for each service, the restart policy
`unless-stopped`, environment variables, and a memory limit.
Nginx starts only after the API reports a healthy state.

Compose reads these variables from the environment or from a `.env` file.
Copy `.env.example` to `.env` to change them.

| Variable | Default | Purpose |
|---|---|---|
| `IMAGE_NAME` | `paradox73/calculator-app` | The image repository |
| `IMAGE_TAG` | `latest` | The image tag, the Jenkins build number |
| `HOST_PORT` | `8081` | The host port of the proxy |

---

## 8. Pipeline design notes

### Security of the credentials

The repository contains no password and no token. The pipeline reads the
Docker Hub login from the Jenkins credentials store with `withCredentials`.
Jenkins masks the values in the console log. The pipeline calls
`docker logout` after the push.

### Idempotent re-run

- `disableConcurrentBuilds()` permits only one deployment at a time.
- `docker compose up -d` replaces only the containers that changed. A re-run
  of the same build does not stop the running deployment.
- Each build tags the image with a unique build number. A rollback to an older
  tag is therefore possible.

### Rollback

A failed **Deploy** or **Verify Health** stage redeploys the image of the last
green build (`currentBuild.previousSuccessfulBuild`). A bad build therefore
never stays live. The smoke test checks the response body: `2 + 3` must return
`"result":5.0`, not only an HTTP 200.

### Coverage gate

`mvn test` runs `jacoco:check`. The build fails when instruction coverage drops
below 85%. The current coverage is 90% of instructions and 100% of branches.

### Actuator exposure

Only `health` and `info` are exposed. The health endpoint shows the `UP`/`DOWN`
status to anonymous callers and hides disk and other details
(`show-details: when-authorized`).

### A Windows Jenkins agent

The `runCmd` helper function in the `Jenkinsfile` calls `sh` on a Linux agent
and `bat` on a Windows agent. The same pipeline therefore runs on both.

---

## 9. Repository structure

```
.
├── src/main/java/com/spe/calculator/
│   ├── CalculatorApplication.java        # Spring Boot entry point
│   ├── controller/CalculatorController.java
│   ├── service/CalculatorService.java    # arithmetic and validation rules
│   ├── dto/                              # response records
│   └── exception/                        # custom exceptions, error handler
├── src/main/resources/application.yml
├── src/test/java/com/spe/calculator/     # 38 JUnit 5 and MockMvc tests
├── nginx/Dockerfile                      # proxy image with the config baked in
├── nginx/nginx.conf                      # reverse proxy configuration
├── pom.xml                               # Maven build
├── Dockerfile                            # multi-stage image build
├── .dockerignore
├── docker-compose.yml                    # API replicas + proxy
├── Jenkinsfile                           # declarative CI/CD pipeline
├── jenkins/                              # Jenkins in Docker, configured as code
│   ├── Dockerfile, plugins.txt, casc.yaml
│   ├── docker-compose.yml                # Jenkins + webhook tunnel
│   └── update-webhook.sh
├── .env.example                          # example configuration
├── .gitignore
└── README.md
```

### The test suite

| Test class | Purpose |
|---|---|
| `CalculatorServiceTest` | Unit tests of the four operations, the divide-by-zero rule, and the input validation |
| `CalculatorControllerTest` | MockMvc web-layer tests of every endpoint and every error case |
| `CalculatorApplicationTest` | End-to-end tests on a real server, including the Actuator health probe |

---

## 10. Verified state

A local run confirmed these results:

- `mvn clean package` — 38 tests pass, 0 failures, and Maven builds the JAR.
- JaCoCo coverage: 90% of instructions, 100% of branches. The 85% gate passes.
- `docker build` — the multi-stage image builds and starts.
- `docker compose up -d` — 2 API replicas and 1 proxy reach a healthy state.
- The four operations answer through the proxy on port 8081.
- The divide-by-zero request, the absent parameter, and the non-numeric
  parameter each return HTTP 400 with the structured error body.
- Nginx sends the requests to both replicas.
- A second `docker compose up -d` recreated only the changed container.
  The deployment stayed available.

---

## 11. Submission checklist

- [ ] The GitHub repository holds the code, the `Jenkinsfile`, the
      `Dockerfile`, and the `docker-compose.yml`.
- [ ] The commit history shows progressive work, one feature branch, and
      one merged pull request.
- [ ] The Docker Hub repository is public. Note the image URL.
- [ ] Screenshot: a green Jenkins pipeline run.
- [ ] Screenshot: the email notification.
- [ ] Screenshot: `docker ps` with the running containers.
- [ ] Screenshot: the sample API responses.
- [ ] A 2-3 page PDF report: the architecture diagram of section 1, a
      description of each stage, and the challenges.
- [ ] The live demo: push a commit and show the automatic deployment.
