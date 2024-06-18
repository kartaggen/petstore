#!/bin/bash

# Function to create an Azure Resource Group
function create_resource_group() {
    local resource_group_name="$1"
    local resource_group_location="$2"

    echo "Creating Resource group: $resource_group_name"
    az group create \
        --name $resource_group_name \
        --location $resource_group_location
}

# Function to create an Azure Container Registry
function create_container_registry() {
    local resource_group_name="$1"
    local container_registry_name="$2"

    echo "Creating Container Registry: $container_registry_name"
    az acr create \
    --resource-group $resource_group_name \
    --name $container_registry_name \
    --sku Basic \
    --admin-enabled true
}

# Function to create an Azure App Service Plan
function create_app_service_plan() {
    local plan_location="$2"
    local plan_name="$1-$2"
    local resource_group_name="$3"

    echo "Creating App Service Plan: $plan_name"
    az appservice plan create \
        --name "$plan_name" \
        --resource-group "$resource_group_name" \
        --location "$plan_location" \
        --sku P1V2 \
        --is-linux \
        --tags "Project=PetStore" \
        --output none
}

# Function to create an Azure Web App
function create_web_app() {
    local repository_name="$1"
    local appservice_plan_name="$2-$3"
    local wep_app_location="$3"
    local resource_group_name="$4"
    local container_registry_name="$5"
    local web_app_name="webapp-$repository_name-$wep_app_location"
    echo "Creating Web App: $web_app_name"
    az webapp create \
        --resource-group $resource_group_name \
        --plan $appservice_plan_name \
        --name $web_app_name \
        --deployment-container-image-name $container_registry_name.azurecr.io/$repository_name:latest \
        --output none
}

# Function to add an environment variable to an Azure Web App
function add_web_app_env_variable() {
    local repository_name="$1"
    local wep_app_location="$2"
    local resource_group_name="$3"
    local web_app_name="webapp-$repository_name-$wep_app_location"
    local environment_pairs="$4"
    echo "Adding to Web App: $web_app_name, the environment variables: $environment_pairs"
    az webapp config appsettings set \
        --name $web_app_name \
        --resource-group $resource_group_name \
        --settings $environment_pairs \
        --output none
}

# Function to add a custom autoscale to an app service plan
function add_autoscale() {
    local asp_name="$1"
    local location="$2"
    local resource_group_name="$3"
    local app_service_plan="$1-$2"
    local autoscale_setting_name="$app_service_plan-custom-autoscale"
    echo "Adding a custom autoscale to App service plan: $app_service_plan"
    az monitor autoscale create \
        --resource-group $resource_group_name \
        --name $autoscale_setting_name \
        --resource $app_service_plan \
        --resource-type "Microsoft.Web/serverFarms" \
        --min-count 1 \
        --max-count 3 \
        --count 1
}

# Function to add an custom autoscale rule to an autoscale
function add_autoscale_rule() {
    local asp_name="$1"
    local location="$2"
    local resource_group_name="$3"
    local app_service_plan="$1-$2"
    local scaling="$4"
    local condition="$5"
    local autoscale_setting_name="$app_service_plan-custom-autoscale"
    echo "Adding a custom autoscale rule to App service plan: $app_service_plan"
    az monitor autoscale rule create \
        --resource-group $resource_group_name \
        --autoscale-name $autoscale_setting_name \
        --scale $scaling \
        --condition $condition
}

#Function to add a deployment slot to a web app
function add_deployment_slot() {
    local repository_name="$1"
    local wep_app_location="$2"
    local resource_group_name="$3"
    local deployment_slot_name="$4"
    local web_app_name="webapp-$repository_name-$wep_app_location"
    echo "Adding a deployment slot: $deployment_slot_name to web app: $web_app_name"
    az webapp deployment slot create \
        --name $web_app_name \
        --resource-group $resource_group_name \
        --slot $deployment_slot_name
}

#Function to add a Traffic Manager Profile
function add_traffic_manager_profile() {
    local traffic_manager_name="$1"
    local traffic_manager_dns="$2"
    local resource_group_name="$3"
    echo "Adding a traffic manager profile with name: $traffic_manager_name"
    az network traffic-manager profile create \
        --name $traffic_manager_name \
        --resource-group $resource_group_name \
        --routing-method "Priority" \
        --unique-dns-name $traffic_manager_dns
}

#Function to add a Traffic Manager Endpoint
function add_traffic_manager_endpoint() {
    local traffic_manager_name="$1"
    local resource_group_name="$2"
    local location="$3"
    local repository_name="$4"
    local priority="$5"
    local endpoint_name="$traffic_manager_name-$location"
    local web_app_id=$(az webapp show \
        --resource-group $resource_group_name \
        --name "webapp-$repository_name-$location" \
        --query id \
        --output tsv)

    echo "ID: $web_app_id"
    echo "resource_group_name: $resource_group_name"
    echo "traffic_manager_name: $traffic_manager_name"
    echo "endpoint_name: $endpoint_name"
    echo "priority: $priority"
    echo "Adding a traffic manager endpoint with name: $endpoint_name"
    az network traffic-manager endpoint create \
    --resource-group "$resource_group_name" \
    --profile-name "$traffic_manager_name" \
    --name "$endpoint_name" \
    --type azureEndpoints \
    --target-resource-id "$web_app_id" \
    --endpoint-status enabled \
    --priority "$priority"
}


# Variables
RG_NAME="ps-rg"
LOCATION_US="eastus"
LOCATION_EU="westeurope"
ACR_NAME="clouxpsacr"

