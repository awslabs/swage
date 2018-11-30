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

import static org.junit.Assert.fail;

public class MetricTest {

    /*
     * Test validation works as expected with new metrics.
     */
    @Test
    public void validation() throws Exception {
        final String[] goods = {
                "snake_case_metric",
                "camelCaseMetric",
                "hyphenated-metric",
                "spaced out metric",
                "digits 0123456789",
                "G\u00FCnther",
                // All of the bad-idea characters that are allowed (and not above)
                "\t\n!\"#$%&'()*+,./:;<=>?@[\\]^`{|}~",
                " "
        };
        for (String s : goods) {
            Metric.define(s);
        }

        final String[] bads = { null, "" };
        for (String s : bads) {
            try {
                // Bad name should throw exception
                Metric.define("");
            } catch (IllegalArgumentException expected) {
                continue;
            }
            fail("Expected exception not thrown for bad name '"+s+"'");
        }
    }
}
