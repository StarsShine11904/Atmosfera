package dev.hephaestus.atmosfera.client.sound.modifiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CommonAttributes {
    public record Bound(float min, float max) {
        public float apply(float x) {
            if (x < min) return 0;
            if (x > max) return 0;
            return x;
        }
    }

    public static Bound getBound(JsonObject object) {
        float min = object.has("min") ? object.get("min").getAsFloat() : -Float.MAX_VALUE;
        float max = object.has("max") ? object.get("max").getAsFloat() : Float.MAX_VALUE;
        return new Bound(min, max);
    }

    public record Range(float lower, float upper) {
        public float apply(float x) {
            if (x <= lower) return 0;
            if (x >= upper) return 1;
            return (x - lower) / (upper - lower);
        }
    }

    public static Range getRange(JsonObject object) {
        if (object.has("range")) {
            JsonArray array = object.getAsJsonArray("range");
            return new Range(array.get(0).getAsFloat(), array.get(1).getAsFloat());
        }

        return new Range(0, 1);
    }
}
