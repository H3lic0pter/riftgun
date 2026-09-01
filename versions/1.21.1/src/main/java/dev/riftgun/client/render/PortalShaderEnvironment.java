package dev.riftgun.client.render;

@FunctionalInterface
interface PortalShaderEnvironment {
    State snapshot();

    default boolean shadowPass() {
        return snapshot().shadowPass();
    }

    record State(boolean shaderPackActive, boolean shadowPass, String shaderPackName) {
        static final State INACTIVE = new State(false, false, "");
        static final State COMPATIBILITY_FALLBACK = new State(true, false, "");

        public State {
            shaderPackName = shaderPackName == null ? "" : shaderPackName;
        }

        static State active(String shaderPackName) {
            return new State(true, false, shaderPackName);
        }
    }
}
