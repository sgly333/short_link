# 多阶段：在镜像内 Maven 打包指定模块的 *-exec.jar（根 pom 已 classifier=exec）
#
# 在仓库根目录执行：
#   docker build -f docker/Dockerfile --build-arg MAVEN_MODULE=shortlink-gateway --build-arg TARGET_DIR=gateway --build-arg APP_PORT=8000 -t shortlink-gateway:latest .
#   docker build -f docker/Dockerfile --build-arg MAVEN_MODULE=shortlink-admin --build-arg TARGET_DIR=admin --build-arg APP_PORT=8002 -t shortlink-admin:latest .
#   docker build -f docker/Dockerfile --build-arg MAVEN_MODULE=shortlink-project --build-arg TARGET_DIR=project --build-arg APP_PORT=8001 -t shortlink-project:latest .

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY format ./format
COPY admin ./admin
COPY project ./project
COPY gateway ./gateway
COPY aggregation ./aggregation

ARG MAVEN_MODULE=shortlink-gateway
RUN mvn -B -pl ${MAVEN_MODULE} -am clean package -DskipTests -Dspotless.apply.skip=true

FROM eclipse-temurin:17-jre-jammy

ARG MAVEN_MODULE=shortlink-gateway
ARG TARGET_DIR=gateway
ARG APP_PORT=8000

WORKDIR /app
COPY --from=build /app/${TARGET_DIR}/target/${MAVEN_MODULE}-exec.jar app.jar

EXPOSE ${APP_PORT}
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
