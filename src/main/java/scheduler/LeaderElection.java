package scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class LeaderElection {

    private static final AtomicBoolean LEADER_LOCK = new AtomicBoolean(false);

    private final String nodeId;

    public LeaderElection(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean tryBecomeLeader() {
        boolean isLeader = LEADER_LOCK.compareAndSet(false, true);
        if (isLeader) {
            System.out.println("👑 Node " + nodeId + " became LEADER");
        }
        return isLeader;
    }

    public void resignLeadership() {
        LEADER_LOCK.set(false);
        System.out.println("❌ Node " + nodeId + " resigned leadership");
    }
}
