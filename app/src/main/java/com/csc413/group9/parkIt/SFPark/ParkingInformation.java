package com.csc413.group9.parkIt.SFPark;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.csc413.group9.parkIt.Database.DatabaseHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

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
 * This class handles the parking information around the current device.
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
    private static final String RADIUS = "0.25";
    private static final String RESPONSE = "json";
    private static final String VERSION = "1.0";

    // SFPark URL
    private static final String SERVICE_URL = "http://api.sfpark.org/sfpark/rest/availabilityservice?";

    // Marker options

    private Context mContext;
    private GoogleMap mMap;
    private String uri;
    private String uriContent;
    private ArrayList<SpaceAvailable> onStreetParkings;
    private ArrayList<SpaceAvailable> offStreetParkings;
    private Marker[] onStreetMarkers;
    private Marker[] offStreetMarkers;
    private HttpGet request;
    private boolean dataReady;

    public ParkingInformation(Context context, String[] lastKnownLocation) {

        mContext = context;

        float latitude = Float.parseFloat(lastKnownLocation[1]);
        float longitude = Float.parseFloat(lastKnownLocation[2]);

        getData(latitude, longitude, "", true);
    }

    public void highlightOnStreetParking(GoogleMap map) {


    }

    public void highlightOffStreetParking(GoogleMap map) {

    }

    public void removeOnStreetHighlight() {

        for (Marker marker : onStreetMarkers)
            marker.remove();
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

    private String getURIContent(String uri) throws Exception {

        try {
            request = new HttpGet();
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

    private void initializeArrays() {

        if (onStreetParkings == null)
            onStreetParkings = new ArrayList<SpaceAvailable>();
        else
            onStreetParkings.clear();

        if (offStreetParkings == null)
            offStreetParkings = new ArrayList<SpaceAvailable>();
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
            // Get the content of the URI
    //        uriContent = getURIContent(uri);

            if (uriContent == null || uriContent == "") {
                return;
            }

            System.out.println(uri);

        //    JSONArray jsonAVL = null;

            JSONObject rootObject = new JSONObject(uriContent);
            JSONArray jsonAVL = rootObject.has("AVL") ? rootObject.getJSONArray("AVL") : null;

            if (jsonAVL == null)
                return;

            initializeArrays();

            for (int i = 0; i < jsonAVL.length(); i++) {
                JSONObject space = jsonAVL.getJSONObject(i);
                SpaceAvailable spaceAvailable = new SpaceAvailable(space);

                if (spaceAvailable.isOnStreet())
                    onStreetParkings.add(spaceAvailable);
                else
                    offStreetParkings.add(spaceAvailable);

            }

            dataReady = true;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class LoadDataTask extends AsyncTask<String, Void, Void> {

        protected void onPreExecute() {
            Toast.makeText(mContext,
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
