-- V001: Enable PostgreSQL extensions required by Qalam
-- uuid-ossp  → uuid_generate_v4() for UUID PKs
-- pg_trgm    → trigram GIN indexes for fuzzy Arabic/translation search
-- unaccent   → accent-insensitive search normalization

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "unaccent";
