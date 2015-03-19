package br.com.netomarin.pomodroid;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
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

    public static final String PREF_LAST_STATE = "last_state";
    public static final String PREF_LAST_STATE_TIME = "last_state_time";

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

        SharedPreferences.Editor editor = getSharedPreferences(Commons.PREFERENCES_NAME,
                MODE_PRIVATE).edit();
        editor.putInt(PREF_LAST_STATE, mCurrentState);
        editor.commit();

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

                broadcastTic();
                SharedPreferences.Editor editor = getSharedPreferences(Commons.PREFERENCES_NAME,
                        MODE_PRIVATE).edit();
                editor.putInt(PREF_LAST_STATE, mCurrentState);
                editor.putLong(PREF_LAST_STATE_TIME, System.currentTimeMillis());
                editor.commit();
            }
        }.start();
        startNotification();
    }

    private void finishBreak() {
        mCurrentState = STATE_READY;

        Intent startIntent = new Intent(this, PomodroidMainActivity.class);
        startIntent.putExtra(PomodroidMainActivity.EXTRA_NOTIFICATION_ACTION_NAME,
                PomodroidMainActivity.EXTRA_ACTION_START_POMODORO);
//        startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getNotificationTitle())
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PomodroidMainActivity.class),
                        PendingIntent.FLAG_CANCEL_CURRENT))
                .setLights(Color.GREEN, 500, 500)
                .addAction(android.R.drawable.ic_media_play, getString(R.string.btn_start),
                        PendingIntent.getActivity(this, STATE_POMODORO, startIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT));
        mNotificationManager.notify(FINISH_NOTIFICATION_ID, mBuilder.build());
    }

    private void finishPomodoro() {
        mCurrentState = STATE_FINISHED;

        Intent startBreakIntent = new Intent(this, PomodroidMainActivity.class);
        startBreakIntent.putExtra(PomodroidMainActivity.EXTRA_NOTIFICATION_ACTION_NAME,
                PomodroidMainActivity.EXTRA_ACTION_START_BREAK);
//        startBreakIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent restartIntent = new Intent(this, PomodroidMainActivity.class);
        restartIntent.putExtra(PomodroidMainActivity.EXTRA_NOTIFICATION_ACTION_NAME,
                PomodroidMainActivity.EXTRA_ACTION_RESTART_POMODORO);
//        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getNotificationTitle())
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PomodroidMainActivity.class), 0))
                .setVibrate(new long[]{0, 1000})
                .setSound(Uri.parse("android.resource://br.com.netomarin.pomodroid/" +
                        R.raw.alarm_clock_sound_short))
                .setLights(Color.YELLOW, 500, 500)
                .setAutoCancel(true);

        mBuilder.addAction(android.R.drawable.ic_media_play, getString(R.string.btn_start_break),
                        PendingIntent.getActivity(this, STATE_BREAK, startBreakIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT));

        mBuilder.addAction(android.R.drawable.ic_menu_revert, getString(R.string.btn_restart),
                        PendingIntent.getActivity(this, STATE_POMODORO, restartIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT));

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
                new Intent(this, PomodroidMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        Intent stopIntent = new Intent(this, PomodroidMainActivity.class);
        stopIntent.putExtra(PomodroidMainActivity.EXTRA_NOTIFICATION_ACTION_NAME,
                PomodroidMainActivity.EXTRA_ACTION_RESTART_POMODORO);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getNotificationTitle())
                .setContentText(getString(R.string.txt_time_remaining) + " " +
                        Commons.getRemainingTimeString(mTimeLeft))
                .setProgress((int)Pomodoro.DEFAULT_LENGHT, (int)Pomodoro.DEFAULT_LENGHT, false)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setLights(mCurrentState == STATE_POMODORO ? Color.RED : Color.YELLOW, 500, 500)
                .addAction(android.R.drawable.ic_media_pause, getString(R.string.btn_stop),
                        PendingIntent.getActivity(this, STATE_BREAK, stopIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT));

        if (mCurrentState == STATE_POMODORO) {
            Intent restartIntent = new Intent(this, PomodroidMainActivity.class);
            restartIntent.putExtra(PomodroidMainActivity.EXTRA_NOTIFICATION_ACTION_NAME,
                    PomodroidMainActivity.EXTRA_ACTION_RESTART_POMODORO);
            
            mBuilder.addAction(android.R.drawable.ic_menu_revert, getString(R.string.btn_restart),
                    PendingIntent.getActivity(this, STATE_POMODORO, restartIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT));
        }

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
                .setProgress(maxTimeValue, maxTimeValue - (int) minsRemaining, false)
                .setLights(mCurrentState == STATE_POMODORO ? Color.RED : Color.YELLOW, 500, 500);

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