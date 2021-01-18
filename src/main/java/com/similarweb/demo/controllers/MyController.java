package com.similarweb.demo.controllers;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.Histogram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping
@Component
public class MyController {
    private static final Logger logger = LogManager.getLogger(MyController.class);

    @Value("${servers}")
    private String serversList;

    @Value("${time.to.wait.for.a.single.server.response.milliseconds}")
    private long timeToWaitForServerMs;

    @Autowired
    private RestTemplate restTemplate;

    private final AtomicInteger counter = new AtomicInteger(-1);

    private final Counter getCallsCounter;
    private final Counter postCallsRetriesCounterRegister;
    private final Counter postCallsRetriesCounterChangePass;
    private final Counter postCallsSuccessRate;
    private final Histogram postCallsFirstReplyLatency;

    MyController(MeterRegistry registry) {
        //Initialize metrics for the controller
        getCallsCounter = Counter.builder("get.calls.count.total")
                .description("The number of GET calls pass through the load balancer service")
                .register(registry);
        postCallsRetriesCounterRegister = Counter.builder("post.calls.num.of.retries")
                .tag("path", "register")
                .description("The number we had to perform retry cause one of the backend server is not responding")
                .register(registry);
        postCallsRetriesCounterChangePass = Counter.builder("post.calls.num.of.retries")
                .tag("path", "changePassword")
                .description("The number we had to perform retry cause one of the backend server is not responding")
                .register(registry);
        postCallsSuccessRate = Counter.builder("post.calls.success.rate")
                .description("The success rate of sending post calls")
                .register(registry);
        postCallsFirstReplyLatency = Histogram.build("post_calls_first_reply_milliseconds",
                "The time in milliseconds took the first reply to get back for post calls " +
                        "(the first server which returned an answer)")
                .buckets(1,10,100,500)
                .register();
    }

    private List<String> getServersList() {
        return Arrays.asList(serversList.split(",", -1));
    }


    @RequestMapping(path = "login", method = RequestMethod.GET)
    public ResponseEntity<?> login() {
        String serverId = getServerByRoundRobin();
        logger.info(String.format("Going to send GET request to server %s", serverId));
        WebClient webClient = WebClient.create();
        //Sending the request and ignoring the response
        webClient.get().uri("https://{serverId}/login", serverId).retrieve().bodyToMono(Void.class);
        getCallsCounter.increment();
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> postRequest(String path) {
        AtomicInteger isOneSuccess = new AtomicInteger(0);
        List<String> servers = getServersList();

        WebClient webClient = WebClient.create();
        Histogram.Timer timer = postCallsFirstReplyLatency.startTimer();
        for (String server : servers) {
            logger.info(String.format("Sending POST request for %s for server %s", path, server));
            webClient
                    .post()
                    .uri(String.format("https://%s/%s", server, path))
                    .exchange()
                    .timeout(Duration.ofMillis(500))
                    .retryWhen(Retry.backoff(10, Duration.ofMillis(500))
                            .doAfterRetry(retrySignal -> {
                                if (path.startsWith("reg")) {
                                    postCallsRetriesCounterRegister.increment();
                                } else if (path.startsWith("change")) {
                                    postCallsRetriesCounterChangePass.increment();
                                }
                                logger.info(String.format("Got a retry for %s request", path));
                            }))
                    .doOnSuccess(clientResponse -> {
                        postCallsSuccessRate.increment();
                        logger.info("Event is received by 1 server at least");
                        isOneSuccess.incrementAndGet();
                        ResponseEntity.ok().build();
                    })
                    .subscribe();
        }

        long start = System.currentTimeMillis();
        while (isOneSuccess.get() < 1) {
            //If timeToWaitForServerMs has negative value, the loop will wait for servers' response forever...
            if ((timeToWaitForServerMs > 0) && ((System.currentTimeMillis() - start) > timeToWaitForServerMs)) {
                logger.info(String.format("Waiting for response from server in %s request, for longer than %d ms, continuing without response", path, timeToWaitForServerMs));
                break;
            }
        }
        timer.observeDuration();
        return ResponseEntity.created(URI.create("")).build();
    }


    @RequestMapping(path = "register", method = RequestMethod.POST)
    public ResponseEntity<?> register() {
        return postRequest("register");
    }

    @RequestMapping(path = "changePassword", method = RequestMethod.POST)
    public ResponseEntity<?> changePassword() {
        return postRequest("changePassword");
    }

    private String getServerByRoundRobin() {
        List<String> serversList = getServersList();
        int currIndex;
        int nextIndex;
        do {
            currIndex = counter.get();
            nextIndex =currIndex < Integer.MAX_VALUE ? currIndex + 1 : 0;

        } while (!counter.compareAndSet(currIndex, nextIndex));
        return serversList.get(nextIndex % serversList.size());
    }

}