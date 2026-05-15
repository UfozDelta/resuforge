# Resume Pipeline

Small full-stack app for managing resume content, generating/ranking bullet points, and building tailored resume PDFs for job applications.

## What Is In This Repo

- `src/main/java/com/resumepipeline` - Spring Boot backend.
- `src/main/resources/db/migration` - Flyway database migrations.
- `src/main/resources/template/resume.tex` - LaTeX resume template used for PDF output.
- `frontend` - React + Vite frontend.
- `legacy` - older resume notes and drafts.

## Backend Overview

- `api` - REST controllers and request/response DTOs.
- `auth` - login, session auth, CORS, and Spring Security setup.
- `profile` - profile/resume identity data.
- `project` - projects and experience entries.
- `bullet` - resume bullet storage and editing.
- `application` - job applications, JD matching, and PDF rendering flow.
- `llm` - Gemini integration for JD cleanup, bullet generation, and ranking.
- `render` - LaTeX escaping, template rendering, and PDF compilation.

## Requirements

- Java 21
- Maven
- Node.js 18 or newer
- PostgreSQL database
- Optional: `tectonic` for PDF generation
- Optional: Gemini API key for LLM features

## Backend Setup

The backend needs database and auth settings before it can start.

Create a local config file at:

```text
src/main/resources/application-local.yml
```

Example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/resume_pipeline
    username: postgres
    password: postgres

auth:
  username: dev
  password-hash: your_bcrypt_password_hash

llm:
  api-key: your_gemini_api_key

tectonic:
  binary: tectonic
```

`application-local.yml` is ignored by git because it can contain secrets.

To generate a BCrypt password hash:

```powershell
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.resumepipeline.auth.BcryptGen" "-Dexec.args=your-password"
```

## Start The Backend

From the repo root:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
mvn spring-boot:run
```

Backend runs at:

```text
http://localhost:8080
```

## Start The Frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Frontend runs at:

```text
http://localhost:5173
```

The frontend talks to the backend at `http://localhost:8080` by default. To override it, set:

```powershell
$env:VITE_API_BASE="http://localhost:8080"
```

## Useful Commands

Run backend tests:

```powershell
mvn test
```

Build frontend:

```powershell
cd frontend
npm run build
```

Build backend jar:

```powershell
mvn package
```
