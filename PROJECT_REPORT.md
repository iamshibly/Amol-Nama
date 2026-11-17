# AmolNama — project and user guide

**Reviewed: 18 November 2026 (Bangladesh).** This report describes the repository implementation and the live screens supplied by the owner. A feature listed as implemented is present in code; it is not a guarantee that every scenario has been tested on the hosted deployment. The final section separates known gaps and proposed upgrades.

## 1. What AmolNama does

AmolNama is a student reflection journal. A student records an Afsos (a regret or a moment worth learning from), identifies its category and impact, captures a lesson, and follows up with a practical promise. A dashboard and semester report summarize those records. Students may also submit a reflection to a moderated community feed.

The core loop is **record → understand → act → revisit**. This is a personal tracking tool, not an automated counselor, academic grading system, or AI analysis service.

The public website is https://amol-nama.vercel.app. Source is maintained at https://github.com/iamshibly/Amol-Nama.

## 2. Accounts and roles

| Person | Available actions |
| --- | --- |
| Visitor | Open login, public registration, and the health endpoint. |
| Student | Manage their own profile, reflections and promises; read approved community posts; react; view dashboards and reports; export their data. |
| Administrator | Use the student features, review shared submissions, approve or hide them, and enable or disable student accounts. |

Registration is now public. There is no invitation code or email allowlist. Every new account receives the STUDENT role and is enabled. Registering does not confer administrator access.

A new account needs a name, valid email, password, and matching password confirmation. University, department, and current semester are optional. Names are limited to 100 characters and email addresses to 150. Email addresses are trimmed and normalized to lowercase. Duplicate email addresses are rejected.

Passwords require 12–72 characters and a maximum of 72 UTF-8 bytes. Passwords are stored with BCrypt hashing. The login form uses email and password. It includes a show/hide password button, an error message for failed sign-in, and a message for disabled accounts.

## 3. A student's first session

1. Open /register and create an account.
2. Sign in at /login.
3. Open Profile and enter academic information.
4. Record a reflection using Record Afsos or Add Afsos.
5. Add the lesson learned when ready.
6. Create a promise directly or from the reflection's follow-up action.
7. Review the dashboard, timeline, and semester report as records accumulate.
8. Optionally submit a reflection for anonymous community sharing.
9. Sign out using the navigation control.

A new account naturally shows zero totals, empty lists, and insufficient-data messages. These states are expected.

## 4. Dashboard

The dashboard is the personal overview. It includes:

- Total reflections for the selected scope.
- Reflections whose regret date falls in the last seven calendar days, including today.
- Active promises and promises currently marked broken.
- Semester Progress Score, or an insufficient-data state.
- Category distribution, most common category, and highest recorded impact.
- Up to five recent reflections and up to four active promises.
- A repeated-category signal when the most common category appears at least three times.
- A weekly recap using recent reflections, their most common category, and a resolved recent reflection when available.
- Actions to record a reflection, make a promise, and open the report.

The semester selector combines semester values from the student's reflections, promises, and current profile. It defaults to the current profile semester when appropriate. Selecting all semesters includes records without a semester. An unknown semester request falls back to all records.

The weekly counter uses the date of the event, not necessarily the day the record was entered.

## 5. My Afsos: private reflection records

Students can create, view, edit, search, filter, and delete their own reflections. Results are paginated in groups of 12 and normally show newest event dates first. The list supports keyword, category, and impact-level filtering.

| Field | Meaning and validation |
| --- | --- |
| Title | Required; up to 100 characters. |
| Description | Required; up to 1,000 characters. |
| Category | Required; one of the supported categories below. |
| Impact level | Required; one of four levels. |
| Regret date | Required; today or an earlier date. |
| Related course | Optional; up to 100 characters. |
| Semester | Optional; up to 50 characters. Defaults from the profile when blank. |
| Consequence | Optional; up to 500 characters. |
| Lesson learned | Up to 500 characters; required when status is Lesson Learned. |
| Status | Required; tracks the reflection's current state. |
| Anonymous sharing | Optional; submits the record for community moderation. |

Categories: Attendance, Assignment, Exam, Presentation, Group Project, Late Start, Sleep, Money, Course Selection, Teacher Issue, Campus Life, and Other.

Impact levels: LOW (Choto Afsos), MEDIUM (Medium Afsos), HIGH (Boro Afsos), and EXTREME (Jibon Shesh Afsos). Internal ordering weights are 1, 2, 3, and 5. These are user-selected labels, not clinical assessments.

