# artisticView — System Design, Schema, API & User Journeys

> Working name: **artisticView** (final name TBD; frontend rename = one constant in `src/config/app.ts`).
> A live community platform: people create profiles, discover others with shared interests, join communities,
> hang out in live rooms and attend or host events. Artists such as **Budnix** seed the first audience, but the
> platform is for everyone.
>
> | Repo | Path | Stack |
> |---|---|---|
> | `kd1-86656-shweta/artisticView-service` (private) | `Documents\Agasty-Office\artisticView-service` | Java 21 target (built on JDK 23), Spring Boot 3.4.1, Gradle multi-module, PostgreSQL 17, Redis (Memurai), Flyway |
> | `kd1-86656-shweta/artisticView-frontend` (public) | `Documents\Agasty-Office\artisticView-frontend` | React 18 + Vite 5 + Bootstrap 5.3 + TypeScript + Redux Toolkit (RTK Query) |
>
> Design reference: `agasty-service` / `agasty-frontend`. Its **module layout, JWT security and the
> role → feature → sub-feature → permission model** are followed. Code is written fresh, nothing is copied from it.
>
> Updated: 2026-10-22 · Status: **Phase 1 (sign-up, login, home) implemented and tested.**

---

## 1. What is built now (Phase 1)

| Area | Backend | Frontend |
|---|---|---|
| Sign up | `POST /auth/sign-up`: validation, unique email/username, MEMBER role, profile row, verification email, auto-login | `/signup`: display name, live username availability, email, password strength meter, confirm, guidelines checkbox |
| Login | `POST /auth/login` by email **or** username, generic error, suspended-account check, rate limit | `/login`: show/hide password, "forgot password", returns to the page you came from |
| Session | JWT access token (15 min) + rotating refresh cookie (7 days), reuse detection, logout | Access token in memory only; silent session restore on reload; auto-refresh on `TOKEN_EXPIRED` |
| Home / Discover | `GET /api/v1/home`: member stats, profile-completeness, people suggestions (shared interests first) | `/home`: hero + stats, verify-email banner, complete-profile card, Live now / Upcoming / Communities sections (empty states until built), people to follow |
| Permissions | Roles, features, sub-features, permissions, Redis-cached, `@PreAuthorize("hasPermission(...)")` | Menu built from the session's features; `usePermission()` / `useSubFeature()` hooks |
| Email flows | Verify email, resend, forgot / reset password (one-time hashed tokens) | `/verify-email`, `/forgot-password`, `/reset-password` |
| Super admin | Stats, user search, suspend/reactivate, assign roles | Menu entries appear (screens are "coming soon") |
| Search | Elasticsearch module planned (agasty pattern), not built yet | — |

### Run it locally

```bash
# backend (Postgres on 5432 with db "artisticview", Memurai/Redis on 6379)
cd artisticView-service
./gradlew :server:bootRun                     # http://localhost:8080, Swagger: /swagger-ui.html
# first super admin: sign up, then restart with
APP_BOOTSTRAP_SUPER_ADMIN_EMAIL=you@example.com ./gradlew :server:bootRun

# frontend
cd artisticView-frontend
npm install && npm run dev                    # http://localhost:3000 (proxies /auth and /api to :8080)
```

Backend runs on **8080** and the frontend on **3000** (agasty-service must be stopped locally, it also uses 8080).
In dev, emails are printed to the backend console (no SMTP needed); set `spring.mail.*` to send real mail.

---

## 2. Architecture

```
 Browser ──▶ artisticView-frontend (Vite/React, Bootstrap)
               │  same-origin via Vite proxy (prod: reverse proxy / same domain)
               ▼
           artisticView-service (Spring Boot, stateless, N replicas)
             ├── PostgreSQL  (source of truth, Flyway migrations)
             ├── Redis       (permission cache, rate limits; later: counters, streams, presence)
             ├── Elasticsearch (planned: people/community/room search, agasty-style indexer)
             └── SMTP        (verification + reset emails)
```

### Backend modules (Gradle, mirrors agasty-service)

