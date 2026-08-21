package dev.riftgun.core.msg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MsgDispatchTest {
    @Test
    void actionBarMessagesUseOnlyTheOverlayTarget() {
        List<String> overlay = new ArrayList<>();
        List<String> system = new ArrayList<>();

        Msg.dispatch("denied", true, overlay::add, system::add);

        assertEquals(List.of("denied"), overlay);
        assertEquals(List.of(), system);
    }

    @Test
    void chatMessagesUseOnlyTheSystemTarget() {
        List<String> overlay = new ArrayList<>();
        List<String> system = new ArrayList<>();

        Msg.dispatch("notice", false, overlay::add, system::add);

        assertEquals(List.of(), overlay);
        assertEquals(List.of("notice"), system);
    }
}
