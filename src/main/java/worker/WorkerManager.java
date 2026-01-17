package worker;

import model.Job;
import model.JobState;
import queue.JobQueue;
import store.JobStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerManager {

    private final Map<String, Long> workerHeartbeats = new ConcurrentHashMap<>();
    private final JobQueue jobQueue;
    private final JobStore jobStore;

    public WorkerManager(JobQueue jobQueue, JobStore jobStore) {
        this.jobQueue = jobQueue;
        this.jobStore = jobStore;
    }

    // Worker sends heartbeat every few seconds
    public void updateHeartbeat(String workerId) {
        workerHeartbeats.put(workerId, System.currentTimeMillis());
    }

    // Check for stuck workers
    public void monitorWorkers(long timeoutMillis) {
        long now = System.currentTimeMillis();
        workerHeartbeats.forEach((workerId, lastHeartbeat) -> {
            if (now - lastHeartbeat > timeoutMillis) {
                System.out.println("Worker " + workerId + " seems dead! Reassigning its jobs...");

                // Reassign jobs in RUNNING state of this worker
                jobStore.getByState(JobState.RUNNING).forEach(job -> {
                    if (workerId.equals(job.getWorkerId())) {
                        job.setWorkerId(null);
                        job.setState(JobState.RETRYING);
                        jobQueue.enqueue(job.getJobId());
                        jobStore.update(job);
                    }
                });

                // Remove dead worker from tracking
                workerHeartbeats.remove(workerId);
            }
        });
    }
}
