package dev.riftgun.internal.shader;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Internal registry mapping known shader-pack families to rendering capabilities. */
public final class ShaderPackProfileRegistry {
    private static final ShaderPackProfile COMPLEMENTARY_R5 = new ShaderPackProfile(
        ShaderPackProfile.EndframeCenter.irisBlockEntity(5025));
    private static final List<Registration> REGISTRATIONS = List.of(
        prefix("complementaryreimagined_r5.", COMPLEMENTARY_R5),
        prefix("complementaryunbound_r5.", COMPLEMENTARY_R5)
    );

    private ShaderPackProfileRegistry() {
    }

    public static ShaderPackProfile resolve(String packName) {
        String normalized = normalize(packName);
        for (Registration registration : REGISTRATIONS) {
            if (registration.matcher().test(normalized)) return registration.profile();
        }
        return ShaderPackProfile.EMPTY;
    }

    private static Registration prefix(String prefix, ShaderPackProfile profile) {
        return new Registration(name -> name.startsWith(prefix), profile);
    }

    private static String normalize(String packName) {
        if (packName == null) return "";
        String normalized = packName.strip().replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        if (separator >= 0) normalized = normalized.substring(separator + 1);
        return normalized.toLowerCase(Locale.ROOT);
    }

    private record Registration(Predicate<String> matcher, ShaderPackProfile profile) {
    }
}
