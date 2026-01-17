package worker;

import redis.RedisClient;
import redis.clients.jedis.Jedis;

public class RedisHeartbeat {

    public static void beat(String workerId) {
        try (Jedis jedis = RedisClient.getResource()) {
            jedis.setex("worker:" + workerId, 5, "alive");
        } catch (Exception e) {
            System.err.println("⚠ Redis heartbeat failed for " + workerId);
        }
    }

    public static boolean isAlive(String workerId) {
        try (Jedis jedis = RedisClient.getResource()) {
            return jedis.exists("worker:" + workerId);
        } catch (Exception e) {
            return false;
        }
    }
}
