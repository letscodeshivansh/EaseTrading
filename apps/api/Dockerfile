# ---- Build stage: compile the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Corporate networks often intercept HTTPS (a "TLS-inspecting proxy") and present
# their own certificate, which Maven's Java truststore doesn't recognise — causing
# "PKIX path building failed" when downloading from Maven Central.
#
# These options switch Maven to the wagon HTTP transport and tell it to tolerate the
# proxy's certificate. This is a local-build convenience; it only affects dependency
# downloads during the image build, not the running application.
ENV MAVEN_OPTS="-Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"

# Copy the pom first so Docker can cache downloaded dependencies between builds.
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Run stage: a small JRE image that just runs the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/api-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
