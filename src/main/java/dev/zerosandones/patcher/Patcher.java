package dev.zerosandones.patcher;

import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;

import jakarta.json.JsonNumber;
import jakarta.json.JsonPatch;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.List;

public class Patcher {

    public BsonDocument generateBson(JsonPatch patch) {
        BsonDocument doc = new BsonDocument();
        patch.toJsonArray().stream()
            .map(JsonValue::asJsonObject)
            .forEach(patchObject -> {
                String mongoOp = "";
                
                String operation = patchObject.getString("op");
                PointerPath path = new PointerPath(patchObject.getString("path"));

                BsonValue value = generateValue(patchObject.get("value"));

                if ("add".equals(operation)) {
                    if (path.doesTargetArray()) {
                        mongoOp = "$push";
                        BsonArray array = new BsonArray();
                        array.add(value);
                        BsonDocument valueDoc = new BsonDocument("$each", array);
                        if (path.getIndex() != null) {
                            valueDoc.put("$position", new BsonInt32(path.getIndex()));
                        }
                        value = valueDoc;
                    } else {
                        mongoOp = "$set";
                    }
                }
                doc.computeIfAbsent(mongoOp, key -> new BsonDocument()).asDocument().put(path.getPath(), value);
            });
        return doc;
    }

    private BsonValue generateValue(JsonValue value) {

        BsonValue generatedValue = null;
        if (value.getValueType() == ValueType.STRING) {
            generatedValue = new BsonString(((JsonString)value).getString());
        } else if (value.getValueType() == ValueType.NUMBER) { //TODO: deal with different number types
            generatedValue = new BsonInt32(((JsonNumber)value).intValueExact());
        } else if (value.getValueType() == ValueType.OBJECT) {
            generatedValue = BsonDocument.parse(value.asJsonObject().toString());
        } else if (value.getValueType() == ValueType.FALSE ) {
            generatedValue = new BsonBoolean(false);
        } else if (value.getValueType() == ValueType.TRUE) {
            generatedValue = new BsonBoolean(true);
        } else if (value.getValueType() == ValueType.NULL) {
            generatedValue = new BsonNull();
        } else if (value.getValueType() == ValueType.ARRAY) {
            List<BsonValue> arrayData = value.asJsonArray().stream()
                .map(this::generateValue)
                .collect(Collectors.toList());
                generatedValue = new BsonArray(arrayData);
        }

        return generatedValue;

    }

    private class PointerPath {

        private Pattern pattern = Pattern.compile("\\d"); //checks for a number, which is used to show array index

        private String path;
        private boolean targetsArray = false;
        private Integer index;

        protected PointerPath(String path) {
            
            this.path = path.substring(1).replace('/', '.');
            int lastIndex = this.path.lastIndexOf(".");
            if (lastIndex >= 0) {
                String lastPathSection = this.path.substring(lastIndex + 1);
                if ("-".equals(lastPathSection)) {
                    this.targetsArray = true;
                    this.path = this.path.substring(0, lastIndex);
                } else if (pattern.matcher(lastPathSection).matches()) {
                    this.targetsArray = true;
                    this.index = Integer.parseInt(lastPathSection);
                    this.path = this.path.substring(0, lastIndex);
                }
            }
            
        }

        public boolean doesTargetArray() {
            return targetsArray;
        }

        public Integer getIndex() {
            return this.index;
        }

        public String getPath() {
            return this.path;
        }

    }
    
}