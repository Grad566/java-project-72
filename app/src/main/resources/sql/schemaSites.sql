DROP TABLE IF EXISTS url_checks;
DROP TABLE IF EXISTS sites;

CREATE TABLE sites (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
