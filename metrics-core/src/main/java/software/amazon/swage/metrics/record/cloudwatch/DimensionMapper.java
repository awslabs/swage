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

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import com.amazonaws.services.cloudwatch.model.Dimension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        for (Map.Entry<Metric, Set<TypedMap.Key>> e : this.filterMap.entrySet()) {
            e.getValue().addAll(globalDimensions);
        }
    }

    /**
     * Pull desired info out of the metric context data and convert to
     * CloudWatch {@link Dimension} objects, according to the filter mapping
     * defined.
     *
     * If the context does not contain an entry for a mapped dimension key,
     * the dimension will be added with a value of "null".
     *
     * @param metric Metric being sent
     * @param context TypedMap containing metric context data
     * @return A list of Dimensions appropriate to send to CloudWatch
     */
    public List<Dimension> getDimensions(final Metric metric, final TypedMap context)
    {
        Set<TypedMap.Key> dimensionKeys = filterMap.get(metric);
        if (dimensionKeys == null) {
            dimensionKeys = Collections.emptySet();
        }

        List<Dimension> dimensions = new ArrayList<>(globalDimensions.size() + dimensionKeys.size());

        for (TypedMap.Key k : globalDimensions) {
            Dimension d = new Dimension();
            d.setName(k.name);
            d.setValue(String.valueOf(context.get(k)));
            dimensions.add(d);
        }

        //TODO: cull duplicate entries?
        for (TypedMap.Key k : dimensionKeys) {
            Dimension d = new Dimension();
            d.setName(k.name);
            d.setValue(String.valueOf(context.get(k)));
            dimensions.add(d);
        }

        return dimensions;
    }

}
