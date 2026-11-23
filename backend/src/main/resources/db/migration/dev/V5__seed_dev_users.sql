-- Dev profile only (classpath:db/migration/dev). REMOVE IN STAGE 10D.
-- password for both accounts: password (BCrypt strength 10)
INSERT INTO users (id, name, email, password_hash, token_version)
VALUES (1, 'Dev User A', 'dev-user-a@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 0),
       (2, 'Dev User B', 'dev-user-b@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 0);

ALTER TABLE users AUTO_INCREMENT = 3;