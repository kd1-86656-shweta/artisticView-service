-- Seed system roles, the feature/sub-feature catalogue and permissions.
-- New features add rows here (in later migrations) and grant them to roles; code checks permission names.

-- ---------------------------------------------------------------- roles
INSERT INTO roles (name, internal_name, description, system_role) VALUES
    ('Super Admin', 'SUPER_ADMIN', 'Full platform access: users, roles, permissions, moderation, settings', TRUE),
    ('Moderator',   'MODERATOR',   'Reviews reports and moderates content, plus everything a member can do', TRUE),
    ('Member',      'MEMBER',      'Default role for every registered user', TRUE);

-- ---------------------------------------------------------------- features
INSERT INTO features (name, feature_name, display_name, description, icon, route, sequence_number) VALUES
    ('HOME',           'Home',           'Home',            'Personal home and Discover',                  'house',         '/home',          1),
    ('PROFILE',        'Profile',        'My Profile',      'Your public profile and privacy settings',   'person-circle', '/profile',       2),
    ('PEOPLE',         'People',         'People',          'Find, follow and block people',              'people',        '/people',        3),
    ('COMMUNITIES',    'Communities',    'Communities',     'Join and run interest-based communities',    'collection',    '/communities',   4),
    ('LIVE_ROOMS',     'Live Rooms',     'Live Rooms',      'Join or host live rooms',                    'broadcast',     '/rooms',         5),
    ('EVENTS',         'Events',         'Events',          'Scheduled live sessions and meetups',        'calendar-event','/events',        6),
    ('NOTIFICATIONS',  'Notifications',  'Notifications',   'Activity that involves you',                 'bell',          '/notifications', 7),
    ('MODERATION',     'Moderation',     'Moderation',      'Review reports and moderate content',        'shield-check',  '/moderation',    8),
    ('ADMINISTRATION', 'Administration', 'Administration',  'Users, roles, permissions and platform stats','gear',          '/admin',         9);

-- ---------------------------------------------------------------- sub-features
INSERT INTO sub_features (feature_id, name, display_name, description, route, sequence_number)
SELECT f.id, s.name, s.display_name, s.description, s.route, s.seq
FROM (VALUES
    ('HOME',           'HOME_DISCOVER',        'Discover',               'Live now, upcoming sessions, communities and people', '/home',                     1),
    ('PROFILE',        'PROFILE_VIEW',         'View Profile',           'See your public profile',                             '/profile',                  1),
    ('PROFILE',        'PROFILE_EDIT',         'Edit Profile',           'Photo, name, bio and interests',                      '/profile/edit',             2),
    ('PROFILE',        'PROFILE_PRIVACY',      'Privacy Settings',       'Who can see your profile',                            '/profile/privacy',          3),
    ('PEOPLE',         'PEOPLE_DISCOVER',      'Find People',            'Search and browse people',                            '/people',                   1),
    ('PEOPLE',         'PEOPLE_FOLLOW',        'Follow',                 'Follow and unfollow people',                          '/people',                   2),
    ('PEOPLE',         'PEOPLE_BLOCK',         'Block & Report',         'Block or report people',                              '/people/blocked',           3),
    ('COMMUNITIES',    'COMMUNITY_BROWSE',     'Browse Communities',     'Find and join communities',                           '/communities',              1),
    ('COMMUNITIES',    'COMMUNITY_MANAGE',     'Manage Communities',     'Create and run your communities',                     '/communities/mine',         2),
    ('LIVE_ROOMS',     'ROOM_JOIN',            'Join Rooms',             'Join live rooms',                                     '/rooms',                    1),
    ('LIVE_ROOMS',     'ROOM_HOST',            'Host Rooms',             'Start and host your own rooms',                       '/rooms/host',               2),
    ('EVENTS',         'EVENT_BROWSE',         'Browse Events',          'Upcoming sessions and meetups',                       '/events',                   1),
    ('EVENTS',         'EVENT_HOST',           'Host Events',            'Schedule your own sessions',                          '/events/host',              2),
    ('NOTIFICATIONS',  'NOTIFICATION_INBOX',   'Inbox',                  'Your notifications',                                  '/notifications',            1),
    ('MODERATION',     'REPORT_REVIEW',        'Report Queue',           'Review reported users and content',                   '/moderation/reports',       1),
    ('MODERATION',     'CONTENT_MODERATE',     'Moderate Content',       'Hide or remove content',                              '/moderation/content',       2),
    ('ADMINISTRATION', 'USER_MANAGE',          'Users',                  'Suspend, reactivate and assign roles',                '/admin/users',              1),
    ('ADMINISTRATION', 'ROLE_PERMISSION_MANAGE','Roles & Permissions',   'Manage roles and what they can do',                   '/admin/roles',              2),
    ('ADMINISTRATION', 'PLATFORM_STATS',       'Platform Stats',         'Growth and health of the platform',                   '/admin/stats',              3)
) AS s(feature, name, display_name, description, route, seq)
JOIN features f ON f.name = s.feature;

