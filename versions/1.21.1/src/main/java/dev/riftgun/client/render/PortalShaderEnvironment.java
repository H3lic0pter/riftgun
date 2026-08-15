package dev.riftgun.client.render;

@FunctionalInterface
interface PortalShaderEnvironment {
    State snapshot();

    record State(boolean shaderPackActive, boolean shadowPass) {
        static final State INACTIVE = new State(false, false);
        static final State COMPATIBILITY_FALLBACK = new State(true, false);
    }
}
