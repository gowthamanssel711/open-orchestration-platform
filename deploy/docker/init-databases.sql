-- Initialize databases for Open Orchestration Platform

CREATE DATABASE oop_runtime;
CREATE DATABASE oop_query;
CREATE DATABASE oop_audit;
CREATE DATABASE oop_analytics;
CREATE DATABASE oop_identity;
CREATE DATABASE oop_keycloak;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE oop_runtime TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_query TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_audit TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_analytics TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_identity TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_keycloak TO oop;
