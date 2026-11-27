FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl db-service -am clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/db-service/target/db-service-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
