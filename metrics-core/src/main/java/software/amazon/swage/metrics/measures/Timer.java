/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License
 * is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.swage.metrics.measures;

import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

import java.time.Instant;

/**
 * A Timer is used to collect duration metrics for some set of operations.
 * Creation the object starts the timer, and it is stopped when the the close(0
 * method is called.
 * The general usage pattern is to use the try-with-resources construct to
 * guarantee that the timer is closed when the timed code block completes.
 *   Example:
 *     try(AutoCloseable timer = new Timer(metricContext, Metric.WorkTime) {
 *         // Do some work
 *     }
 */
public class Timer implements AutoCloseable {

    private final MetricContext context;
    private final Metric label;

    //TODO: explicit start method?
    private final long startTime = System.nanoTime();

    public Timer(
            final MetricContext context,
            final Metric label)
    {
        this.context = context;
        this.label = label;
    }

    /**
     * Stops the timer and records the measured duration.
     * Calling close subsequent times has no effect.
     */
    @Override
    public void close() {
        final long duration = System.nanoTime() - startTime;
        context.record(label, (duration/1000L), Unit.MICROSECOND, Instant.now());
    }

}