| Module | Contents |
|---|---|
| `common` | `ApiResponse {data,error,meta}`, `AppException`, enums, DTOs, cursor utils |
| `persistence` | JPA entities + repositories (`user`, `auth`, `rbac`, `social`, `notification`, `moderation`, `outbox`) |
| `security` | `JwtTokenProvider`, `JwtAuthenticationFilter`, `RolesEvaluator` (PermissionEvaluator), `PermissionProvider`, `RateLimitFilter`, `SecurityConfig`, `AppSecurityConfig`, `CurrentUser` |
| `core` | Services: `AuthService`, `SessionService`, `AccessService`, `PermissionCacheService`, `PeopleService`, `HomeService`, `AdminService`, `EmailService`, `SuperAdminBootstrap` |
| `api` | Controllers (`AuthController`, `HomeController`, `UserController`, `AdminController`), `GlobalExceptionHandler`, OpenAPI |
| `media`, `notification` | Empty placeholders for uploads and real-time notifications |
| `server` | `ArtisticViewApplication`, `application*.properties`, Flyway SQL |

Dependency direction: `api → core → (security, persistence) → common`.

### Frontend structure

```
src/
├── App.tsx                 routes: guest-only (/, /login, /signup, /forgot-password, /reset-password)
│                           and protected (/home, everything else inside AppLayout)
├── store/                  Redux Toolkit: authSlice (token + session), baseApi (RTK Query with re-auth),
│                           authApi, homeApi
├── features/auth/          useSessionBootstrap, RouteGuards, permissions hooks
├── components/             layout (AuthLayout, AppLayout), form (PasswordField, PasswordStrength), common
├── pages/                  LandingPage, auth/*, home/*, ComingSoonPage, NotFoundPage
└── styles/theme.css        Bootstrap 5 theme (night indigo + coral "live" accent)
```

---

## 3. Security & JWT (agasty pattern, hardened)

| Piece | How it works |
|---|---|
| Access token | HS256 JWT, 15 min. Claims: `userId`, `username`, `email`, `roleIds`, `type=access`. Sent as `Authorization: Bearer`. Stored **in memory only** on the frontend. |
| Refresh token | HS256 JWT with random `jti`, 7 days, `type=refresh`. Sent only as cookie `av_refresh` (`HttpOnly`, `SameSite=Lax`, `Path=/auth`, `Secure` in prod). DB stores **SHA-256 of the jti** (`refresh_tokens`). |
| Rotation | Every `POST /auth/refresh-token` revokes the old row and issues a new pair. |
| Reuse detection | Presenting an already-revoked refresh token revokes **all** of that user's sessions (stolen-token defence). Tested. |
| Filter | `JwtAuthenticationFilter`: no header → anonymous; expired → `401 TOKEN_EXPIRED` (frontend refreshes and retries once); invalid → `401 INVALID_TOKEN`. |
| Authorization | `@PreAuthorize("hasPermission('FEATURE', 'PRIVILEGE')")` → `RolesEvaluator` → `PermissionProvider` (Redis set per role). |
| Passwords | BCrypt. Reset revokes every session. Generic "incorrect email/username or password" message. |
| Email tokens | 256-bit random, only hash stored, single use; verify 24 h, reset 30 min. Forgot-password never reveals whether an email exists. |
| Rate limits | Redis fixed window per IP: login 10/min, sign-up 5/min, forgot-password 3/min, resend-verification 3/min, refresh 30/min. Fails open if Redis is down. |
| Suspension | Login and refresh refuse suspended users; suspending revokes all their sessions. |
| Secrets | `APP_JWT_SECRET` (≥ 32 bytes) from the environment; dev profile has a clearly dev-only default; prod has no defaults. |
| Differences from agasty | Secret is not a code constant; refresh tokens are tracked + rotated; principal comes from the verified token instead of re-decoding the payload; CSRF safe because the cookie only reaches `/auth` and is `SameSite=Lax`. |

```
Sign-up/Login ─▶ {accessToken, session} + Set-Cookie av_refresh
API call      ─▶ Bearer access ─▶ 200
              ─▶ 401 TOKEN_EXPIRED ─▶ POST /auth/refresh-token (cookie) ─▶ new pair ─▶ retry
Page reload   ─▶ POST /auth/refresh-token ─▶ session restored (or show landing)
Logout        ─▶ POST /auth/logout ─▶ row revoked + cookie cleared
```

---

## 4. Permission model (follows agasty: role → feature → sub-feature → permission)

```
users ──< user_roles >── roles ──< role_permissions >── permissions ──> features
                           │                                              ▲
                           └──< role_sub_features_permissions >── permission_sub_features ──> sub_features
                                                                   (can_read, can_write, can_delete)
```

