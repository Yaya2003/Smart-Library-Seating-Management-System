package com.example.common.gson;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class StringToInteger2DTypeAdaptor implements JsonSerializer<String>, JsonDeserializer<String> {
    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return jsonElement.toString();
    }

    @Override
    public JsonElement serialize(String s, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray jsonArray = new JsonArray();
        Gson gson = new Gson();
        List<List<Integer>> list = gson.fromJson(s, new TypeToken<List<List<Integer>>>() {}.getType());
        for (List<Integer> innerList : list) {
            JsonArray innerJsonArray = new JsonArray();
            for (Integer integer : innerList) {
                innerJsonArray.add(new JsonPrimitive(integer));
            }
            jsonArray.add(innerJsonArray);
        }
        return jsonArray;
    }
}
