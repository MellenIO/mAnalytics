package io.mellen.manalytics.util;

import org.ocpsoft.prettytime.PrettyTime;

import java.sql.Timestamp;
import java.text.DateFormat;

public class DateUtil {
    private static PrettyTime prettyTime = new PrettyTime();

    public static String relativeFormat(Timestamp timestamp) {
        return prettyTime.format(timestamp);
    }

    public static String utcFormat(Timestamp timestamp) {
        return timestamp.toInstant().toString();
    }
}
