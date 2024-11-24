FROM openjdk:17-jdk
WORKDIR /app
COPY build/libs/kafka_consumer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

#docker build -t kafka-consumer-app .
#docker run --name kafka-consumer-container-2 kafka-consumer-app
