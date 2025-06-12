package com.dobrev;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.events.CronOptions;
import software.amazon.awscdk.services.events.RuleProps;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.LifecycleRule;

import java.util.Collections;
import java.util.List;


public class EventProcessingStack extends Stack {
    public EventProcessingStack(final App scope, final String id, StackProps props) {
        super(scope, id, props);

        Table urlEventsDdb = new Table(this, "UrlEventsDdb",
                TableProps.builder()
                        .partitionKey(Attribute.builder()
                                .name("eventId")
                                .type(AttributeType.STRING)
                                .build())
                        .sortKey(Attribute.builder()
                                .name("timestamp")
                                .type(AttributeType.NUMBER)
                                .build())
                        .tableName("urlevents")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .billingMode(BillingMode.PROVISIONED)
                        .readCapacity(1)
                        .writeCapacity(1)
                        .build());

        urlEventsDdb.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("userIdIdx")
                .partitionKey(Attribute.builder()
                        .name("userId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("timestamp")
                        .type(AttributeType.NUMBER)
                        .build())
                .build());

        urlEventsDdb.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("hashEventTypeIdx")
                .partitionKey(Attribute.builder()
                        .name("shortUrlHash")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("eventType")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        Bucket bucket = new Bucket(this, "DeletedUrlEvents", BucketProps.builder()
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .lifecycleRules(Collections.singletonList(LifecycleRule.builder()
                        .enabled(true)
                        .expiration(Duration.seconds(1))
                        .build()))
                .build());

        Function lambda = new Function(this, "Lambda", FunctionProps.builder()
                .runtime(Runtime.JAVA_17)
                .handler("com.dobrev.handler::clean;")
                .code(Code.fromAsset("lambda"))
                .memorySize(512)
                .timeout(Duration.seconds(15))
                .build());
        lambda.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("dynamodb:GetItem", "dynamodb:Query", "dynamodb:Scan", "dynamodb:DeleteItem"))
                .resources(List.of(urlEventsDdb.getTableArn(),
                        urlEventsDdb.getTableArn() + "/index/*"))
                .build());
        lambda.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("s3:PutObject"))
                .resources(List.of(bucket.getBucketArn() + "/*"))
                .build());

        Rule rule = new Rule(this, "CleanupScheduleRule", RuleProps.builder()
                .schedule(Schedule.cron(CronOptions.builder()
                        .minute("0")
                        .hour("0")
                        .build()))
                .targets(List.of(new LambdaFunction(lambda)))
                .build());
    }
}
