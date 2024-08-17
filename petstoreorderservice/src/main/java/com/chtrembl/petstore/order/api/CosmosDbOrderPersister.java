package com.chtrembl.petstore.order.api;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.chtrembl.petstore.order.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Component
public class CosmosDbOrderPersister {

    static final Logger log = LoggerFactory.getLogger(CosmosDbOrderPersister.class);

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
        client = new CosmosClientBuilder()
                .endpoint(accountHost)
                .key(accountKey)
                .preferredRegions(List.of("Central US"))
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
                return new Order().id(id);
            } else {
                throw new IllegalStateException("Exception while loading order: " + id, e);
            }
        }
    }

    public void saveOrder(Order order) {
        container.upsertItem(order, new PartitionKey(order.getId()), new CosmosItemRequestOptions());
    }

    private void createDatabaseIfNotExists() {
        log.info("Create database {} if not exists.", databaseName);

        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
        database = client.getDatabase(databaseResponse.getProperties().getId());

        log.info("Initializing database {} completed!", database.getId());
    }

    private void createContainerIfNotExists() {
        log.info("Create container {} if not exists.", containerName);

        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/id");
        CosmosContainerResponse containerResponse = database.createContainerIfNotExists(containerProperties);
        container = database.getContainer(containerResponse.getProperties().getId());

        log.info("Initializing container {} completed!\n", container.getId());
    }

}
