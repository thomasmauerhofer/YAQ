package com.bitschupfa.sw16.yaq.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bitschupfa.sw16.yaq.bluetooth.ConnectionListener;
import com.bitschupfa.sw16.yaq.R;
import com.bitschupfa.sw16.yaq.game.HostGameLogic;
import com.bitschupfa.sw16.yaq.ui.PlayerList;
import com.bitschupfa.sw16.yaq.utils.Quiz;

public class Host extends AppCompatActivity {
    private static final int REQUEST_ENABLE_DISCOVERABLE_BT = 42;

    private final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private final ConnectionListener btConnectionListener = new ConnectionListener();
    private PlayerList playerList = new PlayerList(this);
    private Quiz quiz;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        HostGameLogic.getInstance().setHostActivity(this);

        if (setupBluetooth()) {
            new Thread(btConnectionListener, "BT Connection Listener Thread").start();
            TextView hostnameLabel = (TextView) findViewById(R.id.lbl_hostname);
            if (hostnameLabel != null) {
                hostnameLabel.append(btAdapter.getName());
            }
        }
        quiz = new Quiz();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btConnectionListener.close();
        unregisterReceiver(broadcastReceiver);
    }

    @SuppressWarnings("UnusedParameters")
    public void startButtonClicked(View view) {
        Intent intent = new Intent(Host.this, GameAtHost.class);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings("UnusedParameters")
    public void buildQuizButtonClicked(View view) {
        Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("UnusedParameters")
    public void advancedSettingsButtonClicked(View view) {
        Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show();
    }

    private boolean setupBluetooth() {
        registerReceiver(broadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        if (btAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Bluetooth is not supported on this device.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
            return false;
        }

        if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERABLE_BT);
        } else {
            btConnectionListener.setDiscoverable();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_DISCOVERABLE_BT:
                Log.d("Host:onActivityResult", "requestCode: REQUEST_ENABLE_DISCOVERABLE_BT");
                if (resultCode != RESULT_CANCELED) {
                    btConnectionListener.setDiscoverable();
                } else {
                    Log.d("BT", "Could not enable discoverability.");
                    finish();
                }
                break;
            default:
                Log.d("Host:onActivityResult", "unknown requestCode: " + resultCode);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    Log.d("BT", "Bluetooth was turned off!");
                    finish();
                }
            }
        }
    };

    public void updatePlayerList(final String[] playerNames) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playerList.clear();
                playerList.addAll(playerNames);
            }
        });
    }
}
