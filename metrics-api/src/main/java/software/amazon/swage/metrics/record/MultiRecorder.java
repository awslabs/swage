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
package software.amazon.swage.metrics.record;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

/**
 * A MetricRecorder that delegates to multiple other recorders.
 * Used when metrics should be going to multiple places at the same time.
 */
public class MultiRecorder extends MetricRecorder<MultiRecorder.MultiRecorderContext> {

    public static class MultiRecorderContext extends MetricRecorder.RecorderContext {
        private final List<MetricContext> contexts;

        public MultiRecorderContext(TypedMap attributes, List<MetricContext> contexts) {
            super(attributes);
            this.contexts = contexts;
        }

        MultiRecorderContext(TypedMap attributes, List<MetricContext> contexts, MultiRecorder.MultiRecorderContext parent) {
            super(attributes, parent);
            this.contexts = contexts;
        }

        @Override
        public MultiRecorderContext newChild(TypedMap attributes) {
            List<MetricContext> childContexts = contexts.stream().map(context ->  context.newChild(attributes)).collect(Collectors.toList());
            return new MultiRecorderContext(attributes, childContexts, this);
        }
    }

    private final List<MetricRecorder<?>> recorders;

    /**
     * @param recorders the recorders to delegate to
     */
    public MultiRecorder(List<MetricRecorder<?>> recorders) {
        this.recorders = recorders;
    }

    @Override
    protected MultiRecorderContext newRecorderContext(TypedMap attributes) {
        List<MetricContext> contexts = recorders.parallelStream()
                .map(t -> t.context(attributes))
                .collect(Collectors.toList());
        return new MultiRecorderContext(attributes, contexts);
    }

    @Override
    protected void record(Metric label, Number value, Unit unit, Instant time, MultiRecorderContext context) {
        context.contexts.parallelStream().forEach(t -> t.record(label, value, unit, time));
    }

    @Override
    protected void count(Metric label, long delta, MultiRecorderContext context) {
        context.contexts.parallelStream().forEach(t -> t.count(label, delta));
    }

    @Override
    protected void close(MultiRecorderContext context) {
        context.contexts.parallelStream().forEach(MetricContext::close);
    }
}
