package dev.riftgun.appearance.client;

import dev.riftgun.client.appearance.SkinRecommendations;
import dev.riftgun.client.PortalClientState;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Preference changes must follow a matching successful apply, even after the screen closes. */
final class SkinRecommendationAcknowledgementTest {
    @AfterEach
    void clearPending() { SkinRecommendations.logout(null); }

    @Test
    void successfulApplyRunsOnceWithoutAScreen() {
        try (var recommendations = protocol()) {
            SkinRecommendations.onRequest(request("one", "SET_APPEARANCE"));
            CompoundTag response = response("one", true, "");
            SkinRecommendations.receive(response);
            SkinRecommendations.receive(response);
            recommendations.verify(() -> SkinRecommendations.apply(eq("riftgun:aperture_ish"), any()), times(1));
        }
    }

    @Test
    void openingPreviewFailuresAndUnrelatedResponsesDoNotApplyPreferences() {
        try (var recommendations = protocol()) {
            SkinRecommendations.onRequest(request("open", "OPEN_APPEARANCE"));
            SkinRecommendations.receive(response("open", false, ""));
            SkinRecommendations.onRequest(request("failure", "SET_APPEARANCE"));
            SkinRecommendations.receive(response("failure", true, "invalid_gun"));
            SkinRecommendations.onRequest(request("pending", "SET_APPEARANCE"));
            SkinRecommendations.receive(response("unrelated", true, ""));
            recommendations.verify(() -> SkinRecommendations.apply(anyString(), any()), never());
            SkinRecommendations.receive(response("pending", true, ""));
            recommendations.verify(() -> SkinRecommendations.apply(anyString(), any()), times(1));
        }
    }

    @Test
    void explicitReferenceSurvivesAChangeOfScreenContext() {
        CompoundTag reference = new CompoundTag();
        reference.putString("Identity", "original-gun");
        CompoundTag request = new CompoundTag();
        request.put("GunReference", reference.copy());
        PortalClientState.writeGunReference(request);
        assertEquals(reference, dev.riftgun.core.nbt.Nbt.getCompound(request, "GunReference"));
    }

    @Test
    void disconnectDiscardsPendingApplies() {
        try (var recommendations = protocol()) {
            SkinRecommendations.onRequest(request("old", "SET_APPEARANCE"));
            SkinRecommendations.logout(null);
            SkinRecommendations.receive(response("old", true, ""));
            recommendations.verify(() -> SkinRecommendations.apply(anyString(), any()), never());
        }
    }

    private static org.mockito.MockedStatic<SkinRecommendations> protocol() {
        var protocol = mockStatic(SkinRecommendations.class);
        protocol.when(() -> SkinRecommendations.onRequest(any())).thenCallRealMethod();
        protocol.when(() -> SkinRecommendations.receive(any())).thenCallRealMethod();
        protocol.when(() -> SkinRecommendations.logout(any())).thenCallRealMethod();
        return protocol;
    }
    private static CompoundTag request(String id, String action) {
        CompoundTag request = new CompoundTag();
        request.putString("RequestId", id);
        request.putString("Action", action);
        request.putString("Skin", "riftgun:aperture_ish");
        return request;
    }

    private static CompoundTag response(String id, boolean applied, String error) {
        CompoundTag response = request(id, "");
        response.putBoolean("Applied", applied);
        response.putString("Error", error);
        return response;
    }
}
