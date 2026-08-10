package dev.riftgun.service;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Stable wire representation of a locator token bound to one portal-gun instance. */
public final class PortalGunReference {
    private static final String LOCATOR = "Locator";
    private static final String TOKEN = "Token";
    private static final String INSTANCE_ID = "InstanceId";

    public static CompoundTag capture(String locatorId, CompoundTag token, UUID instanceId) {
        CompoundTag reference = new CompoundTag();
        reference.putString(LOCATOR, locatorId);
        reference.put(TOKEN, token.copy());
        reference.putUUID(INSTANCE_ID, instanceId);
        return reference;
    }

    public static String locatorId(CompoundTag reference) {
        return reference.getString(LOCATOR);
    }

    public static CompoundTag token(CompoundTag reference) {
        return reference.getCompound(TOKEN).copy();
    }

    public static boolean matches(CompoundTag reference, UUID instanceId) {
        return reference.hasUUID(INSTANCE_ID) && reference.getUUID(INSTANCE_ID).equals(instanceId);
    }

    private PortalGunReference() {}
}
