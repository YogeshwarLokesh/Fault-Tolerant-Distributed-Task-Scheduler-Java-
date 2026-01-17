package core;

import model.Job;
import queue.RedisJobQueue;
import scheduler.LeaderScheduler;
import scheduler.RedisLeaderElection;
import store.JobStore;
import store.PostgresJobStore;
import worker.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SchedulerMain {

    public static void main(String[] args) {

        // -----------------------------
        // Core components
        // -----------------------------
        JobStore jobStore = new PostgresJobStore();
        RedisJobQueue jobQueue = new RedisJobQueue();

        // -----------------------------
        // Node identity (unique per JVM)
        // -----------------------------
        String nodeId = "scheduler-node-" + UUID.randomUUID();
        RedisLeaderElection leaderElection =
                new RedisLeaderElection(nodeId);

        // -----------------------------
        // Leader Scheduler
        // -----------------------------
        LeaderScheduler leaderScheduler =
                new LeaderScheduler(jobStore, jobQueue, leaderElection);

        Thread leaderThread =
                new Thread(leaderScheduler, "leader-scheduler");
        leaderThread.start();

        // -----------------------------
        // Create jobs ONLY IF DB IS EMPTY
        // -----------------------------
        if (jobStore.getByState(model.JobState.PENDING).isEmpty()) {
            for (int i = 1; i <= 5; i++) {
                Job job = new Job("job-" + i);
                jobStore.save(job);
                jobQueue.enqueue(job.getJobId());
            }
        }

        // -----------------------------
        // Start workers
        // -----------------------------
        List<Worker> workers = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            Worker worker =
                    new Worker("worker-" + i, jobQueue, jobStore);
            workers.add(worker);

            new Thread(worker, "worker-" + i).start();
        }

        // -----------------------------
        // Graceful shutdown
        // -----------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 JVM shutting down gracefully...");

            // stop workers
            for (Worker worker : workers) {
                worker.stop();
            }

            // close DB pool
            try {
                store.DataSourceProvider.getDataSource().close();
                System.out.println("✅ Database pool closed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
