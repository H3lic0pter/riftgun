package dev.riftgun.ui;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Owns modal navigation, unsaved-form confirmation, and target lifetime. */
public final class PortalConfigSession {
    private PortalConfigPage page = PortalConfigPage.NONE;
    private PortalConfigPage returnPage = PortalConfigPage.NONE;
    private @Nullable UUID target;
    private boolean dirty;

    public PortalConfigPage page() {
        return page;
    }

    public PortalConfigPage returnPage() {
        return returnPage;
    }

    public @Nullable UUID target() {
        return target;
    }

    public boolean dirty() {
        return dirty;
    }

    public void navigate(PortalConfigPage next) {
        page = next;
    }

    public void open(PortalConfigPage next, @Nullable UUID nextTarget) {
        if (next == PortalConfigPage.NONE) throw new IllegalArgumentException("page required");
        page = next;
        target = nextTarget;
        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    /** Returns true when caller must rebuild the newly opened confirmation page. */
    public boolean requestClose() {
        if (page == PortalConfigPage.CONFIRM_DIRTY) {
            close();
            return false;
        }
        if (page.hasInputs() && dirty) {
            returnPage = page;
            page = PortalConfigPage.CONFIRM_DIRTY;
            return true;
        }
        close();
        return false;
    }

    /** Restores the dirty form; non-dirty confirmations close. */
    public boolean cancelConfirmation() {
        if (page == PortalConfigPage.CONFIRM_DIRTY) {
            page = returnPage;
            returnPage = PortalConfigPage.NONE;
            return true;
        }
        close();
        return false;
    }

    public void close() {
        page = PortalConfigPage.NONE;
        returnPage = PortalConfigPage.NONE;
        target = null;
        dirty = false;
    }
}
