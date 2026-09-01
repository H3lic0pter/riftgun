package dev.riftgun.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Startup-loaded integration settings needed while datapack recipes are decoded. */
public final class IntegrationConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    private static volatile boolean createMixingRecipesEnabled = true;

    static {
        var configured = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = configured.getLeft();
        SPEC = configured.getRight();
    }

    public static void publishSnapshot() {
        createMixingRecipesEnabled = VALUES.createMixingRecipesEnabled.get();
    }

    /** Safe during early datapack decoding, before NeoForge has loaded any config files. */
    public static boolean createMixingRecipesEnabled() {
        return createMixingRecipesEnabled;
    }

    public static final class Values {
        public final ModConfigSpec.BooleanValue createMixingRecipesEnabled;

        private Values(ModConfigSpec.Builder builder) {
            builder.push("integrations");
            createMixingRecipesEnabled = builder.comment(
                    "Enable RiftGun portal-fluid recipes for Create Mechanical Mixers when Create is installed.")
                .gameRestart()
                .define("createMixingRecipes", true);
            builder.pop();
        }
    }

    private IntegrationConfig() {}
}
