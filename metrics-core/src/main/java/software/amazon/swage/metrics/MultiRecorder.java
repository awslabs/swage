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

import java.time.Instant;
import java.util.List;

/**
 * A MetricRecorder that delegates to multiple other recorders.
 * Used when metrics should be going to multiple places at the same time.
 */
public class MultiRecorder extends MetricRecorder {

    private final List<MetricRecorder> recorders;

    public MultiRecorder(final List<MetricRecorder> recorders) {
        this.recorders = recorders;
    }

    @Override
    protected void record(
            final Metric label,
            final Number value,
            final Unit unit,
            final Instant timestamp,
            final Context context)
    {
        //TODO: something smarter, or at least call out ordered iterated nature
        for (MetricRecorder r : recorders) {
            r.record(label, value, unit, timestamp, context);
        }
    }

    @Override
    protected void count(final Metric label, final long delta, final Context context) {
        for (MetricRecorder r : recorders) {
            r.count(label, delta, context);
        }

    }
}
