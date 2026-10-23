-- Reddit-style communities with members and roles, posts (in a community or on a profile), votes and
-- threaded comments. No secondary indexes yet (by decision); only primary keys and the unique constraints
-- correctness needs.

CREATE TABLE communities (
    id           BIGSERIAL PRIMARY KEY,
    slug         VARCHAR(40)   NOT NULL UNIQUE,  -- lower-case, used in URLs: /c/{slug}
    name         VARCHAR(80)   NOT NULL,
    description  VARCHAR(2000),
    rules        VARCHAR(4000),
    visibility   VARCHAR(20)   NOT NULL DEFAULT 'PUBLIC',  -- PUBLIC: anyone joins | PRIVATE: invite only
    cover_key    VARCHAR(300),
    creator_id   BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    member_count BIGINT        NOT NULL DEFAULT 0,
    post_count   BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE community_members (
    community_id BIGINT      NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- OWNER | MODERATOR | MEMBER
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INVITED
    invited_by   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (community_id, user_id)
);

CREATE TABLE posts (
    id            BIGSERIAL PRIMARY KEY,
    author_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    community_id  BIGINT REFERENCES communities (id) ON DELETE CASCADE,  -- NULL: posted on the author's profile
    title         VARCHAR(300)  NOT NULL,
    body          VARCHAR(10000),
    image_key     VARCHAR(300),
    video_key     VARCHAR(300),  -- a post has an image or a video, not both
    score        BIGINT        NOT NULL DEFAULT 0,
    comment_count BIGINT        NOT NULL DEFAULT 0,
    edited_at     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE post_votes (
    post_id    BIGINT      NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    value      SMALLINT    NOT NULL CHECK (value IN (-1, 1)),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE post_comments (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT        NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    author_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    parent_id  BIGINT REFERENCES post_comments (id) ON DELETE CASCADE,  -- NULL: top-level comment
    body       VARCHAR(5000) NOT NULL,
    deleted    BOOLEAN       NOT NULL DEFAULT FALSE,  -- kept as "[deleted]" so replies stay in place
    edited_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- permissions
INSERT INTO permissions (feature_id, name, description)
SELECT f.id, p.name, p.description
FROM (VALUES
    ('COMMUNITIES', 'POST_VIEW',   'Read posts and comments'),
    ('COMMUNITIES', 'POST_CREATE', 'Write posts, comments and vote')
) AS p(feature, name, description)
JOIN features f ON f.name = p.feature;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.internal_name IN ('MEMBER', 'MODERATOR', 'SUPER_ADMIN')
  AND p.name IN ('POST_VIEW', 'POST_CREATE');
