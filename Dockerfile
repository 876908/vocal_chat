# === 阶段1：构建 ===
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# 先只复制 pom.xml，利用 Docker 缓存层 — 依赖没变就不会重新下载
COPY pom.xml .

RUN mvn dependency:go-offline -q

# 源码变了才重新编译（这一层之上的缓存都失效，但依赖层不动）
COPY src src

RUN mvn clean package -DskipTests -q

# === 阶段2：运行 ===
FROM eclipse-temurin:25-jre

# 安全：非 root 用户运行
RUN useradd --create-home --shell /bin/bash app
USER app

WORKDIR /app
COPY --from=builder --chown=app:app /build/target/*.jar app.jar

EXPOSE 8080

# 容器内存自适应 + GC 调优 + 加速随机数源（避免熵池耗尽阻塞）
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
