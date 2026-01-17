package scheduler;

import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

public class RedisLeaderElection {

    private static final String LEADER_KEY = "scheduler_leader";
    private final String nodeId;

    public RedisLeaderElection(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean tryBecomeLeader() {
        try (Jedis jedis = RedisClient.getResource()) {
            SetParams params = new SetParams().nx().px(10_000);
            String result = jedis.set(LEADER_KEY, nodeId, params);
            return "OK".equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    public void renewLeadership() {
        try (Jedis jedis = RedisClient.getResource()) {
            jedis.pexpire(LEADER_KEY, 10_000);
        } catch (Exception ignored) {}
    }

    public boolean isLeader() {
        try (Jedis jedis = RedisClient.getResource()) {
            return nodeId.equals(jedis.get(LEADER_KEY));
        } catch (Exception e) {
            return false;
        }
    }
}
