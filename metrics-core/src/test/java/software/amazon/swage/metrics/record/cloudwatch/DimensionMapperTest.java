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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.services.cloudwatch.model.Dimension;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.ContextData;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.StandardContext;
import software.amazon.swage.metrics.record.MetricRecorder;

/**
 *
 */
class DimensionMapperTest {

    private static final Metric METRIC = Metric.define("FooMetric");
    private final String ID = UUID.randomUUID().toString();
    private final String SERVICE_NAME = UUID.randomUUID().toString();
    private final String OPERATION = UUID.randomUUID().toString();
    private final String PROGRAM = UUID.randomUUID().toString();
    private final String MARKETPLACE = UUID.randomUUID().toString();

    @Test
    void no_mappings() {
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(ID)
                        .add(StandardContext.SERVICE, SERVICE_NAME)
                        .build());

        DimensionMapper mapper = new DimensionMapper.Builder().build();

        assertTrue(mapper.getDimensions(METRIC, context).isEmpty(),
                "Empty mapping resulted in non-empty dimension list");
    }

    @Test
    void one_global() {
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(ID)
                        .add(StandardContext.SERVICE, SERVICE_NAME)
                        .build());

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        List<Dimension> dims = mapper.getDimensions(METRIC, context);
        assertEquals(1, dims.size(), "Unexpected size of dimension list");
        assertEquals(ContextData.ID.name, dims.get(0).getName(), "Unexpected name for dimension");
        assertEquals(ID, dims.get(0).getValue(), "Unexpected value for dimension");
    }

    @Test
    void one_global_empty_context() {
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(TypedMap.empty());

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .build();

        List<Dimension> dims = mapper.getDimensions(METRIC, context);
        assertEquals(1, dims.size(), "Unexpected size of dimension list");
        assertEquals(ContextData.ID.name, dims.get(0).getName(), "Unexpected name for dimension");
        assertEquals("null", dims.get(0).getValue(), "Unexpected value for missing dimension");
    }

    @Test
    void global_and_normal_dimensions_combine() {
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(ID)
                        .add(StandardContext.SERVICE, SERVICE_NAME)
                        .build());

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .addMetric(METRIC, Arrays.asList(StandardContext.SERVICE))
                .build();
        Dimension expectedIdDimension = new Dimension().withName(ContextData.ID.name).withValue(ID);
        Dimension expectedServiceDimension = new Dimension().withName(StandardContext.SERVICE.name)
                .withValue(SERVICE_NAME);

        List<Dimension> dims = mapper.getDimensions(METRIC, context);
        assertTrue(dims.contains(expectedIdDimension),
                "Dimension list is missing mapped ID dimension");
        assertTrue(dims.contains(expectedServiceDimension),
                "Dimension list is missing mapped SERVICE dimension");
        assertEquals(2, dims.size(), "Unexpected size of dimension list");
    }

    @Test
    void mappings() {
        Metric metricA = Metric.define("Alpha");
        Metric metricB = Metric.define("Beta");
        Metric metricC = Metric.define("Gamma");

        String operation = "doStuff";

        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(ID)
                        .add(StandardContext.SERVICE, SERVICE_NAME)
                        .add(StandardContext.OPERATION, operation)
                        .build());

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addMetric(metricA, Arrays.asList(StandardContext.SERVICE))
                .addMetric(metricB, Arrays.asList(StandardContext.SERVICE, StandardContext.OPERATION))
                .addMetric(metricC, Arrays.asList(StandardContext.ID, StandardContext.SERVICE))
                .build();

        List<Dimension> dimsA = mapper.getDimensions(metricA, context);
        assertEquals(1, dimsA.size(), "Unexpected size of dimension list");
        assertEquals(StandardContext.SERVICE.name, dimsA.get(0).getName(),
                "Unexpected name for dimension");
        assertEquals(SERVICE_NAME, dimsA.get(0).getValue(), "Unexpected value for dimension");

        List<Dimension> dimsB = mapper.getDimensions(metricB, context);
        assertEquals(2, dimsB.size(), "Unexpected size of dimension list");
        boolean seenB[] = {false, false};
        for (Dimension d : dimsB) {
            if (d.getName().equals(StandardContext.SERVICE.name) &&
                    d.getValue().equals(SERVICE_NAME)) {
                seenB[0] = true;
            } else if (d.getName().equals(StandardContext.OPERATION.name) &&
                    d.getValue().equals(operation)) {
                seenB[1] = true;
            } else {
                fail("Unexpected dimension present in mapped list");
            }
        }
        assertTrue(seenB[0], "Dimension list is missing mapped SERVICE dimension");
        assertTrue(seenB[1], "Dimension list is missing mapped OPERATION dimension");

        List<Dimension> dimsC = mapper.getDimensions(metricC, context);
        assertEquals(2, dimsC.size(), "Unexpected size of dimension list");
        boolean seenC[] = {false, false};
        for (Dimension d : dimsC) {
            if (d.getName().equals(StandardContext.SERVICE.name) &&
                    d.getValue().equals(SERVICE_NAME)) {
                seenC[0] = true;
            } else if (d.getName().equals(StandardContext.ID.name) &&
                    d.getValue().equals(ID)) {
                seenC[1] = true;
            } else {
                fail("Unexpected dimension present in mapped list");
            }
        }
        assertTrue(seenC[0], "Dimension list is missing mapped SERVICE dimension");
        assertTrue(seenC[1], "Dimension list is missing mapped ID dimension");
    }

    @Test
    void attributes_are_sorted() {
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(ID)
                        .add(StandardContext.SERVICE, SERVICE_NAME)
                        .add(StandardContext.OPERATION, OPERATION)
                        .add(StandardContext.MARKETPLACE, MARKETPLACE)
                        .add(StandardContext.PROGRAM, PROGRAM)
                        .build());

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .addMetric(METRIC, Arrays.asList(StandardContext.SERVICE,
                        StandardContext.OPERATION,
                        StandardContext.MARKETPLACE,
                        StandardContext.PROGRAM))
                .build();

        Dimension expectedIdDimension = new Dimension().withName(ContextData.ID.name).withValue(ID);
        Dimension expectedServiceDimension = new Dimension().withName(StandardContext.SERVICE.name)
                .withValue(SERVICE_NAME);
        Dimension expectedOperationDimension = new Dimension().withName(
                StandardContext.OPERATION.name).withValue(OPERATION);
        Dimension expectedMarketplaceDimension = new Dimension().withName(
                StandardContext.MARKETPLACE.name).withValue(MARKETPLACE);
        Dimension expectedProgramDimension = new Dimension().withName(StandardContext.PROGRAM.name)
                .withValue(PROGRAM);
        List<Dimension> expected = Arrays.asList(expectedIdDimension,
                expectedMarketplaceDimension,
                expectedOperationDimension,
                expectedProgramDimension,
                expectedServiceDimension);

        List<Dimension> dims = mapper.getDimensions(METRIC, context);
        assertEquals(expected, dims);
    }

    @Test
    void parent_and_child_attributes_combine() {
        String childId = UUID.randomUUID().toString();
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(ID)
                        .add(StandardContext.SERVICE, SERVICE_NAME)
                        .build());

        MetricRecorder.RecorderContext childContext = context.newChild(ContextData.withId(childId)
                .add(StandardContext.OPERATION, OPERATION)
                .build());

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .addMetric(METRIC, Arrays.asList(StandardContext.SERVICE, StandardContext.OPERATION))
                .build();

        Dimension expectedIdDimension = new Dimension().withName(ContextData.ID.name)
                .withValue(childId);
        Dimension expectedServiceDimension = new Dimension().withName(StandardContext.SERVICE.name)
                .withValue(SERVICE_NAME);
        Dimension expectedOperationDimension = new Dimension().withName(
                StandardContext.OPERATION.name).withValue(OPERATION);
        List<Dimension> expected = Arrays.asList(expectedIdDimension,
                expectedOperationDimension,
                expectedServiceDimension);

        List<Dimension> dims = mapper.getDimensions(METRIC, childContext);
        assertEquals(expected, dims);
    }

    @Test
    void child_attributes_override_parent() {
        String childId = UUID.randomUUID().toString();
        String childService = UUID.randomUUID().toString();
        String childOperation = UUID.randomUUID().toString();
        MetricRecorder.RecorderContext context = new MetricRecorder.RecorderContext(
                ContextData.withId(ID)
                        .add(StandardContext.SERVICE, SERVICE_NAME)
                        .add(StandardContext.OPERATION, OPERATION)
                        .build());

        MetricRecorder.RecorderContext childContext = context.newChild(ContextData.withId(childId)
                .add(StandardContext.SERVICE, childService)
                .add(StandardContext.OPERATION, childOperation)
                .build());

        DimensionMapper mapper = new DimensionMapper.Builder()
                .addGlobalDimension(ContextData.ID)
                .addMetric(METRIC, Arrays.asList(StandardContext.SERVICE, StandardContext.OPERATION))
                .build();

        Dimension expectedIdDimension = new Dimension().withName(ContextData.ID.name)
                .withValue(childId);
        Dimension expectedServiceDimension = new Dimension().withName(StandardContext.SERVICE.name)
                .withValue(childService);
        Dimension expectedOperationDimension = new Dimension().withName(StandardContext.OPERATION.name)
                .withValue(childOperation);
        List<Dimension> expected = Arrays.asList(expectedIdDimension,
                expectedOperationDimension,
                expectedServiceDimension);

        List<Dimension> dims = mapper.getDimensions(METRIC, childContext);
        assertEquals(expected, dims);
    }
}
