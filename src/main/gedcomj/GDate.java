package gedcomj;

import java.util.*;
import java.text.*;
import java.util.regex.Pattern;

/** 
 * Represents a GEDCOM Date, which is actually always a range of dates
 * ("1 Jan 2000" is from midnight on 1 Jan to 23:59:59 on 1 Jan).
 * So every date has an optional start value and end value, 
 * and the qualifier determines the accuracy. Dates specified as
 * "Before 1900" or "After 2000" have a null start or end date,
 * and invalid dates have the qualifier "Invalid" and null values
 * for both.
 */
public class GDate extends Record {

    GDate(GEDCOM gedcom, String tag) {
        super(gedcom, tag);
    }

    private Date d0, d1;
    private Qualifier qualifier = Qualifier.Invalid;

    /**
     * Return the start {@link Date} from the value set by {@link #setValue},
     * or null if there is none
     */
    public Date getStart() {
        return d0;
    }

    /**
     * Return the end {@link Date} from the value set by {@link #setValue},
     * or null if there is none
     */
    public Date getEnd() {
        return d1;
    }

    /**
     * Return the type of Date this is
     */
    public Qualifier getQualifier() {
        return qualifier;
    }

    @Override public void setValue(String value) {
        super.setValue(value);
        parseValue();
    }

    private void parseValue() {
        Date d0 = null, d1 = null;
        Qualifier qualifier = null;
        String v = getValue();
        String fixedValue = v;
        String uv = v.toUpperCase();
        if (uv.startsWith("BET ") && uv.contains(" AND ")) {
            qualifier = Qualifier.Range;
            int ix = uv.indexOf(" AND ");
            d0 = parseDate(v.substring(4, ix).trim(), false);
            d1 = parseDate(v.substring(ix + 4).trim(), true);
            if (d1 == null) {
                d0 = null;
            }
        } else if (uv.startsWith("FROM ") && uv.contains(" TO ")) {
            qualifier = Qualifier.Range;
            int ix = v.indexOf(" TO ");
            d0 = parseDate(v.substring(5, ix).trim(), false);
            d1 = parseDate(v.substring(ix + 3).trim(), true);
            if (d1 == null) {
                d0 = null;
            }
        } else if (uv.startsWith("BEF ")) {
            qualifier = Qualifier.Before;
            d0 = null;
            d1 = parseDate(v.substring(4), true);
        } else if (uv.startsWith("TO ")) {
            qualifier = Qualifier.Before;
            d0 = null;
            d1 = parseDate(v.substring(3), true);
        } else if (uv.startsWith("AFT ")) {
            qualifier = Qualifier.After;
            d0 = parseDate(v.substring(4), false);
            d1 = null;
        } else if (uv.startsWith("FROM ")) {
            qualifier = Qualifier.After;
            d0 = parseDate(v.substring(5), false);
            d1 = null;
        } else if (uv.startsWith("ABT ")) {
            qualifier = Qualifier.About;
            d0 = parseDate(v.substring(4), false);
            d1 = parseDate(v.substring(4), true);
        } else if (uv.startsWith("CAL ")) {
            qualifier = Qualifier.Calculated;
            d0 = parseDate(v.substring(4), false);
            d1 = parseDate(v.substring(4), true);
        } else if (uv.startsWith("EST ")) {
            qualifier = Qualifier.Estimated;
            d0 = parseDate(v.substring(4), false);
            d1 = parseDate(v.substring(4), true);
        } else {
            qualifier = Qualifier.Exact;
            d0 = parseDate(v, false);
            d1 = parseDate(v, true);
        }
        if (d0 != null || d1 != null) {
            if (d1 != null) {
                d1 = new Date(d1.getTime() + (24*60*60*1000) - 1);
            }
            this.d0 = d0;
            this.d1 = d1;
            this.qualifier = qualifier;
        } else {
            this.d0 = this.d1 = null;
            this.qualifier = Qualifier.Invalid;
        }
    }

    //--------------------------------------------------------------------------------

    /**
     * Miscellaneous date characters, for ignoring sections of dates - alphanumeric, spaces, and periods
     */
    private static final String FORMAT_DATE_MISC = "[A-Za-z0-9. ]*";

    /**
     * The regex string for a year
     */
    private static final String FORMAT_YEAR = "\\d{1,4}(\\/\\d{2})? ?(BC|B.C.|BCE)?";

