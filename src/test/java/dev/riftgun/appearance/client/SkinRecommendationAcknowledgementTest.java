package dev.riftgun.appearance.client;

import dev.riftgun.appearance.GunPresentation;
import dev.riftgun.client.appearance.SkinRecommendations;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.network.PortalNetworking;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class SkinRecommendationAcknowledgementTest {
    @Test
    void staleRepliesAndSnapshotsCannotUndoTheLatestGunEdit() {
        String gun = UUID.randomUUID().toString();
        SkinRecommendations.receive(open(gun, GunPresentation.DEFAULT));
        List<CompoundTag> requests = new ArrayList<>();
        try (var network = mockStatic(PortalNetworking.class)) {
            network.when(() -> PortalNetworking.sendRequest(any(), any())).thenAnswer(call -> {
                CompoundTag request = new CompoundTag();
                call.<Consumer<CompoundTag>>getArgument(1).accept(request);
                requests.add(request);
                return null;
            });
            SkinRecommendations.selectAnimation(GunShotAnimation.SWING);
            SkinRecommendations.selectAnimation(GunShotAnimation.LOWER);
            String first = Nbt.getString(requests.get(0), "RequestId");
            String second = Nbt.getString(requests.get(1), "RequestId");
            SkinRecommendations.receive(response(gun, first, "GunPresentation",
                GunPresentation.DEFAULT.withAnimation(GunShotAnimation.SWING)));
            SkinRecommendations.receive(response(gun, "", "Snapshot", GunPresentation.DEFAULT));
            assertEquals(GunShotAnimation.LOWER, SkinRecommendations.current().animation());
            SkinRecommendations.receive(response(gun, second, "GunPresentation",
                GunPresentation.DEFAULT.withAnimation(GunShotAnimation.LOWER)));
            SkinRecommendations.receive(response(gun, first, "GunPresentation", GunPresentation.DEFAULT));
            assertEquals(GunShotAnimation.LOWER, SkinRecommendations.current().animation());
        }
    }

    @Test
    void openingAnotherGunDiscardsOldPendingEditAndLateAcknowledgement() {
        String firstGun = UUID.randomUUID().toString();
        String secondGun = UUID.randomUUID().toString();
        SkinRecommendations.receive(open(firstGun, GunPresentation.DEFAULT));
        CompoundTag request = new CompoundTag();
        try (var network = mockStatic(PortalNetworking.class)) {
            network.when(() -> PortalNetworking.sendRequest(any(), any())).thenAnswer(call -> {
                call.<Consumer<CompoundTag>>getArgument(1).accept(request);
                return null;
            });
            SkinRecommendations.selectAnimation(GunShotAnimation.SWING);
            var secondValue = GunPresentation.DEFAULT.withVisual("riftgun:endframe");
            SkinRecommendations.receive(open(secondGun, secondValue));
            SkinRecommendations.receive(response(firstGun, Nbt.getString(request, "RequestId"),
                "GunPresentation", GunPresentation.DEFAULT));
            assertEquals(secondValue, SkinRecommendations.current());
        }
    }

    @Test
    void unrelatedBackgroundSnapshotDoesNotDiscardPendingEditOrItsAcknowledgement() {
        String editedGun = UUID.randomUUID().toString();
        String backgroundGun = UUID.randomUUID().toString();
        SkinRecommendations.receive(open(editedGun, GunPresentation.DEFAULT));
        CompoundTag request = new CompoundTag();
        try (var network = mockStatic(PortalNetworking.class)) {
            network.when(() -> PortalNetworking.sendRequest(any(), any())).thenAnswer(call -> {
                call.<Consumer<CompoundTag>>getArgument(1).accept(request);
                return null;
            });
            SkinRecommendations.selectAnimation(GunShotAnimation.SWING);
            SkinRecommendations.receive(response(backgroundGun, "", "Snapshot", GunPresentation.DEFAULT));
            assertEquals(GunShotAnimation.SWING, SkinRecommendations.current().animation());
            // An authoritative rollback must still match the pending request after the unrelated snapshot.
            SkinRecommendations.receive(response(editedGun, Nbt.getString(request, "RequestId"),
                "GunPresentation", GunPresentation.DEFAULT));
            assertEquals(GunPresentation.DEFAULT, SkinRecommendations.current());
        }
    }

    private static CompoundTag open(String gun, GunPresentation value) {
        CompoundTag response = response(gun, "", "Snapshot", value);
        response.putBoolean("OpenScreen", true);
        return response;
    }

    private static CompoundTag response(String gun, String id, String kind, GunPresentation value) {
        CompoundTag reference = new CompoundTag();
        reference.putString("Identity", gun);
        CompoundTag response = new CompoundTag();
        response.putString("Kind", kind);
        response.putString("RequestId", id);
        response.put("GunReference", reference);
        response.put("Presentation", value.save());
        return response;
    }
}
