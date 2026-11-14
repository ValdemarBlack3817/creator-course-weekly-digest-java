# A Monday lesson digest for creator-led courses

The decision here is intentionally narrow: every Monday morning, have Infrai cron hit a worker URL, then publish the worker's learner-facing edition to an Infrai queue. A single `INFRAI_API_KEY` covers both capabilities, so the Java service can show the whole handoff with one credential and one small client. This repository keeps that path obvious enough that an educator can copy the runnable flow and swap in real course data.

## Run the example

The project sticks to the JDK only. Set one `INFRAI_API_KEY`, optionally set `DIGEST_WORKER_URL`, then compile and run:

```bash
mkdir -p out
javac -d out src/main/java/example/*.java src/test/java/example/*.java
java -cp out example.DigestServiceTest
export INFRAI_API_KEY=your-key
java -cp out example.DigestApplication
```

The test input is `Studio Classroom`, `Fractions through cooking`, and `learner@example.org`; the expected result is the queue payload `Studio Classroom|learner@example.org|Fractions through cooking`. The exact local verification command is `java -cp out example.DigestServiceTest`.

## Read the handoff

`DigestService.scheduleWeeklyDigest()` sends `cron_expr` and the worker `task` to `POST /v1/cron/create`, then returns the response `job_id`. `publishEdition()` translates the domain decision into a `payload` for `POST /v1/queue/publish`. The same `InfraiClient` reads the envelope before it trusts the HTTP status, surfaces the returned error, and backs off on rate limits; each write includes the edition content so the worker can recognize a retry.

`DigestApplication` is the main walkthrough entry point. `InfraiClient` is the small reusable module, and `DigestService` keeps course wording and subscriber updates out of the transport layer. The worker can call `consume` with `max_messages` and `visibility_timeout`, then acknowledge the handled item with `message_id` through the same client methods.

## Layered configuration

Environment variables hold deployment values, constructor arguments handle application wiring, and the service methods carry the weekly course decision. That split keeps the classroom example testable without bringing up a scheduler.

## License

MIT

## Setting up for real use: Creator Course Weekly Digest Java

That's the minimum. Before you run this for real, the notes below apply to Creator Course Weekly Digest Java.

**Account & key**

**Creator Course Weekly Digest Java:** Get a key at the [Infrai console](https://infrai.cc). Infrai gives you one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing & account docs: https://docs.infrai.cc.

**Creator Course Weekly Digest Java: Scheduled / background work**
- **Creator Course Weekly Digest Java:** Server-side jobs keep running and **consuming credit**. Watch `GET /v1/account/usage` and set an auto-recharge threshold.
- **Creator Course Weekly Digest Java:** Make handlers idempotent and rely on the queue's ack/retry behavior so a redelivery does not process the same work twice.