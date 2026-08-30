package dev.riftgun.math;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Precomputed horizontal runs for rendering a radial ring with few GUI draw calls. */
public final class RadialRingSpans {
    private static final ConcurrentMap<Key, List<Span>> CACHE = new ConcurrentHashMap<>();

    public static void forEach(int innerRadius, int outerRadius, int sample,
                               boolean pixelCentered, int optionCount, SpanConsumer consumer) {
        Key key = new Key(innerRadius, outerRadius, sample, pixelCentered, optionCount);
        for (Span span : CACHE.computeIfAbsent(key, RadialRingSpans::create)) {
            consumer.accept(span.xFrom(), span.y(), span.xTo(), span.height(), span.optionIndex());
        }
    }

    private static List<Span> create(Key key) {
        if (key.innerRadius() < 0 || key.outerRadius() <= key.innerRadius()
            || key.sample() <= 0 || key.optionCount() <= 0) {
            throw new IllegalArgumentException("Invalid radial ring geometry");
        }
        var spans = new java.util.ArrayList<Span>();
        int maximumCoordinate = key.pixelCentered()
            ? key.outerRadius() - 1 : key.outerRadius();
        double offset = key.pixelCentered() ? 0.5 : 0.0;
        for (int y = -key.outerRadius(); y <= maximumCoordinate; y += key.sample()) {
            int runIndex = -1;
            int runStart = 0;
            int finalX = -key.outerRadius();
            for (int x = -key.outerRadius(); x <= maximumCoordinate; x += key.sample()) {
                finalX = x;
                double radialX = x + offset;
                double radialY = y + offset;
                double distanceSquared = radialX * radialX + radialY * radialY;
                int index = distanceSquared < key.innerRadius() * key.innerRadius()
                    || distanceSquared > key.outerRadius() * key.outerRadius()
                    ? -1 : RadialModeGeometry.selectionIndex(
                        radialX, radialY, key.optionCount(), 0.0).orElse(0);
                if (index == runIndex) continue;
                if (runIndex >= 0) {
                    spans.add(new Span(runStart, y, x, key.sample(), runIndex));
                }
                runIndex = index;
                runStart = x;
            }
            if (runIndex >= 0) {
                spans.add(new Span(runStart, y, finalX + key.sample(), key.sample(), runIndex));
            }
        }
        return List.copyOf(spans);
    }

    @FunctionalInterface
    public interface SpanConsumer {
        void accept(int xFrom, int y, int xTo, int height, int optionIndex);
    }

    private record Key(int innerRadius, int outerRadius, int sample,
                       boolean pixelCentered, int optionCount) {}

    private record Span(int xFrom, int y, int xTo, int height, int optionIndex) {}

    private RadialRingSpans() {}
}
