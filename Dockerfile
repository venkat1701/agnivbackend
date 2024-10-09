
FROM eclipse-temurin:17-jdk


WORKDIR /app
COPY target/agnivbackend-0.0.1-SNAPSHOT.jar agnivbackend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "agnivbackend.jar"]