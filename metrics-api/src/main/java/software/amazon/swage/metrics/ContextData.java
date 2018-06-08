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

import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;

/**
 * Builder for dimensions carried with the context under which a metric is reported.
 *
 * <p>
 * Roughly equivalent to a request context, this is the dimensions associated with
 * a metric context that allows relations and aggregations between events.
 * Things like request id, tracing id, etc. will be carried here.
 *
 * <p>
 * Built instances are immutable.  Metric recording will use
 * the dimensions across threads as needed, and relies on the dimensions being
 * unchanged for a particular Context.
 *
 * <p>
 * Different application domains may extend the builder to facilitate adding
 * the appropriate dimensions.
 *
 * A domain-specific dimensions builder might look like:
 * <pre>{@code
public class DomainSpecificData extends ContextData {
    public static final TypedMap.Key<String> MARKETPLACE = TypedMap.key("marketplace", String.class);
    public static final TypedMap.Key<String> SERVICE     = TypedMap.key("serviceName", String.class);
    public static final TypedMap.Key<String> OPERATION   = TypedMap.key("operation", String.class);
    public static final TypedMap.Key<String> PROGRAM     = TypedMap.key("programName", String.class);

    public static DomainSpecificData from(TypedMap dimensions) {
        DomainSpecificData b = new DomainSpecificData();
        dimensions.iterator().forEachRemaining((e) -> b.add(e.getKey(), e));
        return b;
    }

    public static DomainSpecificData withId(String id) {
        DomainSpecificData b = new DomainSpecificData();
        b.add(ID, id);
        return b;
    }

    public DomainSpecificData marketplace(final String marketplace) {
        add(MARKETPLACE, marketplace);
        return this;
    }

    public DomainSpecificData serviceName(final String serviceName) {
        add(SERVICE, serviceName);
        return this;
    }

    public DomainSpecificData operation(final String operation) {
        add(OPERATION, operation);
        return this;
    }

    public DomainSpecificData program(final String program) {
        add(PROGRAM, program);
        return this;
    }
}
 * }</pre>
 *
 * TODO: explore auto-generating builders based on keys, perhaps with Immutables lib
 */
public class ContextData extends ImmutableTypedMap.Builder {

    /**
     * Key for ID of the context.  Use like
     * <pre>{@code
     * String id = contextData.get(ContextData.ID);
     * }</pre>
     */
    public static final TypedMap.Key<String> ID = TypedMap.key("id", String.class);

    public static ContextData withId(String id) {
        ContextData b = new ContextData();
        b.add(ID, id);
        return b;
    }

}
