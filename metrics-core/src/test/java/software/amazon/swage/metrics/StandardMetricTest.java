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

import org.junit.Test;

public class StandardMetricTest {

    /*
     * Basic unit test to load the StandardMetric class and run validation on the
     * metric names. This makes bad names a build time breakage instead of
     * load/runtime.
     */
    @Test
    public void validate() throws Exception {
        // Reference one member to force loading all static members.
        // Simpler than doing so explicitly ourselves.
        final Metric time = StandardMetric.TIME;
    }

}
