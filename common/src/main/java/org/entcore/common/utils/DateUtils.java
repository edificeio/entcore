/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.common.utils;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by dbreyton on 30/03/2016.
 */
public final class DateUtils {
    private final static Logger log = LoggerFactory.getLogger(DateUtils.class);

    /** The Constant DEFAULT_DATE_PATTERN. */
    private static final String DEFAULT_DATE_PATTERN = "dd/MM/yyyy";

    /** The Constant DEFAULT_DATE_LOCAL. */
    private static final Locale DEFAULT_DATE_LOCAL = Locale.FRENCH;

    public static final DateTimeFormatter DEFAULT_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    /** The date format used for ISO 8601 dates with milliseconds. */
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /** 
     * Date format with milliseconds and dots as separators (yyyy-MM-dd HH:mm.ss.SSS)
     */
    private static final SimpleDateFormat DOT_SEPARATED_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss.SSS");

    static {
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private DateUtils()  {}

    public static Date create(int year, int month, int day, int hours, int minutes,
                             int seconds) {
        return new GregorianCalendar(year, month, day, hours, minutes, seconds).getTime();
    }

    public static Date create(int year, int month, int day) {
        return create(year, month, day, 0, 0, 0);
    }

    public static int getField(Date date, int field) {
        final Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        return cal.get(field);
    }

    public static Date add(Date date, int field, int value) {
        if (date != null) {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(field, value);
            return cal.getTime();
        }

        return null;
    }

    public static int compare(Date date1, Date date2) {
        return compare((date1 != null) ? date1.getTime() : null,
                (date2 != null) ? date2.getTime() : null);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable> int compare(T valeur1, T valeur2) {
        if (valeur1 == null) {
            if (valeur2 == null) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (valeur2 == null) {
               return -1;
            } else {
                return valeur1.compareTo(valeur2);
            }
        }
    }

    /**
     * Returns True if "Date Between" is between "Min Date" and "Max Date".
     */
    public static Boolean isBetween(Date dateBetween, Date dateMin, Date dateMax) {
        return ((compare(dateBetween, dateMin) >= 0) &&
                (compare(dateMax, dateBetween) >= 0));
    }

    public static Boolean lessOrEqualsWithoutTime(Date d1, Date d2) {
        if ((d1 == null) || (d2 == null)) {
            return false;
        }
        final Date d1Arr = untimed(d1);
        final Date d2Arr = untimed(d2);
        return lessOrEquals(d1Arr, d2Arr);
    }

    public static Boolean lessOrEquals(Date d1, Date d2) {
        if ((d1 == null) || (d2 == null)) {
            return false;
        }
        return (d1.before(d2) || d1.equals(d2));
    }

    private static Date untimed(Date date) {
        if (date != null) {
            final Calendar cal = new GregorianCalendar();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);

            return cal.getTime();
        }
        return null;
    }

    public static Date parse(String date, String pattern) throws ParseException {
        final String myPattern = StringUtils.isEmpty(pattern) ? DEFAULT_DATE_PATTERN : pattern;
        final DateFormat dateFormat = new SimpleDateFormat(myPattern);
        return dateFormat.parse(date);
    }

    public static Date parseIsoDate(JsonObject date) {
        return MongoDb.parseIsoDate(date);
    }

    public static Date parseLongDate(String longStr) {
        return new Date(Long.parseLong(longStr));
    }

    public static Date parseLongDate(Long timestamp) {
        return new Date(timestamp);
    }

    public static Long parseLongDateToLong(String longStr) {
        return new Date(Long.parseLong(longStr)).getTime();
    }


    public static Date parseIsoDate(String date) throws ParseException {
        return MongoDb.parseDate(date);
    }

    public static Date parseDateTime(String date) {
        if (!StringUtils.isEmpty(date)) {
            return DatatypeConverter.parseDateTime(date).getTime();
        }
        return null;
    }

    public static Date parseTimestampWithoutTimezone(String date) throws ParseException {
        return parse(date, "yyyy-MM-dd'T'HH:mm:ss");
    }

    public static Long parseDateTimeToLong(String date) {
        return parseDateTime(date).getTime();
    }

    public static String format(Date d, String pattern) {
        if (d == null) {
            return null;
        }
        final String myPattern = StringUtils.isEmpty(pattern) ? DEFAULT_DATE_PATTERN : pattern;
        return new SimpleDateFormat(myPattern).format(d);
    }

    public static String format(Date d, Locale local, String pattern) {
        if (d == null) {
            return null;
        }
        final String myPattern = StringUtils.isEmpty(pattern) ? DEFAULT_DATE_PATTERN : pattern;
        final Locale myLocal = (local != null) ? local : DEFAULT_DATE_LOCAL;
        return new SimpleDateFormat(myPattern, myLocal).format(d);
    }

    public static String format(Date d) {
        return format(d, DEFAULT_DATE_PATTERN);
    }

    public static String formatIsoDate(Date date) {
        return MongoDb.formatDate(date);
    }


    public static String formatUtcDateTime(final Date date) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static String formatUtcDateTime(final long date) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static JsonObject getDateJsonObject(final Date date) {
        if (date != null) {
            return getDateJsonObject(date.getTime());
        }
        final Long l = null;
        return getDateJsonObject(l);
    }

    public static JsonObject getDateJsonObject(final Long date) {
        return new JsonObject().put("$date", date);
    }


    /**
     * Convert $date: long into $date: string utc since mongo driver change.
     * @param savePayload Documents to save
     */
    public static void reformatDateForMongoDB(JsonArray savePayload) {
        if(savePayload != null) {
            for (Object o : savePayload) {
                if (o instanceof JsonObject) {
                    reformatDateForMongoDB((JsonObject) o);
                }
            }
        }
    }

    /**
     * Convert $date: long into $date: string utc since mongo driver change.
     * @param document Document to save
     */
    public static void reformatDateForMongoDB(JsonObject document) {
        if(document != null) {
            for (Map.Entry<String, Object> objectEntry : document) {
                final String key = objectEntry.getKey();
                final Object value = objectEntry.getValue();
                if(value == null) {
                    // Nothing to do
                } else if("$date".equals(key) && value instanceof Number) {
                    // Try to convert the long value into a utc date
                    try {
                        document.put(key, formatUtcDateTime((long) value));
                    } catch(Exception e) {
                        log.error("An error occurred when we try to convert the timestamp " + value + " into a utc date");
                    }
                } else if(value instanceof JsonArray) {
                    reformatDateForMongoDB((JsonArray) value);
                } else if(value instanceof JsonObject) {
                    reformatDateForMongoDB((JsonObject) value);
                }
            }
        }
    }

    /**
     * Parse an ISO 8601 date string with milliseconds into a Java Date object.
     *
     * @param dateString Date string in ISO 8601 format (e.g., "2023-10-01T12:34:56.789Z")
     * @return Date object representing the parsed date, or the current date if parsing fails
     */
    public static Date parseIsoDateWithMillis(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return new Date();
        }
        
        try {
            return ISO_DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            log.warn("Failed to parse ISO date with milliseconds: {}", dateString);
            return new Date();
        }
    }

    /**
     * Parse various date formats into a Java Date object.
     * Supports Long, String, and MongoDB date format ($date JSON object).
     * 
     * @param date Object representing a date (Long, String, or JsonObject with $date)
     * @return Java Date object, or null if the input format is not supported
     */
    public static Date parseDate(Object date) {
        if (date == null) {
            return null;
        }
        if (date instanceof Long) {
            return new Date((Long) date);
        }
        if (date instanceof String) {
            return parseDateTime((String) date);
        }
        if (date instanceof JsonObject) {
            Object dateValue = ((JsonObject) date).getValue("$date");
            if (dateValue != null) {
                return parseDateTime(dateValue.toString());
            }
        }
        return null;
    }

    /**
     * Parse date from dot-separated format with milliseconds (yyyy-MM-dd HH:mm.ss.SSS)
     * @param dateStr Date string in format "yyyy-MM-dd HH:mm.ss.SSS"
     * @return Parsed Date or current date if parsing fails
     */
    public static Date parseDotSeparatedDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return new Date();
        }
        
        try {
            return DOT_SEPARATED_DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            log.warn("Failed to parse date with dot separators: {}", dateStr);
            return new Date();
        }
    }
}
