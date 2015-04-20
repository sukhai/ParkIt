package com.csc413.group9.parkIt.SFPark;

/**
 * Created by kelvin on 4/20/15.
 */
public class OperationHours {

    private static final String KEY_SCHEDULE = "OPS";
    private static final String KEY_FROM = "FROM";
    private static final String KEY_TO = "TO";
    private static final String KEY_BEGIN = "BEG";
    private static final String KEY_END = "END";
    private static final String KEY_RATE_SCHEDULE = "RS";
    private static final String KEY_RATE = "RATE";
    private static final String KEY_DESCRIPTION = "DESC";
    private static final String KEY_RATE_QUALIFIER = "RQ";
    private static final String VALUE_STREET_SWEEP = "Str sweep";
    private static final String VALUE_NO_CHARGE = "No charge";

    private String oFromDay;
    private String oToDay;
    private String oBeginTime;
    private String oEndTime;
    private Rate[] oRates;

    private class Rate {

        private String rBeginTime;
        private String rEndTime;
        private String rRate;
        private String rDescription;
        private String rRateQualifier;
        private String rRateRestriction;
    }
}
