package dev.zerosandones.patcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatch;
import jakarta.json.JsonValue;

class PatcherTest {

    private static MongoClient connection;

    private MongoCollection<BsonDocument> collection;
    private BsonDocument filter;

    private Patcher testInstance;

    @BeforeAll
    public static void testsInit() {
        String uri = "mongodb://localhost:27017/"; //mongodb://localhost:27017/
        
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .serverApi(serverApi)
                .build();
                
        connection = MongoClients.create(settings);
       
    }

    @AfterAll
    public static void cleanUp() {
        connection.getDatabase("tests").drop();
        connection.close();
    }

    @BeforeEach
    public void testInit() {
        collection = connection.getDatabase("tests").getCollection("people", BsonDocument.class);
        InsertOneResult result = collection.insertOne(new BsonDocument());
        filter = new BsonDocument("_id", result.getInsertedId().asObjectId());

        this.testInstance = new Patcher();
    }

    @Test
    void defaultConstructor() {
        assertNotNull(testInstance);
    }

    @Test
    void addStringToRoot_generateBson_willGenerateAddFieldInBson() {

        JsonPatch patch = Json.createPatchBuilder()
            .add("/name", "Dave")
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);
        
        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        assertEquals("Dave", updatedDoc.getString("name").getValue());
        
    }

    @Test
    void addNumberToRoot_generateBson_willGenerateAddFieldInBson() {

        JsonPatch patch = Json.createPatchBuilder()
            .add("/age", 32)
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        assertEquals(32, updatedDoc.getInt32("age").getValue());
        
    }

    @Test
    void addBooleanToRoot_generateBson_willGenerateAddFieldInBson() {

        JsonPatch patch = Json.createPatchBuilder()
            .add("/dead", false)
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        assertEquals(false, updatedDoc.getBoolean("dead").getValue());
        
    }

    @Test
    void addNullToRoot_generateBson_willGenerateAddFieldInBson() {

        JsonPatch patch = Json.createPatchBuilder()
            .add("/dateOfDeath", JsonValue.NULL)
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        assertTrue(updatedDoc.isNull("dateOfDeath"));
        
    }

    @Test
    void addObjectToRoot_generateBson_willGenerateAddFieldInBson() {

        JsonObject address = Json.createObjectBuilder()
            .add("street", "23 Home Place")
            .add("suburb", "Riccarton")
            .add("city", "Christchurch")
            .add("postCode", 3455)
            .build();

        JsonPatch patch = Json.createPatchBuilder()
            .add("/address", address)
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        BsonDocument actualAddress = updatedDoc.getDocument("address");
        assertEquals("23 Home Place", actualAddress.getString("street").getValue());
        assertEquals("Riccarton", actualAddress.getString("suburb").getValue());
        assertEquals("Christchurch", actualAddress.getString("city").getValue());
        assertEquals(3455, actualAddress.getInt32("postCode").getValue());
        
    }

    @Test
    void addArrayToRoot_generateBson_willGenerateAddFieldInBson() {

        JsonArray fears = Json.createArrayBuilder()
            .add("The dark")
            .add("People")
            .build();

        JsonPatch patch = Json.createPatchBuilder()
            .add("/fears", fears)
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        BsonArray actualArray = updatedDoc.getArray("fears");
        assertTrue(actualArray.contains(new BsonString("The dark")));
        assertTrue(actualArray.contains(new BsonString("People")));
        
    }


    @Test
    void addStringWithNestedPath_generateBson_willGenerateAddFieldInBson() {

        JsonPatch patch = Json.createPatchBuilder()
            .add("/name/firstname", "Dave")
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        BsonDocument nameDoc = updatedDoc.getDocument("name");
        assertEquals("Dave", nameDoc.getString("firstname").getValue());
        
    }

    @Test
    void addStringToEndOfArray_generateBson_willGeneratePushDocInBson() {

        JsonPatch patch = Json.createPatchBuilder()
            .add("/names/-", "Dave")
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        BsonArray nameArray= updatedDoc.getArray("names");
        BsonValue value = nameArray.getLast();
        assertEquals("Dave", value.asString().getValue());

    }

    @Test
    void addStringToIndexInArray_generateBson_willGeneratePushDocInBson() {

        BsonArray array = new BsonArray();
        array.add(new BsonString("James"));
        array.add(new BsonString("Harry"));
        array.add(new BsonString("Amos"));
        BsonDocument initDoc = new BsonDocument("names", array);
        collection.replaceOne(filter, initDoc);

        JsonPatch patch = Json.createPatchBuilder()
            .add("/names/1", "Dave")
            .build();

        BsonDocument doc = testInstance.generateBson(patch);
        collection.updateOne(filter, doc);

        FindIterable<BsonDocument> iterator = collection.find(filter);
        BsonDocument updatedDoc = iterator.first();
        BsonArray nameArray= updatedDoc.getArray("names");
        BsonValue value = nameArray.get(1);
        assertEquals("Dave", value.asString().getValue());

    }

}
