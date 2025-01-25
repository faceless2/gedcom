package gedcomj;

import java.util.*;

/**
 * The Header Record
 */
public class Header extends Record {

    Header(GEDCOM gedcom, String tag) {
        super(gedcom, tag);
    }

    /**
     * Return the charset of the file, or null if it's unspecified or invalid
     */
    public String getCharset() {
        Record r = getRecord("CHAR");
        if (r != null) {
            String s = r.getValue();
            if ("UTF-8".equalsIgnoreCase(s) || "ASCII".equalsIgnoreCase(s) || "ANSEL".equalsIgnoreCase(s)) {
                return s.toUpperCase();
            }
        }
        return null;
    }

    /**
     * Return the version of the file as a String
     */
    public String getVersion() {
        Record r = getRecord("GEDC");
        if (r != null) {
            r = r.getRecord("VERS");
            if (r != null) {
                return r.getValue();
            }
        }
        return null;
    }

    /**
     * Return the major version of the file (typically 5 or 7), or 0 if unspecified
     */
    public int getMajorVersion() {
        Record r = getRecord("GEDC");
        if (r != null) {
            r = r.getRecord("VERS");
            if (r != null) {
                String s = r.getValue();
                if (s.length() > 1) {
                    char c = s.charAt(0);
                    if (c >= '1' && c <= '9' && (s.length() == 1 || s.charAt(1) == '.')) {
                        return c - '0';
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Set the version of the file
     * @param version a value, which really should be "5.5.1" or something that begins with "7.0"
     */
    public void setVersion(String version) {
        Record r = getRecord("GEDC");
        if (r == null) {
            getRecords().add(0, r = gedcom.newRecord("GEDC", null));
        }
        Record r2 = r.getRecord("VERS");
        if (r2 == null) {
            r.getRecords().add(0, r2 = gedcom.newRecord("VERS", version));
        }
        r2.setValue(version);
    }

}
