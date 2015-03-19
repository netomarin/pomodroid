package br.com.netomarin.pomodroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class PomodroidMainActivity extends Activity {
    public static final String EXTRA_NOTIFICATION_ACTION_NAME = "notification_action";
    public static final int EXTRA_ACTION_RESTART_POMODORO = 0;
    public static final int EXTRA_ACTION_START_POMODORO = 1;
    public static final int EXTRA_ACTION_START_BREAK = 2;
    public static final int EXTRA_ACTION_STOP = 3;

    private long mTimeLeft;

    private Button startPomodoroButton;
    private Button stopPomodoroButton;
    private TextView timerTextView;

    private TimerTicReceiver timerTicReceiver;
    private Intent timerService;
    private int currentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodroid_main);

        timerTextView = (TextView) findViewById(R.id.timerTextView);
        startPomodoroButton = (Button) findViewById(R.id.startPomodoroButton);
        startPomodoroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPomodoro();
            }
        });
        stopPomodoroButton = (Button) findViewById(R.id.stopPomodoroButton);
        stopPomodoroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPomodoro();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (timerTicReceiver == null) {
            timerTicReceiver = new TimerTicReceiver();
            registerReceiver(timerTicReceiver, new IntentFilter(PomodoroTimerService.
                    TIC_BROADCAST_MESSAGE));
        }

        Intent actionIntent = getIntent();
        if (actionIntent != null &&
                actionIntent.getIntExtra(EXTRA_NOTIFICATION_ACTION_NAME, -1) != -1) {
            Log.d("Pomodroid", "Action from notification!");

            int notificationAction = actionIntent.getIntExtra(EXTRA_NOTIFICATION_ACTION_NAME, -1);
            switch (notificationAction) {
                case EXTRA_ACTION_RESTART_POMODORO:
                case EXTRA_ACTION_START_POMODORO:
                    currentState = PomodoroTimerService.STATE_READY;
                    startPomodoro();
                    break;
                case EXTRA_ACTION_START_BREAK:
                    currentState = PomodoroTimerService.STATE_FINISHED;
                    startPomodoro();
                    break;
                case EXTRA_ACTION_STOP:
                    stopPomodoro();
                    break;
            }
            getIntent().removeExtra(EXTRA_NOTIFICATION_ACTION_NAME);
        } else {
            SharedPreferences prefs = getSharedPreferences(Commons.PREFERENCES_NAME, MODE_PRIVATE);
            currentState = prefs.getInt(PomodoroTimerService.PREF_LAST_STATE,
                    PomodoroTimerService.STATE_READY);
            updatePomodoroMessage(0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        if (timerTicReceiver != null) {
            unregisterReceiver(timerTicReceiver);
            timerTicReceiver = null;
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void updatePomodoroMessage(long timeLeft) {
        if(currentState == PomodoroTimerService.STATE_POMODORO ||
                currentState == PomodoroTimerService.STATE_BREAK) {
            timerTextView.setText(getString(R.string.txt_time_remaining) + " " +
                    Commons.getRemainingTimeString(timeLeft));
        } else if (currentState == PomodoroTimerService.STATE_FINISHED) {
            timerTextView.setText(getString(R.string.txt_pomodoro_finished));
        } else {
            timerTextView.setText(getString(R.string.txt_state_ready));
        }
    }

    private void updateCycleButtons() {
        if (currentState == PomodoroTimerService.STATE_POMODORO ||
                currentState == PomodoroTimerService.STATE_BREAK) {
            startPomodoroButton.setEnabled(false);
            stopPomodoroButton.setEnabled(true);
        } else {
            startPomodoroButton.setEnabled(true);
            stopPomodoroButton.setEnabled(false);
        }
    }

    private void startPomodoro() {
        if (currentState == PomodoroTimerService.STATE_READY) {
            currentState = PomodoroTimerService.STATE_POMODORO;
        } else if (currentState == PomodoroTimerService.STATE_FINISHED) {
            currentState = PomodoroTimerService.STATE_BREAK;
        } else {
            throw new IllegalStateException("Pomodoro state not recognized");
        }
        startTimerService(currentState);
        updateCycleButtons();
    }

    private void stopPomodoro() {
        stopService(new Intent(this, PomodoroTimerService.class));
        currentState = PomodoroTimerService.STATE_READY;
        updateCycleButtons();
        updatePomodoroMessage(0);
        SharedPreferences.Editor editor = getSharedPreferences(Commons.PREFERENCES_NAME,
                MODE_PRIVATE).edit();
        editor.putInt(PomodoroTimerService.PREF_LAST_STATE, currentState);
        editor.putLong(PomodoroTimerService.PREF_LAST_STATE_TIME, System.currentTimeMillis());
        editor.commit();
    }

    private void startTimerService(int state) {
        timerService = new Intent(this, PomodoroTimerService.class);
        timerService.putExtra(PomodoroTimerService.TIMER_EXTRA_STATE, state);
        startService(timerService);
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

    private class TimerTicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long timeLeft = intent.getLongExtra(PomodoroTimerService.TIC_BROADCAST_PAYLOAD, 0);
            int broadcastState = intent.getIntExtra(PomodoroTimerService.TIC_BROADCAST_STATE, 0);
            if (broadcastState != currentState) {
                currentState = broadcastState;
                updateCycleButtons();
            }
            updatePomodoroMessage(timeLeft);
        }
    }
}
