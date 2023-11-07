package software.amazon.swage.metrics.record.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.swage.metrics.Metric;

class FileRecorderTest {

    @Test
    void validation() {
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
            assertTrue(FileRecorder.isValid(Metric.define(s)), "valid name [" + s + "]");
        }

        final String[] bad = {
                // Each case demonstrates one form of ambiguity:
                "metric_name, dimension=value",
                "metric_name:100ms:",
                "metric_name\nmetric",
                "metric=metric_name"
        };
        for (final String s : bad) {
            assertFalse(FileRecorder.isValid(Metric.define(s)), "invalid name [" + s + "]");
        }
    }
}
