FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# 1) Build scripts first: the dependency layer below is cached and only
#    re-downloaded when build.gradle / settings.gradle change.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew dependencies --no-daemon > /dev/null

# 2) Sources change often, so they are copied last.
COPY src src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=builder --chown=app:app /app/build/libs/*.jar app.jar

USER app

EXPOSE 8080

# Size the heap relative to the container memory limit (JVM default is 25%).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
