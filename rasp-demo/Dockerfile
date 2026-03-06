# 构建阶段：编译 Java（拉取走 daemon registry-mirrors，见 cicd/docker/README.md）
FROM eclipse-temurin:8-jdk-alpine AS builder
WORKDIR /build
COPY DiagnosticsServer.java .
RUN javac -encoding UTF-8 DiagnosticsServer.java

# 运行阶段：仅 JRE，RASP 可挂到该 JVM
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=builder /build/*.class .
COPY index.html .
COPY config /app/config
EXPOSE 8080
# 若需挂 RASP：可在此挂 agent 并加 -javaagent
ENTRYPOINT ["java", "DiagnosticsServer"]