    /**
     * Regex string for a day value
     */
    private static final String FORMAT_DAY = "(0?[1-9]|[12]\\d|3[01])";

    /**
     * Regex string for a month value
     */
    private static final String FORMAT_MONTH_GREGORIAN_JULIAN = "(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)";

    /**
     * Regex string for case insensitivity
     */
    private static final String FORMAT_CASE_INSENSITIVE = "(?i)";

    /**
     * The regex pattern that identifies a single, full Gregorian/Julian date, with year, month, and day
     */
    private static final Pattern PATTERN_SINGLE_DATE_FULL_GREGORIAN_JULIAN = Pattern.compile(FORMAT_CASE_INSENSITIVE + FORMAT_DAY
            + " " + FORMAT_MONTH_GREGORIAN_JULIAN + " " + FORMAT_YEAR);

    /**
     * The regex pattern that identifies a single date, with year, month, but no day
     */
    private static final Pattern PATTERN_SINGLE_DATE_MONTH_YEAR_GREGORIAN_JULIAN = Pattern.compile(FORMAT_CASE_INSENSITIVE
            + FORMAT_MONTH_GREGORIAN_JULIAN + " " + FORMAT_YEAR);

    /**
     * The regex pattern that identifies a single date, year only (no month or day)
     */
    private static final Pattern PATTERN_SINGLE_DATE_YEAR_ONLY = Pattern.compile(FORMAT_CASE_INSENSITIVE + FORMAT_YEAR);

    /**
     * The regex pattern that matches a string ending in a double-entry year
     */
    private static final Pattern PATTERN_ENDS_IN_DOUBLE_ENTRY_YEAR = Pattern.compile(FORMAT_CASE_INSENSITIVE + FORMAT_DATE_MISC
            + "\\d{4}\\/\\d{2}$");

    /**
     * The regex format for matching a Hebrew month (per GEDCOM spec)
     */
    private static final String FORMAT_MONTH_HEBREW = "(TSH|CSH|KSL|TVT|SHV|ADR|ADS|NSN|IYR|SVN|TMZ|AAV|ELL)";

    /**
     * The regex format for matching a French Republican month (per GEDCOM spec)
     */
    private static final String FORMAT_MONTH_FRENCH_REPUBLICAN = "(VEND|BRUM|FRIM|NIVO|PLUV|VENT|GERM|FLOR|PRAI|MESS|THER|FRUC|COMP)";

    /**
     * Pattern for matching a single Hebrew date in GEDCOM format
     */
    private static final Pattern PATTERN_SINGLE_HEBREW_DATE = Pattern.compile(FORMAT_CASE_INSENSITIVE + FORMAT_DAY + "? ?"
            + FORMAT_MONTH_HEBREW + "? ?\\d{4}");

    /**
     * Pattern for matching a single French Republican date in GEDCOM format
     */
    private static final Pattern PATTERN_SINGLE_FRENCH_REPUBLICAN_DATE = Pattern.compile(FORMAT_CASE_INSENSITIVE + FORMAT_DAY
            + "? ?" + FORMAT_MONTH_FRENCH_REPUBLICAN + "? ?\\d{1,4}");

    /**
     * Parse the string as date.
     * 
     * @param dateString
     *            the date string
     * @return the date, if one can be derived from the string
     */
    private static Date parseDate(String dateString, boolean last) {
        String ds = dateString.toUpperCase(Locale.US);
        if (ds.startsWith("@#DHEBREW@ ")) {
            return parseHebrew(ds.substring("@#DHEBREW@ ".length()), last);
        }
        if (ds.startsWith("@#DFRENCH R@ ")) {
            return parseFrenchRepublican(ds.substring("@#DFRENCH R@ ".length()), last);
        }
        if (ds.startsWith("@#DGREGORIAN@ ")) {
            return parseGregorianJulian(ds.substring("@#DGREGORIAN@ ".length()), last);
        }
        if (ds.startsWith("@#DJULIAN@ ")) {
            return parseGregorianJulian(ds.substring("@#DJULIAN@ ".length()), last);
        }
        return parseGregorianJulian(ds, last);
    }

