# Deploying artisticView

Everything runs on **one Linux server** with Docker Compose. **Jenkins** runs on the same server. It builds the images and restarts the containers whenever `main` changes.

```
                 ┌──────────── server (Ubuntu 24.04) ────────────┐
 browser ──443──▶│ web (Caddy: HTTPS, React app) ──▶ backend:8080 │──▶ postgres, redis
                 │ jenkins ──docker.sock──▶ builds + rolls images │
                 └────────────────────────────────────────────────┘
```

| File | What it is |
|---|---|
| `deploy/docker-compose.yml` | The stack: postgres, redis, backend, web, and jenkins (profile `ci`) |
| `deploy/.env.example` | Secrets and addresses; the real copy is `/opt/artisticview/.env` |
| `deploy/backend.env.example` | Optional SMTP settings, so the app sends real emails |
| `deploy/jenkins/` | Jenkins image with the Docker CLI and the plugins it needs |
| `deploy/setup-server.sh` | One-time server setup |
| `Jenkinsfile` (this repo) | Backend pipeline: gradle build → image → restart backend → health check |
| `Jenkinsfile` (frontend repo) | Frontend pipeline: type-check and build in Docker → restart web |

## 1. Server

Use Ubuntu 24.04 with **4 vCPU and 8 GB RAM**. Jenkins and Gradle builds need the memory, and 4 GB is too tight. For example:
- Hetzner CX32 / CPX31
- DigitalOcean 8 GB droplet
- AWS Lightsail 8 GB

## 2. One-time setup

```bash
ssh root@SERVER_IP
git clone https://github.com/kd1-86656-shweta/artisticView-service.git
DOMAIN=artisticview.in bash artisticView-service/deploy/setup-server.sh
```

The script does the following:
- installs Docker
- opens only ports 22, 80 and 443
- adds swap
- creates `/opt/artisticview/.env` with random passwords
- starts postgres, redis and Jenkins

**DNS:** at your domain registrar, add `A` records for `@`, `www` and `jenkins`, all pointing to the server IP.

## 3. Jenkins (first time)

1. Open Jenkins.
   - Before the frontend's first deploy, use an SSH tunnel:
     ```bash
     ssh -L 8081:localhost:8081 root@SERVER_IP
     ```
     Then go to http://localhost:8081.
   - After the frontend's first deploy, use `https://jenkins.<domain>`.
2. Get the unlock password:
   ```bash
   docker exec artisticview-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
   Then choose **Install suggested plugins** and create the admin user.
3. Add a credential for the private repos: **Manage Jenkins → Credentials → Global → Add**.
   - Kind: *Username with password*
   - Username: your GitHub user
   - Password: a GitHub fine-grained token with *Contents: read* on both repos
   - ID: `github`
4. Create two jobs. For each: **New Item → Pipeline → Pipeline script from SCM**, with Git, credential `github`, and branch `*/main`.
   - `artisticview-backend`: `https://github.com/kd1-86656-shweta/artisticView-service.git`
   - `artisticview-frontend`: `https://github.com/kd1-86656-shweta/artisticView-frontend.git`
5. Click **Build Now** on the backend job first, then on the frontend job.

After that, both jobs check GitHub every 5 minutes and deploy new commits on `main` by themselves.

For instant deploys, add a GitHub webhook to `https://jenkins.<domain>/github-webhook/` and tick *GitHub hook trigger* in each job.

## Day to day

| Task | Command |
|---|---|
| Logs | `docker logs -f artisticview-backend` |
| Status | `cd /opt/artisticview && docker compose ps` |
| Roll back the backend | `docker tag artisticview-service:<old build no> artisticview-service:latest && docker compose up -d --no-deps backend` |
| Database backup | `docker exec artisticview-postgres pg_dump -U artisticview artisticview \| gzip > backup-$(date +%F).sql.gz` |

Uploaded images and videos live in the `media` volume. Back it up together with the database.
