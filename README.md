# AmolNama

For the complete GitHub-to-Vercel walkthrough, open [DEPLOYMENT.md](DEPLOYMENT.md).

A student reflection journal with private reflections, promises, semester reports, a moderated anonymous feed, and role-based administration. Java 26, Spring Boot 4.1, Thymeleaf, Maven, and MySQL. The site uses shared JDBC sessions, so routing a browser to another instance does not require a new login. Login cookies expire when the browser session ends or after eight hours of inactivity.

## Local setup

Install JDK 26 and Docker with Compose. From the repository root:

```sh
cd app
cp .env.example .env
```

Enter a personal local `DATABASE_PASSWORD`. Registration is open to anyone; no allowlist or invitation code is required. Keep the local `.env` untracked. If a value contains shell syntax or spaces, quote it in `.env`.

```sh
docker compose up -d --wait
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

Open <http://localhost:8080>. Open <http://localhost:8080/healthz> to check the process and database connection. A first start applies Flyway migrations. Run `./mvnw verify` for the H2 test suite. GitHub Actions also uses MySQL 8.4, builds the container, and drives five HTTP sessions against two JVMs connected to the same database.

## Vercel deployment from a private GitHub repository

1. Create a new **private** repository. Publish a clean initial snapshot so the old public upload history does not enter the new repo. Preserve the original repository separately, and rotate any credentials that were ever valid in its history or ZIP upload. If using the downloadable source, extract it, enter the directory that contains this README, and run:

   ```sh
   git init -b main
   git add .
   git commit -m "Prepare AmolNama pilot"
   git remote add origin YOUR_PRIVATE_GITHUB_REPOSITORY_URL
   git push -u origin main
   ```

2. Confirm the **Pilot checks / verify** GitHub Actions job succeeds. In GitHub Settings → Rules → Rulesets, target `main`, require a pull request and the `verify` status check, and disable force pushes. Private repository protection features depend on the GitHub plan. Keep changes on `feature/*` branches. Use Preview to exercise a branch before going to Production.
3. Provision managed MySQL with **TLS hostname verification**. Use a dedicated database and application user with privileges over that database alone, including the DDL Flyway needs during rollout. Create a separate Preview database and account. Plan around the database provider's connection limits. Vercel custom containers currently do not support static egress IPs, so verify the provider's supported inbound connectivity before committing to a deployment plan.
4. In Vercel, import the private repository using its owner's connected GitHub account. In the project set **Root Directory** to `app`, production branch to `main`, leave custom build and output overrides empty, and use the container configuration from the repo. Set the environment values below for **Production** and **Preview** separately before deploying. If a previously connected project exists, double-check its repo and root directory.
5. Deploy after the checks pass. Confirm HTTPS, `/healthz`, registration, login and logout, moderation, and a reflection round trip in the deployed app. Enable host rate limits for POST requests to `/login` and `/register` where available. Monitor build and runtime logs and review unexpected bursts of sign-in failures. Exercise the checklist below before opening the pilot.

| Variable | Vercel value |
| --- | --- |
| `DATABASE_URL` | Required. Full JDBC URL, e.g. `jdbc:mysql://db.example.com:3306/afsos_nama?sslMode=VERIFY_IDENTITY`. Supply the provider's CA as described below when the JVM does not already trust its signer. |
| `DATABASE_USERNAME` | Required. Dedicated MySQL account. Use different Preview credentials. |
| `DATABASE_PASSWORD` | Required. MySQL account password. Mark sensitive. |
| `PRIVACY_CONTACT_EMAIL` | Organizer address shown in Profile for account deletion requests. If omitted, Profile directs users to the site administrator. |
| `PORT` | Set to `8080` so Vercel routes to the same port that Spring Boot listens on. |
| `SESSION_COOKIE_SECURE` | `true` (also the default). Use `false` only for localhost HTTP testing. |
| `DATABASE_POOL_SIZE` | Optional. Defaults to `5` per instance. Lower it if needed to stay within the managed database's total connection limit. |

Use a provider endpoint whose TLS certificate matches the JDBC hostname. If the provider uses a private CA, import its public CA certificate into a dedicated Java truststore in the runtime image, then configure the JDBC URL with the Connector/J truststore location and type. Keep truststore passwords in platform secrets if the store requires one. Test `VERIFY_IDENTITY` against the actual endpoints before routing users to the deployment.

GitHub hosts the source and runs CI. GitHub Pages only serves static files and cannot run the Spring Boot backend. Other Docker-capable hosts can build the same `Dockerfile.vercel` and use the same environment values. Use `app` as the build context, set `PORT` to the host-assigned port, connect a persistent MySQL service, and set the health check to `/healthz`.

- [Vercel Docker and port configuration](https://vercel.com/kb/guide/docker)
- [Vercel GitHub integration and preview environments](https://vercel.com/docs/git/vercel-for-github)
- [MySQL Connector/J TLS settings](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-using-ssl.html)

## Public registration and the organizer account

Anyone can register at `/register` using a name, email, and password. No invitation code is needed. `PILOT_ALLOWED_EMAILS` and `PILOT_INVITE_SECRET` are no longer used and can be removed from Vercel.

Register the organizer using the same form. Verify their identity, then promote that exact email from the provider's protected SQL console. Replace the example address before running. The account must log out and back in to obtain the admin role.

```sql
UPDATE users SET role = 'ADMIN'
WHERE email = 'organizer@example.com' AND role = 'STUDENT';
```

Admins review pending submissions, hide published submissions, and enable or disable students. Disabling a student removes their persisted sessions. Moderation actions are recorded in `moderation_audit`.

## Existing MySQL data and rollback

- **Empty database:** Flyway applies V1–V4 automatically. Hibernate validates the schema on application start.
- **Existing unversioned database:** Take a provider snapshot or logical dump and restore it into a separate database. Compare `SHOW CREATE TABLE` for `users`, `afsos`, and `student_promises` to V1, including production indexes, column names, and referential integrity. Reconcile differences on the copy and verify data counts. Use the Flyway CLI `baseline -baselineVersion=1` against that validated copy with the same migration locations. Start the upgraded app there and verify V2–V4, then repeat on the live database during a controlled rollout. Leave automatic baselining disabled so an unverified schema fails visibly. Restrict writes during the migration. Preserve the original backup.
- **Legacy data:** Old aggregate reaction totals stay archived on the original reflections. They cannot be attributed to individual people, so the new feed counts start with newly recorded votes. Old promises without a reliable semester appear under **All semesters**. New promises capture the current semester or the source reflection's semester.
- **Rollback:** Restore the previous working application deployment through Vercel, and verify `/healthz` and login. If rolling back a database change, restore the pre-release backup to a separate database, verify records and credentials, then switch the deployment variables. Coordinate the write freeze because a point-in-time restore excludes changes made after the save point. Run a restore drill and record its outcome before starting the real pilot.

## Account data requests

Profile exports the student's own profile, reflections, promises, and reactions as JSON. To request deletion, a pilot participant contacts the organizer from the registered address. The organizer verifies the request, disables the account, and performs the deletion in a database transaction after checking the affected account ID. Delete sessions by `PRINCIPAL_NAME`, then the user's promises, reflections, and user row. The reaction foreign keys clear their reactions and reactions on their deleted reflections. Keep the restricted moderation audit according to the pilot's retention policy. Record how long the provider retains backups and allow expired backups to cycle out.

## Pilot acceptance

- Five testers in separate browsers/devices simultaneously sign in. Each creates and edits at least three reflections, tries a direct URL for another tester's private item, and checks report scope.
- Each creates and completes a linked promise. A moderator verifies identity visibility in the queue and approves one account's shared reflection. The five testers react simultaneously, change a reaction, remove a reaction, and verify the counts.
- Keep all five sessions active for 15–30 minutes. Redeploy, then recheck sign-in state, data, and logs. Browser session cookies are host-specific, so test on the same production hostname across the redeploy.
- Verify desktop/mobile layouts, keyboard focus, errors, HTTPS cookie flags, rollback, and a restore into a separate test database.