* **permissions** = backend privileges checked in `@PreAuthorize` (e.g. `USER_FOLLOW`).
* **features / sub-features + read/write/delete** = what the UI shows (menus, cards, buttons).
* Login and `GET /auth/me` return both: `session.permissions[]` and `session.features[].subFeatures[]`.
* Agasty scopes every row by `centerId/boardId` (multi-tenant school ERP); artisticView is single-tenant, so those columns are dropped.
* Permissions are cached in Redis (`av:permissions:role:{roleId}`), warmed at startup, loaded lazily on a miss, evicted when a role changes.
* **Role changes take effect at the next token refresh** (≤ 15 min), because role ids travel in the access token.

### Seeded roles
| Role | Who |
|---|---|
| `MEMBER` | Every registered user (default). No forced "artist/fan/host" identity, per the PRD. |
| `MODERATOR` | Member + report review and content moderation. |
| `SUPER_ADMIN` | Everything, including users, roles and platform stats. |

### Feature → sub-feature catalogue and access (R = read, W = write, D = delete)

| Feature | Sub-feature | MEMBER | MODERATOR | SUPER_ADMIN |
|---|---|---|---|---|
| HOME | HOME_DISCOVER | R | R | RWD |
| PROFILE | PROFILE_VIEW / PROFILE_EDIT / PROFILE_PRIVACY | R / RW / RW | R / RW / RW | RWD |
| PEOPLE | PEOPLE_DISCOVER / PEOPLE_FOLLOW / PEOPLE_BLOCK | R / RWD / RWD | same | RWD |
| COMMUNITIES | COMMUNITY_BROWSE / COMMUNITY_MANAGE | RW / RWD | same | RWD |
| LIVE_ROOMS | ROOM_JOIN / ROOM_HOST | RW / RWD | same | RWD |
| EVENTS | EVENT_BROWSE / EVENT_HOST | RW / RWD | same | RWD |
| NOTIFICATIONS | NOTIFICATION_INBOX | RWD | RWD | RWD |
| MODERATION | REPORT_REVIEW / CONTENT_MODERATE | — | RW / RWD | RWD |
| ADMINISTRATION | USER_MANAGE / ROLE_PERMISSION_MANAGE / PLATFORM_STATS | — | — | RWD |

### Privileges (permissions table)
| Feature | Privileges | MEMBER | MODERATOR | SUPER_ADMIN |
|---|---|---|---|---|
| HOME | DISCOVER_VIEW | ✓ | ✓ | ✓ |
| PROFILE | PROFILE_UPDATE | ✓ | ✓ | ✓ |
| PEOPLE | USER_VIEW, USER_FOLLOW, USER_BLOCK, REPORT_CREATE | ✓ | ✓ | ✓ |
| COMMUNITIES | COMMUNITY_VIEW, COMMUNITY_CREATE | ✓ | ✓ | ✓ |
| LIVE_ROOMS | ROOM_JOIN, ROOM_HOST | ✓ | ✓ | ✓ |
| EVENTS | EVENT_VIEW, EVENT_HOST | ✓ | ✓ | ✓ |
| NOTIFICATIONS | NOTIFICATION_VIEW | ✓ | ✓ | ✓ |
| MODERATION | REPORT_REVIEW, CONTENT_MODERATE | | ✓ | ✓ |
| ADMINISTRATION | USER_MANAGE, ROLE_MANAGE, PLATFORM_STATS_VIEW | | | ✓ |

### Adding a new feature (checklist)
1. New Flyway migration: insert `features`, `sub_features`, `permissions`, the three `permission_sub_features` levels, and grant rows for each role.
2. Backend: annotate endpoints with `@PreAuthorize("hasPermission('FEATURE', 'PRIVILEGE')")`.
3. Frontend: the menu entry appears automatically; gate buttons with `usePermission('PRIVILEGE')` or `useSubFeature('SUB_FEATURE').canWrite`.
4. Restart (cache warms) or evict the role keys.

---

## 5. Database schema

Flyway owns the schema (`server/src/main/resources/db/migration`), and Hibernate only **validates** it (`ddl-auto=validate`).
**No secondary indexes for now (by decision)**: only primary keys and the unique constraints correctness needs
(unique email/username, idempotent follow via composite PK, single-use token hashes). Add indexes later from real query plans.

### Implemented: `V1__init.sql`, `V2__seed_roles_features_permissions.sql`

