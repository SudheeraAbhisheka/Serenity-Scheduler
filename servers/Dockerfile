FROM openjdk:17-jdk
WORKDIR /app
COPY build/libs/servers-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
