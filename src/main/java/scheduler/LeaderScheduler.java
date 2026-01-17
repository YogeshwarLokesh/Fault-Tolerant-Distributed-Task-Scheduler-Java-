package scheduler;

import model.JobState;
import queue.RedisJobQueue;
import store.JobStore;
import worker.RedisHeartbeat;

public class LeaderScheduler implements Runnable {

    private final JobStore jobStore;
    private final RedisJobQueue jobQueue;
    private final RedisLeaderElection leaderElection;

    private boolean isLeader = false;

    public LeaderScheduler(
            JobStore jobStore,
            RedisJobQueue jobQueue,
            RedisLeaderElection leaderElection
    ) {
        this.jobStore = jobStore;
        this.jobQueue = jobQueue;
        this.leaderElection = leaderElection;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!isLeader) {
                    isLeader = leaderElection.tryBecomeLeader();
                }

                if (isLeader) {
                    recoverJobsFromDeadWorkers();
                    releaseRetryJobs();
                    leaderElection.renewLeadership();
                }

                Thread.sleep(1000);

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Detect dead workers using Redis TTL heartbeats
     */
    private void recoverJobsFromDeadWorkers() {
        jobStore.getByState(JobState.RUNNING).forEach(job -> {
            String workerId = job.getWorkerId();

            if (workerId != null && !RedisHeartbeat.isAlive(workerId)) {
                System.out.println(
                        "⚠ Worker " + workerId +
                                " DEAD (Redis TTL). Reassigning " + job.getJobId()
                );

                job.setWorkerId(null);
                job.setState(JobState.RETRYING);
                jobStore.update(job);
            }
        });
    }

    /**
     * Release jobs after exponential backoff expires
     */
    private void releaseRetryJobs() {
        long now = System.currentTimeMillis();

        jobStore.getByState(JobState.RETRYING).forEach(job -> {
            if (job.getNextRetryTime() <= now) {
                job.setState(JobState.PENDING);
                jobStore.update(job);
                jobQueue.enqueue(job.getJobId());

                System.out.println(
                        "⏱ Re-enqueued " + job.getJobId() + " after backoff"
                );
            }
        });
    }
}
