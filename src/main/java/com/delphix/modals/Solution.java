package com.delphix.modals;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Solution {

    public static final String SOUTH_DIRECTION = "S";
    public static final String WEST_DIRECTION = "W";
    public static final int RANGE_PARAMETER = 15;
    public static final String NORTH_DIRECTION = "N";
    public static final String EAST_DIRECTION = "E";
    public static final String EMPTY_STRING = "";
    public static final String NEGATIVE_STRING = "-";
    public static final String BOSTON = "Boston";
    public static final String NCR = "NCR";
    public static final String SAN_FRANCISCO = "San Francisco";
    public static final String COUNT_NODE = "count";
    public static final String DATA_ARRAY = "data";
    public static final String FIELDS_ARRAY = "fields";
    public static final String LAT_NODE = "lat";
    public static final String LAT_DIR_NODE = "lat-dir";
    public static final String LON_NODE = "lon";
    public static final String LON_DIR_NODE = "lon-dir";
    public static final String ENERGY_NODE = "energy";
    public static final String SIGNATURE_NODE = "signature";
    public static final String VERSION = "version";
    public static final String DATE_MIN = "date-min";
    public static final String DATE_MAX = "date-max";
    public static final String AMPERSAND_STRING = "&";
    public static final String EQUAL_OPERATOR = "=";
    public static final String REQ_LOC = "req-loc";
    public static final String SORT = "sort";
    static String response = "";
    static int energyIndex = 0;
    static int latitudeIndex = 0;
    static int longitudeIndex = 0;
    static int latitudeDirectionIndex = 0;
    static int longitudeDirectionIndex = 0;


    public static void main(String[] args) {

        // start date :2017-01-01
        // end date : 2020-01-01
        // we can update date range
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getNasaUrlWithQueryParams("2017-01-01","2020-01-01")))
                .GET()
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(request, BodyHandlers.ofString());
            int responseStatusCode = httpResponse.statusCode();
            response = httpResponse.body();
            if (responseStatusCode != 200) {
                throw new ApiResponseException(
                        "Response status code received " + responseStatusCode + " response Body " + response);
            } else {
                computeBrightnessDataByLocation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create URI by modifying query parameters
     *
     * @param startDate
     * @param endDate
     * @return
     */
    private static String getNasaUrlWithQueryParams(String startDate,String endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://ssd-api.jpl.nasa.gov/fireball.api?");
        sb.append(DATE_MIN).append(EQUAL_OPERATOR).append(startDate);
        sb.append(AMPERSAND_STRING).append(DATE_MAX).append(EQUAL_OPERATOR).append(endDate);
        sb.append(AMPERSAND_STRING).append(REQ_LOC).append(EQUAL_OPERATOR).append("true");
        sb.append(AMPERSAND_STRING).append(SORT).append(EQUAL_OPERATOR).append("-energy");
        return sb.toString();
    }

    /**
     * Method to compute which location saw brightest star
     *
     * @throws ParseException
     */
    private static void computeBrightnessDataByLocation() throws ParseException {
        Optional<JSONArray> bostonData = fireBall(convertToSignedDegree("42.354558N"),
                convertToSignedDegree("71.054254W"));
        Optional<JSONArray> nCRData = fireBall(convertToSignedDegree("28.574389N"),
                convertToSignedDegree("77.312638E"));
        Optional<JSONArray> sFRData = fireBall(convertToSignedDegree("37.793700N"),
                convertToSignedDegree("122.403906W"));

        Map<String, Optional<JSONArray>> fireBallMap = new HashMap<>();
        fireBallMap.put(BOSTON, bostonData);
        fireBallMap.put(NCR, nCRData);
        fireBallMap.put(SAN_FRANCISCO, sFRData);

        String brightestPlace = "";
        double brightestValue = 0.0;
        String latValueData = EMPTY_STRING;
        String longValueData = EMPTY_STRING;
        String latDirData = EMPTY_STRING;
        String longDirData = EMPTY_STRING;

        for (String place : fireBallMap.keySet()) {

            if (fireBallMap.get(place).isPresent()) {

                JSONArray data = fireBallMap.get(place).get();
                double energyValue = Double.parseDouble(String.valueOf(data.get(energyIndex)));
                String latValue = String.valueOf(data.get(latitudeIndex));
                String longValue = String.valueOf(data.get(longitudeIndex));
                String latDirection = String.valueOf(data.get(latitudeDirectionIndex));
                String longDirection = String.valueOf(data.get(longitudeDirectionIndex));
                if (energyValue > brightestValue) {
                    brightestValue = energyValue;
                    brightestPlace = place;
                    latValueData = latValue;
                    longValueData = longValue;
                    latDirData = latDirection;
                    longDirData = longDirection;
                }
            }
        }

        System.out.println(
                brightestPlace + " saw the brightest star with energy of " + brightestValue + " at latitude of "
                        + latValueData + latDirData + " at longitude of " + longValueData + longDirData);
    }

    /**
     * FireBall Method to calculate brightest star seen near by to provided latitude and longitude
     *
     * @param latitude
     * @param longitude
     * @return
     * @throws ParseException
     */
    private static Optional<JSONArray> fireBall(double latitude, double longitude) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject fireBallResponse = (JSONObject) parser.parse(response);
        String valueCount = (String) fireBallResponse.get(COUNT_NODE);
        JSONObject signatureNode = (JSONObject) fireBallResponse.get(SIGNATURE_NODE);


        if (Integer.parseInt(valueCount) > 0 && signatureNode.get(VERSION).equals("1.0")) {
            JSONArray dataList = (JSONArray) fireBallResponse.get(DATA_ARRAY);
            JSONArray fieldList = (JSONArray) fireBallResponse.get(FIELDS_ARRAY);
            latitudeIndex = fieldList.indexOf(LAT_NODE);
            latitudeDirectionIndex = fieldList.indexOf(LAT_DIR_NODE);
            longitudeIndex = fieldList.indexOf(LON_NODE);
            longitudeDirectionIndex = fieldList.indexOf(LON_DIR_NODE);
            energyIndex = fieldList.indexOf(ENERGY_NODE);

            return dataList.stream().filter(x ->
                    computeBrightestStarInformation(latitude, longitude, latitudeIndex, latitudeDirectionIndex,
                            longitudeIndex,
                            longitudeDirectionIndex, (JSONArray) x)
            ).findFirst();
        } else {
            return Optional.empty();
        }

    }

    /**
     * Utility to convert unsigned value to signed
     *
     * @param unsignedValue
     * @return
     */
    private static double convertToSignedDegree(String unsignedValue) {
        if (unsignedValue != null && !unsignedValue.equalsIgnoreCase(EMPTY_STRING)) {
            String trimmedUnsignedValue = unsignedValue.trim();
            if (trimmedUnsignedValue.matches(".*[a-zA-Z]+.*")) {
                return Double.parseDouble(retrieveSignedValueByDirection(trimmedUnsignedValue));
            } else {
                return Double.parseDouble(trimmedUnsignedValue);
            }
        } else {
            throw new DataValidationException("Request parameter Value should not be empty or null");
        }
    }

    /**
     * Converting Degree+ Compass direction to Signed Degrees
     *
     * @param trimmedUnsignedValue
     * @return
     */
    private static String retrieveSignedValueByDirection(String trimmedUnsignedValue) {
        StringBuilder sb = new StringBuilder();
        if (trimmedUnsignedValue.contains(SOUTH_DIRECTION) || trimmedUnsignedValue.contains(WEST_DIRECTION)) {
            sb.append(NEGATIVE_STRING);
            appendData(trimmedUnsignedValue, sb, SOUTH_DIRECTION, WEST_DIRECTION);
        } else if (trimmedUnsignedValue.contains(NORTH_DIRECTION) || trimmedUnsignedValue
                .contains(EAST_DIRECTION)) {
            appendData(trimmedUnsignedValue, sb, NORTH_DIRECTION, EAST_DIRECTION);
        } else {
            throw new DataValidationException("Direction should be only N,S,E or W");
        }
        return sb.toString();
    }

    /**
     * append unsigned Data information
     *
     * @param unsignedValue
     * @param sb
     * @param southDirection
     * @param westDirection
     */
    private static void appendData(String unsignedValue, StringBuilder sb, String southDirection,
            String westDirection) {
        for (String str : unsignedValue.split(EMPTY_STRING)) {
            if (!(str.equalsIgnoreCase(southDirection) || str.equalsIgnoreCase(westDirection))) {
                sb.append(str);
            }
        }
    }

    /**
     * Compute brightest star information based on range parameter and directions provided.
     *
     * @param latitude
     * @param longitude
     * @param latitudeIndex
     * @param latitudeDirectionIndex
     * @param longitudeIndex
     * @param longitudeDirectionIndex
     * @param data
     * @return
     */
    private static boolean computeBrightestStarInformation(double latitude, double longitude, int latitudeIndex,
            int latitudeDirectionIndex, int longitudeIndex, int longitudeDirectionIndex, JSONArray data) {
        double latitudeValue = Double.parseDouble(String.valueOf(data.get(latitudeIndex)));
        double longitudeValue = Double.parseDouble(String.valueOf(data.get(longitudeIndex)));
        String latDirection = String.valueOf(data.get(latitudeDirectionIndex));
        String longDirection = String.valueOf(data.get(longitudeDirectionIndex));

        if (latDirection.equalsIgnoreCase(SOUTH_DIRECTION)) {
            latitudeValue = -latitudeValue;
        }
        if (longDirection.equalsIgnoreCase(WEST_DIRECTION)) {
            longitudeValue = -longitudeValue;
        }

        return ((latitude - RANGE_PARAMETER) <= latitudeValue && latitudeValue <= (latitude + RANGE_PARAMETER))
                && (
                (longitude - RANGE_PARAMETER) <= longitudeValue && (longitudeValue <= (longitude
                        + RANGE_PARAMETER)));
    }

    /**
     * Custom Exception thrown based on API response
     */
    private static class ApiResponseException extends RuntimeException implements Serializable {
        private static final long serialVersionUID = 1L;

        public ApiResponseException(String message) {
            super(message);
        }
    }

    /**
     * Custom Exception thrown based on Data Validation
     */
    private static class DataValidationException extends RuntimeException implements Serializable{
        private static final long serialVersionUID = 1L;
        public DataValidationException(String message) {
            super(message);
        }
    }
}


