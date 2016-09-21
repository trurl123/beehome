package lapin.shustrik;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class FixedModeActivity extends AppCompatActivity implements View.OnClickListener {
    static MediaPlayer mPlayer = null;
    Timer musicTimer = null;
    Timer photoTimer = null;
    Timer requestTimer = null;
    private Intent mServiceIntent;
    public static int temp;
    public static int vl;
    public static int syr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixed_mode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.btnStart).setOnClickListener(this);
        findViewById(R.id.btnStop).setOnClickListener(this);
        findViewById(R.id.btnMakePhoto).setOnClickListener(this);
        requestTimer = new Timer();
        mServiceIntent = new Intent(this, PhotoService.class);
        //mServiceIntent.setData(Uri.parse(dataUrl));
        photoTimer = new Timer();
        musicTimer = new Timer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btnStart:
                if (mPlayer == null)
                    mPlayer = MediaPlayer.create(FixedModeActivity.this, R.raw.music);
                try {
                    //mPlayer.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //mPlayer.start();
                musicTimer.schedule(new PlayMusicTask(), 0, 10000);
                photoTimer.schedule(new SendTask(), 0, 10000);
                requestTimer.schedule(new RequestTask((ImageView)findViewById(R.id.imageView)), 0, 10000);
                break;
            case R.id.btnStop:
                stop();
                break;
            case R.id.btnMakePhoto:
//                Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(intent, REQUEST_LOAD_PHOTO);
                //dispatchTakePictureIntent();
                break;
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
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_LOAD_PHOTO = 2;

    private static String lastFileName;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        lastFileName = imageFileName;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        storageDir.mkdirs();
        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = image.getAbsolutePath(); //"file:" +
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri outputFileUri = Uri.fromFile(photoFile);
                //Uri photoURI = FileProvider.getUriForFile(this,"com.lapin.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());
            for (File temp : f.listFiles()) {
                if (temp.getName().equals(lastFileName)) {
                    f = temp;
                    File photo = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
                    //pic = photo;
                    break;
                }
            }

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),bmOptions);
            ImageView imageView = (ImageView)findViewById(R.id.imageView);
            bitmap = Bitmap.createScaledBitmap(bitmap,imageView.getWidth(),imageView.getHeight(),true);
            imageView.setImageBitmap(bitmap);
        } else if (requestCode == REQUEST_LOAD_PHOTO && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePath = { MediaStore.Images.Media.DATA };
            Cursor c = getContentResolver().query(selectedImage,filePath, null, null, null);

            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String picturePath = c.getString(columnIndex);
            c.close();

            Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
            ImageView imageView = (ImageView)findViewById(R.id.imageView);
            imageView.setImageBitmap(thumbnail);
        }
    }

    class PlayMusicTask extends TimerTask {
        public void run() {
//            if (mPlayer!=null && !mPlayer.isPlaying())
//                mPlayer.start();
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
