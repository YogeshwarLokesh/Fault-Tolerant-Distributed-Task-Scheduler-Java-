package model;

public class Job {

    private final String jobId;
    private JobState state;
    private int retryCount;
    private String workerId;
    private long lastUpdated;
    private long nextRetryTime;

    public Job(String jobId) {
        this.jobId = jobId;
        this.state = JobState.PENDING;
        this.retryCount = 0;
        this.lastUpdated = System.currentTimeMillis();
        this.nextRetryTime = 0;

    }

    public String getJobId() {
        return jobId;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public long getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(long nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

}
