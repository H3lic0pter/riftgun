package dev.riftgun.internal.shader;

import java.util.Objects;

/** Data-only rendering capabilities selected for the active shader pack. */
public record ShaderPackProfile(EndframeCenter endframeCenter) {
    public static final ShaderPackProfile EMPTY = new ShaderPackProfile(EndframeCenter.empty());

    public ShaderPackProfile {
        Objects.requireNonNull(endframeCenter, "endframeCenter");
    }

    public record EndframeCenter(Mode mode, int materialId) {
        public EndframeCenter {
            Objects.requireNonNull(mode, "mode");
            if (mode == Mode.IRIS_BLOCK_ENTITY && materialId < 0) {
                throw new IllegalArgumentException("Iris material id must be non-negative");
            }
        }

        public static EndframeCenter empty() {
            return new EndframeCenter(Mode.EMPTY, 0);
        }

        public static EndframeCenter irisBlockEntity(int materialId) {
            return new EndframeCenter(Mode.IRIS_BLOCK_ENTITY, materialId);
        }

        public enum Mode {
            EMPTY,
            IRIS_BLOCK_ENTITY
        }
    }
}
