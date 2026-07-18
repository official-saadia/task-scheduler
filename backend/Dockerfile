# ---------- build ----------
# The Maven wrapper is script-only (see .mvn/wrapper/maven-wrapper.properties),
# so it fetches Maven itself. That means a plain JDK image is enough here and we
# don't depend on a maven:*-java25 tag existing.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Wrapper and pom first: this layer is cached and only re-runs when pom.xml
# changes, so editing source doesn't re-download the dependency tree.
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY backend/src ./src
RUN ./mvnw -B clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:25-jre

# The backup and report handlers shell out, so the runtime image needs the
# binaries those scripts call — not just a JVM:
#   mysqldump + gzip -> backup.sh
#   curl            -> dlq_report.sh
#   bash            -> both use `set -o pipefail`, which dash does not support
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      default-mysql-client curl gzip bash \
 && rm -rf /var/lib/apt/lists/*

# Run as a non-root user. This matters more than it looks: as root every
# permission bug in a task script silently passes, then fails in production.
RUN useradd --create-home --uid 1001 scheduler

COPY --from=build /build/target/*.jar /app/app.jar
COPY scripts/ /opt/scripts/

RUN chmod +x /opt/scripts/*.sh \
 && mkdir -p /opt/reports /opt/backups \
 && chown -R scheduler:scheduler /opt/reports /opt/backups

USER scheduler
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
