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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

class MetricRecorderTest {
    private static final Metric METRIC = Metric.define("Metric");

    @Test
    void recorderContextReturnsSuppliedAttributes() {
        TypedMap attributes = TypedMap.empty();
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(attributes);
        assertSame(attributes, context.attributes());
    }

    @Test
    void recorderDelegatesToImplementation() {
        TypedMap attributes = TypedMap.empty();
        MetricRecorder.RecorderContext recorderContext = new MetricRecorder.RecorderContext(attributes);

        MetricRecorder<MetricRecorder.RecorderContext> recorder = spy(new NullRecorder());
        when(recorder.newRecorderContext(attributes)).thenReturn(recorderContext);

        MetricContext context = recorder.context(attributes);

        Instant timestamp = Instant.now();
        context.record(METRIC, 42L, Unit.NONE, timestamp);
        context.count(METRIC, 4L);
        context.close();

        verify(recorder).record(eq(METRIC), eq(42L), eq(Unit.NONE), eq(timestamp), same(recorderContext));
        verify(recorder).count(eq(METRIC), eq(4L), same(recorderContext));
        verify(recorder).close(same(recorderContext));
    }

    @Test
    void recorderPassesParentHierarchyToImplementation() {
        TypedMap parentAttributes = ContextData.withId(UUID.randomUUID().toString()).build();
        TypedMap childAttributes = ContextData.withId(UUID.randomUUID().toString()).build();
        MetricRecorder.RecorderContext parentContext = new MetricRecorder.RecorderContext(parentAttributes);

        MetricRecorder<MetricRecorder.RecorderContext> recorder = spy(new NullRecorder());
        when(recorder.newRecorderContext(parentAttributes)).thenReturn(parentContext);

        MetricContext parentRecorderContext = recorder.context(parentAttributes);
        MetricContext context = parentRecorderContext.newChild(childAttributes);

        MetricRecorder.RecorderContext expected = new MetricRecorder.RecorderContext(childAttributes, parentContext);

        Instant timestamp = Instant.now();
        context.record(METRIC, 42L, Unit.NONE, timestamp);
        context.count(METRIC, 4L);
        context.close();

        verify(recorder).record(eq(METRIC), eq(42L), eq(Unit.NONE), eq(timestamp), argThat(equals(expected)));
        verify(recorder).count(eq(METRIC), eq(4L), argThat(equals(expected)));
        verify(recorder).close(argThat(equals(expected)));
    }

    ArgumentMatcher<MetricRecorder.RecorderContext> equals(MetricRecorder.RecorderContext other) {
        return new ArgumentMatcher<MetricRecorder.RecorderContext>() {
            @Override
            public boolean matches(MetricRecorder.RecorderContext context) {
                return equals(context, other);
            }

            private boolean equals(MetricRecorder.RecorderContext context, MetricRecorder.RecorderContext other) {
                if (context == null && other == null) {
                    return true;
                }
                if (context == null || other == null) {
                    return false;
                }
                return context.attributes().equals(other.attributes()) && equals(context.parent(), other.parent());
            }
        };
    }
}
