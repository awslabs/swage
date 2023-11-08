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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

/**
 * Tests for the RollingFileWriter.
 * <p>
 * Requires the "test.area" system property to be set, specifying directory where temporary log
 * files will be created.
 * <p>
 * TODO: more/robust tests, you know, like actual log rolling
 */
class RollingFileWriterTest {

    private static final Path testDir;

    static {
        String testProp = System.getProperty("test.area");
        if (testProp == null || testProp.isEmpty()) {
            throw new IllegalArgumentException("test.area property not set");
        }
        testDir = Paths.get(testProp);
    }

    private String currentHourExt(TimeZone tz) {
        return DateTimeFormatter
                .ofPattern(".yyyy-MM-dd-HH")
                .format(LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.HOURS),
                        tz.toZoneId()));
    }

    private String currentMinExt(TimeZone tz) {
        return DateTimeFormatter
                .ofPattern(".yyyy-MM-dd-HH-mm")
                .format(LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MINUTES),
                        tz.toZoneId()));
    }

    @Test
    void nullDir() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RollingFileWriter(null, "foo");
        });
    }

    @Test
    void nullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RollingFileWriter(testDir, null);
        });
    }

    @Test
    void nullZone() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RollingFileWriter(testDir, "bar", null, false);
        });
    }


    @Test
    void filenameUTC() throws Exception {
        final String base = "test";

        // Current rolled file for the current hour
        //TODO: this may fail in edge cases where the hour turns during test
        final Path expected = testDir.resolve(base + currentHourExt(TimeZone.getTimeZone("UTC")));

        final RollingFileWriter w = new RollingFileWriter(testDir, base);

        // No writes yet, no file yet
        assertEquals(null, w.getCurrentFile());

        // Force the writer to be created
        w.write("some attributes here", 0, 0);

        // Clean up
        w.close();

        assertEquals(expected, w.getCurrentFile());
    }

    @Test
    void filenameLocalZone() throws Exception {
        final TimeZone tz = TimeZone.getDefault();
        final String base = "what";

        final Path expected = testDir.resolve(base + currentHourExt(tz));

        final RollingFileWriter w = new RollingFileWriter(testDir, base, tz, false);
        w.write("y u no utc", 0, 0);
        w.close();

        assertEquals(expected, w.getCurrentFile());
    }

    @Test
    void filenameMinute() throws Exception {
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final String base = "mins";

        // Current rolled file for the current minute
        final Path expected = testDir.resolve(base + currentMinExt(tz));

        // Set the RollingFileWriter to roll every minute
        final RollingFileWriter w = new RollingFileWriter(testDir, base, tz, true);
        w.write("mo attributes mo problems", 0, 0);
        w.close();

        // Just in case minute rolled during test, check before and after minutes
        Path p = w.getCurrentFile();
        assertTrue(p.equals(expected) ||
                p.equals(testDir.resolve(base + currentMinExt(tz))));
    }

    @Test
    void createDirectory() throws Exception {
        // Create a unique non-existent file path
        Path path;
        do {
            path = testDir.resolve("a/b/" + System.currentTimeMillis());
        } while (Files.exists(path));

        RollingFileWriter w = new RollingFileWriter(path, "test");
        w.write("stuff here");
        w.flush();
        w.close();

        assertTrue(Files.exists(path));
    }

}
