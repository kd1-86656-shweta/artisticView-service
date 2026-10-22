-- Initial schema: identity, roles/features/permissions, social graph, notifications, moderation, outbox.
-- No secondary indexes yet (by decision); only primary keys and the unique constraints correctness needs.

-- ---------------------------------------------------------------- identity
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,  -- stored lower-cased
    username       VARCHAR(30)  NOT NULL UNIQUE,  -- stored lower-cased
    password_hash  VARCHAR(100) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE profiles (
    user_id            BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    display_name       VARCHAR(80),
    bio                VARCHAR(1000),
    avatar_key         VARCHAR(300),
    cover_key          VARCHAR(300),
    interests          TEXT[]      NOT NULL DEFAULT '{}',
    links              JSONB       NOT NULL DEFAULT '{}'::jsonb,
    profile_visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    follower_count     BIGINT      NOT NULL DEFAULT 0,
    following_count    BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 of the refresh JWT's jti
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    device     VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- one-time tokens for email verification and password reset
CREATE TABLE user_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- roles, features & permissions
-- Same model as agasty-service: role -> permissions (privileges checked by @PreAuthorize) and
-- role -> sub-feature read/write/delete grants (drive the menus and cards the frontend shows).
CREATE TABLE roles (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(60)  NOT NULL,
    internal_name VARCHAR(60)  NOT NULL UNIQUE,
    description   VARCHAR(1024),
    system_role   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id    BIGINT      NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE features (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(60)  NOT NULL UNIQUE,
    feature_name    VARCHAR(100) NOT NULL,
    display_name    VARCHAR(200),
    description     VARCHAR(500),
    icon            VARCHAR(60),
    route           VARCHAR(200),
    sequence_number INT,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE sub_features (
    id              BIGSERIAL PRIMARY KEY,
    feature_id      BIGINT       NOT NULL REFERENCES features (id) ON DELETE CASCADE,
    name            VARCHAR(60)  NOT NULL UNIQUE,
    display_name    VARCHAR(200),
    description     VARCHAR(500),
    route           VARCHAR(200),
    sequence_number INT,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    feature_id  BIGINT        REFERENCES features (id) ON DELETE CASCADE,
    name        VARCHAR(80)   NOT NULL UNIQUE,
    description VARCHAR(1024),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT      NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT      NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (role_id, permission_id)
);

-- a read/write/delete combination on one sub-feature
CREATE TABLE permission_sub_features (
    id             BIGSERIAL PRIMARY KEY,
    sub_feature_id BIGINT      NOT NULL REFERENCES sub_features (id) ON DELETE CASCADE,
    can_read       BOOLEAN     NOT NULL DEFAULT FALSE,
    can_write      BOOLEAN     NOT NULL DEFAULT FALSE,
    can_delete     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sub_feature_id, can_read, can_write, can_delete)
);

CREATE TABLE role_sub_features_permissions (
    id                        BIGSERIAL PRIMARY KEY,
    role_id                   BIGINT      NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_sub_feature_id BIGINT      NOT NULL REFERENCES permission_sub_features (id) ON DELETE CASCADE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (role_id, permission_sub_feature_id)
);

-- ---------------------------------------------------------------- social graph
CREATE TABLE follows (
    follower_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);

CREATE TABLE blocks (
    blocker_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

-- ---------------------------------------------------------------- notifications & moderation
CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(30) NOT NULL,
    actor_id     BIGINT REFERENCES users (id) ON DELETE CASCADE,
    entity_type  VARCHAR(30),
    entity_id    BIGINT,
    payload      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    event_id     BIGINT,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, recipient_id)
);

CREATE TABLE reports (
    id          BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type VARCHAR(20)   NOT NULL,
    target_id   BIGINT        NOT NULL,
    reason      VARCHAR(1000) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    reviewed_by BIGINT REFERENCES users (id),
    reviewed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- transactional outbox
CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id   BIGINT      NOT NULL,
    event_type     VARCHAR(60) NOT NULL,
    payload        JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
