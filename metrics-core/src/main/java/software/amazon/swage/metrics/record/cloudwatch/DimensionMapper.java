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
package software.amazon.swage.metrics.record.cloudwatch;

import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.record.MetricRecorder;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Logic for mapping a metric event and associated context to a set of
 * dimensions in CloudWatch.
 *
 * CloudWatch uses dimensions as a filtering mechanism for viewing metric
 * events, and requires the client to specify the dimensions associated
 * with an event when recording it in CloudWatch. An instance of this class
 * contains the desired dimension mappings for an application, to ensure that
 * metric events are filtered as desired.
 *
 * This mapping is configured up front rather than per metric recording call
 * to ensure that the same dimension filtering is present for every event, and
 * separate out the concerns of how metrics are filtered from the spot where
 * they are emitted in the code.
 *
 * This is required as CloudWatch does no real filtering itself.  Rather it
 * depends on each datum sent to include filtering information, and simply
 * does a lookup based on the stored information.  Ideally every event recorded
 * would send with it all available context information, and a separate system
 * (perhaps CloudWatch, perhaps something sitting in front) would allow those
 * consuming the data to filter as needed.
 *
 * A DimensionMapper should be configured at application startup/configuration,
 * like:
 * <pre>
 * {@code
DimensionMapper.Builder builder = new DimensionMapper.Builder();

// Some dimensions may be required for every metric event
// If every metric can/should share the same dimensions, configuring global
// dimensions may be all that is required.
builder.addGlobalDimension(ContextData.ID);

// Specific metrics may have their own distinct set of dimensions
builder.addMetric(StandardMetric.TIME,
                  Arrays.asList(StandardContext.OPERATION, SpecificContext.SOME_DIMENSION));

DimensionMapper mapper = builder.build();
 * }
 * </pre>
 *
 *
 */
public class DimensionMapper {

    public static class Builder {

        //TODO: should ID be a global dimension always?
        private final Set<TypedMap.Key> globalDimensions;
        private final Map<Metric, Set<TypedMap.Key>> filters;

        public Builder() {
            this.filters = new HashMap<>();
            this.globalDimensions = new HashSet<>();
        }

        public Builder addGlobalDimension(final TypedMap.Key dimension) {
            this.globalDimensions.add(dimension);
            return this;
        }

        public Builder addMetric(final Metric metric, final Collection<TypedMap.Key> dimensions) {
            this.filters.put(metric, new HashSet<>(dimensions));
            return this;
        }

        public DimensionMapper build() {
            return new DimensionMapper(globalDimensions, filters);
        }
    }

    //TODO: support list of dimension sets, like
    // LATENCY->[[], [AvailabilityZone], [LoadBalancer], [AvailabilityZone, LoadBalancer]]
    // this doesn't appear to be supported by CloudWatch API, but apparently it's a common case...

    //TODO: should this take opaque strings, then do a compare against key names later?

    //TODO: should this object just be a configured list of keys, leave the pull-from-context for elsewhere?

    private final Set<TypedMap.Key> globalDimensions;
    private final Map<Metric, Set<TypedMap.Key>> filterMap;

    private DimensionMapper(
            final Set<TypedMap.Key> globalDimensions,
            final Map<Metric, Set<TypedMap.Key>> filterMap)
    {
        this.globalDimensions = new HashSet<>(globalDimensions);
        this.filterMap = new HashMap<>(filterMap);
    }

    /**
     * Pull desired info out of the metric context attributes and convert to
     * CloudWatch {@link Dimension} objects, according to the filter mapping
     * defined.
     *
     * If the context does not contain an entry for a mapped dimension key,
     * the dimension will be added with a value of "null".
     *
     * @param metric Metric being sent
     * @param context the context in which the metric was recorded.
     * @return A list of Dimensions appropriate to send to CloudWatch
     */
    public List<Dimension> getDimensions(final Metric metric, final MetricRecorder.RecorderContext context)
    {
        //Flattening the context hierarchy here allows this dimension mapper to work with multiple cloudwatch
        //recorders. If a recorder wants to precompute the flattened hierarchy it can do so in its implementation
        //of RecorderContext.
        TypedMap attributes = flattenContextHierarchy(context);
        Set<TypedMap.Key> dimensionKeys = filterMap.get(metric);
        if (dimensionKeys == null) {
            dimensionKeys = Collections.emptySet();
        }

        Stream<TypedMap.Key> keys = Stream.concat(globalDimensions.stream(), dimensionKeys.stream());
        return keys.map(key -> Dimension.builder().name(key.name).value(String.valueOf(attributes.get(key))).build())
            .distinct()
            .sorted(Comparator.comparing(Dimension::name))
            .collect(Collectors.toList());
    }

    /**
     * Flatten the attributes in the context hierarchy to produce the full set of
     * attributes which describe the environment in which measurements were taken.
     * Parent attributes are overridden by child attributes with the same key.
     *
     * @param context the context used for recording measurements.
     * @return the flattened attributes with parent values overriden by child.
     */
    private TypedMap flattenContextHierarchy(MetricRecorder.RecorderContext context) {
        if (context.parent() == null) {
            return context.attributes();
        }

        MetricRecorder.RecorderContext parent = context;
        ImmutableTypedMap.Builder attributes = ImmutableTypedMap.Builder.from(TypedMap.empty());
        Stack<MetricRecorder.RecorderContext> parents = new Stack<>();
        while(parent != null) {
            parents.push(parent);
            parent = parent.parent();
        }

        //Add the all of the attributes starting with the oldest parent and progressing down the hierarchy. 
        //If there are name conflicts between a parent and one of its children, this order will result in the 
        //value from the parent being overridden. 
        while(!parents.empty()) {
            MetricRecorder.RecorderContext current = parents.pop();
            current.attributes().forEach(entry -> attributes.add(entry.getKey(), entry.getValue()));
        }

        return attributes.build();
    }
}
