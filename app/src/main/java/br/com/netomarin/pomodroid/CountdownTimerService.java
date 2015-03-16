package br.com.netomarin.pomodroid;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

public class CountdownTimerService extends Service {

    private long mTimeLeft;
    private CountDownTimer mCountDownTimer;

    public class CountdownBinder extends Binder {
        CountdownTimerService getService () {
            return CountdownTimerService.this;
        }

        public long getTimeLeft() {
            return mTimeLeft;
        }

        public void startTimer() {
            startCountdownTimer();
        }

        public void stopTimer() {
            mCountDownTimer.cancel();
        }
    }

    private final IBinder mBinder = new CountdownBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Pomodroid", "CountdownTimerService created!");
//        startCountdownTimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Pomodroid", "Start Command received!");
//        startCountdownTimer();
        return START_STICKY;
    }

    private void startCountdownTimer() {
        mTimeLeft = 1000 * 60 * 25;
        mCountDownTimer = new CountDownTimer((1000 * 60 * 25), 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeft = millisUntilFinished;
                Log.d("Pomodroid", "Tempo restante: " + ((mTimeLeft / 1000) / 60) + ":" + ((mTimeLeft / 1000) % 60));
            }

            @Override
            public void onFinish() {
                mTimeLeft = 0;
            }
        };
        mCountDownTimer.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Pomodroid", "CountdownTimerService bound");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Pomodroid", "CountdownTimerService unbound");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("Pomodroid", "CountdownTimerService rebound");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Pomodroid", "CountdownTimerService destroy");
        mCountDownTimer.cancel();
    }
}