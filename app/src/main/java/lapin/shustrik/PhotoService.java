package lapin.shustrik;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class PhotoService extends Service {
    private Timer timer;

    public PhotoService() {
    }

    class MakePhotoTask extends TimerTask {
        @Override
        public void run() {
            takePhoto(PhotoService.this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        takePhoto(PhotoService.this);
//        timer = new Timer();
//        timer.schedule(new MakePhotoTask(), 0, 10000);
    }

    //@SuppressWarnings("deprecation")
    private void takePhoto(final Context context) {
        final SurfaceView preview = new SurfaceView(context);
        SurfaceHolder holder = preview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        //holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {
                showMessage("Surface created");

                Camera camera = null;

                try {
                    camera = Camera.open();
                    showMessage("Opened camera");

                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    camera.startPreview();
                    showMessage("Started preview");

                    camera.takePicture(null, null, new Camera.PictureCallback() {

                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            onGetPhoto(data);
                            camera.release();
                        }
                    });
                } catch (Exception e) {
                    if (camera != null)
                        camera.release();
                    throw new RuntimeException(e);
                }
            }

            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
        });

        WindowManager wm = (WindowManager)context
                .getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                //Don't know if this is a safe default
                PixelFormat.UNKNOWN);

        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(preview, params);
        stopSelf();
    }

    private static void onGetPhoto(byte[] data) {
        Uri.Builder builder = Uri.parse(MainActivity.apiUrl).buildUpon();
        builder.appendQueryParameter("action","send");
        try {
            //ImageView imageView = (ImageView) FixedModeActivity.this.findViewById(R.id.imageView);
//            BitmapDrawable drawable = (BitmapDrawable)viewById.getDrawable();
//            if (drawable == null)
//                return;
//            Bitmap bitmap = drawable.getBitmap();
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
//            byte[] byteArray = stream.toByteArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("bmp", Util.bytesToHex(data));
            jsonObject.put("temp", FixedModeActivity.temp);
            jsonObject.put("vl", FixedModeActivity.vl);
            jsonObject.put("syr", FixedModeActivity.syr);
            JsonWriterAsync jsonWriterAsync = new JsonWriterAsync(new URL(builder.build().toString()));
            jsonWriterAsync.execute(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showMessage(String message) {
        Log.i("Camera", message);
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }
}
