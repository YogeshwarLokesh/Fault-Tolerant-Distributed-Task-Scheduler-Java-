package store;

import model.Job;
import model.JobState;

import java.util.List;

public interface JobStore {

    void save(Job job);

    Job get(String jobId);

    void update(Job job);

    List<Job> getByState(JobState state);

    // ✅ NEW: atomic job claim for exactly-once execution
    boolean claimJob(String jobId, String workerId);
}
