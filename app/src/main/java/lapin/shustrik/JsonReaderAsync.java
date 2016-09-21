package lapin.shustrik;

import android.os.AsyncTask;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

/**
 * Created by lapin on 20.09.2016.
 */
public class JsonReaderAsync extends AsyncTask<URL, Void, JSONObject> {
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
