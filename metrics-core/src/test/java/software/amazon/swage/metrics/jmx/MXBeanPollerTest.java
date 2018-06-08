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
package software.amazon.swage.metrics.jmx;

import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.StandardContext;
import software.amazon.swage.metrics.Unit;
import software.amazon.swage.metrics.jmx.sensor.Sensor;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class MXBeanPollerTest {

    // A matcher that checks the given TypedMap contains the minimal information
    // expected to be added by MXBeanPoller.
    private static final class DataMatcher implements ArgumentMatcher<TypedMap> {
        @Override
        public boolean matches(final TypedMap data) {
            if (!"JMX".equals(data.get(StandardContext.OPERATION))) {
                return false;
            }
            String id = data.get(StandardContext.ID);
            return (id != null && !id.isEmpty());
        }
    }

    // A stub MetricRecorder implementation, used instead of a mock to avoid
    // issues with final stuff.
    private static final class StubRecorder extends MetricRecorder {
        @Override
        protected void record(
                final Metric label,
                final Number value,
                final Unit unit,
                final Instant time,
                final Context context)
        {
        }

        @Override
        protected void count(final Metric label, final long delta, final Context context) {
        }
    }

    @Test
    public void senses() throws Exception {
        final MetricRecorder recorder = new StubRecorder();

        final Sensor sensor1 = mock(Sensor.class);
        final Sensor sensor2 = mock(Sensor.class);
        when(sensor1.addContext(any(TypedMap.class))).then(invocation -> invocation.getArgument(0));
        when(sensor2.addContext(any(TypedMap.class))).then(invocation -> invocation.getArgument(0));

        List<Sensor> sensors = new ArrayList<>();
        sensors.add(sensor1);
        sensors.add(sensor2);

        MXBeanPoller poller = new MXBeanPoller(recorder, 1, sensors);
        Thread.sleep(1500); // wait for a poll to occur
        poller.shutdown();

        final ArgumentMatcher<TypedMap> dataMatch = new DataMatcher();
        verify(sensor1).addContext(argThat(dataMatch));
        verify(sensor2).addContext(argThat(dataMatch));
        verify(sensor1, atLeastOnce()).sense(any(MetricRecorder.Context.class));
        verify(sensor2, atLeastOnce()).sense(any(MetricRecorder.Context.class));
    }

    @Test
    public void shutdown() throws Exception {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        MXBeanPoller poller = new MXBeanPoller(executor, new StubRecorder(), 5, Collections.emptyList());
        poller.shutdown();

        assertTrue("Executor not shutdown on poller shutdown", executor.isShutdown());
    }
}
