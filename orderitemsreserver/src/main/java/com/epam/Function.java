package com.epam;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.Optional;

public class Function {

    @FunctionName("reserve-order")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BlobInput(name = "inputBlob", connection = "MyStorageConnectionAppSetting", path = "ps-storage/orders.json") String inputBlob,
            @BlobOutput(name = "outputBlob", connection = "MyStorageConnectionAppSetting", path = "ps-storage/orders.json") OutputBinding<String> outputBlob,
            final ExecutionContext context) {

        JsonArray ordersJson = JsonParser.parseString(inputBlob).getAsJsonArray();

        Optional<String> body = request.getBody();
        if (body.isEmpty() || body.get().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Missing body!").build();
        }

        JsonObject newOrder;
        try {
            newOrder = JsonParser.parseString(body.get()).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            context.getLogger().severe("Body is not a valid JSON: " + body.get());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Invalid JSON body!").build();
        }

        String id = newOrder.get("id").getAsString();
        if (id == null || id.isEmpty()) {
            context.getLogger().severe("Invalid id: " + id + " in body: " + body.get());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Invalid ID body!").build();
        }

        updateOrder(ordersJson, newOrder);
        outputBlob.setValue(ordersJson.toString());

        return request.createResponseBuilder(HttpStatus.OK).build();
    }

    public void updateOrder(JsonArray allOrders, JsonObject newOrder) {
        String id = newOrder.get("id").getAsString();
        for (int i = 0; i < allOrders.size(); i++) {
            JsonObject currentOrder = allOrders.get(i).getAsJsonObject();
            if (id.equals(currentOrder.get("id").getAsString())) {
                allOrders.set(i, newOrder);
                return;
            }
        }

        allOrders.add(newOrder);
    }

}
