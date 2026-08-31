package dev.riftgun.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Version-neutral domain intent for placing a portal on one selected block face. */
public record SurfaceFaceSelection(BlockPos anchor, Direction face) {
    public SurfaceFaceSelection {
        if (anchor == null || face == null) {
            throw new IllegalArgumentException("anchor and face are required");
        }
        anchor = anchor.immutable();
    }
}
