package dev.riftgun.relocation;

import dev.riftgun.portal.PortalChunkGuard;
import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.sound.PortalSoundRegistry;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.sound.PortalSounds;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class EntityRelocationExitServiceTest {
    @BeforeAll
    static void bootstrap() throws ClassNotFoundException {
        try (var flags = mockStatic(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class)
            //? if >=1.21.11 {
            /*; var environment = mockStatic(net.neoforged.fml.loading.FMLEnvironment.class)
            *///?}
        ) {
            //? if >=1.21.11 {
            /*environment.when(net.neoforged.fml.loading.FMLEnvironment::isProduction).thenReturn(true);
            *///?}
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            //? if >=1.21.11 {
            /*// Unit tests do not run the mod registry events for ticket types and serializers.
            try (var registrations = mockConstruction(net.neoforged.neoforge.registries.DeferredHolder.class,
                    (holder, context) -> {
                        var key = (net.minecraft.resources.ResourceKey<?>) context.arguments().getFirst();
                        Object value = key.identifier().getPath().equals("optional_uuid")
                            ? mock(net.minecraft.network.syncher.EntityDataSerializer.class)
                            : mock(net.minecraft.server.level.TicketType.class);
                        when(holder.get()).thenReturn(value);
                    })) {
                Class.forName(EntityRelocationPortalEntity.class.getName());
            }
            *///?}
        }
    }

    @Test
    void shotOnlyDifferenceReusesExistingExitWithoutSpawningAnother() {
        var sounds = PortalSoundSnapshot.defaults();
        assertReuse(new PortalSoundSnapshot(PortalSoundRegistry.APERTURE_ISH_ID,
            sounds.portal(), sounds.transit(), sounds.splashEnabled()), "riftgun:swirl", true);
    }

    @Test
    void differencesThatAffectTheExitStillRequireSeparateExits() {
        for (String difference : new String[] {"visual", "portal", "transit", "splash"}) {
            var original = PortalSoundSnapshot.defaults();
            var sounds = new PortalSoundSnapshot(original.shot(),
                difference.equals("portal") ? PortalSoundRegistry.NONE_ID : original.portal(),
                difference.equals("transit") ? PortalSoundRegistry.ENDER_ID : original.transit(),
                difference.equals("splash"));
            assertReuse(sounds, difference.equals("visual") ? "riftgun:endframe" : "riftgun:swirl", false);
        }
    }

    private static void assertReuse(PortalSoundSnapshot requestedSounds, String requestedVisual, boolean reuse) {
        EntityRelocationExitService.clear();
        var server = mock(MinecraftServer.class);
        var level = mock(ServerLevel.class);
        var portal = mock(EntityRelocationPortalEntity.class);
        UUID portalId = UUID.randomUUID();
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        when(level.getEntity(portalId)).thenReturn(portal);
        when(level.addFreshEntity(portal)).thenReturn(true);
        when(portal.getUUID()).thenReturn(portalId);
        when(portal.position()).thenReturn(Vec3.ZERO);
        when(portal.isExit()).thenReturn(true);
        when(portal.phase()).thenReturn(PortalLifecycle.Phase.OPEN);
        when(portal.remainingOpenTicks()).thenReturn(200);
        when(portal.visualType()).thenReturn("riftgun:swirl");
        when(portal.soundSnapshot()).thenReturn(PortalSoundSnapshot.defaults());
        when(portal.tryReserve(anyFloat())).thenReturn(true);
        var key = new EntityRelocationExitIndex.DestinationKey(UUID.randomUUID(),
            //? if >=1.21.11 {
            /*Level.OVERWORLD.identifier(),
            *///?} else {
            Level.OVERWORLD.location(),
            //?}
            0, 64, 0);
        try (var entities = mockStatic(EntityRelocationPortalEntity.class);
             var bounds = mockStatic(PortalChunkGuard.class);
             var sounds = mockStatic(PortalSounds.class)) {
            bounds.when(() -> PortalChunkGuard.inWorldBounds(eq(level), any(net.minecraft.core.BlockPos.class))).thenReturn(true);
            entities.when(() -> EntityRelocationPortalEntity.createExit(eq(level), any(),
                anyFloat(), anyInt(), anyInt(), any(), any(PortalOrientation.class), anyFloat(), anyInt()))
                .thenReturn(portal);
            var first = EntityRelocationExitService.open(server, request(level, key,
                PortalSoundSnapshot.defaults(), "riftgun:swirl"));
            assertNotNull(first);
            assertNull(first.sharedExit());
            var second = EntityRelocationExitService.open(server, request(level, key, requestedSounds, requestedVisual));
            assertNotNull(second);
            assertEquals(reuse, second.sharedExit() != null);
            verify(level, times(reuse ? 1 : 2)).addFreshEntity(portal);
            verify(portal, times(reuse ? 1 : 0)).tryReserve(anyFloat());
        } finally {
            EntityRelocationExitService.clear();
        }
    }

    private static EntityRelocationExitService.OpenRequest request(ServerLevel level,
            EntityRelocationExitIndex.DestinationKey key, PortalSoundSnapshot sounds, String visual) {
        return new EntityRelocationExitService.OpenRequest(level, null, Vec3.ZERO, 2, 0xFFFFFF,
            200, sounds, PortalOrientation.TOP, 0, 10, key, visual);
    }
}
