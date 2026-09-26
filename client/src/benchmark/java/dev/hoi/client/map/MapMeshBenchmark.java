package dev.hoi.client.map;

import com.sun.management.ThreadMXBean;
import dev.hoi.protocol.ProtocolAtlasScene.Box;
import dev.hoi.protocol.ProtocolAtlasScene.Material;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class MapMeshBenchmark {
    private static volatile Object result;

    public static void main(String[] args) {
        var allocation = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        allocation.setThreadAllocatedMemoryEnabled(true);
        System.out.println("java=" + System.getProperty("java.version") + " os=" + System.getProperty("os.name")
                + " arch=" + System.getProperty("os.arch") + " warmup=10 samples=15 heap=1g");
        System.out.println("boxes,baseline_ms,current_ms,baseline_bytes,current_bytes");
        for (int size : new int[]{128, 8192, 65536}) {
            var boxes = boxes(size);
            equivalent(boxes);
            Supplier<Object> baseline = () -> MapMeshBaseline.build("hoi:army", boxes);
            Supplier<Object> current = () -> MapSceneMesh.build("hoi:army", boxes);
            for (int i = 0; i < 10; i++) { result = baseline.get(); result = current.get(); }
            long[] before = new long[15], after = new long[15], beforeBytes = new long[15], afterBytes = new long[15];
            for (int i = 0; i < 15; i++) {
                if (i % 2 == 0) {
                    measure(baseline, before, beforeBytes, i, allocation);
                    measure(current, after, afterBytes, i, allocation);
                } else {
                    measure(current, after, afterBytes, i, allocation);
                    measure(baseline, before, beforeBytes, i, allocation);
                }
            }
            Arrays.sort(before); Arrays.sort(after); Arrays.sort(beforeBytes); Arrays.sort(afterBytes);
            System.out.printf(Locale.ROOT, "%d,%.3f,%.3f,%d,%d%n", size, before[7] / 1e6, after[7] / 1e6, beforeBytes[7], afterBytes[7]);
        }
    }

    private static void measure(Supplier<Object> operation, long[] times, long[] bytes, int index, ThreadMXBean allocation) {
        long used = allocation.getThreadAllocatedBytes(Thread.currentThread().threadId());
        long started = System.nanoTime();
        result = operation.get();
        times[index] = System.nanoTime() - started;
        bytes[index] = allocation.getThreadAllocatedBytes(Thread.currentThread().threadId()) - used;
    }

    private static List<Box> boxes(int size) {
        var boxes = new ArrayList<Box>(size);
        var materials = Material.values();
        for (int i = 0; i < size; i++) boxes.add(new Box((i % 512 - 256) * 2, 70, (i / 512 - 64) * 2,
                1 + i % 3, 1 + i % 5, 1 + i % 7, (i % 63 - 31) * .1f, materials[i % materials.length]));
        return List.copyOf(boxes);
    }

    private static void equivalent(List<Box> boxes) {
        var baseline = MapMeshBaseline.build("hoi:army", boxes);
        var current = MapSceneMesh.build("hoi:army", boxes);
        if (!baseline.dimension().equals(current.dimension()) || !baseline.cells().equals(current.cells())
                || !baseline.large().equals(current.large()) || baseline.tiles().size() != current.tiles().size())
            throw new AssertionError("Mesh grouping or index changed");
        for (int i = 0; i < current.tiles().size(); i++) {
            var expected = baseline.tiles().get(i); var actual = current.tiles().get(i);
            if (expected.material() != actual.material() || !expected.bounds().equals(actual.bounds())
                    || !expected.cities().equals(actual.cities()) || !Arrays.equals(expected.vertices(), actual.vertices()))
                throw new AssertionError("Mesh differs at tile " + i);
        }
    }

    private MapMeshBenchmark() {}
}
