# Stage 1: Build the application
FROM maven:3-openjdk-17 AS build
# Set the working directory inside the container
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
# Build the project, running tests and packaging the fat JAR
RUN mvn clean install -DskipTests

# Stage 2: Create the final runtime image
FROM eclipse-temurin:17-jre-alpine
# Set the working directory
WORKDIR /app
# Copy the executable JAR from the build stage's target directory
COPY --from=build /app/target/CustomLogAnalyzer-4.0.0-SNAPSHOT-jar-with-dependencies.jar ./CustomLogAnalyzer.jar

# Define the entry point for the container
# The arguments for tests and log content will be passed when running the container
ENTRYPOINT ["java", "-jar", "CustomLogAnalyzer.jar"]