| Table | Columns (key ones) | Notes |
|---|---|---|
| `users` | id PK, email UQ, username UQ, password_hash, status (ACTIVE/SUSPENDED/DELETED), email_verified, created_at, updated_at | email/username stored lower-case → case-insensitive uniqueness |
| `profiles` | user_id PK/FK, display_name, bio, avatar_key, cover_key, interests text[], links jsonb, profile_visibility (PUBLIC/MEMBERS/FOLLOWERS), follower_count, following_count | counters updated with one atomic UPDATE per follow/unfollow |
| `refresh_tokens` | id, user_id FK, token_hash UQ (sha256 of jti), expires_at, revoked_at, device, created_at | rotation + reuse detection |
| `user_tokens` | id, user_id FK, type (EMAIL_VERIFICATION/PASSWORD_RESET), token_hash UQ, expires_at, used_at | single-use email links |
| `roles` | id, name, internal_name UQ, description, system_role | MEMBER, MODERATOR, SUPER_ADMIN |
| `user_roles` | (user_id, role_id) PK | a user can hold several roles |
| `features` | id, name UQ, feature_name, display_name, description, icon, route, sequence_number, active | top-level menu areas |
| `sub_features` | id, feature_id FK, name UQ, display_name, description, route, sequence_number, active | screens/actions inside a feature |
| `permissions` | id, feature_id FK, name UQ, description | backend privileges |
| `role_permissions` | id, role_id FK, permission_id FK, UQ(role_id, permission_id) | |
| `permission_sub_features` | id, sub_feature_id FK, can_read, can_write, can_delete, UQ(sub_feature_id, r, w, d) | three levels per sub-feature |
| `role_sub_features_permissions` | id, role_id FK, permission_sub_feature_id FK, UQ | |
| `follows` | (follower_id, followee_id) PK, created_at, CHECK follower ≠ followee | `INSERT … ON CONFLICT DO NOTHING` → idempotent |
| `blocks` | (blocker_id, blocked_id) PK, created_at | blocks hide profiles and suggestions both ways |
| `notifications` | id, recipient_id, type, actor_id, entity_type, entity_id, payload jsonb, event_id, read_at, created_at, UQ(event_id, recipient_id) | ready for the notification pipeline |
| `reports` | id, reporter_id, target_type (USER/COMMUNITY/ROOM/EVENT/MESSAGE), target_id, reason, status, reviewed_by, reviewed_at | moderation queue |
| `outbox_events` | id, aggregate_type, aggregate_id, event_type, payload jsonb, created_at, published_at | transactional outbox for async work |

### Planned: `V3` communities, live rooms, events (from the PRD)

| Table | Columns (draft) |
|---|---|
| `communities` | id, slug UQ, name, description, avatar_key, cover_key, interests text[], visibility (PUBLIC/PRIVATE), join_policy (OPEN/REQUEST/INVITE), rules text, owner_id, member_count, created_at |
| `community_members` | (community_id, user_id) PK, role (OWNER/MODERATOR/MEMBER), status (ACTIVE/PENDING/BANNED), joined_at |
| `rooms` | id, community_id null, host_id, title, topic, kind (TALK/LISTENING/JAM), status (SCHEDULED/LIVE/ENDED), visibility, started_at, ended_at, max_participants, participant_count |
| `room_participants` | (room_id, user_id) PK, role (HOST/SPEAKER/LISTENER), joined_at, left_at |
| `events` | id, community_id null, host_id, title, description, starts_at, ends_at, room_id null, visibility, rsvp_count |
| `event_rsvps` | (event_id, user_id) PK, status (GOING/INTERESTED), created_at |
| `messages` | id, room_id or community_id, author_id, body, status, created_at (room chat) |
| `user_settings` | user_id PK, who_can_message, show_online_status, email_prefs jsonb (privacy settings) |

