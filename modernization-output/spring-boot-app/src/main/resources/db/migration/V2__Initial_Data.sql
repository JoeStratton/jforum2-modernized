-- V2__Initial_Data.sql

-- Create default groups
INSERT INTO groups (name, description, created_at, created_by)
VALUES 
('ADMIN', 'Administrators with full system access', CURRENT_TIMESTAMP, 'system'),
('MODERATOR', 'Forum moderators with content management rights', CURRENT_TIMESTAMP, 'system'),
('USER', 'Regular registered users', CURRENT_TIMESTAMP, 'system');

-- Create default permissions
INSERT INTO permissions (name, description, created_at, created_by)
VALUES
('FORUM_VIEW', 'View forum content', CURRENT_TIMESTAMP, 'system'),
('FORUM_CREATE_TOPIC', 'Create new topics in forums', CURRENT_TIMESTAMP, 'system'),
('FORUM_REPLY', 'Reply to existing topics', CURRENT_TIMESTAMP, 'system'),
('FORUM_EDIT_OWN', 'Edit own posts', CURRENT_TIMESTAMP, 'system'),
('FORUM_DELETE_OWN', 'Delete own posts', CURRENT_TIMESTAMP, 'system'),
('FORUM_MODERATOR', 'Moderate forum content', CURRENT_TIMESTAMP, 'system'),
('USER_ADMIN', 'Administer user accounts', CURRENT_TIMESTAMP, 'system'),
('FORUM_ADMIN', 'Administer forum structure', CURRENT_TIMESTAMP, 'system'),
('SYSTEM_ADMIN', 'Administer system settings', CURRENT_TIMESTAMP, 'system');

-- Assign permissions to groups
-- Admin group gets all permissions
INSERT INTO group_permissions (group_id, permission_id)
SELECT 
    (SELECT id FROM groups WHERE name = 'ADMIN'),
    id
FROM permissions;

-- Moderator group gets moderation permissions
INSERT INTO group_permissions (group_id, permission_id)
SELECT 
    (SELECT id FROM groups WHERE name = 'MODERATOR'),
    id
FROM permissions 
WHERE name IN ('FORUM_VIEW', 'FORUM_CREATE_TOPIC', 'FORUM_REPLY', 'FORUM_EDIT_OWN', 'FORUM_DELETE_OWN', 'FORUM_MODERATOR');

-- User group gets basic permissions
INSERT INTO group_permissions (group_id, permission_id)
SELECT 
    (SELECT id FROM groups WHERE name = 'USER'),
    id
FROM permissions 
WHERE name IN ('FORUM_VIEW', 'FORUM_CREATE_TOPIC', 'FORUM_REPLY', 'FORUM_EDIT_OWN', 'FORUM_DELETE_OWN');

-- Create admin user with bcrypt encoded password 'admin'
INSERT INTO users (
    username, 
    password, 
    email, 
    first_name, 
    last_name, 
    registration_date, 
    is_active, 
    is_email_verified, 
    created_at, 
    created_by
)
VALUES (
    'admin',
    '$2a$10$hKDVYxLefVHV/vtuPhWD3OigtRyOykRLDdUAp80Z1crSoS1lFqaFS',
    'admin@example.com',
    'Admin',
    'User',
    CURRENT_TIMESTAMP,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    'system'
);

-- Add admin user to ADMIN group
INSERT INTO user_groups (user_id, group_id)
VALUES (
    (SELECT id FROM users WHERE username = 'admin'),
    (SELECT id FROM groups WHERE name = 'ADMIN')
);

-- Create default category and forum
INSERT INTO categories (name, display_order, created_at, created_by)
VALUES ('General', 0, CURRENT_TIMESTAMP, 'system');

INSERT INTO forums (
    category_id, 
    name, 
    description, 
    display_order, 
    created_at, 
    created_by
)
VALUES (
    (SELECT id FROM categories WHERE name = 'General'),
    'Welcome',
    'Welcome to the forum! Introduce yourself here.',
    0,
    CURRENT_TIMESTAMP,
    'system'
);