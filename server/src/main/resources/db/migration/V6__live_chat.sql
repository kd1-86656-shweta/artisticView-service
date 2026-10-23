-- Live chat: one-to-one and group conversations, members with read positions, and messages.
-- No secondary indexes yet (by decision); only primary keys and the unique constraints correctness needs.

CREATE TABLE conversations (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL,           -- DIRECT | GROUP
    title           VARCHAR(80),                     -- groups only
    avatar_key      VARCHAR(300),                    -- groups only
    direct_key      VARCHAR(41) UNIQUE,              -- DIRECT only: "{smallerUserId}:{largerUserId}", one chat per pair
    created_by      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE conversation_members (
    conversation_id      BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    user_id              BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role                 VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- ADMIN | MEMBER (groups)
    last_read_message_id BIGINT,
    joined_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       BIGINT        REFERENCES users (id) ON DELETE SET NULL,  -- NULL for system notes ("Riya joined")
    body            VARCHAR(4000),
    image_key       VARCHAR(300),
    deleted         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- menu entry, permission, access
INSERT INTO features (name, feature_name, display_name, description, icon, route, sequence_number) VALUES
    ('MESSAGES', 'Messages', 'Messages', 'Live one-to-one and group chats', 'chat-dots', '/messages', 7);

INSERT INTO sub_features (feature_id, name, display_name, description, route, sequence_number)
SELECT f.id, 'MESSAGES_CHAT', 'Chats', 'Talk one-to-one and in groups', '/messages', 1
FROM features f WHERE f.name = 'MESSAGES';

INSERT INTO permissions (feature_id, name, description)
SELECT f.id, 'CHAT_USE', 'Send and receive chat messages'
FROM features f WHERE f.name = 'MESSAGES';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.internal_name IN ('MEMBER', 'MODERATOR', 'SUPER_ADMIN') AND p.name = 'CHAT_USE';

INSERT INTO permission_sub_features (sub_feature_id, can_read, can_write, can_delete)
SELECT sf.id, lvl.r, lvl.w, lvl.d
FROM sub_features sf
CROSS JOIN (VALUES (TRUE, FALSE, FALSE), (TRUE, TRUE, FALSE), (TRUE, TRUE, TRUE)) AS lvl(r, w, d)
WHERE sf.name = 'MESSAGES_CHAT';

INSERT INTO role_sub_features_permissions (role_id, permission_sub_feature_id)
SELECT r.id, psf.id
FROM roles r
JOIN sub_features sf ON sf.name = 'MESSAGES_CHAT'
JOIN permission_sub_features psf ON psf.sub_feature_id = sf.id AND psf.can_read AND psf.can_write AND psf.can_delete
WHERE r.internal_name IN ('MEMBER', 'MODERATOR', 'SUPER_ADMIN');