Reflection statuses: New, Still Suffering, Lesson Learned, Fixed, and Repeated Again.

Changing a reflection resets its feed approval. An edited shared post therefore needs review again. A student's access to another student's private record is rejected or redirected rather than revealing the record.

Although an image URL field exists in legacy model/DTO structures, the current save service excludes imageUrl from copied form properties. A working image attachment/upload feature should not be claimed.

## 6. Timeline

The timeline presents the student's reflections in event-date order, helping them revisit earlier decisions and repeated themes. It uses the student's own records across semesters; it does not currently receive the dashboard's semester selector.

A timeline is a view of existing reflections. It does not generate separate journal entries or a separate history of every edit.

## 7. Promises and Reflect → Act

Promises convert a lesson into a concrete commitment. A promise has a title, target date, status, owner, semester context, and optional source reflection.

Students can create, edit, complete, break, and delete their own promises. New promises start Active. The title is required and limited to 150 characters. The target date is required; a new promise cannot target the past.

Creating a promise from a reflection links it to that reflection and inherits its semester. A standalone promise uses the current profile semester. The source must belong to the student. Deleting the source reflection leaves the promise in place with its source link set to null.

Only active promises can be marked completed or broken. Marking one broken increases its broken counter once for that transition. Editing a broken promise with an acceptable target date reactivates it. Editing an active overdue promise can retain its existing date, but cannot move it to a different past date.

The app does not currently run an automatic overdue task, send reminders, or independently decide whether a student kept a promise.

## 8. Campus Confessions and reactions

Students may choose to share a reflection anonymously to other students. An administrator must approve it before it appears in the community feed. The feed requires sign-in.

**Anonymous to other students does not mean anonymous to administrators.** Moderators can see the author in their review interface. Authors should avoid names or identifying information in the story itself.

Available reactions are Same, bhai; RIP CGPA; and Next semester. Each student has one current reaction per post. Selecting a different reaction replaces it; removing it clears it. The database uses the pair of post ID and user ID as the unique key, so repeated requests do not create duplicate votes.

Reactions are accepted only for eligible shared, approved posts. Current counts come from user-specific reaction rows. Older aggregate counters cannot identify individual voters and are not treated as the new voting history.

There are no comments, direct messages, friend lists, or user-follow features.

## 9. Semester report and progress calculation

The semester report summarizes the selected student's own reflection and promise data. It includes totals, category distribution, highest impact, repeated-category signals, progress, and reflection details. It uses the same summary service as the dashboard.

The current score is a rule-based calculation:

```text
score = 50
      + 20 × (completed promises / all promises)
      + 30 × (resolved reflections / all reflections)
      - 30 × (unresolved High or Extreme reflections / all reflections)
      - 20 × (currently broken promises / all promises)
```

A zero denominator contributes zero. The result is rounded and limited to 0–100. Lesson Learned and Fixed count as resolved. When both the reflection count and promise count are zero, the score is absent.

This score does not measure a person's worth, wellbeing, intelligence, or actual grades. There is no machine learning behind it. Recording more information can change the score in either direction.

The report is a website page. There is no dedicated server-generated PDF report or spreadsheet export. The JSON account export is a separate feature.

## 10. Profile, account data, and photos

Profile editing supports name, university, department, current semester, current CGPA, and target CGPA. The email is the account identifier and is not changed by the profile update service.

CGPA values are optional, between 0.00 and 4.00, and allow at most two decimal places. When both values are present, the target cannot be lower than the current value.

**Profile photo upload/change is not implemented.** There is no avatar field in the current user/profile data model, no upload endpoint, and no durable image storage integration. A displayed placeholder is not an upload feature.

The student can download amolnama-data.json from /profile/export. It contains their profile, reflections, promises, and reactions. It excludes password hashes. The response is marked no-store.

Account deletion currently requires contacting the configured administrator/organizer. There is no self-service deletion button or automated deletion workflow.

## 11. Administrator guide

After creating a normal account, an authorized owner can promote that exact account through the protected database console:

```sql
UPDATE users SET role = 'ADMIN'
WHERE email = 'your-email@example.com' AND role = 'STUDENT';
```

Replace the example email and sign out/in after promotion. Public registration always creates students.

At /admin an administrator can review account and reflection totals, view student accounts and pending shared stories, approve a story, hide a story, and toggle a student's enabled status.

Hiding a story removes both its approved and shared flags. It remains a private journal record. Disabling a student removes persisted sessions as well as changing the account flag. The account toggle service does not disable administrator accounts.