-- ---------------------------------------------------------------- permissions (privileges)
INSERT INTO permissions (feature_id, name, description)
SELECT f.id, p.name, p.description
FROM (VALUES
    ('HOME',           'DISCOVER_VIEW',        'View the home and Discover page'),
    ('PROFILE',        'PROFILE_UPDATE',       'Edit own profile and privacy settings'),
    ('PEOPLE',         'USER_VIEW',            'View people and public profiles'),
    ('PEOPLE',         'USER_FOLLOW',          'Follow and unfollow people'),
    ('PEOPLE',         'USER_BLOCK',           'Block and unblock people'),
    ('PEOPLE',         'REPORT_CREATE',        'Report a person or content'),
    ('COMMUNITIES',    'COMMUNITY_VIEW',       'Browse and join communities'),
    ('COMMUNITIES',    'COMMUNITY_CREATE',     'Create and manage own communities'),
    ('LIVE_ROOMS',     'ROOM_JOIN',            'Join live rooms'),
    ('LIVE_ROOMS',     'ROOM_HOST',            'Host live rooms'),
    ('EVENTS',         'EVENT_VIEW',           'Browse events'),
    ('EVENTS',         'EVENT_HOST',           'Schedule and host events'),
    ('NOTIFICATIONS',  'NOTIFICATION_VIEW',    'Read own notifications'),
    ('MODERATION',     'REPORT_REVIEW',        'Review and resolve reports'),
    ('MODERATION',     'CONTENT_MODERATE',     'Hide or remove content of others'),
    ('ADMINISTRATION', 'USER_MANAGE',          'Suspend users and assign roles'),
    ('ADMINISTRATION', 'ROLE_MANAGE',          'Manage roles, permissions and feature access'),
    ('ADMINISTRATION', 'PLATFORM_STATS_VIEW',  'View platform statistics')
) AS p(feature, name, description)
JOIN features f ON f.name = p.feature;

-- ---------------------------------------------------------------- role -> permissions
-- Members: everything a normal user does.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.internal_name IN ('MEMBER', 'MODERATOR', 'SUPER_ADMIN')
  AND p.name IN ('DISCOVER_VIEW', 'PROFILE_UPDATE', 'USER_VIEW', 'USER_FOLLOW', 'USER_BLOCK', 'REPORT_CREATE',
                 'COMMUNITY_VIEW', 'COMMUNITY_CREATE', 'ROOM_JOIN', 'ROOM_HOST', 'EVENT_VIEW', 'EVENT_HOST',
                 'NOTIFICATION_VIEW');

-- Moderators and super admins: moderation.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.internal_name IN ('MODERATOR', 'SUPER_ADMIN')
  AND p.name IN ('REPORT_REVIEW', 'CONTENT_MODERATE');

-- Super admins: administration.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.internal_name = 'SUPER_ADMIN'
  AND p.name IN ('USER_MANAGE', 'ROLE_MANAGE', 'PLATFORM_STATS_VIEW');

-- ---------------------------------------------------------------- sub-feature access levels
-- Every sub-feature gets the three standard levels: read, read+write, read+write+delete.
INSERT INTO permission_sub_features (sub_feature_id, can_read, can_write, can_delete)
SELECT sf.id, lvl.r, lvl.w, lvl.d
FROM sub_features sf
CROSS JOIN (VALUES (TRUE, FALSE, FALSE), (TRUE, TRUE, FALSE), (TRUE, TRUE, TRUE)) AS lvl(r, w, d);

