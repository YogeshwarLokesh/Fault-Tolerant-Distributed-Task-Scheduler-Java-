package store;

import model.Job;
import model.JobState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryJobStore implements JobStore {

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    @Override
    public void save(Job job) {
        jobs.put(job.getJobId(), job);
    }

    @Override
    public Job get(String jobId) {
        return jobs.get(jobId);
    }

    @Override
    public void update(Job job) {
        jobs.put(job.getJobId(), job);
    }

    @Override
    public List<Job> getByState(JobState state) {
        List<Job> result = new ArrayList<>();
        for (Job job : jobs.values()) {
            if (job.getState() == state) {
                result.add(job);
            }
        }
        return result;
    }

    // ✅ REQUIRED BY INTERFACE
    @Override
    public synchronized boolean claimJob(String jobId, String workerId) {
        Job job = jobs.get(jobId);

        if (job == null) return false;

        if (job.getState() == JobState.PENDING ||
                job.getState() == JobState.RETRYING) {

            job.setState(JobState.RUNNING);
            job.setWorkerId(workerId);
            return true;
        }

        return false;
    }
}
