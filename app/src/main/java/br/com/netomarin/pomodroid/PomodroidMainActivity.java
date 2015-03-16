package br.com.netomarin.pomodroid;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class PomodroidMainActivity extends Activity {

    private CountdownTimerService mBoundService;
    private CountdownTimerService.CountdownBinder mCountdownBinder;

    private long mTimeLeft;

    private Button pomodoroButton;
    private TextView timerTextView;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mCountdownBinder = (CountdownTimerService.CountdownBinder)service;
            mBoundService = ((CountdownTimerService.CountdownBinder)service).getService();
            Log.d("Pomoroid", "Service: " + mBoundService);
            syncLocalTimer();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodroid_main);

        timerTextView = (TextView)findViewById(R.id.timerTextView);
        pomodoroButton = (Button)findViewById(R.id.startPomodoroButton);
        pomodoroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPomodoro();
            }
        });
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        bindService(new Intent(PomodroidMainActivity.this, CountdownTimerService.class), mConnection, Context.BIND_AUTO_CREATE);
//    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(PomodroidMainActivity.this, CountdownTimerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnection != null)
            unbindService(mConnection);
    }

    private void startPomodoro() {

//        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        Notification.Builder mBuilder = new Notification.Builder(this);
//        mBuilder.setContentTitle("Running a Pomodoro...");
//        mBuilder.addAction(android.R.drawable.ic_media_pause, "Parar", null);
//        mBuilder.addAction(android.R.drawable.ic_media_next, "Reiniciar", null);

        mCountdownBinder.startTimer();
        syncLocalTimer();
        createPomodoroNotification();
    }

    private void syncLocalTimer() {
        mTimeLeft = mCountdownBinder.getTimeLeft();
        CountDownTimer mTimer = new CountDownTimer(mTimeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeft = mCountdownBinder.getTimeLeft();
                timerTextView.setText("Tempo Restante: " + ((mTimeLeft / 1000) / 60) + ":" + ((mTimeLeft / 1000) % 60));
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext()).setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("Pomodroid is running...")
                        .setContentText("Tempo Restante: " + ((mTimeLeft / 1000) / 60) + ":" + ((mTimeLeft / 1000) % 60))
                        .setOngoing(true);
                mNotificationManager.notify(1001, mBuilder.build());

                if (mTimeLeft == 0)
                    this.cancel();
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

    private void createPomodoroNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Pomodroid is running...")
                .setContentText("Tempo Restante: " + ((mTimeLeft / 1000) / 60) + ":" + ((mTimeLeft / 1000) % 60))
                .setOngoing(true);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(1001, mBuilder.build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pomodroid_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