-- role -> sub-feature level
INSERT INTO role_sub_features_permissions (role_id, permission_sub_feature_id)
SELECT r.id, psf.id
FROM (VALUES
    -- member
    ('MEMBER', 'HOME_DISCOVER', 'R'),
    ('MEMBER', 'PROFILE_VIEW', 'R'),
    ('MEMBER', 'PROFILE_EDIT', 'RW'),
    ('MEMBER', 'PROFILE_PRIVACY', 'RW'),
    ('MEMBER', 'PEOPLE_DISCOVER', 'R'),
    ('MEMBER', 'PEOPLE_FOLLOW', 'RWD'),
    ('MEMBER', 'PEOPLE_BLOCK', 'RWD'),
    ('MEMBER', 'COMMUNITY_BROWSE', 'RW'),
    ('MEMBER', 'COMMUNITY_MANAGE', 'RWD'),
    ('MEMBER', 'ROOM_JOIN', 'RW'),
    ('MEMBER', 'ROOM_HOST', 'RWD'),
    ('MEMBER', 'EVENT_BROWSE', 'RW'),
    ('MEMBER', 'EVENT_HOST', 'RWD'),
    ('MEMBER', 'NOTIFICATION_INBOX', 'RWD'),
    -- moderator = member + moderation
    ('MODERATOR', 'HOME_DISCOVER', 'R'),
    ('MODERATOR', 'PROFILE_VIEW', 'R'),
    ('MODERATOR', 'PROFILE_EDIT', 'RW'),
    ('MODERATOR', 'PROFILE_PRIVACY', 'RW'),
    ('MODERATOR', 'PEOPLE_DISCOVER', 'R'),
    ('MODERATOR', 'PEOPLE_FOLLOW', 'RWD'),
    ('MODERATOR', 'PEOPLE_BLOCK', 'RWD'),
    ('MODERATOR', 'COMMUNITY_BROWSE', 'RW'),
    ('MODERATOR', 'COMMUNITY_MANAGE', 'RWD'),
    ('MODERATOR', 'ROOM_JOIN', 'RW'),
    ('MODERATOR', 'ROOM_HOST', 'RWD'),
    ('MODERATOR', 'EVENT_BROWSE', 'RW'),
    ('MODERATOR', 'EVENT_HOST', 'RWD'),
    ('MODERATOR', 'NOTIFICATION_INBOX', 'RWD'),
    ('MODERATOR', 'REPORT_REVIEW', 'RW'),
    ('MODERATOR', 'CONTENT_MODERATE', 'RWD'),
    -- super admin = everything, full access
    ('SUPER_ADMIN', 'HOME_DISCOVER', 'RWD'),
    ('SUPER_ADMIN', 'PROFILE_VIEW', 'RWD'),
    ('SUPER_ADMIN', 'PROFILE_EDIT', 'RWD'),
    ('SUPER_ADMIN', 'PROFILE_PRIVACY', 'RWD'),
    ('SUPER_ADMIN', 'PEOPLE_DISCOVER', 'RWD'),
    ('SUPER_ADMIN', 'PEOPLE_FOLLOW', 'RWD'),
    ('SUPER_ADMIN', 'PEOPLE_BLOCK', 'RWD'),
    ('SUPER_ADMIN', 'COMMUNITY_BROWSE', 'RWD'),
    ('SUPER_ADMIN', 'COMMUNITY_MANAGE', 'RWD'),
    ('SUPER_ADMIN', 'ROOM_JOIN', 'RWD'),
    ('SUPER_ADMIN', 'ROOM_HOST', 'RWD'),
    ('SUPER_ADMIN', 'EVENT_BROWSE', 'RWD'),
    ('SUPER_ADMIN', 'EVENT_HOST', 'RWD'),
    ('SUPER_ADMIN', 'NOTIFICATION_INBOX', 'RWD'),
    ('SUPER_ADMIN', 'REPORT_REVIEW', 'RWD'),
    ('SUPER_ADMIN', 'CONTENT_MODERATE', 'RWD'),
    ('SUPER_ADMIN', 'USER_MANAGE', 'RWD'),
    ('SUPER_ADMIN', 'ROLE_PERMISSION_MANAGE', 'RWD'),
    ('SUPER_ADMIN', 'PLATFORM_STATS', 'RWD')
) AS g(role, sub_feature, level)
JOIN roles r ON r.internal_name = g.role
JOIN sub_features sf ON sf.name = g.sub_feature
JOIN permission_sub_features psf ON psf.sub_feature_id = sf.id
     AND psf.can_read = TRUE
     AND psf.can_write = (g.level IN ('RW', 'RWD'))
     AND psf.can_delete = (g.level = 'RWD');
