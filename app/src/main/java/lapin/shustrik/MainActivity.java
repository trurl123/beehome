package lapin.shustrik;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static String apiUrl = "http://10.0.2.2:88/test.php";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button btn1 = (Button)findViewById(R.id.btnFixedMode);
        Button btn2 = (Button)findViewById(R.id.btnMobileMode);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
    }

    public class JsonReaderAsync extends AsyncTask<URL, Void, JSONObject>
    {
        @Override
        protected JSONObject doInBackground(URL... urls) {
            HttpWorker httpWorker = new HttpWorker();
            return httpWorker.getJSONFromUrl(urls[0]);
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            String x = null;
            try {
                x = json.getString("x");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            //TextView textView = (TextView) findViewById(R.id.textView2);
            //textView.setText(x);
        }
    }

    public class JsonWriterAsync extends AsyncTask<JSONObject, Void, Void>
    {
        private URL url;

        public JsonWriterAsync(URL url)
        {
            this.url = url;
        }

        @Override
        protected Void doInBackground(JSONObject... objs) {
            HttpWorker httpWorker = new HttpWorker();
            httpWorker.postJSONToUrl(url, objs[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void dummy) {
            //TextView textView = (TextView) findViewById(R.id.textView2);
            //textView.setText("post finished");
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnMobileMode:
                Intent myIntent = new Intent(this, LoginActivity.class);
                this.startActivity(myIntent);
                break;
            case R.id.btnFixedMode:
                Intent myIntent2 = new Intent(this, FixedModeActivity.class);
                this.startActivity(myIntent2);
                try {
//                    JsonWriterAsync writerAsync = new JsonWriterAsync(new URL("http://10.0.2.2:88/test.php"));
//                    JSONObject jsonObject = new JSONObject();
//                    jsonObject.put("hello","world");
//                    writerAsync.execute(jsonObject);
                    //JsonReaderAsync jsonParser = new JsonReaderAsync();
//                    String urlStr = "http://info.24gim.ru/test.json";
//                    try {
//                        jsonParser.execute(new URL(urlStr));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Log.e("parsing error", "Error converting result " + e.toString() + " from " + urlStr);
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
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


}
