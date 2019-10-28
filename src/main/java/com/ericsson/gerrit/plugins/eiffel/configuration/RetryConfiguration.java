package com.ericsson.gerrit.plugins.eiffel.configuration;

import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

public class RetryConfiguration {

    private static final long INITIAL_INTERVAL = 1000;
    private static final int MULTIPLIER = 2;
    private static final int MAX_ATTEMPTS = 3;
    private static final String RETRY_SERVICE_NAME = "generateAndPublish";

    private Retry retry;

    public RetryConfiguration() {
        IntervalFunction interval = IntervalFunction.ofExponentialBackoff(INITIAL_INTERVAL,
                MULTIPLIER);
        RetryConfig config = RetryConfig.custom()
                                        .intervalFunction(interval)
                                        .maxAttempts(MAX_ATTEMPTS)
                                        .build();
        RetryRegistry registry = RetryRegistry.of(config);
        retry = registry.retry(RETRY_SERVICE_NAME);
    }

    public Retry getRetryPolicy() {
        return retry;
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }
}
