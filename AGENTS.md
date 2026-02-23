# Repository Guidelines

## Project Structure & Module Organization
This repository contains a Java backend and a Vue 2 frontend.

- `exphlp/`: Maven multi-module backend (`pom.xml` at root).
- `exphlp/api/webApp`: main web API entry (`fjnu.edu.Main`).
- `exphlp/api/clientApi`: client-facing API module.
- `exphlp/domain/*`: domain services (`probInstMgr`, `exePlanMgr`, `algRltSave`, `platMgr`, `algLibMgr`).
- `exphlp/foundation`: shared utilities/configuration used by other backend modules.
- `exphlp-front/`: Vue admin frontend (`src/`, `public/`, `build/`).
- `docs/`: deployment notes and project documents.

## Build, Test, and Development Commands
Use separate terminals for backend and frontend.

- Frontend install: `cd exphlp-front && npm install`
- Frontend dev server: `cd exphlp-front && npm run dev`
- Frontend production build: `cd exphlp-front && npm run build:prod`
- Frontend lint/fix: `cd exphlp-front && npm run lint`
- Backend full build: `cd exphlp && mvn clean install`
- Backend tests: `cd exphlp && mvn test`
- Run web API module: `cd exphlp && mvn -pl api/webApp spring-boot:run`

## Version Compatibility Guardrail (Mandatory)
- For any new Java demo/service/module, align framework versions with this repository baseline before coding:
  - Java: `17`
  - Spring Boot: `2.7.18`
  - Spring Cloud: `2021.0.8`
  - Spring Cloud Alibaba: `2021.0.5.0`
- Never introduce older Spring Boot lines (e.g. `2.3.x`) in Java 17 services.
- If Nacos is used, import Cloud + Alibaba BOMs and avoid hardcoding starter versions that bypass BOM alignment.
- Before claiming completion, run at least one build command in the target module (e.g. `mvn -q -DskipTests package` or `mvn spring-boot:run`).

### Required Response Content (when handling Java module setup issues)
- State the root cause explicitly (for example: `Unsupported class file major version 61` means Java 17 bytecode incompatible with old framework versions).
- State exact version changes made (old -> new).
- State why those versions are chosen (must reference repository baseline).
- Provide reproducible verification commands and outcome.

## Coding Style & Naming Conventions
- Frontend formatting follows `exphlp-front/.editorconfig`: UTF-8, LF, 2-space indentation.
- ESLint is configured in `exphlp-front/.eslintrc.js`; run lint before committing.
- Vue components use PascalCase names (rule enabled), e.g. `ModelSelect.vue`.
- Java code uses package roots under `fjnu.edu...`; keep class names PascalCase, methods/fields camelCase.
- Keep module names and directories consistent with existing backend naming (e.g. `algLibMgr`, `exePlanMgr`).

## Testing Guidelines
- Backend uses Maven/Spring Boot test support (JUnit via `spring-boot-starter-test`).
- Place backend tests under `src/test/java` with `*Tests.java` suffix (example: `AlgLibMgrApplicationTests.java`).
- Frontend currently has lint checks but no dedicated unit test setup in this repo.
- No enforced coverage threshold is configured; add tests for new business logic and critical fixes.

## Commit & Pull Request Guidelines
- Current history is minimal (`Init`, `固定版本`), so no strict convention is enforced yet.
- Prefer concise, imperative commit messages with scope, e.g. `webApp: fix plan query null handling`.
- PRs should include:
  - purpose and affected modules,
  - linked issue/task ID,
  - validation steps (`mvn test`, `npm run lint`, manual checks),
  - screenshots/GIFs for UI changes in `exphlp-front`.

## High-Frequency Fix Patterns (Mandatory)

### 1) Unified Delivery Flow For Fixes
- For bugfixes and hardening work, follow this order:
  1. Reproduce and collect evidence (UI behavior + API response + logs/DB where needed).
  2. Fix backend semantics and frontend state handling together when the issue crosses layers.
  3. Verify with backend tests + frontend e2e + runtime/container checks.
  4. Update related docs (especially operation guides/case docs) before closing.

### 2) Java Version Compatibility (Do First, Not Last)
- Before creating any new Java demo/service/module, align versions to repository baseline first:
  - Java `17`
  - Spring Boot `2.7.18`
  - Spring Cloud `2021.0.8`
  - Spring Cloud Alibaba `2021.0.5.0`
- If you hit `Unsupported class file major version 61`, treat it as a version-baseline violation by default.
- In responses about Java setup issues, always include:
  - explicit root cause,
  - exact version changes (`old -> new`),
  - why these versions are used (must reference repo baseline),
  - reproducible verification commands and outcomes.

### 3) Deletion Consistency Pattern (Problem/Algorithm Management)
- Deletion response semantics should be interpreted uniformly:
  - `deletedCount > 0`: deletion succeeded.
  - `noop = true` and `verified = true`: record already absent (idempotent success).
  - `noop = true` and `verified = false`: backend cannot confirm deletion (must be treated as failure).
  - `blocked = true` with `refPlanCount/refPlanNames`: deletion blocked by plan references.
  - `repaired = true`: legacy/dirty data path repaired during deletion.
- Frontend must align messages with these states and refresh current list after delete attempts.
- Backend must not return fake success for unverified no-op deletions.

### 4) Container Rebuild Guardrail (Avoid False Deployment)
- For backend behavior changes, do not rely on container rebuild alone.
- Mandatory sequence:
  1. `cd exphlp && mvn -q -DskipTests package`
  2. `docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build webapp`
- For frontend behavior changes:
  - `docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build exphlp_front`
- If both changed, rebuild both services in one command.

### 5) e2e Stability Rules
- Prefer stable semantic locators (`getByRole`, labeled containers, scoped locators) over broad text matches.
- Avoid ambiguous `getByText(...)` assertions in pages with repeated labels.
- For routing assertions in hash mode, allow expected redirect transitions when needed.
- For delete flows, cover at least:
  - success,
  - noop/idempotent success,
  - repaired legacy success,
  - blocked-by-reference failure.

### 6) Service Connectivity Precheck (Execution Plan)
- Before plan execution, verify algorithm `serviceName` has an available Nacos instance.
- Keep naming consistent across:
  - Nacos registration name,
  - algorithm config `serviceName`,
  - case documentation examples.
- Error messages must be actionable (state missing service name / no available instance).

### 7) Documentation Delivery Rules
- Case materials should be split into:
  - step-by-step operation guide (click path + expected UI states),
  - field/parameter explanation guide (meaning + recommended values + troubleshooting).
- Any user-facing behavior change (delete result semantics, retry behavior, blocked reasons, etc.) requires matching doc updates.