    /**
     * Format a string so BC dates are turned into negative years, for parsing by {@link SimpleDateFormat}
     * @param dateString the date string, which may or may not have a BC suffix
     * @return the date formatted so BC dates have negative years
     */
    private static String formatBC(String dateString) {
        String d = dateString;
        if (d.endsWith("BC") || d.endsWith("BCE") || d.endsWith("B.C.") || d.endsWith("B.C.E.")) {
            String ds = d.substring(0, d.lastIndexOf('B')).trim();
            String yyyy = null;
            if (ds.lastIndexOf(' ') > -1) {
                yyyy = ds.substring(ds.lastIndexOf(' ')).trim();
                int i = Integer.parseInt(yyyy);
                int bc = 1 - i;
                String ddMMM = ds.substring(0, ds.lastIndexOf(' '));
                d = ddMMM + " " + bc;
            } else {
                yyyy = ds.trim();
                int i = Integer.parseInt(yyyy);
                d = Integer.toString(1 - i);
            }
        }
        return d;
    }

    /**
     * Convert a French Republican date string (in proper GEDCOM format) to a (Gregorian) java.util.Date.
     * @param frenchRepublicanDateString the French Republican date in GEDCOM spec format
     * @return the Gregorian date that represents the French Republican date supplied
     */
    private static Date parseFrenchRepublican(String frds, boolean last) {
        if (!PATTERN_SINGLE_FRENCH_REPUBLICAN_DATE.matcher(frds).matches()) {
            return null;
        }
        String[] datePieces = frds.split(" ");
        if (datePieces == null || datePieces.length < 1) {
            return null;
        }

        if (datePieces.length == 3) {
            return parseFrenchRepublicanDayMonthYear(datePieces);
        } else if (datePieces.length == 2) {
            return parseFrenchRepublicanMonthYear(datePieces, last);
        } else if (datePieces.length == 1) {
            return parseFrenchRepublicanYearOnly(datePieces, last);
        } else {
            return null;
        }

    }

    /**
     * Convert a Hebrew date string (in proper GEDCOM format) to a (Gregorian) java.util.Date.
     * 
     * @param hebrewDateString
     *            the Hebrew date in GEDCOM spec format - see DATE_HEBR and MONTH_HEBR in the spec.
     * @return the Gregorian date that represents the Hebrew date supplied
     */
    private static Date parseHebrew(String hds, boolean last) {
        if (!PATTERN_SINGLE_HEBREW_DATE.matcher(hds).matches()) {
            return null;
        }

        String[] datePieces = hds.split(" ");
        if (datePieces == null || datePieces.length < 1) {
            return null;
        }

        if (datePieces.length == 3) {
            return parseHebrewDayMonthYear(datePieces);
        } else if (datePieces.length == 2) {
            return parseHebrewMonthYear(last, datePieces);
        } else if (datePieces.length == 1) {
            return parseHebrewYearOnly(last, datePieces);
        } else {
            return null;
        }

    }

    /**
     * <p>
     * Resolve a date in double-dated format, for the old/new dates preceding the English calendar switch of 1752.
     * </p>
     * <p>
     * Because of the switch to the Gregorian Calendar in 1752 in England and its colonies, and the corresponding change of the
     * first day of the year, it's not uncommon for dates in the range between 1582 and 1752 to be written using a double-dated
     * format, showing the old and new dates simultaneously. For example, today we would render George Washington's birthday in
     * GEDCOM format as <code>22 FEB 1732</code>. However, in 1760 or so, one might have written it as Feb 22 1731/32, thus be
     * entered into a GEDCOM field as <code>22 FEB 1731/32</code>.
     * </p>
     * 
     * @param dateString
     *            the date string. Assumed to have had any BC (or similar) era suffix removed already, so the string is assumed to
     *            end in a year.
     * @return the date, resolved to a Gregorian date
     */
    private static String resolveEnglishCalendarSwitch(String dateString) {
        if (!PATTERN_ENDS_IN_DOUBLE_ENTRY_YEAR.matcher(dateString).matches()) {
            return dateString;
        }

        int l = dateString.length();
        String oldYYYY = dateString.substring(l - 7, l - 3);
        int yyyy = Integer.parseInt(oldYYYY);
        if (yyyy > 1752 || yyyy < 1582) {
            return dateString;
        }

        String newYY = dateString.substring(l - 2);
        int yy = Integer.parseInt(newYY);

        // Handle century boundary
        if (yy == 0 && yyyy % 100 == 99) {
            yyyy++;
        }

        String upToYYYY = dateString.substring(0, l - 7);
        StringBuilder ds = new StringBuilder(upToYYYY);
        ds.append(yyyy / 100);
        ds.append(newYY);
        return ds.toString();
    }

