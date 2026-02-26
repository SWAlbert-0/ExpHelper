ARG WEBAPP_BASE_IMAGE=eclipse-temurin:17-jre
FROM ${WEBAPP_BASE_IMAGE}

ARG JAR_FILE=exphlp/api/webApp/target/webApp-1.0-SNAPSHOT.jar
WORKDIR /app

# Build-task runtime dependencies (for uploaded algorithm source build/start in containerized webapp)
RUN rm -f /etc/apt/apt.conf.d/*proxy* \
  && unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy \
  && apt-get -o Acquire::http::Proxy=false -o Acquire::https::Proxy=false update \
  && apt-get install -y --no-install-recommends docker.io unzip curl jq ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY ${JAR_FILE} /app/webapp.jar
COPY scripts/tasks/build-uploaded-alg.sh /app/scripts/tasks/build-uploaded-alg.sh
RUN chmod +x /app/scripts/tasks/build-uploaded-alg.sh

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/webapp.jar"]
