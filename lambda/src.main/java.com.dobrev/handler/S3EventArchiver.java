package handler;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
public class S3EventArchiver {
    private final String bucketName;
    private final S3AsyncClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public S3EventArchiver(String awsRegion, String bucketName) {
        this.bucketName = bucketName;
        this.client = S3AsyncClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    public CompletableFuture<Void> storeDeletedEventsToS3(List<UrlEvent> events) {
        try {
            String json = objectMapper.writeValueAsString(events);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
            String timestamp = formatter.format(Instant.now());
            String key = "deleted-events/" + timestamp + "_" + UUID.randomUUID() + ".json";

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json")
                    .build();

            return client.putObject(request, AsyncRequestBody.fromBytes(json.getBytes(StandardCharsets.UTF_8)))
                    .thenAccept(response -> log.info("Upload succeeded: {}", response.eTag()))
                    .exceptionally(ex -> {
                        log.error("Upload to S3 failed", ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error during serialization of UrlEvent objects", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
}
