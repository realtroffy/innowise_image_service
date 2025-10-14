FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /opt/app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /application

COPY --from=builder /opt/app/target/*.jar /application/app.jar

ENTRYPOINT ["java", "-jar", "/application/app.jar"]
