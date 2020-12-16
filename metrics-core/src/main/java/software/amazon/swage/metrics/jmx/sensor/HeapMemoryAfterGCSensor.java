package software.amazon.swage.metrics.jmx.sensor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import software.amazon.swage.metrics.Metric;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

public class HeapMemoryAfterGCSensor implements Sensor {

    private static final List<String> LONG_LIVED_MEMORY_POOL_NAMES =
            Arrays.asList("Old", "Tenured", "Survivor");
    // Visible for testing
    static final Metric HEAP_AFTER_GC_USE = Metric.define("HeapMemoryAfterGCUse");

    private final Supplier<List<MemoryPoolMXBean>> memoryPoolMXBeanSupplier;

    public HeapMemoryAfterGCSensor() {
        this(() -> ManagementFactory.getMemoryPoolMXBeans());
    }

    HeapMemoryAfterGCSensor(final Supplier<List<MemoryPoolMXBean>> memoryPoolMXBeanSupplier) {
        this.memoryPoolMXBeanSupplier = memoryPoolMXBeanSupplier;
    }

    @Override
    public void sense(final MetricContext metricContext) {
        final List<MemoryPoolMXBean> memoryPoolMXBeans = memoryPoolMXBeanSupplier.get();
        if (memoryPoolMXBeans == null) {
            throw new IllegalStateException("No memory pool MX beans available");
        }

        // Get the used and available old gen and survivor space after the last GC to calculate the heap usage.
        long usedKb = 0, totalKb = 0;
        for (final MemoryPoolMXBean memoryPool : memoryPoolMXBeanSupplier.get()) {
            if (isLongLivedMemoryPool(memoryPool)) {
                final MemoryUsage memoryPoolData = memoryPool.getCollectionUsage();

                // .getCollectionUsage() is not supported for all memory pools:
                // https://docs.oracle.com/javase/7/docs/api/java/lang/management/MemoryPoolMXBean.html#getCollectionUsage()
                if (memoryPoolData != null) {
                    final long used = memoryPoolData.getUsed();
                    usedKb += used / 1024;

                    final long max = memoryPoolData.getMax();
                    // Max can be undefined (-1) http://docs.oracle.com/javase/7/docs/api/java/lang/management/MemoryUsage.html
                    totalKb += max == -1 ? 0 : max / 1024;
                }
            }
        }
        recordHeapAfterGcUsePercentageMetric(metricContext, usedKb, totalKb);
    }

    private boolean isLongLivedMemoryPool(final MemoryPoolMXBean memoryPoolMXBean) {
        final String memoryPoolName = memoryPoolMXBean.getName();
        if (memoryPoolName != null) {
            return LONG_LIVED_MEMORY_POOL_NAMES.stream()
                .anyMatch(nameComponent -> memoryPoolName.contains(nameComponent));
        }
        return false;
    }

    private void recordHeapAfterGcUsePercentageMetric(final MetricContext metricContext, final long usedKb, final long totalKb) {
        if (totalKb > 0) {
            final double percent = 100.0 * (double) usedKb / (double) totalKb;
            metricContext.record(HEAP_AFTER_GC_USE, percent, Unit.PERCENT);
        }
    }
}