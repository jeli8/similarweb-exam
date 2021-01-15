package com.similarweb.demo.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricsManager {
    private MeterRegistry meterRegistry;
    private static MetricsManager managerInstance;

    private final Counter getCallsCounter;

//    private final Counter postRequestsRetriesCounter = Counter.builder("load.balancer.demo")
//            .description("The number of GET calls pass through the load balancer service")
//            .register(new SimpleMeterRegistry());


    public MetricsManager(MeterRegistry registry) {
        meterRegistry = registry;
        getCallsCounter = Counter.builder("load.balancer.demo")
                .description("The number of GET calls pass through the load balancer service")
                .register(meterRegistry);
    }

    public static MetricsManager getMetricsInstance() {
        if (managerInstance == null) {
            managerInstance = new MetricsManager(new SimpleMeterRegistry());
        }
        return managerInstance;
    }


    public void increaseGetCallsCounter() {
        getCallsCounter.increment();
    }

}
