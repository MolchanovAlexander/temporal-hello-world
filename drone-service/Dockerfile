FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl drone-service -am clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/drone-service/target/drone-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
