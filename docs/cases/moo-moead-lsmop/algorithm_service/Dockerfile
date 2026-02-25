FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar /app/app.jar
EXPOSE 18083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