The parked artwork tables (artworks, images, tags, likes, comments, collections, challenges) are kept in
`Documents\Agasty-Office\artisticView-parked\` and can return later as a "creator showcase" feature.

---

## 6. API list

All JSON responses: `{ "data": …, "error": { "code", "message", "details" }, "meta": … }`.
Error codes used: `VALIDATION_FAILED`, `UNAUTHORIZED`, `TOKEN_EXPIRED`, `INVALID_TOKEN`, `FORBIDDEN`, `ACCOUNT_SUSPENDED`,
`NOT_FOUND`, `CONFLICT`, `BAD_REQUEST`, `RATE_LIMITED`, `INTERNAL_ERROR`.

### Implemented

| Method | Path | Auth / privilege | Purpose |
|---|---|---|---|
| POST | `/auth/sign-up` | public, 5/min/IP | Create account (MEMBER) and log in |
| POST | `/auth/login` | public, 10/min/IP | Log in with email or username |
| POST | `/auth/refresh-token` | refresh cookie | Rotate tokens, returns new access token + session |
| POST | `/auth/logout` | refresh cookie | Revoke this device's session, clear cookie |
| GET | `/auth/me` | logged in | User, privileges and feature/sub-feature access |
| GET | `/auth/check-availability?username=` | public | Live username check for the sign-up form |
| POST | `/auth/verify-email` | public (token) | Confirm email |
| POST | `/auth/resend-verification` | logged in | Send the verification email again |
| POST | `/auth/forgot-password` | public, 3/min/IP | Email a reset link (no account enumeration) |
| POST | `/auth/reset-password` | public (token) | Set a new password, revoke all sessions |
| GET | `/api/v1/home` | DISCOVER_VIEW | Home/Discover payload |
| GET | `/api/v1/users/{username}` | USER_VIEW | Public profile (respects blocks + visibility) |
| PATCH | `/api/v1/users/me/profile` | PROFILE_UPDATE | Edit display name, bio, interests, links, visibility |
| PUT / DELETE | `/api/v1/users/{id}/follow` | USER_FOLLOW | Follow / unfollow (idempotent) |
| GET | `/api/v1/admin/stats` | PLATFORM_STATS_VIEW | Users, active, suspended, new this week, open reports |
| GET | `/api/v1/admin/users?q=&status=&page=&size=` | USER_MANAGE | Search users with their roles |
| PATCH | `/api/v1/admin/users/{id}/status` | USER_MANAGE | Suspend / reactivate (not yourself) |
| PUT | `/api/v1/admin/users/{id}/roles` | USER_MANAGE | Replace roles (cannot drop your own SUPER_ADMIN) |
| GET | `/actuator/health`, `/swagger-ui.html` | public | Ops |

### Planned (next phases)

| Area | Endpoints |
|---|---|
| Profile & privacy | `GET/PATCH /api/v1/users/me/settings`, `POST /api/v1/media/upload-urls` (avatar/cover) |
| People | `GET /api/v1/users/{username}/followers`, `/following`, `PUT/DELETE /api/v1/users/{id}/block`, `GET /api/v1/people?q=&interest=` |
| Reports | `POST /api/v1/reports`; moderator `GET /api/v1/moderation/reports`, `PATCH /api/v1/moderation/reports/{id}` |
| Communities | `GET/POST /api/v1/communities`, `GET/PATCH /api/v1/communities/{slug}`, `PUT/DELETE /api/v1/communities/{slug}/membership`, `GET /api/v1/communities/{slug}/members` |
| Live rooms | `GET /api/v1/rooms?status=LIVE`, `POST /api/v1/rooms`, `POST /api/v1/rooms/{id}/start` / `end`, `PUT/DELETE /api/v1/rooms/{id}/participation`, WebSocket `/ws` (presence, chat, hand raise) |
| Events | `GET/POST /api/v1/events`, `PUT/DELETE /api/v1/events/{id}/rsvp` |
| Notifications | `GET /api/v1/notifications`, `POST /api/v1/notifications/read`, `GET /api/v1/notifications/unread-count`, WS `/user/queue/notifications` |
| Search (Elasticsearch) | `GET /api/v1/search?q=&type=people|communities|rooms|events` |
| Admin | `GET/POST/PATCH /api/v1/admin/roles`, `PUT /api/v1/admin/roles/{id}/permissions`, `PUT /api/v1/admin/roles/{id}/sub-features`, `GET /api/v1/admin/features` |

---

## 7. User journeys

### 7.1 Artist / creator journey (e.g. Budnix)

| # | Step | Screen | API | Status |
|---|---|---|---|---|
| 1 | Lands on the site from a social link, reads "Find your people. Experience it live." | `/` | — | ✅ |
| 2 | Signs up: display name "Budnix", username `budnix` (green "available" tick), email, strong password | `/signup` | `GET /auth/check-availability`, `POST /auth/sign-up` | ✅ |
| 3 | Lands on Home with "Welcome aboard", a verify-email banner and a "Complete your profile" card | `/home` | `GET /api/v1/home` | ✅ |
| 4 | Clicks the link in the email to confirm the address | `/verify-email` | `POST /auth/verify-email` | ✅ |
| 5 | Adds a bio ("Rapper. Live sessions every Friday.") and interests (hip hop, music, production) | `/home` card | `PATCH /api/v1/users/me/profile` | ✅ |
| 6 | Follows people with shared interests from "People you might like" | `/home` | `PUT /api/v1/users/{id}/follow` | ✅ |
| 7 | Creates a community "Budnix Cypher Crew" with rules and interests | `/communities/mine` | `POST /api/v1/communities` | Planned |
| 8 | Schedules a Friday listening session; followers and members get notified | `/events/host` | `POST /api/v1/events` | Planned |
| 9 | Goes live; fans join the room, chat, raise hands, get invited to speak | `/rooms/{id}` | rooms + WebSocket | Planned |
| 10 | After the session: sees who attended, new followers, new community members | profile / community | notifications | Planned |
| 11 | Comes back next day: logs in by username, the session is restored on reload | `/login` | `POST /auth/login`, `/auth/refresh-token` | ✅ |

### 7.2 Member / fan journey

1. Signs up → Home ✅ → adds interests ✅ → follows suggested people ✅.
2. Sees **Live now** and **Upcoming sessions** on Discover (empty states today; real data once rooms/events land).
3. Joins a community, RSVPs to an event, joins a live room as a listener (planned).
4. Blocks or reports someone who is abusive (planned endpoints; privileges already granted).

### 7.3 Super admin journey

| # | Step | How | Status |
|---|---|---|---|
| 1 | First super admin is created without any hard-coded password: sign up normally, set `APP_BOOTSTRAP_SUPER_ADMIN_EMAIL`, restart | `SuperAdminBootstrap` | ✅ |
| 2 | Logs in: menu now also shows **Moderation** and **Administration** (driven by the session's features) | `/home` | ✅ |
| 3 | Checks platform health: total/active/suspended users, new this week, open reports | `GET /api/v1/admin/stats` | ✅ API, screen planned |
| 4 | Searches users, sees their roles and status | `GET /api/v1/admin/users` | ✅ API, screen planned |
| 5 | Promotes a trusted member to **Moderator** (takes effect on their next token refresh) | `PUT /api/v1/admin/users/{id}/roles` | ✅ |
| 6 | Suspends an abusive account: login and refresh are refused, existing sessions revoked; can reactivate later | `PATCH /api/v1/admin/users/{id}/status` | ✅ |
| 7 | Guard-rails: cannot suspend themselves or remove their own SUPER_ADMIN role | service checks | ✅ |
| 8 | Manages roles and what they can do (create role, tick permissions and R/W/D per sub-feature), as in agasty's role-permission screen | admin roles API + screen | Planned |
| 9 | Reviews the report queue and hides content (shared with moderators) | moderation API + screen | Planned |

---

## 8. Roadmap

| Phase | Scope | Status |
|---|---|---|
| 1 | Auth (sign-up, login, refresh, logout, verify, reset), RBAC (roles/features/sub-features/permissions), Home/Discover shell, people suggestions + follow, super-admin user management API | **Done, tested end-to-end with curl** |
| 2 | Profile page (`/users/:username`), edit profile + privacy settings, avatar upload (pre-signed URLs), followers/following, block + report | Next |
| 3 | Communities (create, join, members, rules) + Discover "communities for you" | |
| 4 | Events (schedule, RSVP) + live rooms (WebSocket presence, chat, host controls) | |
| 5 | Notifications (outbox → Redis Streams → DB + WebSocket), email digests | |
| 6 | Search with Elasticsearch (agasty module pattern: documents, repositories, indexer services, search services) | |
| 7 | Admin screens: users, roles & permissions editor, moderation queue, stats | |
| 8 | Hardening: integration tests (Testcontainers), indexes from real query plans, CI, deploy | |

## 9. Decisions log

| Decision | Why |
|---|---|
| Pivot from artist-only to a general live community (PRD) | PRD: "must not be restricted to artists" |
| React + Vite + Bootstrap 5 (not Next.js + Tailwind) | PRD tech stack |
| TypeScript + Redux Toolkit on the frontend | Same store/permission patterns as agasty-frontend |
| Default role `MEMBER`, no forced identity at sign-up | PRD 2.4 |
| No secondary DB indexes yet | Explicit instruction; add from real query plans later |
| Ports 8080 / 3000 | Backend 8080, frontend 3000; stop agasty-service locally first (it also uses 8080) |
| Flyway + `ddl-auto=validate` (agasty uses `update`) | Reviewable, repeatable schema changes |
| Elasticsearch deferred to Phase 6, behind a flag | Not running locally; Postgres covers Phase 1 |
