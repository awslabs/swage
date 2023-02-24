package software.amazon.swage.metrics.jmx.sensor;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.swage.metrics.MetricContext;
import software.amazon.swage.metrics.Unit;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.swage.metrics.jmx.sensor.HeapMemoryAfterGCSensor.HEAP_AFTER_GC_USE;

class HeapMemoryAfterGCSensorTest {
    private static final String OLD_MEM_POOL = "OldPool";
    private static final String EDEN_MEM_POOL = "EdenPool";
    private static final String TENURED_MEM_POOL = "TenuredPool";

    private MetricContext metricContext;

    @BeforeEach
    void initialize() {
        metricContext = mock(MetricContext.class);
    }

    @Test
    void sense_withNullMemoryPoolList_throwsIllegalStateException() {
        Executable test = () -> new HeapMemoryAfterGCSensor(() -> null).sense(metricContext);
        assertThrows(IllegalStateException.class, test);
    }

    @Test
    void sense_withNoMemoryPools_doesNotRecordMetric() {
        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(Collections::emptyList);
        sensor.sense(metricContext);

        verify(metricContext, never()).record(any(), any(), any());
    }

    @Test
    void sense_withNullLongLivedMemoryPoolData_doesNotRecordMetric() {
        final MemoryPoolMXBean oldMemPoolWithNullData = mock(MemoryPoolMXBean.class);
        when(oldMemPoolWithNullData.getName()).thenReturn(OLD_MEM_POOL);
        when(oldMemPoolWithNullData.getCollectionUsage()).thenReturn(null);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> singletonList(oldMemPoolWithNullData));
        sensor.sense(metricContext);

        verify(metricContext, never()).record(any(), any(), any());
    }

    @Test
    void sense_withMemoryPoolWithoutName_doesNotRecordMetric() {
        final MemoryPoolMXBean memPoolWithNullName = mock(MemoryPoolMXBean.class);
        when(memPoolWithNullName.getName()).thenReturn(null);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> singletonList(memPoolWithNullName));
        sensor.sense(metricContext);

        verify(metricContext, never()).record(any(), any(), any());
    }

    @Test
    void sense_withNoLongLivedMemory_doesNotRecordMetric() {
        final long usedMem = 1024L;
        final long totalMem = 10240L;
        final MemoryPoolMXBean edenMemPool = setupMemoryPoolData(EDEN_MEM_POOL, usedMem, totalMem);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> singletonList(edenMemPool));
        sensor.sense(metricContext);

        verify(metricContext, never()).record(any(), any(), any());
    }

    @Test
    void sense_withSingleLongLivedMemoryPool_recordsSingleMetric() {
        final long usedMem = 1024L;
        final long totalMem = 10240L;
        final double expectedUseMetric = 100.0 * usedMem / totalMem;

        final MemoryPoolMXBean oldMemPool = setupMemoryPoolData(OLD_MEM_POOL, usedMem, totalMem);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> singletonList(oldMemPool));
        sensor.sense(metricContext);

        verify(metricContext, times(1)).record(HEAP_AFTER_GC_USE, expectedUseMetric, Unit.PERCENT);
    }

    @Test
    void sense_withSingleLongLivedMemoryPoolButUndefinedMax_doesNotRecordMetric() {
        final long usedMem = 1024L;
        final long totalMem = -1L;

        final MemoryPoolMXBean oldMemPool = setupMemoryPoolData(OLD_MEM_POOL, usedMem, totalMem);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> singletonList(oldMemPool));
        sensor.sense(metricContext);

        verify(metricContext, never()).record(any(), any(), any());
    }

    @Test
    void sense_withShortAndLongLivedMemoryPool_recordsSingleMetric() {
        final long oldUsedMem = 1024L;
        final long oldTotalMem = 10240L;
        final long edenUsedMem = 2048L;
        final long edenTotalMem = 20480L;
        final double expectedUseMetric = 100.0 * oldUsedMem / oldTotalMem;

        final MemoryPoolMXBean oldMemPool = setupMemoryPoolData(OLD_MEM_POOL, oldUsedMem, oldTotalMem);
        final MemoryPoolMXBean edenMemPool = setupMemoryPoolData(EDEN_MEM_POOL, edenUsedMem, edenTotalMem);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> Arrays.asList(oldMemPool, edenMemPool));
        sensor.sense(metricContext);

        verify(metricContext, times(1)).record(HEAP_AFTER_GC_USE, expectedUseMetric, Unit.PERCENT);
    }

    @Test
    void sense_withTwoLongLivedMemoryPools_recordsAggregateMetric() {
        final long oldUsedMem = 1024L;
        final long oldTotalMem = 10240L;
        final long tenuredUsedMem = 2048L;
        final long tenuredTotalMem = 20480L;
        final double expectedUseMetric =
                100.0 * (oldUsedMem + tenuredUsedMem) / (oldTotalMem + tenuredTotalMem);

        final MemoryPoolMXBean oldMemPool = setupMemoryPoolData(OLD_MEM_POOL, oldUsedMem, oldTotalMem);
        final MemoryPoolMXBean tenuredMemPool = setupMemoryPoolData(TENURED_MEM_POOL, tenuredUsedMem, tenuredTotalMem);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> Arrays.asList(oldMemPool, tenuredMemPool));
        sensor.sense(metricContext);

        verify(metricContext, times(1)).record(HEAP_AFTER_GC_USE, expectedUseMetric, Unit.PERCENT);
    }

    @Test
    void sense_withTwoLongLivedMemoryPoolsAndOneUndefinedMax_recordsAggregateMetric() {
        final long oldUsedMem = 1024L;
        final long oldTotalMem = -1L;
        final long tenuredUsedMem = 2048L;
        final long tenuredTotalMem = 20480L;
        final double expectedUseMetric =
                100.0 * (oldUsedMem + tenuredUsedMem) / tenuredTotalMem;

        final MemoryPoolMXBean oldMemPool = setupMemoryPoolData(OLD_MEM_POOL, oldUsedMem, oldTotalMem);
        final MemoryPoolMXBean tenuredMemPool = setupMemoryPoolData(TENURED_MEM_POOL, tenuredUsedMem, tenuredTotalMem);

        final HeapMemoryAfterGCSensor sensor =
                new HeapMemoryAfterGCSensor(() -> Arrays.asList(oldMemPool, tenuredMemPool));
        sensor.sense(metricContext);

        verify(metricContext, times(1)).record(HEAP_AFTER_GC_USE, expectedUseMetric, Unit.PERCENT);
    }

    private MemoryPoolMXBean setupMemoryPoolData(final String memoryPoolName, final long usedBytes, final long maxBytes) {
        final MemoryPoolMXBean memoryPoolMXBean = mock(MemoryPoolMXBean.class);
        final MemoryUsage memoryUsage = mock(MemoryUsage.class);
        when(memoryUsage.getUsed()).thenReturn(usedBytes);
        when(memoryUsage.getMax()).thenReturn(maxBytes);
        when(memoryPoolMXBean.getName()).thenReturn(memoryPoolName);
        when(memoryPoolMXBean.getCollectionUsage()).thenReturn(memoryUsage);

        return memoryPoolMXBean;
    }
}