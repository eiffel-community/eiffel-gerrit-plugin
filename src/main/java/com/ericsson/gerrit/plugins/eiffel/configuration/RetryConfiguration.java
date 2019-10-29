/*
   Copyright 2019 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

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
