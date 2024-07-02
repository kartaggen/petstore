package com.chtrembl.petstore.order.api;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.chtrembl.petstore.order.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosmosDbOrderPersister {

    @Value("${petstore.cosmosdb.accountkey:}")
    private String accountKey;

    @Value("${petstore.cosmosdb.accounthost:}")
    private String accountHost;

    @Value("${petstore.cosmosdb.database:}")
    private String databaseName;

    @Value("${petstore.cosmosdb.container:}")
    private String containerName;

    private CosmosClient client;
    private CosmosDatabase database;
    private CosmosContainer container;

    @PostConstruct
    public void init() {
        List<String> preferredRegions = new ArrayList<>();
        preferredRegions.add("Central US");

        //  Create sync client
        client = new CosmosClientBuilder()
                .endpoint(accountHost)
                .key(accountKey)
                .preferredRegions(preferredRegions)
                .userAgentSuffix("CosmosDBOrderPersister")
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient();

        createDatabaseIfNotExists();
        createContainerIfNotExists();
    }

    @PreDestroy
    public void close() {
        client.close();
    }

    public Order loadOrder(String id) {
        try {
            return container.readItem(id, new PartitionKey(id), Order.class).getItem();
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404) {
                return new Order();
            } else {
                throw new IllegalStateException("Exception while loading order: " + id, e);
            }
        }
    }

    public void saveOrder(Order order) {
        container.upsertItem(order, new PartitionKey(order.getId()), new CosmosItemRequestOptions());
    }

    private void createDatabaseIfNotExists() {
        System.out.println("Create database " + databaseName + " if not exists.");

        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
        database = client.getDatabase(databaseResponse.getProperties().getId());

        System.out.println("Initializing database " + database.getId() + " completed!\n");
    }

    private void createContainerIfNotExists() {
        System.out.println("Create container " + containerName + " if not exists.");

        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/id");
        CosmosContainerResponse containerResponse = database.createContainerIfNotExists(containerProperties);
        container = database.getContainer(containerResponse.getProperties().getId());

        System.out.println("Initializing container " + container.getId() + " completed!\n");
    }


}
