package com.dobrev;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriver;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateServiceProps;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.FargateTaskDefinitionProps;
import software.amazon.awscdk.services.ecs.LoadBalancerTargetOptions;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddNetworkTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseNetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.TopicProps;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueEncryption;
import software.amazon.awscdk.services.sqs.QueueProps;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StatsServiceStack extends Stack {
    private final Topic urlEventsTopic;

    public StatsServiceStack(final Construct construct, final String id, final StackProps props,
                             StatsServiceProps statsServiceProps) {
        super(construct, id, props);

        this.urlEventsTopic = new Topic(this, "UrlEventsTopic", TopicProps.builder()
                .displayName("URL events topic")
                .topicName("url-events")
                .build());

        Table urlEventsDdb = new Table(this, "UrlEventsDdb",
                TableProps.builder()
                        .partitionKey(Attribute.builder()
                                .name("id")
                                .type(AttributeType.STRING)
                                .build())
                        .tableName("urlevents")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .billingMode(BillingMode.PROVISIONED)
                        .readCapacity(1)
                        .writeCapacity(1)
                        .build());

        Queue urlEventsDlq = new Queue(this, "UrlEventsDlq",
                QueueProps.builder()
                        .queueName("url-events-dlq")
                        .retentionPeriod(Duration.days(8))
                        .enforceSsl(false)
                        .encryption(QueueEncryption.UNENCRYPTED)
                        .build()
        );

        Queue urlEventsQueue = new Queue(this, "UrlEventsQueue",
                QueueProps.builder()
                        .queueName("url-events")
                        .enforceSsl(false)
                        .encryption(QueueEncryption.UNENCRYPTED)
                        .deadLetterQueue(DeadLetterQueue.builder()
                                .queue(urlEventsDlq)
                                .maxReceiveCount(2)
                                .build())
                        .build()
        );
        urlEventsTopic.addSubscription(new SqsSubscription(urlEventsQueue));

        FargateTaskDefinition fargateTaskDefinition = new FargateTaskDefinition(this, "TaskDefinition",
                FargateTaskDefinitionProps.builder()
                        .family("stats-service")
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        .build());
        urlEventsDdb.grantReadWriteData(fargateTaskDefinition.getTaskRole());
        urlEventsQueue.grantConsumeMessages(fargateTaskDefinition.getTaskRole());

        LogGroup logGroup = new LogGroup(this, "LogGroup",
                LogGroupProps.builder()
                        .logGroupName("StatsService")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_MONTH)
                        .build());
        AwsLogDriver logDriver = new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(logGroup)
                .streamPrefix("StatsService")
                .build());

        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("SERVER_PORT", "8080");
        envVariables.put("AWS_URLEVENTSDDB_NAME", urlEventsDdb.getTableName());
        envVariables.put("AWS_REGION", this.getRegion());
        envVariables.put("SPRING_PROFILES_ACTIVE", "prod");
        envVariables.put("AWS_SQS_QUEUE_URLEVENT_URL", urlEventsQueue.getQueueUrl());

        fargateTaskDefinition.addContainer("StatsServiceContainer",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromEcrRepository(statsServiceProps.repository(), "latest"))
                        .containerName("statsService")
                        .logging(logDriver)
                        .portMappings(Collections.singletonList(PortMapping.builder()
                                .containerPort(8080)
                                .protocol(Protocol.TCP)
                                .build()))
                        .environment(envVariables)
                        .cpu(384)
                        .memoryLimitMiB(896)
                        .build());

        ApplicationListener applicationListener = statsServiceProps.applicationLoadBalancer()
                .addListener("StatsServiceAlbListener", ApplicationListenerProps.builder()
                        .port(8080)
                        .protocol(ApplicationProtocol.HTTP)
                        .loadBalancer(statsServiceProps.applicationLoadBalancer())
                        .build());

        FargateService fargateService = new FargateService(this, "StatsService",
                FargateServiceProps.builder()
                        .serviceName("StatsService")
                        .cluster(statsServiceProps.cluster())
                        .taskDefinition(fargateTaskDefinition)
                        .desiredCount(2)
                        .assignPublicIp(false)
                        .build());
        statsServiceProps.repository().grantPull(Objects.requireNonNull(fargateTaskDefinition.getExecutionRole()));
        fargateService.getConnections().getSecurityGroups().get(0).addIngressRule(Peer.anyIpv4(), Port.tcp(8080));

        Role execRole = (Role) fargateTaskDefinition.obtainExecutionRole();
        execRole.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        "logs:CreateLogStream",
                        "logs:PutLogEvents"
                ))
                .resources(Collections.singletonList(logGroup.getLogGroupArn()))
                .build());

        applicationListener.addTargets("StatsServiceAlbTarget",
                AddApplicationTargetsProps.builder()
                        .targetGroupName("statsServiceAlb")
                        .port(8080)
                        .protocol(ApplicationProtocol.HTTP)
                        .targets(Collections.singletonList(fargateService))
                        .deregistrationDelay(Duration.seconds(30))
                        .healthCheck(HealthCheck.builder()
                                .enabled(true)
                                .interval(Duration.seconds(30))
                                .timeout(Duration.seconds(10))
                                .path("/actuator/health")
                                .port("8080")
                                .build())
                        .build()
        );

        NetworkListener networkListener = statsServiceProps.networkLoadBalancer()
                .addListener("StatsServiceNlbListener", BaseNetworkListenerProps.builder()
                        .port(8080)
                        .protocol(
                                software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP
                        )
                        .build());

        networkListener.addTargets("StatsServiceNlbTarget",
                AddNetworkTargetsProps.builder()
                        .port(8080)
                        .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
                        .targetGroupName("statsServiceNlb")
                        .targets(Collections.singletonList(
                                fargateService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                                        .containerName("statsService")
                                        .containerPort(8080)
                                        .protocol(Protocol.TCP)
                                        .build())
                        ))
                        .build()
        );

        ScalableTaskCount scalableTaskCount = fargateService.autoScaleTaskCount(
                EnableScalingProps.builder()
                        .maxCapacity(4)
                        .minCapacity(2)
                        .build());
        scalableTaskCount.scaleOnCpuUtilization("StatsServiceAutoScaling",
                CpuUtilizationScalingProps.builder()
                        .targetUtilizationPercent(80)
                        .scaleInCooldown(Duration.seconds(60))
                        .scaleOutCooldown(Duration.seconds(60))
                        .build());
    }

    public Topic getUrlEventsTopic() {
        return urlEventsTopic;
    }
}

record StatsServiceProps(
        Vpc vpc,
        Cluster cluster,
        NetworkLoadBalancer networkLoadBalancer,
        ApplicationLoadBalancer applicationLoadBalancer,
        Repository repository
) {
}