package handler;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class Handler implements RequestHandler<Object, String> {

    public String handleRequest(Object o, Context context) {
        DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create();

        DynamoDbTable<UrlEvent> urlEventTable = client.table("urlevents", TableSchema.fromBean(UrlEvent.class));
        new EventCleaner(urlEventTable).clean();
        return "Cleanup completed";
    }
}
