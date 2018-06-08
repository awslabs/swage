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
package software.amazon.swage.metrics.record.file;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.swage.collection.TypedMap;
import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricRecorder;
import software.amazon.swage.metrics.Unit;

/**
 * Simple MetricRecorder that sends metrics events to a file.
 *
 * The metadata will be written to a file with the current time slice appended to
 * the end of the name, and periodically rolled over to a new file.
 *
 * This implementation is bare-bones, and does not serialize to any
 * existing/usable file format. It is intended mainly as an example and
 * building block for future file logging.
 *
 * TODO: output in a useful format
 */
public class FileRecorder extends MetricRecorder {

    private static final Logger log = LogManager.getLogger(FileRecorder.class);

    private final Path outPath;
    private final BlockingQueue<String> logQueue;
    private final Thread writeThread;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Convenience factory method to create a FileRecorder and register a JVM
     * shutdown hook to automatically shut it down on application exit.
     *
     * @param filename Base name of file metrics will be written to
     * @return A FileRecorder instance that will automatically shutdown on JVM exit.
     */
    public static final FileRecorder withAutoShutdown(
            final String filename)
    {
        FileRecorder recorder = new FileRecorder(filename);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> recorder.shutdown()));
        return recorder;
    }

    /**
     *
     * @param filename Base name of file metrics will be written to
     */
    public FileRecorder(final String filename) {
        this.outPath = Paths.get(filename);

        this.logQueue = new LinkedBlockingQueue<>();

        //TODO: take an injected executor, or maybe use common fork/join pool
        this.writeThread = new Thread() {
            @Override
            public void run() {
                Writer writer = new RollingFileWriter(outPath.getParent(), outPath.getFileName().toString());

                while (running.get() || !logQueue.isEmpty()) {
                    try {
                        String line = logQueue.take();
                        writer.write(line);

                        //TODO: periodic/appropriate flushing
                        writer.flush();
                    } catch (IOException e) {
                        log.warn("Problem writing metrics to file log", e);
                        continue;
                    } catch (InterruptedException e) {
                        // thread interrupted while waiting to take, attempt cleanup
                        running.set(false);
                        // Preserve interrupt status
                        Thread.currentThread().interrupt();
                    }
                }

                try {
                    writer.close();
                } catch (IOException e) {
                    log.warn("Problem closing metric file writer", e);
                }
            }
        };

        running.set(true);

        writeThread.start();
    }

    /**
     * Signal that the recorder should shutdown.
     * Queued up metric events will be flushed.
     * Any new metric record/count calls on this recorder will be ignored.
     */
    public void shutdown() {
        running.set(false);
    }

    @Override
    protected void record(
            final Metric label,
            final Number value,
            final Unit unit,
            final Instant timestamp,
            final Context context)
    {
        if (!running.get()) {
            log.debug("record called on shutdown recorder");
            //TODO: something besides silently ignore?
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("metric=")
          .append(label.toString());
        serializeContext(sb, context.metadata());
        sb.append(":")
          .append(String.valueOf(value))
          .append(unit.toString())
          .append('@').append(timestamp.toString())
          .append('\n');

        logQueue.add(sb.toString());
    }

    @Override
    protected void count(
            final Metric label,
            final long delta,
            final Context context)
    {
        if (!running.get()) {
            log.debug("count called on shutdown recorder");
            //TODO: something besides silently ignore?
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("metric=")
          .append(label.toString());
        serializeContext(sb, context.metadata());
        sb.append(":count:")
          .append(delta)
          .append('\n');

        logQueue.add(sb.toString());
    }

    private void serializeContext(StringBuilder sb, TypedMap context) {
        context.forEach(
                (e) -> {
                    sb.append(",")
                      .append(e.getKey().name)
                      .append("=")
                      .append(e.getKey().valueType.cast(e.getValue()).toString());
                }
        );
    }

}
