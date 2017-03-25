package com.example.gnana.funshine;

import android.*;
import android.Manifest;
import android.app.DownloadManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.gnana.funshine.Model.DailyWeatherReport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener , LocationListener {

    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast";
    final String URL_COORDS = "?lat=";
    final String URL_UNITS = "&units=metric";
    final String URL_API_KEY = "&APPID=f8f318b8ad41dabc878dec49cc676c91";
    final int Permission_Location = 111;

    private ImageView weatherIcon;
    private ImageView weatherminiIcon;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView country;
    private TextView weatherDesc;

    private GoogleApiClient mGoogleApiClient;
    private ArrayList<DailyWeatherReport> weatherReport = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this,this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        weatherIcon = (ImageView)findViewById(R.id.weatherIcon);
        weatherminiIcon = (ImageView)findViewById(R.id.weatherminiIcon);
        weatherDate = (TextView)findViewById(R.id.weatherDate);
        currentTemp = (TextView)findViewById(R.id.currentTemp);
        lowTemp = (TextView)findViewById(R.id.lowTemp);
        country = (TextView)findViewById(R.id.country);
        weatherDesc = (TextView)findViewById(R.id.weatherDesc);

    }

    public void  downloadData(Location location){
        final String UrlCoords = URL_COORDS + location.getLatitude() + "&lon=" + location.getLongitude();
        final String url = URL_BASE + UrlCoords + URL_UNITS + URL_API_KEY;
        Log.v("Url","url:" + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
               try{
                   JSONObject city = response.getJSONObject("city");
                   String cityname = city.getString("name");
                   String country = city.getString("country");
                   JSONArray list = response.getJSONArray("list");
                   for (int x = 0; x <= 4 ; x++ ){
                       JSONObject obj = list.getJSONObject(x);
                       JSONObject main = obj.getJSONObject("main");
                       Double temp = main.getDouble("temp");
                       Double temp_min = main.getDouble("temp_min");
                       Double temp_max = main.getDouble("temp_max");
                       JSONArray weatherdata = obj.getJSONArray("weather");
                       JSONObject weather = weatherdata.getJSONObject(0);
                       String desc = weather.getString("main");
                       String rawDate = obj.getString("dt_txt");

                       DailyWeatherReport report = new DailyWeatherReport(cityname,country,temp.intValue(),temp_max.intValue(),temp_min.intValue(),desc,rawDate);
                       weatherReport.add(report);
                   }

               } catch (JSONException e){
                    Log.v("JSON","Exc: " + e.getLocalizedMessage());
               }
                    updateUI();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("Funshine","Err:"+ error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    public  void  updateUI(){
        if(weatherReport.size() > 0){
            DailyWeatherReport report = weatherReport.get(0);

            switch (report.getWeather()){
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherminiIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case  DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherminiIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;
                case  DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    weatherminiIcon.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherminiIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
            }

            weatherDate.setText("Today ,May 1");
            currentTemp.setText(Integer.toString(report.getMaxTemp()));
            lowTemp.setText(Integer.toString(report.getMinTemp()));
            country.setText(report.getCityName()+","+report.getCountry());
            weatherDesc.setText(report.getWeather());

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        downloadData(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Permission_Location);
        }else {
            startLocationServices();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void startLocationServices(){
        try{
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,req,this);
        } catch(SecurityException exception){

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case Permission_Location: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startLocationServices();
                } else{
                    Toast.makeText(this,"I can't give you weather until you give me permission",Toast.LENGTH_LONG).show();
                }
            }

        }
    }
}

