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
