
FROM eclipse-temurin:17-jdk


WORKDIR /app
COPY target/agnivbackend.jar agnivbackend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "agnivbackend.jar"]