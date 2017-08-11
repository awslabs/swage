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
package software.amazon.swage.metrics;

import static software.amazon.swage.metrics.Metric.define;

/**
 * Metrics that are common across many applications/services.
 *
 * TODO: break this up into actual standard metrics, specific service metrics
 *
 */
public class StandardMetric {
    private StandardMetric() {}

    /**
     * Wallclock time for handling a piece of work.
     * The Time measurement is a metric that was defined by some of Amazon's original
     * request-reply frameworks to be a measurement of the amount of time spent processing
     * a request. This time includes time spent in the framework, libraries and service
     * logic. However, it doesn't capture request reading from the network or response
     * writing to the network.
     */
    public static final Metric TIME = define("Time");

    /**
     * Wallclock time, from start to finish, for handling a piece of work.
     * The TotalTime measurement is a metric that is similar to Time, the only
     * difference being it will include the time spent reading and writing to
     * the network from the service's perspective.
     */
    public static final Metric TOTAL_TIME = define("TotalTime");

    /**
     * Wallclock time, including only service logic, for handling a piece of
     * work.
     * The ActivityTime measurement is a metric that captures the amount of
     * time spent in service-specific 'activity' code only, independent of any
     * framework scaffolding or network time.
     */
    public static final Metric ACTIVITY_TIME = define("ActivityTime");

    /**
     * Time spent in system code.
     */
    public static final Metric SYSTEM_TIME = define("SystemTime");

    /**
     * Time spent in user code.
     */
    public static final Metric USER_TIME = define("UserTime");

    /**
     * The amount of time the request spent in a framework queue waiting to be
     * handled by the service logic.
     * Generally only present in systems with an explicit queue, such as those
     * using continuations.
     */
    public static final Metric QUEUE_TIME = define("QueueTime");

    /**
     * Number of concurrent requests.
     * Useful for determining the percent of your fleet's capacity in use.
     * OutstandingRequests is generally measured right as a request begins, and
     * emitted after the request finishes so one can introspect how busy the
     * system was when the request was processed.
     * On systems using a threadpool, this may result in a maximum value the
     * size of the pool, not necessarily including queued requests.
     */
    public static final Metric OUTSTANDING_REQUESTS = define("OutstandingRequests");

    /**
     * Number of requests rejected to prevent brownout.
     * If a brownout protection feature is present, this metric counts requests
     * explicitly rejected due to overloading.
     */
    public static final Metric LOAD_SHED = define("LoadShed");

    /**
     * Size of the result (in bytes) of a piece of work.
     */
    public static final Metric RESPONSE_SIZE = define("ResponseSize");

    /**
     * An error that was the service's fault (think 500 error codes)
     */
    public static final Metric FAULT = define("Fault");

    /**
     * An error that was the client's fault (think 4xx error codes)
     */
    public static final Metric ERROR = define("Error");

    /**
     * An internal failure.
     * This may or may not result in a fault response.
     */
    public static final Metric FAILURE = define("Failure");

    /**
     * Time spent marshaling the request
     */
    public static final Metric TRANSMUTER_TIME = define("TransmuterTime");

    /**
     * The number of requests that were rejected because they don't match the
     * validation rules in the service's model.
     */
    public static final Metric VALIDATION_FAILURES = define("ValidationFailures");

    /**
     * Time spent to create a connection.
     */
    public static final Metric CONNECT = define("Connect");

    /**
     * Time spent on the wire by the request from the client's perspective.
     */
    public static final Metric REMOTE = define("Remote");

    /**
     * The time spent by the client for all its actions.
     * It is the latency felt by the user code, spent inside the client.
     */
    public static final Metric LATENCY = define("Latency");

    /**
     * Time spent performing DNS lookup.
     */
    public static final Metric DNS_TIME = define("DnsTime");

    /**
     * Count whether critical timeouts have occured, for example if an
     * HttpTimeoutException was caught.
     */
    public static final Metric TIMEOUT_CRITICAL = define("Timeout:Critical");

    /**
     * Number of milliseconds left until the server's SSL/TLS certificate expires.
     */
    public static final Metric SERVER_SSL_LIFE_REMAINING = define("ServerSSLLifeRemaining");

    /**
     * Number of pending requests when the maximum number of parallel requests has been reached.
     */
    public static final Metric MAX_REQUESTS_QUEUE_SIZE = define("MaxRequestsQueueSize");

    /**
     * Percentage of in flight requests out of the maximum supported capacity.
     */
    public static final Metric CAPACITY_USED = define("CapacityUsed");

    /**
     * Number of in flight requests.
     */
    public static final Metric IN_FLIGHT = define("InFlight");

    /**
     * Time spent while waiting for a lock to allow the client to perform a call,
     * when the number of maximum parallel requests has been reached.
     */
    public static final Metric MAX_REQUESTS_QUEUE_TIME = define("MaxRequestsQueueTime");

    /**
     * Service's availability, expressed as percentage.
     * (100% - Availability:Critical) is the proportion of requests impacted by availability issues.
     */
    public static final Metric AVAILABILITY_CRITICAL = define("Availability:Critical");

    /**
     * Number of failures for a request, when retries are used.
     */
    public static final Metric BACKOFF_CRITICAL = define("Backoff:Critical");

    /**
     * Number of requests present in the batch, when using the batch client.
     */
    public static final Metric BATCH_SIZE = define("BatchSize");

}
