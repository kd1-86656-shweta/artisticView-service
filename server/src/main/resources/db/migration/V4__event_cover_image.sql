-- Optional cover image for an event (a media key such as event/{userId}/{uuid}.jpg).
ALTER TABLE events ADD COLUMN cover_key VARCHAR(300);
