package worker;

import model.Job;
import model.JobState;
import queue.RedisJobQueue;
import scheduler.RetryBackoff;
import store.JobStore;

public class Worker implements Runnable {

    private final String workerId;
    private final RedisJobQueue jobQueue;
    private final JobStore jobStore;

    // ✅ Graceful shutdown flag
    private volatile boolean running = true;

    public Worker(String workerId, RedisJobQueue jobQueue, JobStore jobStore) {
        this.workerId = workerId;
        this.jobQueue = jobQueue;
        this.jobStore = jobStore;
    }

    @Override
    public void run() {
        // ❤️ Start Redis heartbeat
        startHeartbeat();

        while (running) {
            try {
                // 🔥 Blocking Redis dequeue (BRPOP)
                String jobId = jobQueue.dequeue();

                if (!running) break;

                Job job = jobStore.get(jobId);
                if (job == null) continue;

                if (job.getState() != JobState.PENDING &&
                        job.getState() != JobState.RETRYING) {
                    continue;
                }

                executeJob(job);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("🛑 Worker " + workerId + " stopped");
    }

    private void executeJob(Job job) {
        // ✅ Exactly-once claim
        boolean claimed = jobStore.claimJob(job.getJobId(), workerId);
        if (!claimed) return;

        try {
            System.out.println(workerId + " is processing " + job.getJobId());

            Thread.sleep(2000); // simulate work

            job.setState(JobState.SUCCESS);
            jobStore.update(job);

            System.out.println(workerId + " completed " + job.getJobId());

        } catch (Exception e) {
            handleFailure(job);
        }
    }

    private void handleFailure(Job job) {
        if (job.getRetryCount() < 3) {
            job.incrementRetry();

            long delay = RetryBackoff.calculateDelay(job.getRetryCount());
            job.setNextRetryTime(System.currentTimeMillis() + delay);
            job.setState(JobState.RETRYING);
            jobStore.update(job);

            System.out.println(
                    "Retrying " + job.getJobId() + " in " + delay + " ms"
            );

        } else {
            job.setState(JobState.FAILED);
            jobStore.update(job);

            System.out.println(
                    "❌ " + job.getJobId() + " permanently FAILED"
            );
        }
    }

    private void startHeartbeat() {
        Thread heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    RedisHeartbeat.beat(workerId);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, workerId + "-heartbeat");

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    // ✅ REQUIRED for graceful shutdown
    public void stop() {
        running = false;
    }
}
