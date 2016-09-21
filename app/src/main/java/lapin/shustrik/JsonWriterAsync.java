package lapin.shustrik;

import android.os.AsyncTask;
import org.json.JSONObject;

import java.net.URL;

/**
 * Created by lapin on 20.09.2016.
 */
public class JsonWriterAsync extends AsyncTask<JSONObject, Void, Void> {
    private URL url;

    public JsonWriterAsync(URL url) {
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
