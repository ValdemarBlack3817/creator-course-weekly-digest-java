package example;

import java.io.IOException;

/** Domain service: schedule a weekly digest and hand each edition to a queue worker. */
public final class DigestService {
    // The domain handoff mirrors the documented cron.create capability.
    private final InfraiClient infrai;
    private final String workerUrl;

    public DigestService(InfraiClient infrai, String workerUrl) {
        this.infrai = infrai;
        this.workerUrl = workerUrl;
    }

    public String scheduleWeeklyDigest() throws IOException, InterruptedException {
        return infrai.createCron("0 9 * * 1", workerUrl);
    }

    public String prepareEdition(String creator, String lessonTitle, String subscriber) {
        if (creator.isBlank() || lessonTitle.isBlank() || subscriber.isBlank())
            throw new IllegalArgumentException("creator, lessonTitle, and subscriber are required");
        return creator + "|" + subscriber + "|" + lessonTitle;
    }

    public void publishEdition(String creator, String lessonTitle, String subscriber) throws IOException, InterruptedException {
        infrai.publish(prepareEdition(creator, lessonTitle, subscriber));
    }
}
