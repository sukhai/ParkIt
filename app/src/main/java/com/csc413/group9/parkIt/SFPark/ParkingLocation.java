package com.csc413.group9.parkIt.SFPark;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represent a parking location on the map. This parking location may or may not have the
 * information like type of parking (on/off street), name of parking location, description,
 * phone number, latitude, longitude, operation hours schedule, and/or rate schedule.
 *
 * Created by Su Khai Koh on 4/20/15.
 */
public class ParkingLocation {

    /**
     * The value for a rate schedule indicates that the parking location is having a street cleaning.
     */
    public static final String VALUE_STREET_SWEEP      = "Str sweep";

    /**
     * The value for a rate schedule indicates that the parking location is free.
     */
    public static final String VALUE_NO_CHARGE         = "No charge";

    // General keys
    private static final String KEY_TYPE               = "TYPE";
    private static final String KEY_NAME               = "NAME";
    private static final String KEY_DESCRIPTION        = "DESC";
    private static final String KEY_TELEPHONE          = "TEL";
    private static final String KEY_LOCATION           = "LOC";
    private static final String KEY_OPERATION_HOURS    = "OPHRS";
    private static final String KEY_RATES              = "RATES";
    private static final String KEY_DAY                = "DAY";

    // Operating hours keys, for off-street parking only
    private static final String KEY_OPERATION_SCHEDULE = "OPS";
    private static final String KEY_FROM               = "FROM";
    private static final String KEY_TO                 = "TO";
    private static final String KEY_BEGIN              = "BEG";
    private static final String KEY_END                = "END";

    // Hours keys
    private static final String KEY_RATE               = "RATE";
    private static final String KEY_RATE_SCHEDULE      = "RS";
    private static final String KEY_RATE_QUALIFIER     = "RQ";
    private static final String KEY_RATE_RESTRICTION   = "RR";

    private boolean onStreet;               // Is the parking location on the street?
    private String name;                    // The name of parking location
    private String description;             // Returned for OSP only – usually address for the parking location
    private String phoneNumber;             // Returned for OSP only – Contact telephone number for parking location
    private Location[] locations;           // Location of this parking
    private OperationHours[] hours;         // Returned for OSP only - the operating hours schedule information for this location
    private RateSchedule[] rates;           // General pricing or rate information for this location

    /**
     * Constructor that get an available parking location and parse the data to class members. The
     * parking location may or may not the following data name, description, phone number, location
     * coordinate, operation hours schedule, and/or pricing information.
     * @param space the available space that contains the data
     */
    public ParkingLocation(JSONObject space) {

        try {
            parseJSONObject(space);
        } catch (Exception ex) {
            System.err.println("Error at creating SpaceAvailable: " + ex.getMessage());
        }
    }

    /**
     * Parse the given JSON object to class members. The given JSON object may or may not the
     * following data name, description, phone number, location coordinate, operation hours
     * schedule, and/or pricing information.
     * @param jsonObject the JSON object to get parsed
     * @throws JSONException any error when parsing the given JSON object
     */
    private void parseJSONObject(JSONObject jsonObject) throws JSONException {

        if (jsonObject == null)
            return;

        // Get the type
        String type = jsonObject.has(KEY_TYPE) ? jsonObject.getString(KEY_TYPE) : "";
        this.onStreet = type.equalsIgnoreCase("on");

        // Get the name
        String name = jsonObject.has(KEY_NAME) ? jsonObject.getString(KEY_NAME) : "";
        this.name = name == null ? "" : name;

        // Get the description
        String desc = jsonObject.has(KEY_DESCRIPTION) ? jsonObject.getString(KEY_DESCRIPTION) : "";
        this.description = desc == null ? "" : desc;

        // Get the pricing information (rates)
        JSONObject mRates = jsonObject.getJSONObject(KEY_RATES);
        this.rates = getRates(mRates);

        // Get the location coordinate on the map
        String location = jsonObject.has(KEY_LOCATION) ? jsonObject.getString(KEY_LOCATION) : "";
        this.locations = getLocation(location);

        // All data below is only for off-street parking space (parking lot building)
        if (!onStreet) {
            // Get the phone number
            phoneNumber = jsonObject.has(KEY_TELEPHONE) ? jsonObject.getString(KEY_TELEPHONE) : "";

            // Get the operation hours schedule
            JSONObject operationHours = jsonObject.has(KEY_OPERATION_HOURS) ?
                    jsonObject.getJSONObject(KEY_OPERATION_HOURS) : null;
            this.hours = getOperationHours(operationHours);
        }
    }

