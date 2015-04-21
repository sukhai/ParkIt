package com.csc413.group9.parkIt.SFPark;

import android.graphics.Color;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.csc413.group9.parkIt.MainActivity;
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
 * This class handles the parking information around the device's current location.
 *
 * Created by Su Khai Koh on 4/19/15.
 */
public class ParkingInformation {

    // On/Off constants
    private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;
    private static final int SOCKET_BUFFER_SIZE = 8192;

    // Request parameters
    public static final String TYPE_ON_STREET = "on";
    public static final String TYPE_OFF_STREET = "off";
    public static final String PRICE_ON = "yes";
    public static final String PRICE_OFF = "no";

    // Request parameters
    private static final String RADIUS = "1000.0";
    private static final String RESPONSE = "json";
    private static final String VERSION = "1.0";

    // SFPark URL
    private static final String SERVICE_URL = "http://api.sfpark.org/sfpark/rest/availabilityservice?";

    private MainActivity mMainActivity;
    private String uri;
    private String uriContent;
    private ArrayList<ParkingLocation> onStreetParkings;
    private ArrayList<ParkingLocation> offStreetParkings;
    private Object[] onStreetMarkers;
    private Marker[] offStreetMarkers;
    private boolean dataReady;
    private boolean onStreetDrawn;
    private boolean offStreetDrawn;

    public ParkingInformation(MainActivity mainActivity, String[] lastKnownLocation) {

        mMainActivity = mainActivity;

        float latitude = Float.parseFloat(lastKnownLocation[1]);
        float longitude = Float.parseFloat(lastKnownLocation[2]);

        getData(latitude, longitude, "", true);
    }

    public ArrayList<ParkingLocation> getOnStreetParkingLocations() {
        return onStreetParkings;
    }

    public ArrayList<ParkingLocation> getOffStreetParkingLocations() {
        return offStreetParkings;
    }

    public void highlightOnStreetParking(GoogleMap map) {

        if (onStreetDrawn)
            return;

        CircleOptions circleOptions = new CircleOptions()
                .radius(1f)
                .strokeColor(Color.GREEN)
                .fillColor(Color.GREEN);

        onStreetMarkers = new Object[onStreetParkings.size()];

        for (int i = 0; i < onStreetParkings.size(); i++) {

            LatLng[] latLngs = onStreetParkings.get(i).getLatLng();

            // If there are 2 points, then draw a line from point1 to point2
            if (latLngs.length == 2) {
                // Draw a line
                PolylineOptions lineOptions = new PolylineOptions()
                        .add(latLngs[0])
                        .add(latLngs[1])
                        .color(Color.GREEN)
                        .width(1f);

                Polyline polyline = map.addPolyline(lineOptions);

            } else {
                // Draw a circle
                circleOptions.center(latLngs[0]);
                Circle circle = map.addCircle(circleOptions);

                onStreetMarkers[i] = circle;
            }
        }

        onStreetDrawn = true;
    }

    public void highlightOffStreetParking(GoogleMap map) {


        if (offStreetDrawn)
            return;

        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        offStreetMarkers = new Marker[offStreetParkings.size()];

        for (int i = 0; i < offStreetParkings.size(); i++) {

            LatLng[] latLngs = offStreetParkings.get(i).getLatLng();
            markerOptions.position(latLngs[0]);

            Marker marker = map.addMarker(markerOptions);
            offStreetMarkers[i] = marker;
        }

        offStreetDrawn = true;
    }

    public void removeOnStreetHighlight() {

        for (Object marker : onStreetMarkers) {
            if (marker instanceof Circle) {
                Circle circle = (Circle) marker;
                circle.remove();
            } else {
                Polyline line = (Polyline) marker;
                line.remove();
            }
        }
    }

    public void removeOffStreetHighlight() {

        for (Marker marker : offStreetMarkers)
            marker.remove();
    }

    public boolean isDataReady() {
        return dataReady;
    }

    public void getData(float latitude, float longitude, String streetType, boolean showPrice) {

        uri = SERVICE_URL + "radius=" + RADIUS +
                            "&response=" + RESPONSE +
                            "&lat=" + latitude +
                            "&long=" + longitude +
                            "&version=" + VERSION +
                            "&pricing=" + (showPrice ? PRICE_ON : PRICE_OFF) +
                ((streetType == null || streetType.equals("")) ? "" : "&type=" + streetType);

        LoadDataTask ld = new LoadDataTask();
        ld.execute("String");
    }

    private void initializeArrays() {

        if (onStreetParkings == null)
            onStreetParkings = new ArrayList<ParkingLocation>();
        else
            onStreetParkings.clear();

        if (offStreetParkings == null)
            offStreetParkings = new ArrayList<ParkingLocation>();
        else
            offStreetParkings.clear();
    }

    public boolean loadData() {

        if (uriContent != null) {
            return true;
        }

        boolean didLoad = false;

        try {
            uriContent = getURIContent(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (uriContent != null) {
            didLoad = true;
        }

        return didLoad;
    }

    public void readData() {

        try {

            if (uriContent == null || uriContent == "") {
                return;
            }

            JSONObject rootObject = new JSONObject(uriContent);
            JSONArray jsonAVL = rootObject.has("AVL") ? rootObject.getJSONArray("AVL") : null;

            if (jsonAVL == null)
                return;

            initializeArrays();

            for (int i = 0; i < jsonAVL.length(); i++) {
                JSONObject location = jsonAVL.getJSONObject(i);
                ParkingLocation parkingLocation = new ParkingLocation(location);

                if (parkingLocation.isOnStreet())
                    onStreetParkings.add(parkingLocation);
                else
                    offStreetParkings.add(parkingLocation);

            }

            dataReady = true;

            // Update the map
            mMainActivity.updateMap();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

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

        }
    }

    private class LoadDataTask extends AsyncTask<String, Void, Void> {

        protected void onPreExecute() {
            Toast.makeText(mMainActivity,
                    "Fetching data ...",
                    Toast.LENGTH_SHORT)
                    .show();
        }

        protected Void doInBackground(String... urls) {
            loadData();
            return null;
        }

        protected void onPostExecute(Void unused) {

            try {
                readData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
