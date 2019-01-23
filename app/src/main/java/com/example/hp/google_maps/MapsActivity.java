package com.example.hp.google_maps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener

{

    RelativeLayout uplayout;
    Animation uptodown,lefttoright;
    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Location lastlocation;
    ProgressBar progressBar;
    TextView onoffline;
    private Marker currentlocmarker;
    public static final int REQUEST_LOCATION_CODE=99;
    List<Address> addressList=null;
    LatLng latLngDest;
    Double currentLatitude,currentLongitude,endlatitude,endlongitude;
    EditText tf_location;
    TextView tv_currentLoc;
    String currentLocation;
    ArrayList markerPoints = new ArrayList();
    LatLng latLng,m;
    Button proceedbutton;
    String Searchlocation="";
    MarkerOptions markerOptions,mo;
    MarkerOptions modest;
    int count=0,check=0;
    AlertDialog alert;
    int onoroffloccheck=0,i=0,b=1,d=1;
    float distance,distancecurrdest;




 //   private TextView mValueLat,mValueLng;
    private Firebase mRefLat,mRefLng;
    Double[] dLng=new Double[10];
    Double[] dLat=new Double[10];
    Marker friendmarker;
    LatLng dLatLng;
    float results[] = new float[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            checkLocationPermission();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);





        tv_currentLoc=findViewById(R.id.tv_currentLocation);
        tf_location=findViewById(R.id.TF_location);
        proceedbutton=findViewById(R.id.button_proceed);
        onoffline=findViewById(R.id.onoffline);
        progressBar = findViewById(R.id.progressbar);
        uplayout=findViewById(R.id.uplayout);
        uptodown = AnimationUtils.loadAnimation(this,R.anim.uptodown);
        lefttoright = AnimationUtils.loadAnimation(this,R.anim.lefttoright);



    }

    @Override
    protected void onPause() {
        super.onPause();
        if (alert!=null)
        alert.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkCheck();
        displayLocationSettingsRequest(this);

    }

    private void NetworkCheck() {

        if (isNetworkAvailable(this)){
            onoffline.setBackgroundResource(R.color.online);

        }
        else {
            check=0;
            AlertDialog.Builder builder=new AlertDialog.Builder(MapsActivity.this);
            builder.setTitle("NO INTERNET");
            builder.setMessage("this app requires internet to work!");
            builder.setCancelable(true);
            onoffline.setBackgroundResource(R.color.offline);

            alert=builder.create();
            alert.show();
        }
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager=(ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo()!=null
                &&connectivityManager.getActiveNetworkInfo().isConnected();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_LOCATION_CODE:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                    {
                        if (client==null)
                        {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                  }
                  else
                {
                    Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                }
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
            proceedbutton.setVisibility(View.VISIBLE);
            proceedbutton.setAnimation(lefttoright);
            uplayout.setAnimation(uptodown);
            tf_location.setAnimation(uptodown);




    }

    protected synchronized void buildGoogleApiClient(){
        client=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        client.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest=new LocationRequest();
        locationRequest.setInterval(3500);
        locationRequest.setFastestInterval(3500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);

        }
    }

    @Override
    public void onConnectionSuspended(int i) {


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


    }

    @Override
    public void onLocationChanged(Location location) {
        mMap.clear();


        count++;

        if (currentlocmarker != null) {
            currentlocmarker.remove();
        }

        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        Geocoder geocoder = new Geocoder(this);
        try {
            addressList = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);
            currentLocation = addressList.get(0).getAddressLine(0);
            tv_currentLoc.setText(currentLocation);
            if (count > 1)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12), 1200, null);

        } catch (IOException e) {
            e.printStackTrace();

        }

        /*markerOptions = new MarkerOptions();
        markerOptions.position(latLng);

        markerOptions.title(currentLocation);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currentlocmarker = mMap.addMarker(markerOptions);*/


        if (isNetworkAvailable(this)) {
            onoffline.setBackgroundResource(R.color.online);
        } else {
            onoffline.setBackgroundResource(R.color.offline);
            Toast.makeText(this, "offline", Toast.LENGTH_SHORT).show();
        }


        for (b = 1; b < 4; b++) {
            mRefLat = new Firebase("https://firead-f1f62.firebaseio.com/" + b + "/Lat");
            mRefLat.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    dLat[b] = dataSnapshot.getValue(Double.class);

                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    count = 0;
                    mMap.clear();

                }
            });
        }

        for (d = 1; d < 4; d++) {
            mRefLng = new Firebase("https://firead-f1f62.firebaseio.com/" + d + "/Lng");
            mRefLng.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    dLng[d] = dataSnapshot.getValue(Double.class);

                }


                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    mMap.clear();
                    count = 0;
                }
            });
        }
        b=1;
        d=1;


        if (count >= 2) {
           for (b=1,d = 1; d==b; d++,b++) {
               for (b=1;b<4;b++)
               {
                    if (dLat[b]!=null&&dLng[d]!=null)
                    {
                    Location.distanceBetween(currentLatitude, currentLongitude, dLat[b], dLng[d], results);

                    if (results[0] <= 100000) {

                        mMap.addMarker(new MarkerOptions().position(new LatLng(dLat[b], dLng[d])).title("friend "+b+d).icon(BitmapDescriptorFactory.fromResource(R.drawable.ola)));

                    }
                }

                }
        }
    }

    }






    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_CODE);
            }
            else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_CODE);
            }
            return false;
        }
        else
            return true;
    }


    public void onClickSearch(View view) {
        displayLocationSettingsRequest(this);
        progressBar.setVisibility(View.VISIBLE);

        Snackbar.make(view,"please wait!",Snackbar.LENGTH_SHORT)
                .setAction("Action",null).show();


        NetworkCheck();


        modest=new MarkerOptions();
        Searchlocation=tf_location.getText().toString();
        if (Searchlocation.length()!=0) {
            if (isNetworkAvailable(this)){
                mMap.clear();


            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(Searchlocation, 1);
                if (addressList.size() != 0) {

                    check++;
                    Address myAddress = addressList.get(0);
                    latLngDest = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
                    modest.position(latLngDest);
                    modest.title(Searchlocation);
                    mMap.addMarker(modest);
                    if (markerOptions!=null)
                    mMap.addMarker(markerOptions);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngDest, 18));


                    endlatitude = myAddress.getLatitude();
                    endlongitude = myAddress.getLongitude();


                    float resultsdistance[]=new float[10];
                    Location.distanceBetween(currentLatitude,currentLongitude,endlatitude,endlongitude,resultsdistance);
                    distancecurrdest=resultsdistance[0];
                    Toast.makeText(MapsActivity.this, (int) distancecurrdest / 1000 + "km away", Toast.LENGTH_SHORT).show();




                    markerPoints.add(latLngDest);
                    markerPoints.add(latLng);
                    LatLng origin = (LatLng) markerPoints.get(0);
                    LatLng dest = (LatLng) markerPoints.get(1);


                    String url = getUrl(origin, dest);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();
                    FetchUrl.execute(url);
                    markerPoints.clear();
                    proceedbutton.setClickable(true);

                } else {
                    Toast.makeText(this, "no such address", Toast.LENGTH_SHORT).show();
                    check = 0;
                    proceedbutton.setClickable(false);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        }
        else {
            Toast.makeText(this,"How about MARS?",Toast.LENGTH_SHORT).show();
            Snackbar.make(view,"please enter some proper address!",Snackbar.LENGTH_SHORT)
                    .setAction("Action",null).show();
            check=0;
            mMap.clear();
        }
        progressBar.setVisibility(View.INVISIBLE);
    }


    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    public void proceedbuttonclicked(View view) {

        if (isNetworkAvailable(this)){
            if (latLngDest!=null) {
                displayLocationSettingsRequest(this);
                Toast.makeText(this, "NEXT", Toast.LENGTH_SHORT).show();


            }
            else
                Toast.makeText(this,"Search for destination",Toast.LENGTH_SHORT).show();
        }
        else {
            NetworkCheck();
        }
    }


    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }


    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                DirectionsJSONParser parser = new DirectionsJSONParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(15);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute", "onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
                Toast.makeText(MapsActivity.this,"path not drawn,retry..",Toast.LENGTH_SHORT).show();
            }

        }





    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        /*locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(50000);
        locationRequest.setFastestInterval(50000 / 2);*/

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        onoroffloccheck=1;
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        onoroffloccheck=0;

                        try {
                            status.startResolutionForResult(MapsActivity.this, REQUEST_LOCATION_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            return;
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setNegativeButton("No", null).show();
    }


}