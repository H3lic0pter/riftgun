package dev.riftgun.appearance.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalGunAppearanceSelectionTest {
    private static final String DEFAULT = "riftgun:default";
    private static final String FIRST = "example:first";
    private static final String SECOND = "example:second";

    @Test
    void loadingAndUnchangedSelectionsCannotBeSubmitted() {
        PortalGunAppearanceSelection selection = new PortalGunAppearanceSelection();
        assertFalse(selection.ready());
        assertFalse(selection.canApply());
        assertThrows(IllegalStateException.class, selection::submit);
        selection.initialize(DEFAULT);
        assertTrue(selection.ready());
        assertEquals(DEFAULT, selection.current());
        assertEquals(DEFAULT, selection.selected());
        assertFalse(selection.canApply());
        assertThrows(IllegalStateException.class, selection::submit);
    }

    @Test
    void previewLeavesCurrentSkinUnchangedAndCanBeCancelledByReselectingIt() {
        PortalGunAppearanceSelection selection = initialized();
        selection.select(FIRST);
        assertEquals(FIRST, selection.selected());
        assertEquals(DEFAULT, selection.current());
        assertTrue(selection.canApply());
        assertFalse(selection.pending());
        selection.select(DEFAULT);
        assertFalse(selection.canApply());
        assertEquals(DEFAULT, selection.current());
    }

    @Test
    void submissionWaitsForAcknowledgementAndPreventsDuplicateRequests() {
        PortalGunAppearanceSelection selection = initialized();
        selection.select(FIRST);
        assertEquals(FIRST, selection.submit());
        assertTrue(selection.pending());
        assertEquals(DEFAULT, selection.current());
        assertFalse(selection.canApply());
        assertThrows(IllegalStateException.class, selection::submit);
        selection.acknowledge(FIRST);
        assertFalse(selection.pending());
        assertEquals(FIRST, selection.current());
        assertEquals(FIRST, selection.selected());
        assertFalse(selection.canApply());
    }

    @Test
    void rejectedRequestRetainsThePreviewAndAllowsRetry() {
        PortalGunAppearanceSelection selection = initialized();
        selection.select(FIRST);
        selection.submit();
        selection.reject();
        assertEquals(DEFAULT, selection.current());
        assertEquals(FIRST, selection.selected());
        assertFalse(selection.pending());
        assertTrue(selection.canApply());
        assertEquals(FIRST, selection.submit());
    }

    @Test
    void acknowledgementOfEarlierSubmissionDoesNotReplaceANewerPreview() {
        PortalGunAppearanceSelection selection = initialized();
        selection.select(FIRST);
        selection.submit();
        selection.select(SECOND);
        assertEquals(SECOND, selection.selected());
        assertFalse(selection.canApply());
        selection.acknowledge(FIRST);
        assertEquals(FIRST, selection.current());
        assertEquals(SECOND, selection.selected());
        assertTrue(selection.canApply());
        assertEquals(SECOND, selection.submit());
    }

    @Test
    void reinitializingForAnotherGunDiscardsPreviousPreviewAndPendingRequest() {
        PortalGunAppearanceSelection selection = initialized();
        selection.select(FIRST);
        selection.submit();
        selection.initialize(SECOND);
        assertEquals(SECOND, selection.current());
        assertEquals(SECOND, selection.selected());
        assertFalse(selection.pending());
        assertFalse(selection.canApply());
    }

    private static PortalGunAppearanceSelection initialized() {
        PortalGunAppearanceSelection selection = new PortalGunAppearanceSelection();
        selection.initialize(DEFAULT);
        return selection;
    }
}
