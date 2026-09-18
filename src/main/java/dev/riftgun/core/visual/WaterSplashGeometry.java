package dev.riftgun.core.visual;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Stable geometric water masks. No pixels are copied out of the game's animated atlas. */
public final class WaterSplashGeometry {
    public static final float OPENING_TICKS = 6.0F;
    private static final int RIM_SEGMENTS = 128;
    private static final int DROP_COUNT = 7;
    private static final int DROP_SEGMENTS = 12;
    private static final double TAU = Math.PI * 2.0;
    private static final int CACHE_LIMIT = 128;
    private static final Map<UUID, Mesh> CACHE = new LinkedHashMap<>(32, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, Mesh> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    public static synchronized Mesh forPortal(UUID id) {
        return CACHE.computeIfAbsent(id, WaterSplashGeometry::create);
    }

    private static Mesh create(UUID id) {
        Random random = new Random(id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 23));
        double[] angles = new double[11];
        double[] lengths = new double[angles.length];
        double[] widths = new double[angles.length];
        for (int i = 0; i < angles.length; i++) {
            angles[i] = TAU * (i + random.nextDouble() * 0.7) / angles.length;
            lengths[i] = 0.12 + random.nextDouble() * 0.18;
            widths[i] = 0.045 + random.nextDouble() * 0.11;
        }
        float[] rim = new float[RIM_SEGMENTS * 2];
        double phase = random.nextDouble() * TAU;
        for (int i = 0; i < RIM_SEGMENTS; i++) {
            double angle = TAU * i / RIM_SEGMENTS;
            double radius = 0.61 + 0.035 * Math.sin(3 * angle + phase)
                + 0.025 * Math.sin(7 * angle - phase);
            for (int lobe = 0; lobe < angles.length; lobe++) {
                double distance = Math.atan2(Math.sin(angle - angles[lobe]), Math.cos(angle - angles[lobe]));
                radius += lengths[lobe] * Math.exp(-0.5 * distance * distance / (widths[lobe] * widths[lobe]));
            }
            radius = Math.min(radius, 0.88);
            rim[i * 2] = (float) (Math.cos(angle) * radius);
            rim[i * 2 + 1] = (float) (Math.sin(angle) * radius);
        }
        float[] vertices = new float[(RIM_SEGMENTS + DROP_COUNT * DROP_SEGMENTS) * 8];
        int offset = fan(vertices, 0, 0, 0, rim);
        for (int drop = 0; drop < DROP_COUNT; drop++) {
            double angle = phase + TAU * (drop + random.nextDouble() * 0.4) / DROP_COUNT;
            float centerX = (float) (0.94 * Math.cos(angle));
            float centerY = (float) (0.94 * Math.sin(angle));
            double radialSize = 0.025 + random.nextDouble() * 0.025;
            double tangentSize = radialSize * (0.55 + random.nextDouble() * 0.25);
            float[] dropRim = new float[DROP_SEGMENTS * 2];
            for (int i = 0; i < DROP_SEGMENTS; i++) {
                double a = TAU * i / DROP_SEGMENTS;
                double x = Math.cos(a) * radialSize;
                double y = Math.sin(a) * tangentSize;
                dropRim[i * 2] = centerX + (float) (x * Math.cos(angle) - y * Math.sin(angle));
                dropRim[i * 2 + 1] = centerY + (float) (x * Math.sin(angle) + y * Math.cos(angle));
            }
            offset = fan(vertices, offset, centerX, centerY, dropRim);
        }
        return new Mesh(vertices, rim, phase);
    }

    /** Triangle fans encoded as quads, matching the vanilla entity material's vertex format. */
    private static int fan(float[] out, int offset, float centerX, float centerY, float[] rim) {
        for (int i = 0; i < rim.length; i += 2) {
            int next = (i + 2) % rim.length;
            out[offset++] = centerX;
            out[offset++] = centerY;
            out[offset++] = rim[i];
            out[offset++] = rim[i + 1];
            out[offset++] = rim[next];
            out[offset++] = rim[next + 1];
            out[offset++] = centerX;
            out[offset++] = centerY;
        }
        return offset;
    }

    public static final class Mesh {
        private final float[] vertices;
        private final float[] rim;
        private final double phase;

        private Mesh(float[] vertices, float[] rim, double phase) {
            this.vertices = vertices;
            this.rim = rim;
            this.phase = phase;
        }
        public int vertexCount() { return vertices.length / 2; }
        public float x(int vertex) { return vertices[vertex * 2]; }
        public float y(int vertex) { return vertices[vertex * 2 + 1]; }
        public int rimCount() { return rim.length / 2; }
        public float rimX(int index) { return rim[index * 2]; }
        public float rimY(int index) { return rim[index * 2 + 1]; }

        /** Small radial waves; the mask never rotates and detached droplets stay fixed. */
        public float wave(float x, float y, float age) {
            if (x * x + y * y < 0.01F) return 1;
            double angle = Math.atan2(y, x);
            return 1 + (float) (0.012 * Math.sin(angle * 3 + phase + age * 0.07)
                + 0.008 * Math.sin(angle * 5 - phase - age * 0.045));
        }

        public float vertexWave(int vertex, float age) {
            return vertex < RIM_SEGMENTS * 4 ? wave(x(vertex), y(vertex), age) : 1;
        }

        /** Broken highlight arcs, rather than a uniform luminous outline. */
        public float highlight(int index, float age) {
            double angle = TAU * index / rimCount();
            return (float) Math.max(0, Math.sin(angle * 3 + phase + age * 0.025) - 0.35);
        }
    }

    private WaterSplashGeometry() {}
}
