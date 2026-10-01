# CareConnect

CareConnect is organized into two application folders: `frontend/` contains the Angular patient/provider/admin app, and `backend/` contains the Spring Boot REST API, Oracle persistence, migrations, and legacy import utility. Root-level files provide local workspace commands and Oracle container configuration. The Angular app runs on port 4200 and proxies `/api` requests to the API on port 8080.

## Run locally

Requirements: Node.js 20.19+, Java 17+, and Docker. Maven is provided by the wrapper in `backend/`.

```bash
npm run install:frontend
npm run dev
```

You can also run the frontend independently with `cd frontend && npm install && npm start`, and the API from `backend/` with `./mvnw spring-boot:run` after Oracle is available.

`npm run dev` starts Oracle and waits for its health check before launching Spring Boot and Angular. The first Oracle startup initializes the persistent `careconnect_oracle_data` volume and may take a few minutes. Spring Boot applies the Flyway migrations, validates the schema, and inserts fictional demo data. Open <http://localhost:4200> after the API starts. API health is available at <http://localhost:8080/api/health>.

Local demo accounts use the shared password `CareConnectDemo2026!`:

| Portal | Email |
|---|---|
| Patient | `patient@careconnect.local` |
| Provider | `provider@careconnect.local` |
| Admin | `admin@careconnect.local` |

Additional demo patients are `maya@careconnect.local`, `noah@careconnect.local`, `emma@careconnect.local`, and `liam@careconnect.local`. The role-session shortcut is available only outside the `production` Spring profile.

To stop the local services, press Ctrl+C in the dev terminal and run `docker compose stop oracle`. `docker compose down` stops and removes the container but keeps the named Oracle data volume. `docker compose down -v` also deletes the database volume.

## Backend layout

`frontend/` contains the Angular application, assets, theme and 3D anatomy model, Angular configuration, proxy configuration, and its npm manifest. `backend/` contains the Spring Boot application. REST controllers expose DTOs, services enforce authorization and workflows, Spring Data JPA repositories persist entities, and Flyway owns schema changes under `backend/src/main/resources/db/migration`. Hibernate is configured to validate the migrated schema; it does not create or mutate tables. The Oracle Free container uses the `FREEPDB1` pluggable database and a persistent named volume.

Protected routes use an eight-hour Bearer JWT. Passwords are BCrypt hashes. Patient records are scoped to the signed-in patient; provider access is limited to patients with an appointment relationship. Complaint changes and key care actions are audited. Validation and API errors are returned as JSON.

| Method | Route | Access |
|---|---|---|
| GET | `/api/health` | Public health check |
| POST | `/api/auth/login` | Public login |
| POST | `/api/auth/register` | Public patient registration |
| POST | `/api/auth/demo-session/:role` | Development profile only |
| GET | `/api/me`, `/api/providers`, `/api/overview` | Signed in |
| GET/POST/PATCH | `/api/appointments` and `/api/appointments/:id/status` | Scoped patient/provider/admin access |
| GET | `/api/medications`, `/api/records`, `/api/vitals`, `/api/billing/invoices` | Patient or authorized care team |
| POST | `/api/records` | Assigned provider |
| POST/GET/DELETE | `/api/patient/body-complaints` and `/api/patient/body-complaints/:patientId` | Own patient account |
| GET | `/api/doctor/patients`, `/api/doctor/patients/:patientId/body` | Assigned provider |
| PUT | `/api/doctor/body-complaints/:complaintId` | Assigned provider |
| GET | `/api/admin/users`, `/api/admin/audit` | Admin |

## Configuration and production

Copy `.env.example` to `.env` for local overrides. Compose reads Oracle credentials from the environment; Spring reads `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `CARECONNECT_JWT_SECRET`, `CORS_ORIGIN`, and `API_PORT`. Keep real secrets out of source control.

If you have data in the former SQLite database, import it once into the Oracle database with `CARECONNECT_SQLITE_IMPORT_PATH="/absolute/path/to/careconnect.sqlite" npm run dev`. The importer runs after development seed data, merges records by their existing IDs, preserves account password hashes and audit history, and records the source checksum so the same file is not imported twice. It is disabled when the path is unset and refuses to run under the production profile. Keep the SQLite file as a backup until the imported data has been reviewed.

For deployment, use a managed/persistent Oracle database, run with the `production` profile (`npm run serve:api`), set `CARECONNECT_PRODUCTION=true`, provide a unique random `CARECONNECT_JWT_SECRET` of at least 32 characters, and set `CORS_ORIGIN` to the deployed frontend origin(s). The production profile disables demo session access and demo seeding; startup fails if the known demo user IDs exist in the production schema. Apply Flyway migrations as part of startup. Create real provider/admin users through a controlled provisioning process.

The seeded information is fictional demo data. A real healthcare deployment still needs organization-managed identity provisioning, secure hosting and backups, operational monitoring, and review of applicable privacy/security requirements before real patient data is used.

## Interactive body map

The Patient portal's **3D Body Map** lets patients search, rotate, zoom and select anatomy before submitting a complaint. Providers can search appointment-linked patients, review complaint history, and add diagnosis, notes, treatment recommendation and status. The viewer is a modular Angular/Three.js component; its Human Atlas BodyParts3D-derived model is loaded on demand from `public/models/human-atlas/` (about 33 MB compressed). Source application code is MIT licensed; the anatomy dataset is CC BY 4.0, with attribution in `public/models/human-atlas/ATTRIBUTION.md`. The model is educational and not a diagnostic tool.
