package com.dobrev;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.amazon.awscdk.services.ecr.TagStatus;
import software.constructs.Construct;

import java.util.List;

public class EcrStack extends Stack {
    private final Repository urlStatsRepository;

    public EcrStack(final Construct scope, final String id, final StackProps props){
        super(scope, id, props);

        this.urlStatsRepository = new Repository(this, "UrlStatsRepository",
                RepositoryProps.builder()
                        .repositoryName("urlstatsservice")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .imageTagMutability(TagMutability.MUTABLE)
                        .lifecycleRules(List.of(LifecycleRule.builder()
                                .rulePriority(1)
                                .description("Delete untagged images older than 30 days")
                                .tagStatus(TagStatus.UNTAGGED)
                                .maxImageAge(Duration.days(30))
                                .build()))
                        .build());
    }

    public Repository getUrlStatsRepository() {
        return urlStatsRepository;
    }
}
