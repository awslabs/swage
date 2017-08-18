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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

/**
 * Provide file rolling capability with file naming based on hourly rolling.
 * This only rolls after a {@link #flush()} so it will not split records.
 *
 * <p>This class is <i>not thread-safe</i>. It is designed to be used
 * exclusively by {@code FileRecorder}, which provides external
 * synchronization.
 *
 * TODO: not this, leverage log4j or some other robust implementation
 */
class RollingFileWriter extends Writer {

    private final ChronoUnit rollInterval;
    private final ZoneId timeZone;

    private final DateTimeFormatter format;
    private final Path directory;
    private final String baseName;

    private Instant rollAt;

    private Writer writer;
    private Path outPath;
    private boolean flushed = true;

    /**
     * Create a RollingFileWriter that will create one log file per hour
     * in the given directory, named as "name.[yyyy-MM-dd-HH]" with timestamps
     * in UTC.
     *
     * @param directory Path where the log files will be created
     * @param name Base name to use for log files
     */
    public RollingFileWriter(Path directory, String name)
    {
        this(directory, name, TimeZone.getTimeZone("UTC"), false);
    }

    /**
     * Create a RollingFileWriter as specified
     *
     * @param directory Path where the log files will be created
     * @param name Base name to use for log files
     * @param zone Time zone of timestamps on rolling file names
     * @param minuteRotation If true, rotate logs every minute, if false rotate every hour
     */
    RollingFileWriter(Path directory, String name, TimeZone zone, boolean minuteRotation)
    {
        if (directory == null) {
            throw new IllegalArgumentException("Directory required");
        }
        if (name == null) {
            throw new IllegalArgumentException("File base name required");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Time zone required");
        }

        this.directory = directory;
        this.baseName = name;
        this.timeZone = zone.toZoneId();

        final String formatPattern = (minuteRotation ? ".yyyy-MM-dd-HH-mm" : ".yyyy-MM-dd-HH");
        this.format = DateTimeFormatter.ofPattern(formatPattern);

        if (minuteRotation) {
            rollInterval = ChronoUnit.SECONDS;
        } else {
            rollInterval = ChronoUnit.HOURS;
        }

        // Set to roll immediately on first write, to ensure file is created
        this.rollAt = Instant.now().truncatedTo(rollInterval);
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
        rollIfNeeded();
        writer.write(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        flushed = true;
    }


    /**
     * @return Path of the current file being written to,
     *         null if no writes have occurred yet.
     */
    public Path getCurrentFile() {
        return this.outPath;
    }

   /**
     * If necessary, close the current logfile and open a new one,
     * updating {@link #writer}.
     */
    private void rollIfNeeded() throws IOException {
        // Do not roll unless record is complete, as indicated by flush
        if (!flushed) return;
        flushed = false;

        // If we have not yet passed the roll over mark do nothing
        Instant now = Instant.now();
        if (now.isBefore(rollAt)) {
            return;
        }

        // New file time, may not be the rollAt time if one or more intervals
        // have passed without anything being written
        Instant rollTime = now.truncatedTo(rollInterval);

        // Determine the name of the file that will be written to
        String name = this.baseName + format.format(LocalDateTime.ofInstant(rollTime, timeZone));
        this.outPath = this.directory.resolve(name);

        // Finish writing to previous log
        if (writer != null) {
            writer.flush();
            writer.close();
        }

        // Ensure the parent directory always exists, even if it was removed out from under us.
        // A no-op if the directory already exists.
        Files.createDirectories(outPath.getParent());

        // Create new file, set it as our write destination
        writer = Files.newBufferedWriter(
                    outPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

        // Point to the next time we want to roll the log, update rollAt
        this.rollAt = rollTime.plus(1, rollInterval);
    }

}
