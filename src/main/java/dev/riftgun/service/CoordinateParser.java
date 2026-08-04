package dev.riftgun.service;

public final class CoordinateParser {
    public static double parse(String input, double relativeBase) {
        String value = input == null ? "" : input.strip();
        double result;
        if (value.startsWith("~")) {
            String offset = value.substring(1).strip();
            result = relativeBase + (offset.isEmpty() ? 0.0 : Double.parseDouble(offset));
        } else {
            result = Double.parseDouble(value);
        }
        if (!Double.isFinite(result)) throw new NumberFormatException("Coordinate must be finite");
        return result;
    }

    public static float parseYaw(String input, float relativeBase) {
        String value = input == null ? "" : input.strip();
        if (value.isEmpty()) return relativeBase;
        double result = parse(value, relativeBase);
        return (float) result;
    }

    private CoordinateParser() {}
}

