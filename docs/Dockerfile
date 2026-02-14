# 使用官方 OpenJDK 8 映像作为基础镜像
FROM openjdk:8-jdk-alpine

# 复制应用程序的JAR文件到容器中
ADD webApp-1.0-SNAPSHOT.jar webapp-1.0.jar

# 暴露应用程序的端口
EXPOSE 8080

# 启动Spring Boot应用程序
CMD ["java", "-jar", "webapp-1.0.jar"]