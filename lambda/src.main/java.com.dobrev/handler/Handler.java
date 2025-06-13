package handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class Handler implements RequestHandler<Object, String> {
    private final UrlEventStore urlEventStore;
    private final S3EventArchiver eventRepository;

    public Handler() {
        String awsRegion = System.getenv("AWS_REGION");
        String bucketName = System.getenv("BUCKET_NAME");
        this.eventRepository = new S3EventArchiver(awsRegion, bucketName);

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.create();
        DynamoDbTable<UrlEvent> urlEventTable = enhancedClient.table("urlevents", TableSchema.fromBean(UrlEvent.class));
        this.urlEventStore = new UrlEventStore(urlEventTable);
    }

    public String handleRequest(Object o, Context context) {
        new EventCleaner(urlEventStore, eventRepository).clean();
        return "Cleanup completed";
    }
}
