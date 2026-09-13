package dev.riftgun.appearance.client;

import com.google.gson.Gson;
import dev.riftgun.appearance.PortalGunSkin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Keeps each world's custom sounds across preset changes and restarts. No Minecraft client dependency. */
public final class SkinSoundMemory {
    private static final Gson JSON = new Gson();

    public record Sounds(String shot, String portal, String transit, boolean splash) {
        public Sounds {
            if (!PortalGunSkin.validId(shot) || !PortalGunSkin.validId(portal) || !PortalGunSkin.validId(transit)) {
                throw new IllegalArgumentException("Invalid saved sound selection");
            }
        }
    }

    public record Entry(String scope, Sounds custom) {
        public Entry {
            if (scope == null || scope.isBlank()) throw new IllegalArgumentException("Missing sound scope");
            Objects.requireNonNull(custom);
        }
    }

    public record Change(List<String> memory, Sounds sounds) {}

    public static Change apply(List<? extends String> memory, String scope, Sounds current,
                               UnaryOperator<Sounds> recommendation) {
        Sounds custom = custom(memory, scope, current);
        Sounds effective = recommendation.apply(custom);
        return new Change(store(memory, new Entry(scope, custom)), effective);
    }

    public static List<String> rememberCustom(List<? extends String> memory, String scope, Sounds custom) {
        return store(memory, new Entry(scope, custom));
    }

    /** Capture the original value before sending any replacement, including if the connection then drops. */
    public static List<String> captureCustom(List<? extends String> memory, String scope, Sounds current) {
        return find(memory, scope) == null ? rememberCustom(memory, scope, current) : List.copyOf(memory);
    }

    public static Sounds custom(List<? extends String> memory, String scope, Sounds current) {
        Entry saved = find(memory, scope);
        // A snapshot is not evidence of a manual edit. Only an explicit accepted custom edit replaces this backup.
        return saved == null ? current : saved.custom();
    }

    private static Entry find(List<? extends String> memory, String scope) {
        for (String text : memory) {
            Entry entry = parse(text);
            if (entry != null && entry.scope().equals(scope)) return entry;
        }
        return null;
    }

    private static List<String> store(List<? extends String> memory, Entry replacement) {
        List<String> result = new ArrayList<>();
        for (String text : memory) {
            Entry entry = parse(text);
            if (entry != null && !entry.scope().equals(replacement.scope())) result.add(text);
        }
        result.add(JSON.toJson(replacement));
        return List.copyOf(result);
    }

    private static Entry parse(String text) {
        try { return JSON.fromJson(text, Entry.class); }
        catch (RuntimeException ignored) { return null; }
    }

    private SkinSoundMemory() {}
}
