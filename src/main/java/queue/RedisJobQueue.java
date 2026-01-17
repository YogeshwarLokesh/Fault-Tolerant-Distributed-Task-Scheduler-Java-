package queue;

import redis.RedisClient;
import redis.clients.jedis.Jedis;

public class RedisJobQueue {

    private static final String QUEUE_KEY = "job_queue";

    public void enqueue(String jobId) {
        try (Jedis jedis = RedisClient.getResource()) {
            jedis.lpush(QUEUE_KEY, jobId);
        }
    }

    public String dequeue() throws InterruptedException {
        try (Jedis jedis = RedisClient.getResource()) {
            // BRPOP blocks until job is available
            return jedis.brpop(0, QUEUE_KEY).get(1);
        }
    }
}
