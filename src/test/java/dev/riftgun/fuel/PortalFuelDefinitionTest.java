package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.junit.jupiter.api.Test;

final class PortalFuelDefinitionTest {
    @Test
    void parsesAFluidDefinition() {
        //? if >=1.21.11 {
        /*Identifier id = Identifier.fromNamespaceAndPath("example", "bright_fuel");
        *///?} else {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("example", "bright_fuel");
        //?}
        PortalFuelDefinition definition = PortalFuelDefinition.parse(id,
            JsonParser.parseString("""
                {"fluid":"example:bright_fluid","color":"#12ABEF",
                 "cross_dimension":true,"minimum_consumption":3,"maximum_consumption":7}
                """).getAsJsonObject());

        assertEquals(id, definition.id());
        //? if >=1.21.11 {
        /*assertEquals(Identifier.parse("example:bright_fluid"), definition.fluid());
        *///?} else {
        assertEquals(ResourceLocation.parse("example:bright_fluid"), definition.fluid());
        //?}
        assertEquals(0x12ABEF, definition.rgb());
        assertEquals(3, definition.minimumConsumption());
        assertEquals(7, definition.maximumConsumption());
    }

    @Test
    void requiresExactlyOneSelector() {
        //? if >=1.21.11 {
        /*Identifier id = Identifier.fromNamespaceAndPath("example", "invalid");
        *///?} else {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("example", "invalid");
        //?}
        assertThrows(JsonParseException.class, () -> PortalFuelDefinition.parse(id,
            JsonParser.parseString("""
                {"fluid":"example:a","tag":"example:b","color":"FFFFFF",
                 "minimum_consumption":1,"maximum_consumption":1}
                """).getAsJsonObject()));
    }
}
