package com.epam;

import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Function {

    @FunctionName("reserve-order")
    public void run(
            @ServiceBusQueueTrigger(name = "message", queueName = "orderqueue", connection = "MyServiceBusConnectionAppSetting") String order,
            final ExecutionContext context) throws IOException {

        ExponentialBackoffOptions exponentialBackoffOptions = new ExponentialBackoffOptions();
        exponentialBackoffOptions.setMaxRetries(3);
        exponentialBackoffOptions.setBaseDelay(Duration.ofSeconds(3));
        exponentialBackoffOptions.setMaxDelay(Duration.ofSeconds(30));

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(System.getenv("MyStorageConnectionAppSetting"))
                .retryOptions(new RetryOptions(exponentialBackoffOptions))
                .buildClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("ps-storage");

        BlobClient inputBlobClient = containerClient.getBlobClient("orders.json");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputBlobClient.downloadStream(outputStream);
        String inputBlob = outputStream.toString(StandardCharsets.UTF_8);
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

        BlobClient outputBlobClient = containerClient.getBlobClient("orders.json");
        outputStream = new ByteArrayOutputStream();
        outputStream.write(ordersJson.toString().getBytes(StandardCharsets.UTF_8));
        outputBlobClient.upload(BinaryData.fromBytes(outputStream.toByteArray()), true);
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