ASP_APP_NAME="asp-ps-web"
ASP_API_NAME="asp-ps-api"

REPO_PS_APP="petstoreapp"
REPO_PS_ORDER_SERVICE="petstoreorderservice"
REPO_PS_PET_SERVICE="petstorepetservice"
REPO_PS_PRODUCT_SERVICE="petstoreproductservice"

PETSTOREORDERSERVICE_URL="https://webapp-petstoreorderservice-eastus.azurewebsites.net"
PETSTOREPETSERVICE_URL="https://webapp-petstorepetservice-eastus.azurewebsites.net"
PETSTOREPRODUCTSERVICE_URL="https://webapp-petstoreproductservice-eastus.azurewebsites.net"

PETSTOREORDERSERVICE_ENV_PAIR="PETSTOREORDERSERVICE_URL=$PETSTOREORDERSERVICE_URL"
PETSTOREPETSERVICE_ENV_PAIR="PETSTOREPETSERVICE_URL=$PETSTOREPETSERVICE_URL"
PETSTOREPRODUCTSERVICE_ENV_PAIR="PETSTOREPRODUCTSERVICE_URL=$PETSTOREPRODUCTSERVICE_URL"

PETSTORE_APP_ENV_PAIRS="$PETSTOREORDERSERVICE_ENV_PAIR $PETSTOREPETSERVICE_ENV_PAIR $PETSTOREPRODUCTSERVICE_ENV_PAIR"
PETSTORE_ORDER_SERVICE_ENV_PAIRS="$PETSTOREPRODUCTSERVICE_ENV_PAIR"

SCALING_OUT="out 1"
CONDITION_OUT="CpuPercentage > 70 avg 5m"
SCALING_IN="in 1"
CONDITION_IN="CpuPercentage < 25 avg 5m"

DEPLOYMENT_SLOT_NAME="Testing"

TM_PS_NAME="tm-petstore-app"
TM_DNS="cloudx-petstore-app"
TM_US_PRIORITY="1"
TM_EU_PRIORITY="2"

#Executed functions
create_resource_group "$RG_NAME" "$LOCATION_US"
create_container_registry "$RG_NAME" "$ACR_NAME"

echo "Extract Azure Container Registry secret:"
az acr credential show --name $ACR_NAME --output yaml
#Copy password2 to GitHib Actions - Repository Secrets

create_app_service_plan "$ASP_APP_NAME" "$LOCATION_US" "$RG_NAME"
create_app_service_plan "$ASP_APP_NAME" "$LOCATION_EU" "$RG_NAME"
create_app_service_plan "$ASP_API_NAME" "$LOCATION_US" "$RG_NAME"

create_web_app "$REPO_PS_APP" "$ASP_APP_NAME" "$LOCATION_US" "$RG_NAME" "$ACR_NAME"
create_web_app "$REPO_PS_APP" "$ASP_APP_NAME" "$LOCATION_EU" "$RG_NAME" "$ACR_NAME"
create_web_app "$REPO_PS_ORDER_SERVICE" "$ASP_API_NAME" "$LOCATION_US" "$RG_NAME" "$ACR_NAME"
create_web_app "$REPO_PS_PET_SERVICE" "$ASP_API_NAME" "$LOCATION_US" "$RG_NAME" "$ACR_NAME"
create_web_app "$REPO_PS_PRODUCT_SERVICE" "$ASP_API_NAME" "$LOCATION_US" "$RG_NAME" "$ACR_NAME"

add_web_app_env_variable "$REPO_PS_APP" "$LOCATION_US" "$RG_NAME" "$PETSTORE_APP_ENV_PAIRS"
add_web_app_env_variable "$REPO_PS_APP" "$LOCATION_EU" "$RG_NAME" "$PETSTORE_APP_ENV_PAIRS"
add_web_app_env_variable "$REPO_PS_ORDER_SERVICE" "$LOCATION_US" "$RG_NAME" "$PETSTORE_ORDER_SERVICE_ENV_PAIRS"

add_autoscale "$ASP_API_NAME" "$LOCATION_US" "$RG_NAME"
add_autoscale_rule "$ASP_API_NAME" "$LOCATION_US" "$RG_NAME" "$SCALING_OUT" "$CONDITION_OUT"
add_autoscale_rule "$ASP_API_NAME" "$LOCATION_US" "$RG_NAME" "$SCALING_IN" "$CONDITION_IN"

add_deployment_slot "$REPO_PS_APP" "$LOCATION_US" "$RG_NAME" "$DEPLOYMENT_SLOT_NAME"

docker tag ee7e3726f674 clouxpsacr.azurecr.io/petstoreapp:latest
docker push clouxpsacr.azurecr.io/petstoreapp:latest

add_traffic_manager_profile "$TM_PS_NAME" "$TM_DNS" "$RG_NAME"

RG_NAME="ps-rg"
LOCATION_US="eastus"
LOCATION_EU="westeurope"
REPO_PS_APP="petstoreapp"
TM_PS_NAME="tm-petstore-app"
TM_US_PRIORITY="1"
TM_EU_PRIORITY="2"

#Not working due to web app id
add_traffic_manager_endpoint "$TM_PS_NAME" "$RG_NAME" "$LOCATION_US" "$REPO_PS_APP" "$TM_US_PRIORITY"
add_traffic_manager_endpoint "$TM_PS_NAME" "$RG_NAME" "$LOCATION_EU" "$REPO_PS_APP" "$TM_EU_PRIORITY"