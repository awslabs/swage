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

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

public class MetricRecorderTest {
    private static final Metric METRIC = Metric.define("Metric");

    @Test
    public void recorderContextReturnsSuppliedAttributes() {
        TypedMap attributes = TypedMap.empty();
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(attributes);
        assertSame(attributes, context.attributes());
    }

    @Test
    public void recorderDelegatesToImplementation() throws Exception {
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
}
