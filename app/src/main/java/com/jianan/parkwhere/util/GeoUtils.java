package com.jianan.parkwhere.util;

/**
 * Utility class for geographic calculations related to car park locations
 *
 * Includes a Haversine formula implementation to calculate great circle distance in meters
 *
 * Usage example
 * double meters = GeoUtils.calculateHaversineDistance(lat1, lon1, lat2, lon2)
 */
public class GeoUtils {
    private static final double EARTH_RADIUS_M = 6371000;

    /**
     * Calculate the Haversine distance between two latitude longitude points in meters
     *
     * Formula accounts for Earth's curvature and returns distance in meters
     *
     * @param lat1 latitude of first point in decimal degrees
     * @param lon1 longitude of first point in decimal degrees
     * @param lat2 latitude of second point in decimal degrees
     * @param lon2 longitude of second point in decimal degrees
     * @return distance in meters between the two points
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c; // distance in meters
    }
}
