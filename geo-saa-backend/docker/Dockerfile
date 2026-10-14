# syntax=docker/dockerfile:1
# ============================================================
# 后端生产镜像（多阶段构建）
#
# 与上游原版的差异（本次修复）：
#  1. 原版 COPY target/geo-saa-backend.jar —— 但 .gitignore 第 6 行忽略 *.jar，
#     全新 clone 的仓库里 target/ 是空的，`docker compose up --build` 直接构建失败。
#     改为在容器内执行 mvn package，jar 成为构建产物而非前置依赖。
#  2. 原版基础镜像 openjdk:17-jdk-slim 不含 curl，而 docker-compose.yml 的
#     healthcheck 用 `curl -fsS ...` 探测 → 容器永远 unhealthy →
#     frontend 的 `depends_on: service_healthy` 死锁等不到后端 → 整条链路起不来。
#     改用 JVM 自身发起 HTTP 探测，不依赖镜像里额外装包。
#  3. jdk-slim 换成 jre，运行时体积更小（无需 javac）。
# ============================================================

# ---------- 构建阶段：需要完整 JDK + Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先只拷 pom.xml，让依赖层能被 Docker 缓存复用：
# 仅改 Java 源码时不会重新下载依赖，重建从数分钟降到数十秒。
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# 把健康探针编成独立 jar：JRE 镜像不含 javac，只能在构建阶段编译。
# java -cp probe.jar HealthProbe —— 不污染应用 jar，探针代码与应用生命周期解耦。
RUN mkdir -p probe-src && cp docker/HealthProbe.java probe-src/ \
    && javac -d probe-classes probe-src/HealthProbe.java \
    && jar cfe probe.jar HealthProbe -C probe-classes .


# ---------- 运行阶段：只需 JRE ----------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 以非 root 运行，降低容器逃逸后的影响面
RUN groupadd --system geo && useradd --system --gid geo --home /app geo
# jar 名必须与 pom 的 artifactId-version 精确一致：pom.xml 未配 <finalName>，
# 产出为 geo-saa-backend-2.0.0.jar（原版写 geo-saa-backend.jar，本地 mvn 打包成功也会 COPY 失败）。
# 不使用通配符：spring-boot repackage 会同时留下 *.jar.original，通配会匹配到两个文件。
COPY --from=builder --chown=geo:geo /build/target/geo-saa-backend-2.0.0.jar app.jar
COPY --from=builder --chown=geo:geo /build/probe.jar probe.jar

USER geo

# 中文内容生成需要正确时区与字符集，否则日志与落库内容会出现乱码
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8

EXPOSE 8080

# 健康检查：不用 curl（JRE 镜像不含该工具），改用 JVM 的 java.net.http。
# 与 compose 中 backend.healthcheck 的语义保持一致：业务健康端点、免鉴权。
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD ["java", "-cp", "probe.jar", "HealthProbe"]

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]