Approval, hiding, enabling, and disabling actions are recorded in moderation_audit. A dedicated searchable audit-history interface is not currently implemented.

## 12. Privacy and access controls

The app uses Spring Security authentication, role checks for /admin, server-side ownership checks for private records, BCrypt passwords, and CSRF protection for state-changing forms.

Login sessions are persisted in MySQL rather than held only in one container's memory. This supports requests reaching different instances. Cookies are HttpOnly, SameSite=Lax, and Secure by default. The session has an eight-hour inactivity timeout and the cookie is configured as a browser-session cookie.

The application sets frame protection and a no-referrer policy. Data access controls and moderation reduce exposure; the journal is not end-to-end encrypted. Hosting and database operators with appropriate access can access stored data.

Email verification, password reset, two-factor authentication, social sign-in, and an application-level sign-in/signup rate limiter are absent. Public signup therefore does not prove ownership of an entered email address. Host-level protections depend on the deployed platform configuration.

## 13. Frontend and experience

The application uses server-rendered Thymeleaf HTML, CSS, JavaScript, and SVG icons. Auth forms retain Spring Security/Thymeleaf form actions and CSRF handling.

This revision redesigns login and registration with a navy/gold showcase, Manrope typography, calmer form styling, clearer focus states, a shared reflection-journey panel, and responsive layouts. On smaller screens the form appears before the showcase. Sample journey numbers are explicitly illustrative; they are not pulled from a signed-out visitor's account.

The dashboard, journal, timeline, promises, community, and report retain the existing design. This revision is not a full redesign of every authenticated page.

## 14. Architecture and database

| Layer | Current implementation |
| --- | --- |
| Java runtime | Java 26 in the deployed container. |
| Framework | Spring Boot 4.1.0, Spring MVC, Spring Security. |
| HTML rendering | Thymeleaf. |
| Data access | Spring Data JPA plus JDBC for selected operations. |
| Database | MySQL-compatible TiDB in the owner's hosted setup. |
| Sessions | Spring Session JDBC. |
| Schema migrations | Flyway; Hibernate validates instead of silently altering schema. |
| Build | Maven wrapper and a multistage container build. |
| Hosting | GitHub source, GitHub Actions checks, Vercel container deployment. |

Main tables: users, afsos, student_promises, afsos_reactions, moderation_audit, Flyway schema history, and Spring Session tables.

V1 creates the initial records; V2 adds JDBC sessions; V3 adds per-user reactions and promise context; V4 adds the moderation audit. Once a migration has been applied, editing its historical SQL can cause checksum validation failures. Future schema changes should use a new versioned migration.

The deployment experienced a partially applied V3 on TiDB. Missing schema elements were completed and the history record was updated; subsequent screenshots confirmed the source foreign key and successful V1–V3 history. This is incident history, not an instruction to manually mark future failed migrations successful.

## 15. Deployment and environment

Vercel imports the GitHub repository with Root Directory set to app and the Container framework. app/Dockerfile.vercel builds the JAR and runs it as a non-root user. Source pushes to the connected production branch trigger a deployment.

| Variable | Purpose |
| --- | --- |
| DATABASE_URL | Full JDBC URL for the database, with TLS hostname verification. |
| DATABASE_USERNAME | Full database username supplied by the provider. |
| DATABASE_PASSWORD | Database password; keep in the host's secret settings. |
| PORT | HTTP listener; this project uses 8080. |
| SESSION_COOKIE_SECURE | true on the HTTPS website; false only for local HTTP tests. |
| DATABASE_POOL_SIZE | Maximum database connections per application instance; default 5. |
| PRIVACY_CONTACT_EMAIL | Address displayed for account-data requests. |

PILOT_ALLOWED_EMAILS and PILOT_INVITE_SECRET are no longer used. No actual passwords or signing secrets belong in the repository or this report.

The current TiDB host is in ap-northeast-1 (Tokyo), using database port 4000. The Vercel function selects Tokyo hnd1 to reduce distance from that database. Database port 4000 and website port 8080 serve different purposes.

The container uses -XX:TieredStopAtLevel=1 to favor startup speed over peak long-running JVM throughput. This is a mitigation for recurring cold-start timeouts, not proof that every live start will meet Vercel's deadline.

## 16. Operations and troubleshooting

/healthz performs SELECT 1 through JDBC and returns ok. A successful response confirms that request reached the app and a database query worked. It does not exercise every user workflow.

A deployment marked Ready confirms the deployment pipeline completed. Runtime startup and private page behavior need separate checks. Maven warnings about deprecated Unsafe or reflective access do not by themselves identify a failed build.

