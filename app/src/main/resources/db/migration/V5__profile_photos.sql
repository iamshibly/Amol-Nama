CREATE TABLE user_profile_photos (
    user_id INTEGER NOT NULL PRIMARY KEY,
    image_data MEDIUMBLOB NOT NULL,
    CONSTRAINT fk_profile_photo_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
