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

        // A 'good' metric name
        Metric.define("a_23.5lKJ/L99:at@se-wat");

        // Not exhaustive, but hopefully representative
        final String[] bads = {
                "some bad name",
                "no|pipes",
                "also!nogood",
                "#$%&'*"
        };
        for (String s : bads) {
            try {
                // Bad name should throw exception
                Metric.define(s);
            } catch (IllegalArgumentException expected) {
                continue;
            }
            fail("Expected exception not thrown for bad name '"+s+"'");
        }

        // An excessively long name
        StringBuilder sb = new StringBuilder(255);
        for (int i = 0; i < 255; i++) {
            sb.append('a');
        }
        try {
            Metric.define(sb.toString());
        } catch (IllegalArgumentException expected) {
            return;
        }
        fail("Expected exception not thrown for excessively long name");

    }
}
