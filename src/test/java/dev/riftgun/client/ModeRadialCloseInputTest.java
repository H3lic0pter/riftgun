package dev.riftgun.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

final class ModeRadialCloseInputTest {
    private MockedStatic<ModeRadialClientAccess> access;
    private int requestId;

    @BeforeEach
    void setup() {
        access = mockStatic(ModeRadialClientAccess.class);
        keys(false, false);
        ModeRadialInput.cancelFromScreen();
        ModeRadialInput.tick();
        access.when(() -> ModeRadialClientAccess.sendOpenRequest(anyInt(), eq(false), eq(true)))
            .thenAnswer(call -> { requestId = call.getArgument(0); return null; });
    }

    @AfterEach
    void cleanup() { access.close(); }

    private void keys(boolean down, boolean clicked) {
        access.when(ModeRadialClientAccess::keys).thenReturn(
            new ModeRadialClientAccess.Keys(false, false, false, false, down, clicked, false));
    }

    private void hold() {
        keys(true, true);
        for (int tick = 0; tick < 6; tick++) ModeRadialInput.tick();
        access.verify(() -> ModeRadialClientAccess.sendOpenRequest(requestId, false, true));
        access.verify(ModeRadialClientAccess::sendCloseAllRequest, never());
    }

    @Test
    void tapClosesAllOnlyOnRelease() {
        keys(true, true);
        ModeRadialInput.tick();
        access.verify(ModeRadialClientAccess::sendCloseAllRequest, never());
        keys(false, false);
        ModeRadialInput.tick();
        ModeRadialInput.tick();
        access.verify(ModeRadialClientAccess::sendCloseAllRequest, times(1));
        access.verify(() -> ModeRadialClientAccess.sendOpenRequest(anyInt(), anyBoolean(), anyBoolean()), never());
    }

    @Test
    void tapBetweenTicksIsNotLost() {
        keys(false, true);
        ModeRadialInput.tick();
        access.verify(ModeRadialClientAccess::sendCloseAllRequest);
    }

    @Test
    void heldKeyOpensCloseWheelAndReleaseCommitsIt() {
        hold();
        ModeRadialInput.openFromServer(requestId);
        access.verify(() -> ModeRadialClientAccess.openOrRefresh(null, true));
        access.when(ModeRadialClientAccess::radialScreenOpen).thenReturn(true);
        keys(false, false);
        ModeRadialInput.tick();
        access.verify(() -> ModeRadialClientAccess.commitAndClose(false));
        access.verify(ModeRadialClientAccess::sendCloseAllRequest, never());
    }

    @Test
    void releasingBeforeAcknowledgementCancelsWithoutClosingAnything() {
        hold();
        keys(false, false);
        ModeRadialInput.tick();
        ModeRadialInput.openFromServer(requestId);
        access.verify(() -> ModeRadialClientAccess.openOrRefresh(any(), anyBoolean()), never());
        access.verify(() -> ModeRadialClientAccess.commitAndClose(anyBoolean()), never());
        access.verify(ModeRadialClientAccess::sendCloseAllRequest, never());
    }

    @Test
    void rejectedHoldAndEscapeNeverFallBackToClosingAll() {
        hold();
        ModeRadialInput.rejectFromServer(requestId);
        ModeRadialInput.cancelFromScreen();
        keys(false, false);
        ModeRadialInput.tick();
        ModeRadialInput.tick();
        access.verify(ModeRadialClientAccess::sendCloseAllRequest, never());
    }
}
