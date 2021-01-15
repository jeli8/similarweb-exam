package com.similarweb.demo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinOnServers {
    private final List<String> serversList;
    private final AtomicInteger counter = new AtomicInteger(-1);

    public RoundRobinOnServers(List<String> serversList) {
        this.serversList = serversList;
    }

    public String getServer() {
        int currIndex;
        int nextIndex;
        do {
            currIndex = counter.get();
            nextIndex =currIndex < Integer.MAX_VALUE ? currIndex + 1 : 0;

        } while (!counter.compareAndSet(currIndex, nextIndex));
        return serversList.get(nextIndex % serversList.size());
    }
}

