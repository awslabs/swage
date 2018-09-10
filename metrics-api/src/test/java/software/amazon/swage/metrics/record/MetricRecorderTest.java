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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

public class MetricRecorderTest {
    private static final Metric METRIC = Metric.define("Metric");
    private TypedMap attributes = TypedMap.empty();
    private MetricRecorder.RecorderContext recorderContext = new MetricRecorder.RecorderContext(attributes);
    private MetricRecorder<MetricRecorder.RecorderContext> recorder = spy(new NullRecorder());

    @Before
    public void setup() {
        when(recorder.newRecorderContext(attributes)).thenReturn(recorderContext);
    }

    @Test
    public void recorderContextReturnsSuppliedAttributes() {
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(attributes);

        assertSame(attributes, context.attributes());
    }

    @Test
    public void recorderDelegatesRecordToImplementation() throws Exception {
        MetricContext context = recorder.context(attributes);

        Instant timestamp = Instant.now();
        context.record(METRIC, 42L, Unit.NONE, timestamp);

        verify(recorder).record(eq(METRIC), eq(42L), eq(Unit.NONE), eq(timestamp), same(recorderContext));
    }

    @Test
    public void recorderDoesNotDelegateCountToImplementationBeforeClose() throws Exception {
        MetricContext context = recorder.context(attributes);

        context.count(METRIC, 4L);
        context.count(METRIC, -2L);
        context.count(METRIC, 40L);

        verify(recorder, times(0)).count(any(Metric.class), anyLong(), any(MetricRecorder.RecorderContext.class));
    }

    @Test
    public void recorderDelegatesCountToImplementationOnClose() throws Exception {
        MetricContext context = recorder.context(attributes);

        context.count(METRIC, 4L);
        context.close();

        verify(recorder).count(eq(METRIC), eq(4L), same(recorderContext));
        verify(recorder).close(same(recorderContext));
    }

    @Test
    public void recorderAggregatesCountsAndDelegatesOnce() throws Exception {
        MetricContext context = recorder.context(attributes);

        context.count(METRIC, 4L);
        context.count(METRIC, -2L);
        context.count(METRIC, 40L);
        context.close();

        verify(recorder, times(1)).count(eq(METRIC), eq(42L), same(recorderContext));
        verify(recorder).close(same(recorderContext));
    }
}
