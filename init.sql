CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE media (
                       id SERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       media_type VARCHAR(50) NOT NULL,
                       release_year INT,
                       genres TEXT[] DEFAULT '{}',
                       age_restriction INT DEFAULT 0,
                       creator_id INT NOT NULL,
                       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_media_creator
                           FOREIGN KEY (creator_id)
                               REFERENCES users (id)
                               ON DELETE CASCADE
);

CREATE TABLE tokens (
    token VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_token_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE TABLE ratings (
                         id SERIAL PRIMARY KEY,
                         media_id INT NOT NULL,
                         user_id INT NOT NULL,
                         stars INT NOT NULL CHECK (stars >= 1 AND stars <= 5),
                         comment TEXT,
                         visible BOOLEAN DEFAULT FALSE,
                         created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_rating_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE,
                         CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                         CONSTRAINT unique_rating_per_user_media UNIQUE (media_id, user_id)
);

CREATE TABLE favorites (
                           user_id INT NOT NULL,
                           media_id INT NOT NULL,
                           created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (user_id, media_id), -- Ein User kann einen Film nur 1x favorisieren
                           CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                           CONSTRAINT fk_fav_media FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE
);

CREATE TABLE rating_likes (
                              user_id INT NOT NULL,
                              rating_id INT NOT NULL,
                              created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (user_id, rating_id), -- Ein Like pro User pro Rating
                              CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                              CONSTRAINT fk_like_rating FOREIGN KEY (rating_id) REFERENCES ratings (id) ON DELETE CASCADE
);

INSERT INTO users (username, password_hash)
VALUES ('admin', '$2a$10$wKz0b9I/v6K2L5Bw0S5Y6e.dYxYl8K8P1w3Xv2A7A0V9V8F5F1D7K'); -- Passwort ist 'testpw' (nur zum testen, Hash sollte neu generiert werden)