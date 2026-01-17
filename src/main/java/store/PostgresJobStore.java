package store;

import model.Job;
import model.JobState;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresJobStore implements JobStore {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/task_scheduler";
    private static final String USER = "postgres";
    private static final String PASSWORD = "yolo";

    public PostgresJobStore() {
        createTableIfNotExists();
    }

    private Connection getConnection() throws SQLException {
        return DataSourceProvider.getDataSource().getConnection();
    }


    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS jobs (
                job_id VARCHAR(50) PRIMARY KEY,
                state VARCHAR(20) NOT NULL,
                worker_id VARCHAR(50),
                retry_count INT NOT NULL,
                next_retry_time BIGINT
            )
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create jobs table", e);
        }
    }

    @Override
    public void save(Job job) {
        String sql = """
            INSERT INTO jobs (job_id, state, worker_id, retry_count, next_retry_time)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (job_id) DO NOTHING
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, job.getJobId());
            ps.setString(2, job.getState().name());
            ps.setString(3, job.getWorkerId());
            ps.setInt(4, job.getRetryCount());
            ps.setObject(5, job.getNextRetryTime());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Job job) {
        String sql = """
            UPDATE jobs
            SET state = ?, worker_id = ?, retry_count = ?, next_retry_time = ?
            WHERE job_id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, job.getState().name());
            ps.setString(2, job.getWorkerId());
            ps.setInt(3, job.getRetryCount());
            ps.setObject(4, job.getNextRetryTime());
            ps.setString(5, job.getJobId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Job get(String jobId) {
        String sql = "SELECT * FROM jobs WHERE job_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jobId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            return mapRow(rs);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Job> getByState(JobState state) {
        String sql = "SELECT * FROM jobs WHERE state = ?";
        List<Job> jobs = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, state.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                jobs.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return jobs;
    }

    private Job mapRow(ResultSet rs) throws SQLException {
        Job job = new Job(rs.getString("job_id"));
        job.setState(JobState.valueOf(rs.getString("state")));
        job.setWorkerId(rs.getString("worker_id"));
        job.setRetryCount(rs.getInt("retry_count"));
        job.setNextRetryTime(rs.getLong("next_retry_time"));
        return job;
    }
    @Override
    public boolean claimJob(String jobId, String workerId) {

        String sql = """
        UPDATE jobs
        SET state = 'RUNNING', worker_id = ?
        WHERE job_id = ?
          AND state IN ('PENDING', 'RETRYING')
    """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, workerId);
            ps.setString(2, jobId);

            int updated = ps.executeUpdate();

            return updated == 1; // 👈 atomic guarantee

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
