package dev.zerosandones.patcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonString;

import jakarta.json.JsonPatch;
import jakarta.json.JsonValue;

public class Patcher {

    private static final Logger logger = LogManager.getLogger(Patcher.class);

    public BsonDocument generateBson(JsonPatch patch) {
        BsonDocument doc = new BsonDocument();
        patch.toJsonArray().stream()
            .map(JsonValue::asJsonObject)
            .forEach(patchObject -> {
                String mongoOp = "";
                String operation = patchObject.getString("op");
                String path = patchObject.getString("path").substring(1).replace('/', '.');
                String value = patchObject.getString("value");
                if ("add".equals(operation)) {
                    mongoOp = "$add";
                }
                doc.computeIfAbsent(mongoOp, key -> new BsonDocument()).asDocument().put(path, new BsonString(value));
            });
        return doc;
    }
    
}