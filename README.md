# SimilarWeb exercise --> Load Balancer (Eli Yaacov)

This repository describes a solution to the problem presented by SimilarWeb company for 
a load balancer which is required to handle a state of N servers and serve GET and POST requests.

### Minimal assumptions:
 - The code only support POST (register, changePassword) and GET (login) requests.
 - The service doesn't handle body or parameters in the above calls and only deliver them 
 to a given list of servers according to the following strategy - 
 GET requests are handle with Round-Robin algorithm between servers 
 POST calls sent to all servers, waiting for a success response 
 and verify it for each one of the servers for about 5 seconds max (also configurable)
 
 
## Installation process:
There are two given ways to install the service and run it:
    1. Docker file - as a docker container / kubernetes pod (using the docker file image)
    2. java command line (directly as java process) --> might be easier for running locally
    
1. For docker file, please run the following command:
    - From the service root directory: `./mvnw package` --> 
    it will create a JAR file under `target` folder.
    - Also from root directory: `docker build -f Dockerfile . -t demo-load-balancer` --> 
    This will build a docker image on local docker environment which already contain the JAR and ready to run the service
    
2. For java command, simply go to the root directory and run:
    `./mvnw package && java -jar target/demo-0.0.1-SNAPSHOT.jar` --> The command will 
    package all the relevant for the JAR and then run it. 
    
For both cases the configuration file which will be used is under `src/main/resources/application.properties`.
In order to test the application or deploy it with various parameters, change the mentioned file, and the 
docker image will use it. 

NOTE: For both cases, the version is being handled by pom.xml file (root directory)

Another NOTE: All code was written and compiled with IntelliJ IDEA, all the relevant files for IDEA are 
committed to the repository in order to allow quick review, testing and changing the exist code.    


### Service configuration
All configuration properties are under `resources/application.properties` file.
The main parameter and most critical for the service is *`servers`*. 
Its a comma separated string which should contain the exact hostnames we want to route the calls

Another important param is `time.to.wait.for.a.single.server.response.milliseconds`. 
I defined it because of a case of a server that is currently down for any reason and not answering 
is bad practice and can cost us in loop forever, waste of resources and basically not providing 
a service for the rest of the calls. I took a business decision and limit it to X ms. It's configurable, 
so we can change per environment if we would like.
   
   
### Monitoring
The service expose metrics in prometheus structure 
(the assumption was it's a generic structure enough so we can easily integrate this tool into exist environment
using existing tool, such as prometheus).
All service metrics, including JVM metrics, Spring, incoming-outgoing bytes and requests, 
fails/success rate for calls and more, are exposed via the service hostname and port (8080)
under - `http://localhost:8080/actuator/prometheus` path.

## Proposal for Testing
In order to test the app, deploy it in a docker-compose env or kubernetes cluster using the 
image from the dockerFile. Then, deploy another sample app/service which answer on /login, 
for example and define that hostname in `application.properties` file. after performing few 
GET and POST, you'll be able to see the calls in logs (tracking them), view metrics for the service 
and get the responses for all calls.


# Written by: Eli Yaacov   
