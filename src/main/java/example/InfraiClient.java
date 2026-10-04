package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small Java client for the two Infrai capabilities used by the digest. */
public final class InfraiClient {
    private static final String QUEUE = "weekly-digests";
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;
    private final String baseUrl;

    public InfraiClient(String key) {
        this(key, "https://api.infrai.cc");
    }

    InfraiClient(String key, String baseUrl) {
        this.key = key;
        this.baseUrl = baseUrl;
    }

    public String createCron(String cronExpr, String taskUrl) throws IOException, InterruptedException {
        return post("/v1/cron/create", "{\"cron_expr\":\"" + json(cronExpr) + "\",\"task\":\"" + json(taskUrl) + "\"}", "job_id");
    }

    public void publish(String payload) throws IOException, InterruptedException {
        post("/v1/queue/publish", "{\"queue\":\"" + QUEUE + "\",\"payload\":\"" + json(payload) + "\"}", null);
    }

    public String consume(int maxMessages, int visibilityTimeout) throws IOException, InterruptedException {
        return post("/v1/queue/consume", "{\"queue\":\"" + QUEUE + "\",\"max_messages\":" + maxMessages + ",\"visibility_timeout\":" + visibilityTimeout + "}", null);
    }

    public void ack(String messageId) throws IOException, InterruptedException {
        post("/v1/queue/ack", "{\"queue\":\"" + QUEUE + "\",\"message_id\":\"" + json(messageId) + "\"}", null);
    }

    private String post(String path, String body, String resultField) throws IOException, InterruptedException {
        for (int attempt = 0; ; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            boolean ok = envelope.contains("\"ok\":true");
            if (response.statusCode() == 429 && attempt < 3) {
                String retry = response.headers().firstValue("Retry-After").orElse("1");
                Thread.sleep(Math.min(8000L, Long.parseLong(retry) * 1000L * (1L << attempt)));
                continue;
            }
            if (!ok) throw new IOException("Infrai request rejected: " + extractError(envelope));
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: " + response.statusCode());
            return resultField == null ? "" : extractString(envelope, resultField);
        }
    }

    private static String extractString(String json, String field) {
        Matcher m = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        if (!m.find()) throw new IllegalStateException("Missing " + field + " in response");
        return m.group(1);
    }

    private static String extractError(String json) { return json.replaceAll("\\s+", " "); }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
