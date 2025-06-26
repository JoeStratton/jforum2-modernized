-- V1__Initial_Schema.sql

-- Users and authentication
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    registration_date TIMESTAMP NOT NULL,
    last_visit TIMESTAMP,
    activation_key VARCHAR(32),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url VARCHAR(255),
    signature TEXT,
    post_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50)
);

CREATE TABLE groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (parent_id) REFERENCES groups(id)
);

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50)
);

CREATE TABLE group_permissions (
    group_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, permission_id),
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE user_groups (
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, group_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (group_id) REFERENCES groups(id)
);

-- Forum structure
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    moderated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50)
);

CREATE TABLE forums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    topic_count INT NOT NULL DEFAULT 0,
    post_count INT NOT NULL DEFAULT 0,
    last_post_id BIGINT,
    moderated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE forum_moderators (
    forum_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (forum_id, user_id),
    FOREIGN KEY (forum_id) REFERENCES forums(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    forum_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    first_post_id BIGINT,
    last_post_id BIGINT,
    moderated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (forum_id) REFERENCES forums(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    forum_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    edit_count INT NOT NULL DEFAULT 0,
    last_edit_time TIMESTAMP,
    poster_ip VARCHAR(45),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (forum_id) REFERENCES forums(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Add foreign key constraints for posts
ALTER TABLE topics ADD CONSTRAINT fk_topics_first_post FOREIGN KEY (first_post_id) REFERENCES posts(id);
ALTER TABLE topics ADD CONSTRAINT fk_topics_last_post FOREIGN KEY (last_post_id) REFERENCES posts(id);
ALTER TABLE forums ADD CONSTRAINT fk_forums_last_post FOREIGN KEY (last_post_id) REFERENCES posts(id);

-- Attachments
CREATE TABLE attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    physical_filename VARCHAR(255) NOT NULL,
    filesize BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Subscriptions
CREATE TABLE forums_watch (
    forum_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (forum_id, user_id),
    FOREIGN KEY (forum_id) REFERENCES forums(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE topics_watch (
    topic_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (topic_id, user_id),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Private messaging
CREATE TABLE private_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    from_user_id BIGINT NOT NULL,
    parent_message_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (from_user_id) REFERENCES users(id),
    FOREIGN KEY (parent_message_id) REFERENCES private_messages(id)
);

CREATE TABLE private_message_recipients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    folder VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (message_id) REFERENCES private_messages(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Search
CREATE TABLE search_words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50)
);

CREATE TABLE search_index (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    forum_id BIGINT NOT NULL,
    relevance FLOAT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (word_id) REFERENCES search_words(id),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (forum_id) REFERENCES forums(id)
);

-- Bookmarks
CREATE TABLE bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    element_id BIGINT NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Moderation
CREATE TABLE moderation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    post_id BIGINT,
    topic_id BIGINT,
    forum_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (forum_id) REFERENCES forums(id)
);

-- Bans
CREATE TABLE banlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    ip_address VARCHAR(45),
    email VARCHAR(255),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_by VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_forums_category ON forums(category_id);
CREATE INDEX idx_topics_forum ON topics(forum_id);
CREATE INDEX idx_topics_user ON topics(user_id);
CREATE INDEX idx_posts_topic ON posts(topic_id);
CREATE INDEX idx_posts_forum ON posts(forum_id);
CREATE INDEX idx_posts_user ON posts(user_id);
CREATE INDEX idx_attachments_post ON attachments(post_id);
CREATE INDEX idx_attachments_user ON attachments(user_id);
CREATE INDEX idx_pm_from_user ON private_messages(from_user_id);
CREATE INDEX idx_pmr_user ON private_message_recipients(user_id);
CREATE INDEX idx_pmr_folder ON private_message_recipients(user_id, folder);
CREATE INDEX idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_element ON bookmarks(type, element_id);
CREATE INDEX idx_modlog_user ON moderation_log(user_id);
CREATE INDEX idx_modlog_forum ON moderation_log(forum_id);
CREATE INDEX idx_search_word ON search_index(word_id);
CREATE INDEX idx_search_post ON search_index(post_id);