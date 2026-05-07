# =====================================================================
# Stage 1 — build
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Cache de dependências: copia só o pom primeiro
COPY pom.xml ./
RUN mvn -B -e -ntp dependency:go-offline

# Agora copia o código e empacota
COPY src ./src
RUN mvn -B -e -ntp -DskipTests -Djacoco.skip=true package

# =====================================================================
# Stage 2 — runtime (JRE Alpine, sem Maven/Gradle/JDK)
# =====================================================================
FROM eclipse-temurin:21-jre-alpine

ENV LANG=C.UTF-8 \
    JAVA_OPTS="" \
    QUARKUS_HTTP_HOST=0.0.0.0

WORKDIR /app

# Quarkus fast-jar layout
COPY --from=build /workspace/target/quarkus-app/lib/      /app/lib/
COPY --from=build /workspace/target/quarkus-app/*.jar     /app/
COPY --from=build /workspace/target/quarkus-app/app/      /app/app/
COPY --from=build /workspace/target/quarkus-app/quarkus/  /app/quarkus/

# Usuário não-root
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]
