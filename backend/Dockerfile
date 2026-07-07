FROM docker.1ms.run/library/maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 和源代码
COPY pom.xml .
COPY src ./src

# 构建 JAR 包
RUN mvn clean package -DskipTests

# 第二阶段：运行时镜像
FROM docker.1ms.run/library/eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /app/target/algorithm-viz-backend-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
