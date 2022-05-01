FROM eclipse-temurin:11.0.15_10-jre

COPY build/libs/kafka-ops.jar /usr/local/bin/kafka-ops.jar

ENTRYPOINT ["java", "-jar", "/usr/local/bin/kafka-ops.jar"]
