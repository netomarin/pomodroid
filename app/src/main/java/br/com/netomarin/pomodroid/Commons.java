package br.com.netomarin.pomodroid;

/**
 * Created by netomarin on 3/16/15.
 */
public class Commons {
    public static final long MILLIS_PER_SECOND = 1000;
    public static final long SECONDS_PER_MINUTE = 60;

    public static String getRemainingTimeString(long timeLeft) {
        int s = (int) ((timeLeft / 1000) % 60);
        return ((timeLeft / 1000) / 60) + ":" + (s < 10 ? "0"+s : s);
    }
}
