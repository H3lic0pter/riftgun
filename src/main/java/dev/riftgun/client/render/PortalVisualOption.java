package dev.riftgun.client.render;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public sealed interface PortalVisualOption permits PortalVisualOption.Toggle, PortalVisualOption.Range {
    String labelKey();

    boolean active();

    void reset();

    record Toggle(String labelKey, BooleanSupplier value, Consumer<Boolean> update,
                  boolean defaultValue) implements PortalVisualOption {
        @Override
        public boolean active() {
            return true;
        }

        public void toggle() {
            update.accept(!value.getAsBoolean());
        }

        @Override
        public void reset() {
            update.accept(defaultValue);
        }
    }

    record Range(String labelKey, DoubleSupplier value, DoubleConsumer update,
                 BooleanSupplier enabled, double minimum, double maximum,
                 double step, double defaultValue) implements PortalVisualOption {
        public Range {
            if (maximum <= minimum) throw new IllegalArgumentException("Range maximum must exceed minimum");
            if (step <= 0.0) throw new IllegalArgumentException("Range step must be positive");
        }

        @Override
        public void reset() {
            update.accept(defaultValue);
        }

        @Override
        public boolean active() {
            return enabled.getAsBoolean();
        }

        public double normalizedValue() {
            return (clampAndSnap(value.getAsDouble()) - minimum) / (maximum - minimum);
        }

        public double valueAt(double normalized) {
            return clampAndSnap(minimum + clamp(normalized, 0.0, 1.0) * (maximum - minimum));
        }

        public double currentValue() {
            return clampAndSnap(value.getAsDouble());
        }

        private double clampAndSnap(double candidate) {
            double clamped = clamp(candidate, minimum, maximum);
            double snapped = minimum + Math.round((clamped - minimum) / step) * step;
            return clamp(snapped, minimum, maximum);
        }

        private static double clamp(double value, double minimum, double maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }
    }
}
