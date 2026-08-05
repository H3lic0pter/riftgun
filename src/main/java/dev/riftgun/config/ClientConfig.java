package dev.riftgun.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    static {
        var configured = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = configured.getLeft();
        SPEC = configured.getRight();
    }

    public static final class Values {
        public final ModConfigSpec.ConfigValue<String> portalVisualType;

        private Values(ModConfigSpec.Builder builder) {
            portalVisualType = builder.comment("Client-local portal visual type ID")
                .define("portalVisualType", "riftgun:classic");
        }
    }

    private ClientConfig() {}
}
