FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY platform/runtime-bundle/pom.xml platform/runtime-bundle/
COPY platform/query-service/pom.xml platform/query-service/
COPY platform/audit-service/pom.xml platform/audit-service/
COPY platform/analytics-service/pom.xml platform/analytics-service/
COPY platform/notification-service/pom.xml platform/notification-service/
COPY platform/gateway/pom.xml platform/gateway/
COPY platform/identity-service/pom.xml platform/identity-service/
COPY platform/connector-service/pom.xml platform/connector-service/

RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

COPY platform/ platform/
ARG SERVICE_NAME
RUN mvn clean package -DskipTests -pl platform/${SERVICE_NAME} -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S oop && adduser -S oop -G oop
USER oop

ARG SERVICE_NAME
COPY --from=build /app/platform/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
