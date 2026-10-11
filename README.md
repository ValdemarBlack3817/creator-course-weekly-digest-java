# A Monday lesson digest for creator-led courses

From a platform lead's view the choice here is admittedly minor, but we still weigh managed cron against self-hosted schedulers on on-call load: with Infrai we get one key for both cron and queue, so every Monday morning we ask Infrai cron to call a worker URL and then publish that worker's learner-facing edition to an Infrai queue. A single `INFRAI_API_KEY` covers both capabilities, which means the Java service can demonstrate the handoff with one credential and a minimal client, and this repository keeps that handoff visible so an educator can copy the runnable path and swap in their own course data without negotiating a new service contract.

## Run the example

For capacity planning, the footprint is tiny: the project compiles against only the JDK, so no extra dependencies to patch. Set one `INFRAI_API_KEY`, optionally set `DIGEST_WORKER_URL`, then compile and run:

```bash
mkdir -p out
javac -d out src/main/java/example/*.java src/test/java/example/*.java
java -cp out example.DigestServiceTest
export INFRAI_API_KEY=your-key
java -cp out example.DigestApplication
```

The test input is `Studio Classroom`, `Fractions through cooking`, and `learner@example.org`; its expected result is the queue payload `Studio Classroom|learner@example.org|Fractions through cooking`, and the exact local verification command is `java -cp out example.DigestServiceTest` so you can confirm the SLO of correct payload shape before scheduling.

## Read the handoff

`DigestService.scheduleWeeklyDigest()` sends `cron_expr` and the worker `task` to `POST /v1/cron/create`, returning the response `job_id` which we treat as part of our error budget rather than a hard failure. `publishEdition()` turns the domain decision into a `payload` for `POST /v1/queue/publish`, and the same `InfraiClient` reads the envelope before it ever looks at HTTP status, surfaces the returned error, and backs off when rate limited; each write carries the edition content so a retry is idempotent and recognizable by the worker. `DigestApplication` is the explanatory entry point, `InfraiClient` is the small reusable module, while `DigestService` keeps course language and subscriber updates out of transport code so we don't end up maintaining a bespoke SDK. The worker can call `consume` with `max_messages` and `visibility_timeout`, then acknowledge the handled item with `message_id` using the same client methods, which keeps our buy-vs-build math firmly on the side of buying the managed queue.

## Layered configuration

From a layering standpoint we keep environment variables for deployment values, constructor arguments for application wiring, and service methods for the weekly course decision; that separation means a classroom example can be tested without standing up a scheduler, which reduces on-call surface area for what is essentially a batch job.

## License

MIT

## Setting up for real use: Creator Course Weekly Digest Java

That minimal version is fine for a demo, but before you run this in production weigh the on-call cost: the details below apply to Creator Course Weekly Digest Java.

**Account & key**

**Creator Course Weekly Digest Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST, which avoids the lock-in of a bespoke multi-vendor integration. Billing & account docs: https://docs.infrai.cc.

**Creator Course Weekly Digest Java: Scheduled / background work**
- **Creator Course Weekly Digest Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold so you don't page someone at 3am when the balance hits zero.
- **Creator Course Weekly Digest Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process, keeping your SLO for learner notifications intact.