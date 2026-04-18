# Deploy AmolNama from GitHub to Vercel

The source is prepared. Configure a MySQL database and the Vercel environment before launching the website.

## 1. Extract the source

Extract AmolNama_Source.zip. Open the AmolNama folder. Its immediate contents include README.md, DEPLOYMENT.md, .github, .gitignore, and app. The app folder contains pom.xml, Dockerfile.vercel, vercel.json, and src.

## 2. Create a GitHub repository

Sign in at https://github.com/new. Choose your account as Owner, name the repository amolnama, choose its visibility, and click Create repository. A private repo is suitable for the pilot. For the terminal instructions below, create an empty repo with no generated README, license, or .gitignore.

An existing repo can also hold these files. Create a branch and replace its source with the prepared snapshot, review the changes, and merge after checks pass. The ZIP contains the source snapshot without the original upload history.

## 3. Upload with Git on your Mac

Open Terminal. Type cd followed by a space, drag the extracted AmolNama folder into Terminal, then press Enter. This selects the folder containing this guide, not app.

Run the following, replacing YOUR_USERNAME with your GitHub username.

```sh
git init -b main
git add .
git commit -m "Prepare AmolNama"
git remote add origin https://github.com/YOUR_USERNAME/amolnama.git
git push -u origin main
```

Authenticate using your normal GitHub HTTPS credential manager or GitHub CLI. GitHub account passwords do not work for Git pushes. If using GitHub Desktop, choose File → Add local repository for this folder, create its Git repository if prompted, commit, then Publish repository.

The Git workflow includes the hidden .github directory. Confirm GitHub displays .github/workflows/ci.yml and app/pom.xml in the uploaded source. Give the CI job time to finish under the Actions tab. Investigate any failed check before releasing.

## 4. Obtain managed MySQL

Create or use a managed MySQL service and database. Record its hostname, port, database name, username, password, and TLS/CA requirements from its dashboard. Vercel hosts the application. This MySQL service stores the records and login sessions.

A new database receives the included Flyway migrations automatically. For an existing populated database, follow the backup, schema comparison, and baseline procedure in README.md before starting the new application against it.

The production JDBC URL looks like this, using the actual provider values:

```text
jdbc:mysql://YOUR_DB_HOST:YOUR_DB_PORT/YOUR_DATABASE_NAME?sslMode=VERIFY_IDENTITY
```

Set the provider's required CA/truststore configuration if its certificate is not trusted by the JVM. The database must accept connections from the deployment. Check the provider's network configuration. Vercel's custom container runtime currently does not supply static outbound IPs.

## 5. Start a Vercel project

Visit https://vercel.com and sign in. Open Add New → Project. Choose the GitHub account that owns amolnama. Authorize the Vercel GitHub integration for that repository if required. Find amolnama and click Import.

If the repository is missing, use Adjust GitHub App Permissions or Configure GitHub App on the import page and grant access to it. Then return to the import screen.

## 6. Set project configuration

| Setting | Value |
| --- | --- |
| Project Name | amolnama, or an available variation |
| Root Directory | app |
| Framework | Container, using the included vercel.json |
| Build Command | Leave its override disabled |
| Output Directory | Leave its override disabled |
| Install Command | Leave its override disabled |
| Production branch | main |

The app/vercel.json explicitly selects the container framework. Dockerfile.vercel installs Maven dependencies, builds the Java application, and runs it using Java 26. Do not enter an npm build command.

The deployment selects Tokyo (`hnd1`) to be close to this project's TiDB database in AWS ap-northeast-1. If you move the database, update `regions` in app/vercel.json accordingly. The runtime uses `-XX:TieredStopAtLevel=1` to prioritize short JVM startup over peak long-running throughput. CI starts its two JVMs with the same setting. The HTTP port remains `${PORT:8080}`. A successful build does not establish that every cold start meets Vercel's startup deadline; verify the live app after an idle interval as well.

If an existing Vercel project uses this repository, update its Root Directory to app before redeploying.

## 7. Add environment variables before deploying

Expand Environment Variables on the import screen. Add each Name and Value separately, without wrapping the value in quotes. The table below is the complete list of application-specific settings.

| Name | Value / how to obtain it |
| --- | --- |
| DATABASE_URL | The full JDBC MySQL URL from step 4 |
| DATABASE_USERNAME | Your managed MySQL account name |
| DATABASE_PASSWORD | Its password, marked sensitive |
| PORT | 8080 |
| SESSION_COOKIE_SECURE | true |
| DATABASE_POOL_SIZE | 5 initially. This is the maximum per app instance. |
| PRIVACY_CONTACT_EMAIL | Your contact address for account deletion requests |

Registration is public; no invitation code or email allowlist is required. You may remove old PILOT_ALLOWED_EMAILS and PILOT_INVITE_SECRET variables from Vercel.

Configure Production first. For branch Preview deployments, set separate database credentials under Preview. A Preview deployment starts database migrations too, so give it a separate database.

When changing variables after the first deployment, open Project → Settings → Environment Variables, save the changes, and redeploy. Existing deployments keep their previous environment.

## 8. Deploy and check the result

Click Deploy. Follow the build logs until the deployment reaches Ready. Open the assigned HTTPS URL shown in Vercel, then visit /healthz on that exact hostname. It should return ok.

If deployment fails, open its Build Logs. If the page reports a server error after building, inspect Runtime Logs and check database connectivity, TLS, credentials, and schema. Ready alone does not verify registration, sign-in, and record persistence. Test those flows next.

## 9. Register your account

Open /register on the live site. Enter your name, email, and password, then confirm the password. No invitation code is required. Passwords require 12–72 characters and at most 72 UTF-8 bytes.

## 10. Create your administrator account

Register the organizer through the public signup form first. In your managed database's protected SQL console, replace the example address and run:

```sql
UPDATE users SET role = 'ADMIN'
WHERE email = 'your-email@example.com' AND role = 'STUDENT';
```

Log out of the website and log back in. Open /admin. Approve a test confession, check the feed, and confirm that students cannot access admin controls.

## 11. Verify the pilot

Use five separate browser profiles/devices and accounts. Each tester creates, edits, and deletes their own reflection, creates a linked promise, and views a report for a selected semester. Verify that direct links to another account's private records do not reveal them.

Approve one shared confession. Have testers react simultaneously, change their reaction, and remove it. Verify totals. Keep the sessions active for 15–30 minutes.

Redeploy the same app and revisit the same production hostname. Verify records and sessions. Test a managed MySQL backup restore into a separate database and check its record counts.

The earlier source passed seven automated tests and a five-account local MySQL/two-instance smoke test. This branding revision has structural checks. Hosted HTTPS, live redeployment, UI appearance, and the longer pilot require verification on the configured deployment.

## 12. Publish subsequent changes

Create a feature branch, make changes, commit, push, and open a pull request:

```sh
git switch -c feature/your-change
git add .
git commit -m "Describe your change"
git push -u origin feature/your-change
```

Wait for GitHub Actions to pass and inspect the Vercel Preview URL attached to the branch or pull request. Merge the reviewed pull request into main. Vercel automatically creates a Production deployment for the connected production branch.

Use GitHub branch rules to require the verify check before merging, where supported by your GitHub plan. Vercel's Git integration may deploy while independent GitHub checks are still running. Require review and passing checks before merging production changes.

## Official references

- https://vercel.com/kb/guide/docker
- https://vercel.com/docs/git/vercel-for-github
- https://vercel.com/docs/environment-variables
- https://docs.github.com/en/get-started/using-git/pushing-commits-to-a-remote-repository

