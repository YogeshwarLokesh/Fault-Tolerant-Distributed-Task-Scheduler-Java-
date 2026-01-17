package queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class JobQueue {

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    // Enqueue job by ID
    public void enqueue(String jobId) {
        queue.offer(jobId);
    }

    // Dequeue job (blocks if empty)
    public String dequeue() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}
