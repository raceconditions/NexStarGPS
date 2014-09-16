package net.raceconditions.telescopegps;

import android.location.Location;

/**
 * Created by ubuntu on 9/16/14.
 */
public class LocationSerializer {
    private static final int DEGREES = 0;
    private static final int MINUTES = 1;
    private static final int SECONDS = 2;

    public static byte[] serialize(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        int lat_degrees = getCoordinate(latitude, DEGREES);
        int lat_ns = lat_degrees < 0 ? 1 : 0;
        lat_degrees = Math.abs(lat_degrees);
        int lat_minutes = getCoordinate(latitude, MINUTES);
        int lat_seconds = getCoordinate(latitude, SECONDS);
        int long_degrees = getCoordinate(longitude, DEGREES);
        int long_ew = long_degrees < 0 ? 1 : 0;
        long_degrees = Math.abs(long_degrees);
        int long_minutes = getCoordinate(longitude, MINUTES);
        int long_seconds = getCoordinate(longitude, SECONDS);

        byte[] gps = new byte[] {'W', (byte)lat_degrees, (byte)lat_minutes, (byte)lat_seconds, (byte)lat_ns,
                (byte)long_degrees, (byte)long_minutes, (byte)long_seconds, (byte)long_ew};

        return gps;
    }

    private static int getCoordinate(double coordinate, int index)
    {
        String hms = Location.convert(coordinate, Location.FORMAT_SECONDS);
        String [] hms_split = hms.split(":");

        return (int)Math.round(Double.parseDouble(hms_split[index]));
    }
}
