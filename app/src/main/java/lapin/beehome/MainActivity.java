package lapin.beehome;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView infoView;

    //public static String apiUrl = "http://192.168.10.167:88/api.php";
    //public static String apiUrl = "http://10.0.2.2:88/test.php";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        infoView = ((TextView) findViewById(R.id.textInfo));
        start();
    }

    @Override
    protected void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private void start() {
        findBT();
        openBT();
        beginListenForData();
    }

    private void stop() {
        closeBT();
    }

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothSocket mmSocket = null;
    BluetoothDevice mmDevice = null;
    volatile boolean stopWorker;
    OutputStream mmOutputStream = null;
    InputStream mmInputStream = null;
    Thread workerThread = null;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.textCover:
                if (mmOutputStream == null) {
                    infoView.setText("OutputStream is null");
                }
                sendCoverAction();
                break;
        }
    }

    void sendCoverAction() {
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    infoView.setText("Sendind cover action");
                    String coverAction = "zaslonka\n";
                    byte[] bytes = coverAction.getBytes(Charset.forName("UTF-8"));
                    mmOutputStream.write(bytes);
                    infoView.setText("Cover action was sent");
                }
                catch (Exception ex) {
                    infoView.setText(ex.getMessage());
                }
            }
        }).start();
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            infoView.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                Log.i("bt", "device found " + device.getName());
                if(device.getName().contains("HC"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        if (mmDevice==null)
            return;
        Log.i("bt", "device found '" + mmDevice.getName() + "' addr="+mmDevice.getAddress());
        infoView.setText("Bluetooth Device Found " + mmDevice.getName());
    }

    void openBT()
    {
        if (mmDevice == null)
            return;
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            //mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            //mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            try {
                infoView.setText("Connect Bluetooth method 1");
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                mmSocket.connect();
                mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
                infoView.setText(e.getMessage());
                Thread.sleep(1000);
                infoView.setText("Connect Bluetooth method 2");
                try {
                    Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    mmSocket = (BluetoothSocket) m.invoke(mmDevice, Integer.valueOf(1));
                    mmSocket.connect();
                    mmOutputStream = mmSocket.getOutputStream();
                    mmInputStream = mmSocket.getInputStream();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    infoView.setText(e2.getMessage());
                    Thread.sleep(1000);
                    infoView.setText("Connect Bluetooth method 3");
                    mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                    mmSocket.connect();
                    mmOutputStream = mmSocket.getOutputStream();
                    mmInputStream = mmSocket.getInputStream();
                }
            }
            //beginListenForData();
            infoView.setText("Bluetooth Opened");
        } catch (Exception e) {
            e.printStackTrace();
            infoView.setText(e.getMessage());
        }
    }

    void beginListenForData()
    {
        if (mmInputStream == null)
            return;
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if (b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    //final String data = Util.bytesToHex(encodedBytes);
                                    final String data = new String(encodedBytes, "UTF-8");
                                    readBufferPosition = 0;
                                    Log.i("bt", "data="+data);
                                    try {
                                        if (data.matches("\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s*")) {
                                            String[] split = data.split("\\s+");
                                            setTextById(R.id.textNoise,split[0]);
                                            String coverState = split[1];
                                            if (coverState.equals("1")) {
                                                setTextById(R.id.textCover, "Открыто");
                                            }
                                            else {
                                                setTextById(R.id.textCover, "Закрыто");
                                            }
                                            setTextById(R.id.textTempInside,split[2]);
                                            setTextById(R.id.textTempOutside, split[3]);
                                            String alert = split[4];
                                            View alertView = findViewById(R.id.textAlert);
                                            if (alert.equals("1")) {
                                                setTextById(R.id.textAlert, "РОЕНИЕ");
                                                alertView.setBackgroundResource(R.color.alertBg);
                                            }
                                            else {
                                                setTextById(R.id.textAlert, "");
                                                alertView.setBackgroundResource(0);
                                            }
                                            //((TextView) findViewById(R.id.textNoise)).setText(split[1]);
                                            //stateView.setText("Temp=" + noise + ", vl=" + cover + ", tInside=" + tInside);
                                        }
                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            infoView.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                        Thread.sleep(1000);
                    }
                    catch (Exception ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void setTextById(int id, String text) {
        ((TextView) findViewById(id)).setText(text);
    }

    void closeBT()
    {
        stopWorker = true;
        if (mmSocket != null) {
            try {
                mmOutputStream.close();
                mmInputStream.close();
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        infoView.setText("Bluetooth Closed");
    }

}
