package lapin.shustrik;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class FixedModeActivity extends AppCompatActivity implements View.OnClickListener {
    static MediaPlayer mPlayer = null;
    Timer musicTimer = null;
    Timer photoTimer = null;
    Timer requestTimer = null;
    private Intent mServiceIntent;
    public static int temp = 24;
    public static int vl = 61;
    public static int syr = 0;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixed_mode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.btnStart).setOnClickListener(this);
        findViewById(R.id.btnStop).setOnClickListener(this);
        requestTimer = new Timer();
        mServiceIntent = new Intent(this, PhotoService.class);
        //mServiceIntent.setData(Uri.parse(dataUrl));
        photoTimer = new Timer();
        musicTimer = new Timer();

        textView = (TextView) findViewById(R.id.textInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btnStart:
                start();
                break;
            case R.id.btnStop:
                stop();
                break;
        }
    }

    private void start() {
        if (mPlayer == null)
            mPlayer = MediaPlayer.create(FixedModeActivity.this, R.raw.music);
        mPlayer.start();
        musicTimer.schedule(new PlayMusicTask(), 0, 60*60*1000);
        photoTimer.schedule(new SendTask(), 0, 30*60*1000);
        findBT();
        openBT();
        beginListenForData();
        BtAsyncTask btAsyncTask = new BtAsyncTask();
        btAsyncTask.execute();
        //requestTimer.schedule(new RequestTask((ImageView)findViewById(R.id.imageView)), 0, 10000);
    }

    public class BtAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... objs) {
            return null;
        }

        @Override
        protected void onPostExecute(Void dummy) {
            //TextView textView = (TextView) findViewById(R.id.textView2);
            //textView.setText("post finished");
        }
    }

    private void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer = null;
        }
        musicTimer.cancel();
        musicTimer = new Timer();
        photoTimer.cancel();
        photoTimer = new Timer();
        requestTimer.cancel();
        requestTimer = new Timer();
        this.stopService(mServiceIntent);
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

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            textView.setText("No bluetooth adapter available");
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
        textView.setText("Bluetooth Device Found " + mmDevice.getName());
    }

    void openBT()
    {
        if (mmDevice == null)
            return;
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            //mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            //mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            Method m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            mmSocket = (BluetoothSocket) m.invoke(mmDevice, Integer.valueOf(1));
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            //beginListenForData();
            textView.setText("Bluetooth Opened");
        } catch (Exception e) {
            e.printStackTrace();
            textView.setText(e.getMessage());
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
                                    if (data.matches("\\d+\\s+\\d+\\s+\\d+\\s*")) {
                                        String[] split = data.split("\\s+");
                                        temp = Integer.parseInt(split[1]);
                                        vl = Integer.parseInt(split[0]);
                                        syr = Integer.parseInt(split[2]);
                                        TextView stateView = (TextView) findViewById(R.id.stateView);
                                        stateView.setText("Temp="+temp+", vl="+vl+", syr="+syr);
                                    }
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            textView.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException
    {
        String msg = textView.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        textView.setText("Data Sent");
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
        textView.setText("Bluetooth Closed");
    }

    class PlayMusicTask extends TimerTask {
        public void run() {
            if (mPlayer!=null && !mPlayer.isPlaying())
                mPlayer.start();
        }
    }

    class SendTask extends TimerTask {
        public void run() {
            FixedModeActivity.this.startService(mServiceIntent);
        }
    }


    class RequestTask extends TimerTask {

        private ImageView viewById;

        public RequestTask(ImageView imageView) {

            this.viewById = imageView;
        }

        public void run() {
//            HttpWorker httpWorker = new HttpWorker();
//            Uri.Builder builder = Uri.parse(MainActivity.apiUrl).buildUpon();
//            builder.appendQueryParameter("action","send_photo");
//            builder.query(MainActivity.apiUrl);
//            try {
//                //ImageView imageView = (ImageView) FixedModeActivity.this.findViewById(R.id.imageView);
//                BitmapDrawable drawable = (BitmapDrawable)viewById.getDrawable();
//                if (drawable == null)
//                    return;
//                Bitmap bitmap = drawable.getBitmap();
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
//                byte[] byteArray = stream.toByteArray();
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("bmp", Util.bytesToHex(byteArray));
//                httpWorker.postJSONToUrl(new URL(builder.build().toString()), jsonObject);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }
}
