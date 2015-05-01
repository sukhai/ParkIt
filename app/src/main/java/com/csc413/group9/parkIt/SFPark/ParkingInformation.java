package com.csc413.group9.parkIt.SFPark;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import com.csc413.group9.parkIt.MainActivity;
import com.csc413.group9.parkIt.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/**
 * This class handles the parking information around the device's current location. This class can
 * also highlight the street parking information on the map.
 *
 * Created by Su Khai Koh on 4/19/15.
 */
public class ParkingInformation {

    // Constants for HTTP request
    private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;
    private static final int SOCKET_BUFFER_SIZE = 8192;

    // Request parameters
    private static final String RADIUS = "1000.0";              // 100 miles within the SFPark default latitude and longitude
    private static final String RESPONSE = "json";              // Request data in JSON format
    private static final String VERSION = "1.0";                // SFPark data version

    // SFPark URL
    private static final String SERVICE_URL = "http://api.sfpark.org/sfpark/rest/availabilityservice?";

    private MainActivity mMainActivity;                         // The activity that contains the main context
    private GoogleMap mMap;                                     // The Google map
    private String uri;                                         // The URI of the SFPark
    private String uriContent;                                  // The content of the SFPark data
    private ArrayList<ParkingLocation> onStreetParkings;        // A list of on-street parking locations
    private ArrayList<ParkingLocation> offStreetParkings;       // A list of off-street parking locations
    private ArrayList<Object> onStreetParkingIcons;             // A list of on-street parking location icons
    private ArrayList<Marker> offStreetParkingIcons;            // A list of off-street parking location icons
    private boolean sfParkDataReady = false;                    // Is the SFPark data ready for read?
    private boolean highlightedOnStreetParking;                 // Is the on-street parking location highlighted?
    private boolean highlightedOffStreetParking;                // Is the off-street parking location highlighted?

    public ParkingInformation(MainActivity mainActivity, GoogleMap map) {

        mMainActivity = mainActivity;
        mMap = map;

        getSFParkData();          // Get data from SFPark
    }

    /**
     * Highlight on-street and off-street parking locations based on the given parameters.
     * @param drawOnStreet whether to draw on-street parking or not
     * @param drawOffStreet whether to draw off-street parking or not
     */
    public void highlightStreet(boolean drawOnStreet, boolean drawOffStreet) {

        if (sfParkDataReady && drawOnStreet && !highlightedOnStreetParking) {
            highlightOnStreetParking();
            highlightedOnStreetParking = true;
        } else if (sfParkDataReady && !drawOnStreet) {
            removeOnStreetHighlighted();
            highlightedOnStreetParking = false;
        }

        if (sfParkDataReady && drawOffStreet && !highlightedOffStreetParking) {
            highlightOffStreetParking();
            highlightedOffStreetParking = true;
        } else if (sfParkDataReady && !drawOffStreet) {
            removeOffStreetHighlighted();
            highlightedOffStreetParking = false;
        }

    }

    /**
     * Highlight on-street parking on the map.
     */
    private void highlightOnStreetParking() {

        for (int i = 0; i < onStreetParkingIcons.size(); i++) {

            Object icon = onStreetParkingIcons.get(i);

            if (icon instanceof Polyline) {
                Polyline polyline = (Polyline) icon;
                polyline.setVisible(true);
            } else {
                Circle circle = (Circle) icon;
                circle.setVisible(true);
            }
        }
    }

    /**
     * Highlight the off-street parking (parking lot building) on the map.
     */
    private void highlightOffStreetParking() {

        for (int i = 0; i < offStreetParkingIcons.size(); i++) {
            offStreetParkingIcons.get(i).setVisible(true);
        }
    }

    /**
     * Remove the on-street parking highlight from the map.
     */
    private void removeOnStreetHighlighted() {

        for (int i = 0; i < onStreetParkingIcons.size(); i++) {

            Object icon = onStreetParkingIcons.get(i);

            if (icon instanceof Polyline) {
                Polyline polyline = (Polyline) icon;
                polyline.setVisible(false);
            } else {
                Circle circle = (Circle) icon;
                circle.setVisible(false);
            }
        }
    }

    /**
     * Remove the off-street parking highlight from the map.
     */
    private void removeOffStreetHighlighted() {

        for (int i = 0; i < offStreetParkingIcons.size(); i++) {
            offStreetParkingIcons.get(i).setVisible(false);
        }
    }

