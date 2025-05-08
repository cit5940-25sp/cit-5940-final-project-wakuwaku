import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages a countdown timer for a player's turn.
 */
public class TurnTimer {
    private final int seconds;
    private Timer timer;
    private Runnable onTimeout;

    public TurnTimer(int seconds, Runnable onTimeout) {
        this.seconds = seconds;
        this.onTimeout = onTimeout;
    }

    /**
     * Starts the countdown. Calls onTimeout.run() if time elapses.
     */
    public void start() {
        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimeout.run();
            }
        }, seconds * 1000L);
    }

    /**
     * Stops the timer if the player responded in time.
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}

