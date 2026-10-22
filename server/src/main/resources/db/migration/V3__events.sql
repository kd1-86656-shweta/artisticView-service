-- Events: hosted sessions and meetups, with co-host collaborators, RSVPs and time-boxed promotion.
-- No secondary indexes yet (by decision); only primary keys and the unique constraints correctness needs.

CREATE TABLE events (
    id             BIGSERIAL PRIMARY KEY,
    host_id        BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title          VARCHAR(120)  NOT NULL,
    description    VARCHAR(4000),
    category       VARCHAR(40),
    starts_at      TIMESTAMPTZ   NOT NULL,
    ends_at        TIMESTAMPTZ,
    location_type  VARCHAR(20)   NOT NULL,  -- ONLINE | IN_PERSON
    location       VARCHAR(200),
    meeting_url    VARCHAR(500),
    capacity       INTEGER,
    status         VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    promoted_until TIMESTAMPTZ,
    rsvp_count     BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CHECK (ends_at IS NULL OR ends_at > starts_at),
    CHECK (capacity IS NULL OR capacity > 0)
);

-- Co-hosts invited by the host; they can edit the event once they accept.
CREATE TABLE event_collaborators (
    event_id   BIGINT      NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL DEFAULT 'INVITED',  -- INVITED | ACCEPTED | DECLINED
    invited_by BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, user_id)
);

CREATE TABLE event_rsvps (
    event_id   BIGINT      NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, user_id)
);
