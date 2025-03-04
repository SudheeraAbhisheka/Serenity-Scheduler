FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/CassandraInit-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
