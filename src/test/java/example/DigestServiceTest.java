package example;

public final class DigestServiceTest {
    public static void main(String[] args) {
        DigestService service = new DigestService(null, "https://example.org/worker");
        String payload = service.prepareEdition("Studio Classroom", "Fractions through cooking", "learner@example.org");
        if (!payload.equals("Studio Classroom|learner@example.org|Fractions through cooking"))
            throw new AssertionError(payload);
        try {
            service.prepareEdition("", "A lesson", "learner@example.org");
            throw new AssertionError("blank creator accepted");
        } catch (IllegalArgumentException expected) {
            System.out.println("digest decision test passed");
        }
    }
}
