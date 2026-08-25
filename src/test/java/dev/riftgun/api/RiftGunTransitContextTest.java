package dev.riftgun.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

final class RiftGunTransitContextTest {
    private static final PortalTransitAuthorization AUTHORIZATION = new PortalTransitAuthorization(
        RiftResourceId.parse("riftworld:entry"),
        RiftResourceId.parse("riftworld:reality/123e4567-e89b-12d3-a456-426614174000"));

    @Test
    void exposesAuthorizationOnlyInsideTheSynchronousTransitScope() {
        assertTrue(RiftGunTransitContext.currentAuthorization().isEmpty());

        String result = RiftGunApiBootstrap.withTransitAuthorization(
            Optional.of(AUTHORIZATION), () -> {
                assertEquals(Optional.of(AUTHORIZATION),
                    RiftGunTransitContext.currentAuthorization());
                return "moved";
            });

        assertEquals("moved", result);
        assertTrue(RiftGunTransitContext.currentAuthorization().isEmpty());
    }

    @Test
    void clearsAuthorizationWhenTransferThrows() {
        assertThrows(IllegalStateException.class, () ->
            RiftGunApiBootstrap.withTransitAuthorization(Optional.of(AUTHORIZATION), () -> {
                throw new IllegalStateException("transfer failed");
            }));

        assertTrue(RiftGunTransitContext.currentAuthorization().isEmpty());
    }
}
