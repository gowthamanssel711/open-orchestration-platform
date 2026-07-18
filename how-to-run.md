
Service	Port
Gateway	8080
Runtime Bundle	8081
Query Service	8082
Audit Service	8083
Analytics	8084
Notifications	8085
Identity	8087
Keycloak	8180
Kafka	29092
PostgreSQL	5432
Redis	6379


# Drop and recreate the query database
podman exec oop-postgres psql -U oop -c "DROP DATABASE IF EXISTS oop_query;"
podman exec oop-postgres psql -U oop -c "CREATE DATABASE oop_query OWNER oop;"
