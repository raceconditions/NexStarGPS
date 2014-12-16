package net.raceconditions.telescopegps;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by ubuntu on 9/16/14.
 */
public class DateSerializer {
    /**
     * Serialize a Calendar object to NexStar protocol dateTime
     *
     * @param calendar Calendar instance to serialize
     * @return Byte array containing NexStar dateTime
     */
    public static byte[] serialize(Calendar calendar) {
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR) - 2000;
        long zoneOffset_millis = calendar.get(Calendar.ZONE_OFFSET);
        int zoneOffset_hours = (int) TimeUnit.HOURS.convert(zoneOffset_millis, TimeUnit.MILLISECONDS);
        int dstOffset_millis = calendar.get(Calendar.DST_OFFSET);
        int dstOffset_hours = (int)TimeUnit.HOURS.convert(dstOffset_millis, TimeUnit.MILLISECONDS);

        if(zoneOffset_hours < 0)
            zoneOffset_hours += 256;

        byte[] dateTime = new byte[] {'H', (byte)hours, (byte)minutes, (byte)seconds, (byte)month, (byte)day, (byte)year, (byte)zoneOffset_hours, (byte)dstOffset_hours};
        return dateTime;
    }
}
