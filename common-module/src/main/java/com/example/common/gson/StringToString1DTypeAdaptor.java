package com.example.common.gson;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class StringToString1DTypeAdaptor implements JsonSerializer<String>, JsonDeserializer<String> {
    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return jsonElement.toString();
    }

    @Override
    public JsonElement serialize(String s, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray jsonArray = new JsonArray();
        Gson gson = new Gson();
        List<String> list = gson.fromJson(s, new TypeToken<List<String>>() {}.getType());
        for (String string : list) {
            jsonArray.add(new JsonPrimitive(string));
        }
        return jsonArray;
    }
}
