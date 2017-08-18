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
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.StandardContext;
import com.amazonaws.services.cloudwatch.model.Dimension;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class DimensionMapperTest {

    @Test
    public void no_mappings() throws Exception {
        Metric metric = Metric.define("FooMetric");
        String id = UUID.randomUUID().toString();

        TypedMap context = ContextData.withId(id)
                                      .add(StandardContext.SERVICE, "FooService")
                                      .build();

        DimensionMapper mapper = new DimensionMapper.Builder().build();

        assertTrue("Empty mapping resulted in non-empty dimension list",
                   mapper.getDimensions(metric, context).isEmpty());
    }

    @Test
    public void one_global() throws Exception {
        Metric metric = Metric.define("BarMetric");
        String id = UUID.randomUUID().toString();

        TypedMap context = ContextData.withId(id)
                                      .add(StandardContext.SERVICE, "BarService")
                                      .build();

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        List<Dimension> dims = mapper.getDimensions(metric, context);
        assertEquals("Unexpected size of dimension list", 1, dims.size());
        assertEquals("Unexpected name for dimension", ContextData.ID.name, dims.get(0).getName());
        assertEquals("Unexpected value for dimension", id, dims.get(0).getValue());
    }

    @Test
    public void one_global_empty_context() throws Exception {
        Metric metric = Metric.define("MetricBaz");
        TypedMap context = TypedMap.empty();

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        List<Dimension> dims = mapper.getDimensions(metric, context);
        assertEquals("Unexpected size of dimension list", 1, dims.size());
        assertEquals("Unexpected name for dimension", ContextData.ID.name, dims.get(0).getName());
        assertEquals("Unexpected value for missing dimension", "null", dims.get(0).getValue());
    }

    @Test
    public void mappings() throws Exception {
        Metric metricA = Metric.define("Alpha");
        Metric metricB = Metric.define("Beta");
        Metric metricC = Metric.define("Gamma");
        String id = UUID.randomUUID().toString();

        String service = "VeryImportantApp";
        String operation = "doStuff";

        TypedMap context = ContextData.withId(id)
                                      .add(StandardContext.SERVICE, service)
                                      .add(StandardContext.OPERATION, operation)
                                      .build();

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addMetric(metricA, Arrays.asList(StandardContext.SERVICE))
                .addMetric(metricB, Arrays.asList(StandardContext.SERVICE, StandardContext.OPERATION))
                .addMetric(metricC, Arrays.asList(StandardContext.ID, StandardContext.SERVICE))
                .build();

        List<Dimension> dimsA = mapper.getDimensions(metricA, context);
        assertEquals("Unexpected size of dimension list", 1, dimsA.size());
        assertEquals("Unexpected name for dimension", StandardContext.SERVICE.name, dimsA.get(0).getName());
        assertEquals("Unexpected value for dimension", service, dimsA.get(0).getValue());


        List<Dimension> dimsB = mapper.getDimensions(metricB, context);
        assertEquals("Unexpected size of dimension list", 2, dimsB.size());
        boolean seenB[] = {false, false};
        for (Dimension d : dimsB) {
            if (d.getName().equals(StandardContext.SERVICE.name) &&
                    d.getValue().equals(service)) {
                seenB[0] = true;
            }
            else if (d.getName().equals(StandardContext.OPERATION.name) &&
                d.getValue().equals(operation)) {
                seenB[1] = true;
            }
            else {
                fail("Unexpected dimension present in mapped list");
            }
        }
        assertTrue("Dimension list is missing mapped SERVICE dimension", seenB[0]);
        assertTrue("Dimension list is missing mapped OPERATION dimension", seenB[1]);


        List<Dimension> dimsC = mapper.getDimensions(metricC, context);
        assertEquals("Unexpected size of dimension list", 2, dimsC.size());
        boolean seenC[] = {false, false};
        for (Dimension d : dimsC) {
            if (d.getName().equals(StandardContext.SERVICE.name) &&
                d.getValue().equals(service)) {
                seenC[0] = true;
            }
            else if (d.getName().equals(StandardContext.ID.name) &&
                     d.getValue().equals(id)) {
                seenC[1] = true;
            }
            else {
                fail("Unexpected dimension present in mapped list");
            }
        }
        assertTrue("Dimension list is missing mapped SERVICE dimension", seenC[0]);
        assertTrue("Dimension list is missing mapped OPERATION dimension", seenC[1]);
    }

}
