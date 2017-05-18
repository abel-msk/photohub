package home.abel.photohub.utils;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *    http://mericleclerin.blogspot.jp/2013/07/internetdateformat-for-rfc3339.html
 *    
 * InternetDateFormat is supported RFC3339.
 * e.g. "1937-01-01T12:00:27.87+00:20"
 * Because SimpleDateFormat can't parse or format to RFC3339.
 * InternetDateFormat not invoke performance tuning, perhaps it is slowly.
 * @version 1.13
 * @author Bladean Mericle
 */
public class InternetDateFormat extends DateFormat {

    /** Auto generated serialVersionUID. */
    private static final long serialVersionUID = 1223084646264103173L;

    /** Date/time formatting with time offset. */
    private boolean offset = true;

    /** Fractional seconds digits. */
    private int fractionalSecondsDigits = 0;

    /**
     * Default constructor.
     * Output time offset, and not output fractional seconds.
     */
    public InternetDateFormat() {
        super();
        setCalendar(Calendar.getInstance());
        setNumberFormat(NumberFormat.getInstance());
    }

    /**
     * Constructor.
     * @param zone the given new time zone.
     * @param aLocale the given locale.
     */
    public InternetDateFormat(TimeZone zone, Locale aLocale)  {
        super();
        setCalendar(Calendar.getInstance(zone, aLocale));
        setNumberFormat(NumberFormat.getInstance(aLocale));
    }

    /**
     * Constructor.
     * @param anOffset date/time formatting with time offset
     * @param aFractionalSecondsDigits fractional seconds digits (0-3)
     */
    public InternetDateFormat(
            boolean anOffset,
            int aFractionalSecondsDigits)  {
        this();
        setOffset(anOffset);
        setFractionalSecondsDigits(aFractionalSecondsDigits);
    }

    /**
     * Constructor.
     * @param zone the given new time zone.
     * @param aLocale the given locale.
     * @param anOffset date/time formatting with time offset
     * @param aFractionalSecondsDigits fractional seconds digits (0-3)
     */
    public InternetDateFormat(
            TimeZone zone,
            Locale aLocale,
            boolean anOffset,
            int aFractionalSecondsDigits)  {
        this(zone, aLocale);
        setOffset(anOffset);
        setFractionalSecondsDigits(aFractionalSecondsDigits);
    }

    /**
     * Tell whether date/time formatting with time offset.
     * The offset is used only format method.
     * @return true is formatting with time offset; false otherwise.
     */
    public boolean isOffset() {
        return this.offset;
    }

    /**
     * Specify whether or not date/time formatting with time offset.
     * The offset is used only format method.
     * @param anOffset true is formatting with time offset; false otherwise.
     */
    public void setOffset(boolean anOffset) {
        this.offset = anOffset;
    }

    /**
     * Gets the number of fractional seconds digits.
     * The digits is used only format method.
     * @return fractional seconds digits (0-3)
     */
    public int getFractionalSecondsDigits() {
        return this.fractionalSecondsDigits;
    }

    /**
     * Sets the number of fractional seconds digits.
     * Digits range is 0 to 3.
     * If digits over range, this method throws IllegalArgumentException.
     * The digits is used only format method.
     * @param fractionalSecondsDigits fractional seconds digits (0-3)
     */
    public void setFractionalSecondsDigits(int aFractionalSecondsDigits) {
        if (aFractionalSecondsDigits < 0 || 3 < aFractionalSecondsDigits) {
            throw new IllegalArgumentException(
                    "Fractional seconds digits range is 0 to 3. ("
                    + aFractionalSecondsDigits
                    + ")");
        }
        this.fractionalSecondsDigits = aFractionalSecondsDigits;
    }

    /**
     * Formats a Date into a date/time string.
     * @param date a Date to be formatted into a date/time string.
     * @param toAppendTo the string buffer for the returning time string.
     * @param fieldPosition keeps track of the position of the field within the returned string.
     */
    @Override
    public StringBuffer format(
            Date date,
            StringBuffer toAppendTo,
            FieldPosition fieldPosition) {
        Calendar formatCalendar = (Calendar) getCalendar().clone();
        formatCalendar.setTime(date);
        if (isOffset()) {
            int formatedOffset = formatCalendar.get(Calendar.ZONE_OFFSET);
            toAppendTo.append(String.format(
                    "%1$tFT%1$tH:%1$tM:%1$tS", formatCalendar.getTime()));
            toAppendTo.append(formatFractionalSeconds(formatCalendar));
            toAppendTo.append((formatedOffset >= 0) ? "+" : "-");
            toAppendTo.append(String.format(
                    "%1$02d:%2$02d",
                    Integer.valueOf(formatedOffset / (60 * 60 * 1000)),
                    Integer.valueOf(formatedOffset % (60 * 60 * 1000) / 1000)));
        } else {
            // Correct offset
            formatCalendar.add(Calendar.MILLISECOND, -formatCalendar.get(Calendar.ZONE_OFFSET));
            toAppendTo.append(String.format(
                    "%1$tFT%1$tH:%1$tM:%1$tS", formatCalendar.getTime()));
            toAppendTo.append(formatFractionalSeconds(formatCalendar));
            toAppendTo.append("Z");
        }
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(toAppendTo.length());
        return toAppendTo;
    }

