package com.epam;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

public class Function {

    @FunctionName("reserve-order")
    public void run(
            @ServiceBusQueueTrigger(name = "message", queueName = "orderqueue", connection = "your-connection-name") String order,
            @BlobInput(name = "inputBlob", connection = "MyStorageConnectionAppSetting", path = "ps-storage/orders.json") String inputBlob,
            @BlobOutput(name = "outputBlob", connection = "MyStorageConnectionAppSetting", path = "ps-storage/orders.json") OutputBinding<String> outputBlob,
            final ExecutionContext context) {

        JsonArray ordersJson = JsonParser.parseString(inputBlob).getAsJsonArray();

        if (order == null || order.isEmpty()) {
            throw new IllegalArgumentException("Missing order!");
        }

        JsonObject newOrder;
        try {
            newOrder = JsonParser.parseString(order).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            context.getLogger().severe("Body is not a valid JSON: " + order);
            throw new IllegalArgumentException("Invalid JSON order!");
        }

        String id = newOrder.get("id").getAsString();
        if (id == null || id.isEmpty()) {
            context.getLogger().severe("Invalid id: " + id + " in body: " + order);
            throw new IllegalArgumentException("Invalid ID in order!");
        }

        updateOrder(ordersJson, newOrder);
        outputBlob.setValue(ordersJson.toString());
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
