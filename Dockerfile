FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY common-module common-module
COPY account-module account-module
COPY transaction-module transaction-module
COPY application application

RUN chmod +x ./gradlew
RUN ./gradlew :application:bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/application/build/libs/wirebarley-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
