# Alpine Linux with OpenJDK JRE
FROM openjdk:11.0.7-jre-slim

COPY target/demo-0.0.1-SNAPSHOT.jar /app.jar

# run application with this command line
CMD ["/usr/local/openjdk-11/bin/java", "-jar", "-Dspring.profiles.active=default", "/app.jar"]