    /**
     * Get the location coordinate (latitude and longitude) of the parking location.
     * @param location the string that contains parking location coordinate
     * @return the coordinate of the parking location
     */
    private Location[] getLocation(String location) {

        String[] points = location.split(",");

        Location[] tempLocations = new Location[points.length / 2];

        for (int i = 0, j = 0; i < points.length; i+=2, j++) {
            double lat = Double.parseDouble(points[i]);
            double lon = Double.parseDouble(points[i + 1]);

            tempLocations[j] = new Location("");
            tempLocations[j].setLatitude(lon);
            tempLocations[j].setLongitude(lat);
        }

        return tempLocations;
    }

    /**
     * Get the operation hours schedule if available for this parking location from the given
     * JSON object.
     * @param schedule the JSON object that may or may not contains operation hours schedule for
     *                 this parking location
     * @return an array of operation hours if available, otherwise null
     * @throws JSONException any error when parsing data from the given JSON object
     */
    private OperationHours[] getOperationHours(JSONObject schedule) throws JSONException {

        if (schedule == null)
            return null;

        OperationHours[] oHours = null;

        // If the operation schedule is available for this parking location
        if (!schedule.isNull(KEY_OPERATION_SCHEDULE))
        {
            Object item = schedule.get(KEY_OPERATION_SCHEDULE);

            if (item instanceof JSONArray)
            {   // The object is a JSON array
                JSONArray operationSchedules = (JSONArray) item;

                oHours = new OperationHours[operationSchedules.length()];

                // Get the data for each operation hours schedule
                for (int i = 0; i < oHours.length; i++) {
                    oHours[i] = new OperationHours(operationSchedules.getJSONObject(i));
                }
            }
            else
            {   // The object is a single JSON object
                JSONObject operationSchedule = (JSONObject) item;

                oHours = new OperationHours[1];
                oHours[0] = new OperationHours(operationSchedule);
            }
        }

        return oHours;
    }

    /**
     * Get all the available rate schedules for this parking location from the given JSON object.
     * @param rates the JSON object that may or may not contains the rate schedule
     * @return an array of rate schedule if available, otherwise null
     * @throws JSONException any error when parsing data from the given JSON object
     */
    private RateSchedule[] getRates(JSONObject rates) throws JSONException {

        if (rates == null)
            return null;

        RateSchedule[] rRates = null;

        // If the rate schedule is available for this parking location
        if (!rates.isNull(KEY_RATE_SCHEDULE))
        {
            Object item = rates.get(KEY_RATE_SCHEDULE);

            if (item instanceof JSONArray)
            {   // The object is a JSON array
                JSONArray rateSchedules = (JSONArray) item;

                rRates = new RateSchedule[rateSchedules.length()];

                // Get the data for each rate schedule
                for (int i = 0; i < rRates.length; i++) {
                    rRates[i] = new RateSchedule(rateSchedules.getJSONObject(i));
                }
            }
            else
            {   // The object is a single JSON object
                JSONObject rateSchedule = (JSONObject) item;

                rRates = new RateSchedule[1];
                rRates[0] = new RateSchedule(rateSchedule);
            }
        }

        return rRates;
    }

    /**
     * Get the name for this parking location if available, otherwise an empty string. Usually
     * contains the name of parking location or street with from and to address.
     * @return the name for this parking location if available, otherwise an empty string
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description of this parking location if available, otherwise an empty string. Usually
     * contains the address for this parking location, and only available for off street parking.
     * @return the description of this parking location if available, otherwise an empty string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the phone number for this parking location if available, otherwise an empty string. Only
     * available for off-street parking space.
     * @return the phone number for this parking location if this parking space is off street,
     *         otherwise an empty string
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Get the location coordinate of this parking location.
     * @return the location coordinate of this parking location
     */
    public Location[] getLocation() {
        return locations;
    }

    /**
     * Get the operation hours schedule for this parking location if available, otherwise null. Only
     * available for off street parking location.
     * @return the operation hours schedule for this parking location if available, otherwise null
     */
    public OperationHours[] getOperationHours() {
        return hours;
    }

    /**
     * Get the rate schedule for this parking location if available, otherwise null.
     * @return the rate schedule for this parking location if available, otherwise null
     */
    public RateSchedule[] getRateSchedule() {
        return rates;
    }

    /**
     * Determine whether this parking location is on street or off street (in a parking building).
     * @return true if this parking location is on street, otherwise false
     */
    public boolean isOnStreet() {
        return onStreet;
    }

    /**
     * Represent the operation hours schedule for the parking location. The operation hours schedule
     * may or may not contains starting day, end day, begin time, and end time.
     */
    public class OperationHours {

        private String oFromDay;            // Starting day
        private String oToDay;              // End day
        private String oBeginTime;          // Begin time
        private String oEndTime;            // End time