    /**
     * Initialize the on-street parking locations and get ready to get the street highlight.
     */
    private void initializeOnStreetParking() {

        CircleOptions circleOptions = new CircleOptions()
                .radius(5f)
                .strokeColor(Color.GREEN)
                .fillColor(Color.GREEN)
                .visible(false);

        for (int i = 0; i < onStreetParkings.size(); i++) {

            Location[] locations = onStreetParkings.get(i).getLocation();

            // If there are 2 points, then draw a line from point1 to point2
            if (locations.length == 2) {
                // Draw a line
                PolylineOptions lineOptions = new PolylineOptions()
                        .add(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()))
                        .add(new LatLng(locations[1].getLatitude(), locations[1].getLongitude()))
                        .color(Color.GREEN)
                        .width(5f)
                        .visible(false);

                Polyline polyline = mMap.addPolyline(lineOptions);
                onStreetParkingIcons.add(polyline);

            } else {
                // Draw a circle
                circleOptions.center(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()));
                Circle circle = mMap.addCircle(circleOptions);
                onStreetParkingIcons.add(circle);
            }
        }
    }

    /**
     * Initialize off-street parking locations (parking lot buildings) and get ready to get the
     * street highlight.
     */
    private void initializeOffStreetParking() {

        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_offstreet_parking))
                .visible(false);

        for (int i = 0; i < offStreetParkings.size(); i++) {

            ParkingLocation location = offStreetParkings.get(i);

            String name = location.getName();
            String description = location.getDescription();
            ParkingLocation.RateSchedule[] rateSchedules = location.getRateSchedule();
            StringBuilder snippet = new StringBuilder();

            for (int j = 0; j < rateSchedules.length; j++) {

                ParkingLocation.RateSchedule rate = rateSchedules[j];

                if (!rate.getBeginTime().equals("")) {
                    snippet.append(rate.getBeginTime() + "-" + rate.getEndTime() + " : $" +
                            rate.getRate() + " " + rate.getRateQualifier() + "\n");
                }
            }

            Location[] locations = location.getLocation();

            markerOptions.position(new LatLng(locations[0].getLatitude(), locations[0].getLongitude()));
            markerOptions.title(name);
            markerOptions.snippet(description + "%" + snippet);

            offStreetParkingIcons.add(mMap.addMarker(markerOptions));
        }
    }

    /**
     * Determine if the SFPark data is ready.
     * @return true if the SFPark data is ready, otherwise false
     */
    public boolean isSFParkDataReady() {
        return sfParkDataReady;
    }

    /**
     * Get the SFPark data from the SFPark URI. The fetching and parsing data process will be
     * done in a different thread.
     */
    private void getSFParkData() {

        // Construct the URI
        uri = SERVICE_URL + "radius=" + RADIUS +
                            "&response=" + RESPONSE +
                            "&version=" + VERSION +
                            "&pricing=yes";

        LoadSFParkDataTask ld = new LoadSFParkDataTask();
        ld.execute("String");
    }

    /**
     * Initialize onStreetParkings, offStreetParkings, onStreetParkingIcons, and
     * offStreetParkingIcons arrays.
     */
    private void initializeArrays() {

        if (onStreetParkings == null)
            onStreetParkings = new ArrayList<ParkingLocation>();
        else
            onStreetParkings.clear();

        if (offStreetParkings == null)
            offStreetParkings = new ArrayList<ParkingLocation>();
        else
            offStreetParkings.clear();

        if (onStreetParkingIcons == null)
            onStreetParkingIcons = new ArrayList<Object>();
        else
            onStreetParkingIcons.clear();

        if (offStreetParkingIcons == null)
            offStreetParkingIcons = new ArrayList<Marker>();
        else
            offStreetParkingIcons.clear();
    }

    /**
     * Load SFPark data from the SFPark website. Must be called by getSFParkData().
     */
    private void loadSFParkData() {

        if (uriContent != null)
            return;

        try {
            uriContent = getURIContent(uri);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Read the SFPark data. loadSFParkData must be called first before this method.
     */
    private void readSFParkData() {

        try {
            if (uriContent == null || uriContent.equals(""))
                return;

            JSONObject rootObject = new JSONObject(uriContent);
            JSONArray jsonAVL = rootObject.has("AVL") ? rootObject.getJSONArray("AVL") : null;

            if (jsonAVL == null)
                return;

            // Initialize both onStreetParkings and offStreetParkings array list
            initializeArrays();

            for (int i = 0; i < jsonAVL.length(); i++) {
                // Get the AVL value (a location JSON data) and parse it
                JSONObject location = jsonAVL.getJSONObject(i);
                ParkingLocation parkingLocation = new ParkingLocation(location);

                if (parkingLocation.isOnStreet())
                    onStreetParkings.add(parkingLocation);
                else
                    offStreetParkings.add(parkingLocation);

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get the URI content, which is the data from the SFPark.
     * @param uri the SFPark URI
     * @return a string that contains the content (SFPark data) from the SFPark website
     * @throws Exception any error when doing HTTP request
     */
    private String getURIContent(String uri) throws Exception {

        try {
            HttpGet request = new HttpGet();
            request.setURI(new URI(uri));
            request.addHeader("Accept-Encoding", "gzip");

            final HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 30 * SECOND_IN_MILLIS);
            HttpConnectionParams.setSoTimeout(params, 30 * SECOND_IN_MILLIS);
            HttpConnectionParams.setSocketBufferSize(params, SOCKET_BUFFER_SIZE);

            final DefaultHttpClient client = new DefaultHttpClient(params);

            client.addResponseInterceptor(new HttpResponseInterceptor() {

                @Override
                public void process(HttpResponse response, HttpContext context) {

                    final HttpEntity entity = response.getEntity();
                    final Header encoding = entity.getContentEncoding();

                    if (encoding != null) {
                        for (HeaderElement element : encoding.getElements()) {
                            if (element.getName().equalsIgnoreCase("gzip")) {
                                response.setEntity(new InflatingEntity(response.getEntity()));
                                break;
                            }
                        }
                    }
                }
            });

            return client.execute(request, new BasicResponseHandler());

        } finally {
            // No clean up code is needed
        }
    }

    /**
     * This class allows to perform background operations when reading and parsing the SFPark data.
     */
    private class LoadSFParkDataTask extends AsyncTask<String, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {

            try {
                progressDialog = new ProgressDialog(mMainActivity);
                progressDialog.setTitle("Please wait");
                progressDialog.setMessage("Displaying parking data ...");
                progressDialog.show();
                progressDialog.setCancelable(false);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(String... urls) {
            // Load then read the SFPark data in the background
            loadSFParkData();

            readSFParkData();

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {

            initializeOnStreetParking();
            initializeOffStreetParking();

            sfParkDataReady = true;

            highlightStreet(true, true);

            progressDialog.dismiss();
        }
    }

    /**
     * Base class for wrapping entities. Keeps a wrappedEntity and delegates all calls to it.
     */
    private static class InflatingEntity extends HttpEntityWrapper {

        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }
}
