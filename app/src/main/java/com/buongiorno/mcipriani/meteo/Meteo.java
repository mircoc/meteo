package com.buongiorno.mcipriani.meteo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.support.v7.app.ActionBar;
import android.widget.Toast;

import meteo.mcipriani.com.buongiorno.meteo.R;


public class Meteo extends ActionBarActivity {

    private static final String LOGTAG = "MCMETEO";
    private static final String APIURL = "http://api.worldweatheronline.com/free/v1/weather.ashx";
    private static final String APIKEY = "531e2a67bb450906adf33e5db0cf995abd4442c0";

    private static final String PREF_NAME = "MCMETEO_PREF";

    public static final String VALUE_SEL_CITY = "SELECTED_CITY";

    public static final int CHOOSE_CITY_REQUEST = 1;

    private ImageView mIconWeather;
    private TextView mTitle;
    private TextView mDegree;

    private Menu mainMenu;

    private String selectedCity = "Florence";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meteo);

        mIconWeather = (ImageView) findViewById(R.id.imageView);
        mTitle = (TextView) findViewById(R.id.textViewTitle);
        mDegree = (TextView) findViewById(R.id.textViewDegree);

        LoadPreferences();

        requestUpdate();
    }

    public void ChangeCity(String newCity) {
        selectedCity = newCity;
        SavePreferences();
        requestUpdate();
    }

    public void SavePreferences() {
        // save station selected to shared preferences
        SharedPreferences settings = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(VALUE_SEL_CITY, selectedCity);
        // Commit the edits!
        editor.commit();

        Log.d(LOGTAG, "SharedPreferences saved from string as: " + selectedCity);
    }

    public void LoadPreferences() {

        // load from shared preferences
        SharedPreferences settings = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        selectedCity = settings.getString(VALUE_SEL_CITY, selectedCity);
    }

    private void requestUpdate() {
        new jsonDownloaderAsyncTask().execute(selectedCity);
    }

    private void showWeatherUpdate(WeatherInfo winfo) {
        Date now = new Date();

        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(now);   // assigns calendar to given date

        String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ITALIAN);
        String year = String.valueOf(calendar.get(Calendar.YEAR));

        String title = selectedCity + ", " + day + " " + month + " " + year;
        mDegree.setText(winfo.getDegree()+"°C");
        mTitle.setText(title);
        //mIconWeather.setImageBitmap(winfo.getIcon());

        int resID = getResources().getIdentifier("icon"+winfo.getCode(), "drawable",  getPackageName());
        mIconWeather.setImageResource(resID);
        Toast.makeText(this, "Using image: "+"icon"+winfo.getCode()+" ("+resID+")", Toast.LENGTH_SHORT).show();
    }

    protected class WeatherInfo {
        private String UrlImage;
        private String Degree;
        private Bitmap Icon;
        private String Code;

        public WeatherInfo(String urlImage, String degree, Bitmap icon, String code) {
            UrlImage = urlImage;
            Degree = degree;
            Icon = icon;
            Code = code;
        }

        public String getCode() { return Code; }

        public void setCode(String code) { Code = code; }

        public String getUrlImage() {
            return UrlImage;
        }

        public void setUrlImage(String urlImage) {
            UrlImage = urlImage;
        }

        public String getDegree() {
            return Degree;
        }

        public void setDegree(String degree) {
            Degree = degree;
        }

        public Bitmap getIcon() {
            return Icon;
        }

        public void setIcon(Bitmap icon) {
            Icon = icon;
        }
    }

    private WeatherInfo parseWeatherInfo(JSONObject json) {
        String urlImage = "";
        String degree = "";
        String code = "";
        try {
            JSONObject jData = json.getJSONObject("data")
                .getJSONArray("current_condition")
                .getJSONObject(0);

            urlImage = jData.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value");
            degree = jData.getString("temp_C");
            code = jData.getString("weatherCode");
        }
        catch (Exception ex) {
            Log.e(LOGTAG, "Error parsing json", ex);
        }

        return new WeatherInfo(urlImage, degree, null, code);
    }

    private String downloadUrl(String url, List<NameValuePair> params) {

        String response = "";

        try {
            // http client
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpEntity httpEntity = null;
            HttpResponse httpResponse = null;

            // appending params to url
            if (params != null) {
                String paramString = URLEncodedUtils
                        .format(params, "utf-8");
                url += "?" + paramString;
            }
            HttpGet httpGet = new HttpGet(url);

            httpResponse = httpClient.execute(httpGet);

            httpEntity = httpResponse.getEntity();
            response = EntityUtils.toString(httpEntity);
        }
        catch (IOException ioEx) {
            Log.e(LOGTAG, "Error getting Json from: "+url+" ", ioEx);
        }

        return response;
    }

    private JSONObject decodeJson(String jsonString) {
        JSONObject json = null;

        try {
            json = new JSONObject(jsonString);
        }
        catch (JSONException jEx) {
            Log.e(LOGTAG, "Error decoding json", jEx);
        }

        return json;
    }

    protected class jsonDownloaderAsyncTask extends AsyncTask<String, Void, WeatherInfo> {
        @Override
        protected WeatherInfo doInBackground(String... cities) {

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("q", cities[0]));
            params.add(new BasicNameValuePair("format", "json"));
            params.add(new BasicNameValuePair("lang", "it"));
            params.add(new BasicNameValuePair("key", APIKEY));

            String response = downloadUrl(APIURL, params);

            JSONObject json = decodeJson(response);

            WeatherInfo weatherInfo = parseWeatherInfo(json);

            /*
            // download the image
            try {
                InputStream in = new java.net.URL(weatherInfo.getUrlImage()).openStream();
                Bitmap icon = BitmapFactory.decodeStream(in);
                weatherInfo.setIcon(icon);
            } catch (Exception e) {
                Log.e(LOGTAG, "Error loading weather icon", e);
            }
            */

            return weatherInfo;
        }

        @Override
        protected void onPostExecute(WeatherInfo weatherInfo) {
            super.onPostExecute(weatherInfo);
            if (weatherInfo != null) {
                showWeatherUpdate(weatherInfo);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.meteo, menu);
        mainMenu = menu; // save my menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent prefIntent = new Intent(Meteo.this, Settings.class);
            prefIntent.putExtra(VALUE_SEL_CITY, selectedCity);

            startActivityForResult(prefIntent, CHOOSE_CITY_REQUEST);
            return true;
        }

        if (id == R.id.action_update) {
            requestUpdate();
            Toast.makeText(this, R.string.msg_updating, Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_info) {


            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast_layout,
                    (ViewGroup) findViewById(R.id.toast_layout_root));

            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText("Meteo Demo App\nby Mirco Cipriani\n14/09/2014");

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();

            //Toast toast = Toast.makeText(this, "Meteo Demo App\nby Mirco Cipriani\n14/09/2014", Toast.LENGTH_LONG);
            //toast.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_CITY_REQUEST) {
            if (resultCode == RESULT_OK) {
                ChangeCity(data.getStringExtra(VALUE_SEL_CITY));
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        /*
            Show my menu if press hardware menu
         */
        if(event.getAction() == KeyEvent.ACTION_UP){
            switch(keyCode) {
                case KeyEvent.KEYCODE_MENU:

                    mainMenu.performIdentifierAction(R.id.a_More, 0);
                    Log.d("Menu", "menu button pressed");
                    return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }
}
