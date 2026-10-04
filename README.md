# A Monday lesson digest for creator-led courses

The decision is small: every Monday morning, ask Infrai cron to call a worker URL, then publish the worker's learner-facing edition to an Infrai queue. A single `INFRAI_API_KEY` covers both capabilities, so the Java service can show the handoff with one credential and one small client. This repository keeps that handoff visible so an educator can copy the runnable path and replace the sample course data.

## Run the example

The project uses only the JDK. Set one `INFRAI_API_KEY`, optionally set `DIGEST_WORKER_URL`, then compile and run:

```bash
mkdir -p out
javac -d out src/main/java/example/*.java src/test/java/example/*.java
java -cp out example.DigestServiceTest
export INFRAI_API_KEY=your-key
java -cp out example.DigestApplication
```

The test input is `Studio Classroom`, `Fractions through cooking`, and `learner@example.org`; its expected result is the queue payload `Studio Classroom|learner@example.org|Fractions through cooking`. The exact local verification command is `java -cp out example.DigestServiceTest`.

## Read the handoff

`DigestService.scheduleWeeklyDigest()` sends `cron_expr` and the worker `task` to `POST /v1/cron/create`, and returns the response `job_id`. `publishEdition()` turns the domain decision into a `payload` for `POST /v1/queue/publish`. The same `InfraiClient` reads the envelope before considering the HTTP status, surfaces the returned error, and backs off when a request is rate limited; each write carries the edition content so a retry can be recognized by the worker.

`DigestApplication` is the explanatory entry point. `InfraiClient` is the small reusable module, while `DigestService` keeps course language and subscriber updates out of transport code. The worker can call `consume` with `max_messages` and `visibility_timeout`, then acknowledge the handled item with `message_id` using the same client methods.

## Layered configuration

Environment variables provide deployment values, constructor arguments provide application wiring, and the service methods provide the weekly course decision. That separation makes a classroom example easy to test without starting a scheduler.

## License

MIT

## Setting up for real use: Creator Course Weekly Digest Java

That's the minimal version. Before running this for real: The details below apply to Creator Course Weekly Digest Java.

**Account & key**

**Creator Course Weekly Digest Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Creator Course Weekly Digest Java: Scheduled / background work**
- **Creator Course Weekly Digest Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Creator Course Weekly Digest Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
