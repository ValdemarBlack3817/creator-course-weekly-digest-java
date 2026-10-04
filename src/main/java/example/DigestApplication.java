package example;

/** Runnable entry point; configuration is supplied by environment variables. */
public final class DigestApplication {
    public static void main(String[] args) throws Exception {
        String key = require("INFRAI_API_KEY");
        String worker = System.getenv().getOrDefault("DIGEST_WORKER_URL", "https://example.org/digest-worker");
        DigestService service = new DigestService(new InfraiClient(key), worker);
        String jobId = service.scheduleWeeklyDigest();
        service.publishEdition("Studio Classroom", "Fractions through cooking", "learner@example.org");
        System.out.println("scheduled weekly digest job " + jobId);
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Set " + name);
        return value;
    }
}
