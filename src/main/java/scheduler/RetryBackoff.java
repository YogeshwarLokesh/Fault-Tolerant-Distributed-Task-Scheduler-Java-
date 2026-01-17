package scheduler;

public class RetryBackoff {

    private static final long BASE_DELAY_MS = 1000; // 1 second

    public static long calculateDelay(int retryCount) {
        return BASE_DELAY_MS * (1L << retryCount);
    }
}
