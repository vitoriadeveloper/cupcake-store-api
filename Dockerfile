FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system cupcake && useradd --system --gid cupcake cupcake
COPY --from=build --chown=cupcake:cupcake /build/target/api-0.0.1-SNAPSHOT.jar app.jar
USER cupcake
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
