package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class LocalizationContractTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("%\\d*\\$?[a-zA-Z]");
    private static final Pattern MESSAGE_KEY = Pattern.compile("message\\.riftgun\\.[a-z0-9_.]+");

    @Test
    void bundledLanguagesExposeTheSameKeysAndPlaceholders() {
        Map<String, String> english = language("en_us");
        Map<String, String> chinese = language("zh_cn");

        assertEquals(english.keySet(), chinese.keySet());
        for (String key : english.keySet()) {
            assertEquals(placeholders(english.get(key)), placeholders(chinese.get(key)),
                () -> "Placeholder mismatch for " + key);
        }
    }

    @Test
    void displayTextUsesAsciiHyphensForSeparators() {
        for (String language : List.of("en_us", "zh_cn")) {
            for (var entry : language(language).entrySet()) {
                assertFalse(entry.getValue().contains("·"),
                    () -> entry.getKey() + " still uses a middle-dot separator");
                assertFalse(entry.getValue().contains("—"),
                    () -> entry.getKey() + " still uses an em-dash separator");
            }
        }
    }

    @Test
    void runtimeMessageKeysHaveBundledTranslations() throws Exception {
        Set<String> translated = language("en_us").keySet();
        Set<String> referenced = new TreeSet<>();
        for (Path root : List.of(Path.of("src/main/java"), Path.of("versions"))) {
            if (!Files.isDirectory(root)) continue;
            try (var paths = Files.walk(root)) {
                for (Path source : paths
                    .filter(LocalizationContractTest::isMainJavaSource)
                    .toList()) {
                    var matcher = MESSAGE_KEY.matcher(Files.readString(source));
                    while (matcher.find()) referenced.add(matcher.group());
                }
            }
        }

        Set<String> missing = referenced.stream()
            .filter(key -> !translated.contains(key))
            .collect(Collectors.toCollection(TreeSet::new));
        assertTrue(missing.isEmpty(), () -> "Missing runtime translations: " + missing);
    }

    private static Map<String, String> language(String name) {
        String path = "/assets/riftgun/lang/" + name + ".json";
        try (var stream = LocalizationContractTest.class.getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("Missing resource " + path);
            JsonObject json = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return json.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> entry.getValue().getAsString()));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static List<String> placeholders(String value) {
        var result = new ArrayList<String>();
        var matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) result.add(matcher.group());
        result.sort(String::compareTo);
        return result;
    }

    private static boolean isMainJavaSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.endsWith(".java")
            && (!normalized.startsWith("versions/")
                || normalized.contains("/src/main/java/"));
    }
}