For a failed build, inspect Build Logs and the final Maven/container error. For a 500 or port timeout, inspect the request's application startup logs, Flyway validation, database access, and whether Tomcat reached its started message. Do not change port numbers or database schema solely because of a generic timeout.

The project has experienced Vercel startup timeouts near 29 seconds and a registry upload retry. Check the latest deployment and request timestamps when interpreting logs.

Use a separate database for Preview because starting a deployment applies migrations. Back up the database before schema changes and verify a restore into a separate database. Durable application data is in the database; container-local files must not be used as permanent uploads.

The requested setup uses free tiers. Their quotas and eligibility can change; consult the providers' current plan pages before expanding usage. This report does not guarantee unlimited or permanently free hosting.

## 17. Verification

GitHub CI runs a Java test suite, JavaScript syntax check, container build, and an HTTP smoke scenario with five student clients and two application instances sharing MySQL.

The smoke scenario covers public signup, login, reflection and promise operations, ownership isolation, reporting, moderation, changing/removing reactions, logout, disabled users, and shared sessions. Earlier runs passed. These are controlled test conditions, not a production load-capacity guarantee.

The user's screenshots establish that login, account creation, dashboard navigation, and empty feature screens were reachable. Populated reports, avatar upload, password recovery, prolonged live concurrency, and repeated hosted cold starts should not be treated as verified merely from those screens.

## 18. Route reference

| Route | Purpose |
| --- | --- |
| / | Redirects into the authenticated dashboard flow. |
| /login | Sign in and authentication messages. |
| /register | Public account creation. |
| /dashboard | Personal overview and semester scope. |
| /afsos | Search/filter/paginate own reflections. |
| /afsos/new | Create a reflection. |
| /afsos/details/{id} | View an owned reflection. |
| /afsos/edit/{id} | Edit an owned reflection. |
| /afsos/timeline | Own chronological reflection view. |
| /afsos/report | Semester summary and report. |
| /promises | Own promises and actions. |
| /promises/new | Create a standalone or linked promise. |
| /promises/edit/{id} | Edit an owned promise. |
| /confessions | Approved shared stories and reactions. |
| /profile | View/edit profile and access data-request options. |
| /profile/export | Download own account data as JSON. |
| /admin | Administrator dashboard and moderation. |
| /healthz | App/database health probe. |
| /access-denied | Authorization error page. |

Create/update/delete and moderation actions use corresponding POST routes; they are not public read-only links.

## 19. Known gaps and next upgrade priorities

These are recommendations for future work, not features delivered by this revision.

| Priority | Gap | Concrete next step |
| --- | --- | --- |
| 1 | No profile-photo control | Add avatar storage reference, authenticated upload/replace/remove endpoints, image decoding/validation, size limits, cropping, a fallback, and durable storage. Show it in Profile/navigation without exposing it on anonymous posts. |
| 1 | No account recovery | Add verified-email ownership and expiring single-use password reset links. Select a mail service within the free-budget constraint. |
| 1 | Intermittent cold starts | Measure multiple fresh hosted starts in Tokyo; inspect startup timing and memory; consider a different runtime/deployment strategy if timeouts persist. |
| 2 | Authenticated page polish | Improve filter/select alignment, reduce oversized empty states, standardize type/spacing, and verify at phone/tablet widths using real populated records. |
| 2 | Report export | Add printable layout and/or downloadable PDF using the same authorized report data. |
| 2 | Profile and deletion workflow | Add change-password, verified email change, and authenticated account deletion with a clearly defined retention policy. |
| 2 | Unbounded views | Add pagination to growing community, timeline, and admin lists; test query counts and connection limits. |
| 3 | Promise reminders | Add explicit timezone rules and opt-in scheduling/notifications; keep overdue and broken states distinct. |
| 3 | Administrative oversight | Add audit search and moderation explanations without exposing private student journals unnecessarily. |

Before expanding beyond the initial small group, verify long-running use, backups/restores, account recovery, accessibility, realistic data volume, and hosting limits.

## 20. Suggested demonstration

Create a reflection about a late assignment, set category Assignment and a recent date, then capture a lesson and mark it Lesson Learned. Create a linked promise to use the course calendar. Show the dashboard and selected semester report. Submit a separate story for community sharing, approve it using an administrator, then demonstrate a student reaction. Finish by showing profile editing and the JSON data export.

Use fictitious content in demonstrations. The project demonstrates journaling, follow-up commitments, scoped reporting, moderated sharing, and server-side access control. Profile images and advanced account-management features remain planned work.
