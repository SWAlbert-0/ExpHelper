ARG WEBAPP_BASE_IMAGE=eclipse-temurin:17-jre
FROM ${WEBAPP_BASE_IMAGE}

ARG JAR_FILE=exphlp/api/webApp/target/webApp-1.0-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} /app/webapp.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/webapp.jar"]
