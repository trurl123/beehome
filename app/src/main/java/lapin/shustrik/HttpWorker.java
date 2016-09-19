package lapin.shustrik;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class HttpWorker {


    // constructor
    public HttpWorker() {

    }

    public void postJSONToUrl(URL url, JSONObject obj) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            OutputStream stream = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            writer.write(obj.toString());
            writer.flush();
            writer.close();
            int responseCode = conn.getResponseCode();
            Log.e("response", "code = " + responseCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJSONFromUrl(URL url) {

        JSONObject json = null;
        String outPut = "";
        InputStream is = null;
        // Making the HTTP request
        try {
            is = url.openStream();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    is, "utf-8"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = in.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            outPut = sb.toString();
            Log.e("JSON", outPut);
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }

        try {
            json = new JSONObject(outPut);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }

        // return JSON String
        return json;
    }
}