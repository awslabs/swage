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
package software.amazon.swage.metrics.jmx.sensor;

import software.amazon.swage.collection.ImmutableTypedMap;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This has naught to do with MXBeans, and arguably shouldn't be in the jmx metrics
 */
public class DiskUsageSensor implements Sensor {

    private static final long G = 1024L * 1024L * 1024L;

    public static final TypedMap.Key<Long> DISK_SIZE = TypedMap.key("DiskSize", Long.class);
    public static final Metric DISK_USED = Metric.define("DiskUse");

    @Override
    public TypedMap addContext(final TypedMap existing)
    {
        try {
            // Determine the file store for the directory the JVM was started in
            FileStore fileStore = Files.getFileStore(Paths.get(System.getProperty("user.dir")));

            long size = fileStore.getTotalSpace();
            return ImmutableTypedMap.Builder
                    .from(existing)
                    .add(DISK_SIZE, Long.valueOf(size / G))
                    .build();
        } catch (IOException e) {
            // log?
            return existing;
        }
    }

    @Override
    public void sense(final MetricContext metricContext) throws SenseException
    {
        try {
            // Determine the file store for the directory the JVM was started in
            FileStore fileStore = Files.getFileStore(Paths.get(System.getProperty("user.dir")));

            long total = fileStore.getTotalSpace();
            long free = fileStore.getUsableSpace();
            double percentFree = 100.0 * ((double)(total - free) / (double)total);
            metricContext.record(DISK_USED, percentFree, Unit.PERCENT);
        } catch (IOException e) {
            throw new SenseException("Problem reading disk space", e);
        }
    }

}
