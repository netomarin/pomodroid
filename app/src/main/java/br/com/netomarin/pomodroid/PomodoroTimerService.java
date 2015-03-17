package br.com.netomarin.pomodroid;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class PomodoroTimerService extends Service {

    /**
     * Pomodoro life cycle states
     */
    public static final int STATE_READY = 0;
    public static final int STATE_POMODORO = 1;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_BREAK = 3;

    public static final String TIMER_EXTRA_STATE = "STATE";

    /**
     * Configuration variables to the tic-tac broadcaster
     */
    public static final String TIC_BROADCAST_MESSAGE = PomodoroTimerService.
            class.getPackage() + "TIMER_TIC";
    public static final String TIC_BROADCAST_PAYLOAD = "SECONDS_LEFT";
    public static final String TIC_BROADCAST_STATE = "CYCLE_STATE";

    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final int FINISH_NOTIFICATION_ID = 1002;

    private long mTimeLeft;
    private int mCurrentState;
    private CountDownTimer mCountDownTimer;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Pomodroid", "PomodoroTimerService created!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Pomodroid", "Start Command received!");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mCurrentState = intent.getIntExtra(TIMER_EXTRA_STATE, STATE_POMODORO);
        startCountdownTimer();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startCountdownTimer() {
        mTimeLeft = Commons.MILLIS_PER_SECOND*Commons.SECONDS_PER_MINUTE*
                (mCurrentState == STATE_POMODORO ? Pomodoro.DEFAULT_LENGHT :
                        Pomodoro.DEFAULT_BREAK);
        /**
         * Para testes
         */
        mTimeLeft = Commons.MILLIS_PER_SECOND * 10;
        mCountDownTimer = new CountDownTimer((mTimeLeft), Commons.MILLIS_PER_SECOND) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeft = millisUntilFinished;
                broadcastTic();
                refreshNotification();
                Log.d("Pomodroid", "Tempo restante: " + Commons.getRemainingTimeString(mTimeLeft));
            }

            @Override
            public void onFinish() {
                mTimeLeft = 0;
                broadcastTic();
                mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotificationManager.cancelAll();
                if (mCurrentState == STATE_POMODORO) {
                    finishPomodoro();
                } else if (mCurrentState == STATE_BREAK) {
                    finishBreak();
                }
            }
        }.start();
        startNotification();
    }

    private void finishBreak() {
        mCurrentState = STATE_READY;
        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getNotificationTitle())
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PomodroidMainActivity.class), 0));
        mNotificationManager.notify(FINISH_NOTIFICATION_ID, mBuilder.build());
    }

    private void finishPomodoro() {
        mCurrentState = STATE_FINISHED;
        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getNotificationTitle())
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PomodroidMainActivity.class), 0));
        mNotificationManager.notify(FINISH_NOTIFICATION_ID, mBuilder.build());
    }

    private void broadcastTic() {
        Intent i = new Intent(TIC_BROADCAST_MESSAGE);
        i.putExtra(TIC_BROADCAST_PAYLOAD, mTimeLeft);
        i.putExtra(TIC_BROADCAST_STATE, mCurrentState);
        sendBroadcast(i);
    }

    private void startNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, PomodroidMainActivity.class), 0);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getNotificationTitle())
                .setContentText(getString(R.string.txt_time_remaining) + " " +
                        Commons.getRemainingTimeString(mTimeLeft))
                .setProgress((int)Pomodoro.DEFAULT_LENGHT, (int)Pomodoro.DEFAULT_LENGHT, false)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
        mNotificationManager.cancel(FINISH_NOTIFICATION_ID);
        mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
        Toast.makeText(this, getString(R.string.toast_pomodoro_started), Toast.LENGTH_SHORT).show();
    }

    private void refreshNotification() {
        long minsRemaining = mTimeLeft / Commons.MILLIS_PER_SECOND / Commons.SECONDS_PER_MINUTE;
        int maxTimeValue = (int)(mCurrentState == STATE_POMODORO ? Pomodoro.DEFAULT_LENGHT :
                Pomodoro.DEFAULT_BREAK);
        mBuilder.setContentTitle(getNotificationTitle())
                .setContentText(getString(R.string.txt_time_remaining) + " " +
                        Commons.getRemainingTimeString(mTimeLeft))
                .setProgress(maxTimeValue, maxTimeValue - (int) minsRemaining, false);

        mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
    }

    private String getNotificationTitle() {
        switch (mCurrentState) {
            case STATE_READY:
                return getString(R.string.txt_title_state_ready);
            case STATE_POMODORO:
                return getString(R.string.txt_title_state_pomodoro);
            case STATE_FINISHED:
                return getString(R.string.txt_title_state_finished);
            case STATE_BREAK:
                return getString(R.string.txt_title_state_break);
            default: return getString(R.string.txt_title_state_pomodoro);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Pomodroid", "PomodoroTimerService bound");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Pomodroid", "PomodoroTimerService unbound");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("Pomodroid", "PomodoroTimerService rebound");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d("Pomodroid", "PomodoroTimerService destroy");
        mCountDownTimer.cancel();
        mNotificationManager.cancelAll();
        stopSelf();
        super.onDestroy();
    }
}