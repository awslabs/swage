package software.amazon.swage.metrics.record.file;

import org.junit.Test;

import software.amazon.swage.metrics.Metric;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileRecorderTest {
    @Test
    public void validation() {
        final String[] good = {
            "snake_case_metric",
            "camelCaseMetric",
            "hyphenated-metric",
            "spaced out metric",
            "digits 0123456789",
            "G\u00FCnther",
            // All of the bad-idea characters that are allowed (and not above)
            "\t!\"#$%&'()*+./;<>?@[\\]^`{|}~" // no ',' ':' '\n'
        };
        for (final String s : good) {
            assertTrue("valid name [" + s + "]", FileRecorder.isValid(Metric.define(s)));
        }

        final String[] bad = {
            // Each case demonstrates one form of ambiguity:
            "metric_name, dimension=value",
            "metric_name:100ms:",
            "metric_name\nmetric",
            "metric=metric_name"
        };
        for (final String s : bad) {
            assertFalse("invalid name [" + s + "]", FileRecorder.isValid(Metric.define(s)));
        }
    }
}
