# Alpine Linux with OpenJDK JRE
FROM openjdk:11.0.7-jre-slim

WORKDIR /opt/app
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/application.properties config/application.properties


# run application with this command line
CMD ["/usr/local/openjdk-11/bin/java", "-jar", "-Dspring.profiles.active=default", "-Dspring.config.location=/opt/app/config/", "/opt/app/app.jar"]