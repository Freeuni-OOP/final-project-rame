# ---- build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy Maven wrapper first so dependency layer is cached
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Fix Windows CRLF line endings so the script runs on Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build the jar
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ---- runtime stage ----
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
