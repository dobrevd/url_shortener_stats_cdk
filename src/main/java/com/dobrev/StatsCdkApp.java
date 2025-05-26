package com.dobrev;

import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.HashMap;
import java.util.Map;

public class StatsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        Dotenv dotenv = Dotenv.load();
        Environment environment = Environment.builder()
                .account(dotenv.get("AWS_ACCOUNT_ID"))
                .region(dotenv.get("AWS_REGION"))
                .build();

        Map<String, String> infraTags = new HashMap<>();
        infraTags.put("team", "dobrevd");
        infraTags.put("cost", "StatsInfra");

        EcrStack ecrStack = new EcrStack(app, "Ecr", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        VpcStack vpcStack = new VpcStack(app, "Vpc", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        ClusterStack clusterStack = new ClusterStack(app, "Cluster", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new ClusterStackProps(vpcStack.getVpc()));
        clusterStack.addDependency(vpcStack);

        NlbStack nlbStack = new NlbStack(app, "Nlb", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new NlbStackProps(vpcStack.getVpc()));
        nlbStack.addDependency(vpcStack);

        ApiStack apiStack = new ApiStack(app, "Api", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new ApiStackProps(nlbStack.getVpcLink(), nlbStack.getNetworkLoadBalancer()));
        apiStack.addDependency(nlbStack);

        Map<String, String> statsServiceTags = new HashMap<>();
        statsServiceTags.put("team", "dobrevd");
        statsServiceTags.put("cost", "StatsService");

        StatsServiceStack statsServiceStack = new StatsServiceStack(app, "StatsService", StackProps.builder()
                .env(environment)
                .tags(statsServiceTags)
                .build(),
                new StatsServiceProps(vpcStack.getVpc(),
                        clusterStack.getCluster(),
                        nlbStack.getNetworkLoadBalancer(),
                        nlbStack.getApplicationLoadBalancer(),
                        ecrStack.getUrlStatsRepository()));
        statsServiceStack.addDependency(vpcStack);
        statsServiceStack.addDependency(clusterStack);
        statsServiceStack.addDependency(nlbStack);
        statsServiceStack.addDependency(ecrStack);

        app.synth();
    }
}

