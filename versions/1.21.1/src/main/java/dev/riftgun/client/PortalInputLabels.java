package dev.riftgun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** User-facing labels for configurable vanilla controls used as action modifiers. */
public final class PortalInputLabels {
    public static Component sneakKey() {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
    }

    private PortalInputLabels() {}
}
