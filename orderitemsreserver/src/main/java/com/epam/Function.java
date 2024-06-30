package com.epam;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

public class Function {

    @FunctionName("reserve-order")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BlobOutput(name = "outputBlob", connection = "MyStorageConnectionAppSetting", path = "ps-storage/orders.json") OutputBinding<String> outputBlob,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        JsonObject ordersJson = JsonParser.parseString(outputBlob.getValue()).getAsJsonObject();

        context.getLogger().info("ordersJson before: " + ordersJson);

        Optional<String> body = request.getBody();
        if (body.isEmpty() || body.get().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Missing body!").build();
        }

        JsonObject order = JsonParser.parseString(body.get()).getAsJsonObject();
        context.getLogger().info("order: " + order);

        ordersJson.add(order.get("id").getAsString(), order);


        outputBlob.setValue(ordersJson.getAsString());
        context.getLogger().info("ordersJson after: " + ordersJson);

        return request.createResponseBuilder(HttpStatus.OK).build();

    }

}
