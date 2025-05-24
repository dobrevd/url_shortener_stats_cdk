package com.dobrev;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.ConnectionType;
import software.amazon.awscdk.services.apigateway.Integration;
import software.amazon.awscdk.services.apigateway.IntegrationOptions;
import software.amazon.awscdk.services.apigateway.IntegrationProps;
import software.amazon.awscdk.services.apigateway.IntegrationType;
import software.amazon.awscdk.services.apigateway.JsonWithStandardFieldProps;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
import software.amazon.awscdk.services.apigateway.MethodLoggingLevel;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigateway.VpcLink;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class ApiStack extends Stack {
    public ApiStack(final Construct scope, final String id, final StackProps props,
                    ApiStackProps apiStackProps) {
        super(scope, id, props);

        LogGroup logGroup = new LogGroup(this, "StatsApiLogs", LogGroupProps.builder()
                .logGroupName("StatsApi")
                .removalPolicy(RemovalPolicy.DESTROY)
                .retention(RetentionDays.ONE_MONTH)
                .build());

        RestApi restApi = new RestApi(this, "RestApi",
                RestApiProps.builder()
                        .restApiName("StatsApi")
                        .cloudWatchRole(true)
                        .deployOptions(StageOptions.builder()
                                .loggingLevel(MethodLoggingLevel.INFO)
                                .accessLogDestination(new LogGroupLogDestination(logGroup))
                                .accessLogFormat(AccessLogFormat.jsonWithStandardFields(
                                        JsonWithStandardFieldProps.builder()
                                                .caller(true)
                                                .httpMethod(true)
                                                .ip(true)
                                                .protocol(true)
                                                .requestTime(true)
                                                .resourcePath(true)
                                                .responseLength(true)
                                                .status(true)
                                                .user(true)
                                                .build()
                                ))
                                .build())
                        .build());

        this.createStatsResource(restApi, apiStackProps);
    }

    private void createStatsResource(RestApi restApi, ApiStackProps apiStackProps) {
        Resource resource = restApi.getRoot().addResource("stats");
        resource.addMethod("POST", new Integration(IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("POST")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() +
                        ":8080/api/stats")
                .options(IntegrationOptions.builder()
                        .vpcLink(apiStackProps.vpcLink())
                        .connectionType(ConnectionType.VPC_LINK)
                        .build())
                .build()));

        Map<String, String> eventsIdIntegrationParameters = new HashMap<>();
        eventsIdIntegrationParameters.put("integration.request.path.userId", "method.request.path.userId");

        Map<String, Boolean> eventsIdMethodParameters = new HashMap<>();
        eventsIdMethodParameters.put("method.request.path.userId", true);

        Resource eventsIdResource = resource.addResource("{userId}");
        eventsIdResource.addMethod("GET", new Integration(
                        IntegrationProps.builder()
                                .type(IntegrationType.HTTP_PROXY)
                                .integrationHttpMethod("GET")
                                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() +
                                        ":8080/api/stats/{userId}")
                                .options(IntegrationOptions.builder()
                                        .vpcLink(apiStackProps.vpcLink())
                                        .connectionType(ConnectionType.VPC_LINK)
                                        .requestParameters(eventsIdIntegrationParameters)
                                        .build())
                                .build()),
                MethodOptions.builder()
                        .requestParameters(eventsIdMethodParameters)
                        .build());
    }
}

record ApiStackProps(
        VpcLink vpcLink,
        NetworkLoadBalancer networkLoadBalancer
) {
}