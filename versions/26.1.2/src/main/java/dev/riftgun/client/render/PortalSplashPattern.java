package dev.riftgun.client.render;

import dev.riftgun.portal.PortalLifecycle;

/** Tick-rate and perimeter math for the vanilla splash emitter. */
final class PortalSplashPattern {
    private static final float MINIMUM_EDGE_SCALE = 0.08F;

    static int particleCount(PortalLifecycle.Phase phase) {
        return switch (phase) {
            case CHARGING -> 4;
            case OPENING -> 6;
            case CLOSING -> 4;
            case OPEN, CLOSED -> 0;
        };
    }

    static float edgeScale(PortalLifecycle.Phase phase, int phaseTicks) {
        return switch (phase) {
            case CHARGING -> 0.08F + 0.12F * clamp((phaseTicks + 1.0F) / PortalLifecycle.CHARGE_TICKS);
            case OPENING, CLOSING -> {
                float visible = PortalLifecycle.visibleProgress(phase, phaseTicks + 1, 0.0F);
                yield Math.max(MINIMUM_EDGE_SCALE,
                    (float) Math.sin(visible * Math.PI * 0.5));
            }
            case OPEN -> 1.0F;
            case CLOSED -> MINIMUM_EDGE_SCALE;
        };
    }

    static EdgePoint sampleEdge(float width, float height, float scale, double unitPosition) {
        double halfWidth = width * scale * 0.5;
        double halfHeight = height * scale * 0.5;
        double horizontalLength = halfWidth * 2.0;
        double verticalLength = halfHeight * 2.0;
        double perimeter = (horizontalLength + verticalLength) * 2.0;
        if (perimeter <= 0.0) return new EdgePoint(0.0, 0.0);

        double cursor = wrapUnit(unitPosition) * perimeter;
        if (cursor < horizontalLength) {
            return new EdgePoint(-halfWidth + cursor, halfHeight);
        }
        cursor -= horizontalLength;
        if (cursor < verticalLength) {
            return new EdgePoint(halfWidth, halfHeight - cursor);
        }
        cursor -= verticalLength;
        if (cursor < horizontalLength) {
            return new EdgePoint(halfWidth - cursor, -halfHeight);
        }
        cursor -= horizontalLength;
        return new EdgePoint(-halfWidth, -halfHeight + cursor);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static double wrapUnit(double value) {
        return value - Math.floor(value);
    }

    record EdgePoint(double right, double up) {}

    private PortalSplashPattern() {}
}