    /***
     * Formats a Date into a fractional seconds string.
     * @param formatCalendar be formatted date
     * @return fractional seconds string
     */
    protected String formatFractionalSeconds(Calendar formatCalendar) {
        String fractionalSeconds = "";
        int digits = getFractionalSecondsDigits();
        if (digits != 0) {
            Integer milllisecond = Integer.valueOf(formatCalendar.get(Calendar.MILLISECOND));
            fractionalSeconds = "." + String.format("%03d", milllisecond).substring(0, digits);
        }
        return fractionalSeconds;
    }

    /**
     * Parse a date/time string according to the given parse position.
     * @param source The date/time string to be parsed
     * @param pos the parsing position
     * the position at which parsing terminated, or the start position if the parse failed.
     */
    @Override
    public Date parse(String source, ParsePosition pos) {
        try {
            Calendar parseCalendar = (Calendar) getCalendar().clone();
            parseCalendar.set(Calendar.YEAR, parseNumber(source, pos, 4));
            checkSeparator(source, pos, "-");
            parseCalendar.set(Calendar.MONTH, parseNumber(source, pos, 2) - 1);
            checkSeparator(source, pos, "-");
            parseCalendar.set(Calendar.DAY_OF_MONTH, parseNumber(source, pos, 2));
            checkSeparator(source, pos, "T");
            parseCalendar.set(Calendar.HOUR_OF_DAY, parseNumber(source, pos, 2));
            checkSeparator(source, pos, ":");
            parseCalendar.set(Calendar.MINUTE, parseNumber(source, pos, 2));
            checkSeparator(source, pos, ":");
            parseCalendar.set(Calendar.SECOND, parseNumber(source, pos, 2));
            if (source.substring(pos.getIndex()).startsWith(".")) {
                pos.setIndex(pos.getIndex() + 1);
                parseCalendar.set(Calendar.MILLISECOND, parseFractionalSeconds(source, pos));
            }
            String next = source.substring(pos.getIndex());
            if (next.equals("Z")) {
                pos.setIndex(pos.getIndex() + 1);
                // "Z" equals 00:00
                parseCalendar.set(Calendar.ZONE_OFFSET, 0);
                return parseCalendar.getTime();
            } else if (next.startsWith("+")) {
                pos.setIndex(pos.getIndex() + 1);
                parseCalendar.set(Calendar.ZONE_OFFSET, parseOffset(source, pos));
                return parseCalendar.getTime();
            } else if (next.startsWith("-")) {
                pos.setIndex(pos.getIndex() + 1);
                parseCalendar.set(Calendar.ZONE_OFFSET, -parseOffset(source, pos));
                return parseCalendar.getTime();
            }
            pos.setErrorIndex(pos.getIndex());
            return null;
        } catch (IndexOutOfBoundsException e) {
            pos.setErrorIndex(pos.getIndex());
            return null;
        } catch (NumberFormatException e) {
            pos.setErrorIndex(pos.getIndex());
            return null;
        }
    }

    /**
     * Parse number, and increment parse position.
     * @param source A String whose beginning should be parsed.
     * @param pos the parsing position
     * @param length parse length
     * @return parsed number
     */
    protected static int parseNumber(
            String source,
            ParsePosition pos,
            int length) {
        int index = pos.getIndex();
        int number = Integer.parseInt(source.substring(index, index + length));
        pos.setIndex(index + length);
        return number;
    }

    /**
     * Check separator string is valid.
     * If separetaor is invalid, this method throws IndexOutOfBoundsException.
     * @param source A String whose beginning should be parsed.
     * @param pos the parsing position
     * @param separator separator string
     */
    protected static void checkSeparator(
            String source,
            ParsePosition pos,
            String separator) {
        int index = pos.getIndex();
        int length = separator.length();
        if (!source.substring(index, index + length).equals(separator)) {
            throw new IndexOutOfBoundsException();
        }
        pos.setIndex(index + length);
    }

    /**
     * Parse fractional seconds.
     * @param source A String whose beginning should be parsed.
     * @param pos the parsing position
     * @return parsed fractional seconds
     */
    protected static int parseFractionalSeconds(
            String source,
            ParsePosition pos) {
        String milliSecond = source.substring(pos.getIndex()).split("\\D")[0];
        int number = Integer.parseInt((milliSecond + "000").substring(0, 3));
        pos.setIndex(pos.getIndex() + milliSecond.length());
        return number;
    }

    /**
     * Parse offset.
     * But it is not include "+" or "-".
     * When this method is called, these operator have to be already parsed.
     * @param source A String whose beginning should be parsed.
     * @param pos the parsing position
     * @return offset millisecond
     */
    protected static int parseOffset(
            String source,
            ParsePosition pos) {
        int hour = parseNumber(source, pos, 2);
        checkSeparator(source, pos, ":");
        int minute = parseNumber(source, pos, 2);
        return (hour * 60 + minute) * 60 * 1000;
    }
}