# QuizGame

A multiplayer quiz game with live rooms, a question bank in English, French and Arabic, a
leaderboard, and an optional voice-reading mode for visually impaired players.

## Features

- **Live multiplayer rooms** over STOMP/WebSocket, with host controls, rematches and kicks.
- **Solo games** with category, difficulty, language, round count and round-time filters.
- **Practice mode**: single-player and untimed, with immediate feedback, explanations and no
  effect on the leaderboard.
- **Daily challenge**: one shared, deterministic five-question set per UTC day, one attempt per
  player, a 60-second limit per question and a separate daily leaderboard.
- **Three question languages** (EN / FR / AR) and a bank that expands without overwriting
  existing questions or administrator edits.
- **Voice reading** using the browser's Web Speech API. It is off by default; players opt in
  from **Accessibility settings**. Questions, choices, feedback and explanations can be read
  aloud.
- **Post-answer explanations** shown after the answer is submitted and read aloud with the
  feedback. Explanations are never sent with the question itself.
- **Accessibility**: skip link, landmarks, focus management on route changes, labelled
  controls, table captions, live regions and a visible keyboard focus ring.
- **Leaderboard, player profile and match history**.
- **Question admin** for adding, editing and deleting questions and explanations.

## Tech stack

| Layer | Technology |
| --- | --- |
| Frontend | Angular 16, TypeScript, SCSS, STOMP.js |
| Backend | Spring Boot 4, Spring Security, Spring Data JPA, WebSocket/STOMP |
| Database | H2 file database (development/tests) |
| Tests | JUnit 5 / Spring Boot Test, Karma + Jasmine |

## Prerequisites

- Java 17 or newer
- Node.js 20 or newer and npm
- Chrome or Edge for the frontend tests

## Run locally

### Backend

```powershell
cd Backend
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
cd Backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. With no explicit profile the application uses the
`dev` default profile: it creates `Backend/data/quizdb.mv.db`, seeds the question bank, and
creates two local accounts:

| Account | Username | Password |
| --- | --- | --- |
| Player | `demo` | `demo123` |
| Administrator | `admin` | `admin123` |

### Frontend

```powershell
cd Frontend
npm install
npm start
```

Open `http://localhost:4200`. The development API/WebSocket URLs are configured in
`Frontend/src/environments/environment.ts`.

## Game modes

- **Multiplayer room** – the host chooses the filters and starts the room when enough players
  have joined.
- **Solo** – the same configuration, played immediately with a server-enforced round timer.
- **Practice** – untimed and single-player. Choose the filters from the lobby's **Practice**
  button; answers and explanations appear immediately. Practice games are never listed as open
  rooms and never write competitive history.
- **Daily challenge** – start it from the lobby. The question set is fixed for the UTC day, so
  every player answers the same questions; it is created once and reused. Each account gets one
  attempt, which can be resumed until it finishes. Leaving mid-challenge consumes the attempt.
  Daily results are stored separately from all-time game history, so the normal leaderboard is
  unaffected.

All modes support the optional voice-reading settings described below.

## Tests and build

Backend:

```powershell
cd Backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd Frontend
npm run format:check
npm run test:ci
npm run build
```

`npm run format` rewrites the frontend with Prettier; `npm run format:check` verifies it
without changing files. `karma.conf.js` detects a locally installed Chrome or Edge
automatically. If your browser is somewhere else, set `CHROME_BIN` to its executable before
running the tests.

## Configuration

### Backend

The main settings are in `Backend/src/main/resources/application.properties`:

- `app.cors.allowed-origins` — origins allowed to call the API and open the WebSocket.
- `app.jwt.secret` — HMAC key. The shipped value is development-only and is rejected outside
  the `dev`, `local` and `test` profiles.
- `app.auth.login.*` — login rate-limit window and lockout.

### Production

Build the backend jar, then start it with an explicit profile and a real JWT secret:

```bash
cd Backend
./mvnw package
SPRING_PROFILES_ACTIVE=prod \
APP_JWT_SECRET='replace-with-a-random-secret-of-at-least-32-characters' \
java -jar target/QuizGame-0.0.1-SNAPSHOT.jar
```

The `prod` profile disables demo-account/question seeding. Build the frontend with
`npm run build` (output in `Frontend/dist/front-end`). It uses same-origin `/api` and `/ws`
URLs, so serve it behind a reverse proxy that forwards those paths to the backend.

### Question content

- `Backend/src/main/resources/question-bank-expansion.json` is loaded by the development
  seeder. It contains 40 questions × EN/FR/AR, each with an explanation.
- `new_questions.json` is the editable source copy for the expansion bank, also used by
  `Backend/tools/SeedNewQuestions.java`.
- Seeding is additive: questions are matched by `(text, language)` and existing rows are never
  overwritten.

## Accessibility

Open **Accessibility** from the login screen or lobby (`/settings`) to:

- enable or disable voice reading;
- choose whether new questions and selected choices are read automatically;
- test the installed voice.

Voice synthesis runs on the device. If the browser or a voice pack does not support the
selected language, the controls remain usable without audio. The application also supports
keyboard-only navigation and announces round changes, time-outs and results to screen readers.

## Continuous integration

`.github/workflows/ci.yml` runs the backend tests and the frontend format check, frontend
tests, and production build on every push and pull request.