    /**
     * Attempt to parse <code>dateString</code> using date format <code>fmt</code>. If successful, return the date. Otherwise return
     * null.
     * 
     * @param dateString
     *            the date string
     * @param pattern
     *            the date format to try parsing with
     * @return the date if successful, or null if the date cannot be parsed using the format supplied
     */
    private static Date getDateWithFormatString(String dateString, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(dateString);
        } catch (@SuppressWarnings("unused") ParseException ignored) {
            return null;
        }
    }

    /**
     * Get the date from a date string when the string is formatted with a month and year but no day
     * 
     * @param dateString
     *            the date string
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date getYearMonthDay(String dateString) {
        String bc = formatBC(dateString);
        String e = resolveEnglishCalendarSwitch(bc);
        return getDateWithFormatString(e, "dd MMM yyyy");
    }

    /**
     * Get the date from a date string when the string is formatted with a month and year but no day
     * 
     * @param dateString
     *            the date string
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date getYearMonthNoDay(String dateString, boolean last) {
        String bc = formatBC(dateString);
        String e = resolveEnglishCalendarSwitch(bc);
        Date d = getDateWithFormatString(e, "MMM yyyy");
        Calendar c = Calendar.getInstance(Locale.US);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTime(d);
        c.set(Calendar.DAY_OF_MONTH, 1);
        if (last) {
            c.add(Calendar.MONTH, 1);
            c.add(Calendar.DAY_OF_YEAR, -1);
        }
        return c.getTime();
    }

    /**
     * Get the date from a date string when the string is formatted with a year but no month or day
     * 
     * @param dateString
     *            the date string
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date getYearOnly(String dateString, boolean last) {
        String bc = formatBC(dateString);
        String e = resolveEnglishCalendarSwitch(bc);
        Date d = getDateWithFormatString(e, "yyyy");
        Calendar c = Calendar.getInstance(Locale.US);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTime(d);
        c.set(Calendar.DAY_OF_YEAR, 1);
        if (last) {
            // Last day of year
            c.set(Calendar.MONTH, Calendar.DECEMBER);
            c.set(Calendar.DAY_OF_MONTH, 31);
        }
        return c.getTime();
    }

    /**
     * Get the date from a French Republican date string when the string is formatted with a year, month, and day
     * @param datePieces 3-element array with the day, month, and year (in that order).
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date parseFrenchRepublicanDayMonthYear(String... datePieces) {
        FrenchRepublicanCalendarParser.Month frMonth = FrenchRepublicanCalendarParser.Month.getFromGedcomAbbrev(datePieces[1]);
        if (frMonth == null) {
            return null;
        }
        FrenchRepublicanCalendarParser frc = new FrenchRepublicanCalendarParser();
        int frYear = Integer.parseInt(datePieces[2]);
        int frDay = Integer.parseInt(datePieces[0]);

        return frc.convertFrenchRepublicanDateToGregorian(frYear, frMonth.getGedcomAbbrev(), frDay);
    }

    /**
     * Get the date from a French Republican date string when the string is formatted with a year and month but no day
     * @param datePieces 2-element array with month and year (in that order)
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date parseFrenchRepublicanMonthYear(String[] datePieces, boolean last) {
        FrenchRepublicanCalendarParser.Month frMonth = FrenchRepublicanCalendarParser.Month.getFromGedcomAbbrev(datePieces[0]);
        if (frMonth == null) {
            return null;
        }
        FrenchRepublicanCalendarParser frc = new FrenchRepublicanCalendarParser();
        int frYear = Integer.parseInt(datePieces[1]);
        int frDay = 1;
        if (last) {
            if (frMonth == FrenchRepublicanCalendarParser.Month.JOUR_COMPLEMENTAIRS) {
                if (frc.isFrenchLeapYearRomme(frYear)) {
                    frDay = 6;
                } else {
                    frDay = 5;
                }
            } else {
                frDay = 30;
            }
        }
        return frc.convertFrenchRepublicanDateToGregorian(frYear, frMonth.getGedcomAbbrev(), frDay);
    }

    /**
     * Get the date from a French Republican date string when the string is formatted with a year but no month or day
     * @param datePieces 1-element array containing the year
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date parseFrenchRepublicanYearOnly(String[] datePieces, boolean last) {
        FrenchRepublicanCalendarParser frc = new FrenchRepublicanCalendarParser();
        int frYear = Integer.parseInt(datePieces[0]);
        int frDay;
        FrenchRepublicanCalendarParser.Month frMonth = FrenchRepublicanCalendarParser.Month.VENDEMIAIRE;
        frDay = 1;
        if (last) {
            frMonth = FrenchRepublicanCalendarParser.Month.JOUR_COMPLEMENTAIRS;
            if (frc.isFrenchLeapYearRomme(frYear)) {
                frDay = 6;
            } else {
                frDay = 5;
            }
        }
        return frc.convertFrenchRepublicanDateToGregorian(frYear, frMonth.getGedcomAbbrev(), frDay);
    }

    /**
     * Parse a Gregorian or Julian date string
     * @param dateString the date string to parse
     * @return the date, if one can be derived from the string
     */
    private static Date parseGregorianJulian(String ds, boolean last) {
        Date d = null;
        if (PATTERN_SINGLE_DATE_FULL_GREGORIAN_JULIAN.matcher(ds).matches()) {
            d = getYearMonthDay(ds);
        } else if (PATTERN_SINGLE_DATE_MONTH_YEAR_GREGORIAN_JULIAN.matcher(ds).matches()) {
            d = getYearMonthNoDay(ds, last);
        } else if (PATTERN_SINGLE_DATE_YEAR_ONLY.matcher(ds).matches()) {
            d = getYearOnly(ds, last);
        }
        return d;
    }

    /**
     * Get the date from a Hebrew date string when the string is formatted with a year, month, and day
     * @param datePieces 3-element array with the day, month, and year (in that order).
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date parseHebrewDayMonthYear(String... datePieces) {
        HebrewCalendarParser.Month hebrewMonth = HebrewCalendarParser.Month.getFromAbbreviation(datePieces[1]);
        if (hebrewMonth == null) {
            // Didn't find a matching month abbreviation
            return null;
        }
        HebrewCalendarParser hc = new HebrewCalendarParser();
        int hebrewDay = Integer.parseInt(datePieces[0]);
        int hebrewYear = Integer.parseInt(datePieces[2]);
        return hc.convertHebrewDateToGregorian(hebrewYear, hebrewMonth.getGedcomAbbrev(), hebrewDay);
    }

    /**
     * Get the date from a Hebrew date string when the string is formatted with a year and month but no day
     * @param datePieces 2-element array with month and year (in that order)
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date parseHebrewMonthYear(boolean last, String... datePieces) {
        HebrewCalendarParser.Month hebrewMonth = HebrewCalendarParser.Month.getFromAbbreviation(datePieces[0]);
        if (hebrewMonth == null) {
            return null;
        }
        HebrewCalendarParser hc = new HebrewCalendarParser();
        int hebrewYear = Integer.parseInt(datePieces[1]);
        int hebrewDay = 1;
        if (last) {
            hebrewDay = hc.getMonthLength(hebrewYear, hebrewMonth);
        }
        return hc.convertHebrewDateToGregorian(hebrewYear, hebrewMonth.getGedcomAbbrev(), hebrewDay);
    }

    /**
     * Get the date from a Hebrew date string when the string is formatted with a year but no month or day
     * @param datePieces 1-element array containing the year
     * @return the date found, if any, or null if no date could be extracted
     */
    private static Date parseHebrewYearOnly(boolean last, String... datePieces) {
        HebrewCalendarParser hc = new HebrewCalendarParser();
        int hebrewYear = Integer.parseInt(datePieces[0]);
        HebrewCalendarParser.Month hebrewMonth = HebrewCalendarParser.Month.TISHREI;
        int hebrewDay = 1;
        if (last) {
            hebrewMonth = HebrewCalendarParser.Month.ELUL;
            hebrewDay = hc.getMonthLength(hebrewYear, hebrewMonth);
        }
        return hc.convertHebrewDateToGregorian(hebrewYear, hebrewMonth.getGedcomAbbrev(), hebrewDay);
    }

    public enum Qualifier {
        Invalid, Exact, After, Before, Range, About, Calculated, Estimated
    }
}
