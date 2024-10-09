package dev.zerosandones.patcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonValue;

class PatcherTest {

    private Patcher testInstance;

    @BeforeEach
    public void testInit() {
        this.testInstance = new Patcher();
    }

    @Test
    void defaultConstructor() {
        assertNotNull(testInstance);
    }

    @Test
    void addOperationWithRootPath_generateBson_willGenerateAddFieldInBson() {
        JsonPatch patch = Json.createPatchBuilder()
            .add("/name", "Dave")
            .build();

        BsonDocument doc = testInstance.generateBson(patch);

        assertTrue(doc.containsKey("$add"));
        BsonDocument addDoc = doc.getDocument("$add");
        BsonString value = addDoc.getString("name");
        assertEquals("Dave", value.getValue());
        
    }

    @Test
    void addOperationWithNestedPath_generateBson_willGenerateAddFieldInBson() {
        JsonPatch patch = Json.createPatchBuilder()
            .add("/name/firstname", "Dave")
            .build();

        BsonDocument doc = testInstance.generateBson(patch);

        assertTrue(doc.containsKey("$add"));
        BsonDocument addDoc = doc.getDocument("$add");
        BsonString value = addDoc.getString("name.firstname");
        assertEquals("Dave", value.getValue());
        
    }

}
