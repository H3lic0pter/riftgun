package dev.riftgun.network;

final class PortalRequestException extends RuntimeException {
    private final String translationKey;

    PortalRequestException(String translationKey) {
        this.translationKey = translationKey;
    }

    String translationKey() {
        return translationKey;
    }
}