        /**
         * Default constructor.
         */
        public OperationHours() {
            oFromDay = "";
            oToDay = "";
            oBeginTime = "";
            oEndTime = "";
        }

        /**
         * The constructor that takes in a JSON object and get the values for this class' members.
         * The JSON object may or may not contains the following data for the rate schedule (begin
         * day, end day, begin time, and/or end time).
         * @param hours the operation hours JSON object
         * @throws JSONException any error when parsing data
         */
        public OperationHours(JSONObject hours) throws JSONException {

            if (hours == null)
                return;

            // Get the values from the JSON object if available, otherwise assign an empty string
            oFromDay = hours.has(KEY_FROM) ? hours.getString(KEY_FROM) : "";
            oToDay = hours.has(KEY_TO) ? hours.getString(KEY_TO) : "";
            oBeginTime = hours.has(KEY_BEGIN) ? hours.getString(KEY_BEGIN) : "";
            oEndTime = hours.has(KEY_END) ? hours.getString(KEY_END) : "";
        }

        /**
         * Get the begin day for this operation hours schedule.
         * @return the begin day for this operation hours schedule
         */
        public String getDayFrom() {
            return oFromDay;
        }

        /**
         * Get the end day for this operation hours schedule.
         * @return the end day for this operation hours schedule
         */
        public String getDayTo() {
            return oToDay;
        }

        /**
         * Get the begin time for this operation hours.
         * @return the begin time for this operation hours
         */
        public String getBeginTime() {
            return oBeginTime;
        }

        /**
         * Get the end time for this operation hours.
         * @return the end time for this operation hours
         */
        public String getEndTime() {
            return oEndTime;
        }
    }

    /**
     * Represent the rate schedule for the parking location. The rate schedule may or may not
     * contains the begin time, end time, applicable rate, description, rate qualifier, and rate
     * restriction.
     */
    public class RateSchedule {

        private String rBeginTime;              // Begin time
        private String rEndTime;                // End time
        private String rRate;                   // Applicable rate
        private String rDescription;            // Description of the rate schedule
        private String rRateQualifier;          // Rate qualifier e.g. Per Hr
        private String rRateRestriction;        // Rate restriction
        private String rDay;                    // Rate per day

        /**
         * The constructor that takes in a JSON object and get the values for this class' members.
         * The JSON object may or may not contains the following data for the rate schedule (begin
         * time, end time, applicable rate, description, rate qualifier, and/or rate restriction).
         *
         * @param rate the JSON object that contains the rate schedule
         * @throws JSONException any error when parsing the JSON object's data
         */
        public RateSchedule(JSONObject rate) throws JSONException {

            if (rate == null)
                return;

            // Get the values from the JSON object if available, otherwise assign an empty string
            rBeginTime = rate.has(KEY_BEGIN) ? rate.getString(KEY_BEGIN) : "";
            rEndTime = rate.has(KEY_END) ? rate.getString(KEY_END) : "";
            rRate = rate.has(KEY_RATE) ? rate.getString(KEY_RATE) : "";
            rDescription = rate.has(KEY_DESCRIPTION) ? rate.getString(KEY_DESCRIPTION) : "";
            rRateQualifier = rate.has(KEY_RATE_QUALIFIER) ? rate.getString(KEY_RATE_QUALIFIER) : "";
            rRateRestriction = rate.has(KEY_RATE_RESTRICTION) ? rate.getString(KEY_RATE_RESTRICTION) : "";
            rDay = rate.has(KEY_DAY) ? rate.getString(KEY_DAY) : "";
        }

        /**
         * Get the begin time for this rate schedule.
         *
         * @return the begin time for this rate schedule
         */
        public String getBeginTime() {
            return rBeginTime;
        }

        /**
         * Get the end time for this rate schedule.
         *
         * @return the end time for this rate schedule
         */
        public String getEndTime() {
            return rEndTime;
        }

        /**
         * Get the applicable rate for this rate schedule.
         *
         * @return the applicable rate for this rate schedule
         */
        public String getRate() {
            return rRate;
        }

        /**
         * Get the description for this rate schedule.
         *
         * @return the description for this rate schedule
         */
        public String getDescription() {
            return rDescription;
        }

        /**
         * Get the rate qualifier for this rate schedule, e.g. Per Hr.
         *
         * @return the rate qualifier for this rate schedule, e.g. Per Hr
         */
        public String getRateQualifier() {
            return rRateQualifier;
        }

        /**
         * Get the rate restriction for this rate schedule.
         *
         * @return the rate restriction for this rate schedule
         */
        public String getRateRestriction() {
            return rRateRestriction;
        }

        public String getDay() {
            return rDay;
        }
    }
}
