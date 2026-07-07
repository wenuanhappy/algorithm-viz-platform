FROM docker.1ms.run/library/eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY app.jar app.jar

EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
