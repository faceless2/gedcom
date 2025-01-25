package gedcomj;

import java.io.*;
import java.util.*;
import java.nio.charset.*;

/**
 * Represents a GEDCOM file
 */
public class GEDCOM {
    
    /**
     * An option to force a newline after a NOTE. Corrects WikiTree (as of 202405) Biography output
     */
    public static final String OPTION_NL_AFTER_NOTE = "note-cont-insert-nl";

    /**
     * An option to always allow whitespace before levels during parsing, even for v7 files
     */
    public static final String OPTION_TOLERATE_WHITESPCE = "leading-whitespace";

    /**
     * An option that sets the default character set (by default, its UTF-8
     */
    public static final String OPTION_DEFAULT_CHARSET = "charset";



    private static final int CS_UTF8 = 0, CS_ASCII = 1, CS_ANSEL = 2;

    private final List<Record> records = new RecordList(this, null) {
        @Override protected void notifyRemoved(Record r) {
            String id = r.getId();
            if (id != null) {
                idtable.remove(id);
            }
        }
        @Override protected void notifyAdded(Record r) {
            String id = r.getId();
            if (id != null) {
                if (idtable.put(id, r) != null) {
                    throw new IllegalStateException("Duplicate id \"" + id + "\"");
                }
            }
        }
    };

    private final Map<String,String> options = new HashMap<String,String>();
    private final Map<String,Record> idtable = new HashMap<String,Record>();

    public GEDCOM() {
    }

    /**
     * Get a Map which can contain various options to control parsing
     */
    public Map<String,String> getOptions() {
        return options;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Record r : records) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(r);
            sb.append('\n');
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Load a GEDCOM from the specified file. Any records that
     * alread exist are removed;
     * @param in the InputStream
     */
    public void read(InputStream in) throws IOException {
        getRecords().clear();
        idtable.clear();
        in = new BufferedInputStream(in);
        int charset = CS_UTF8;
        if ("ASCII".equalsIgnoreCase(options.get(OPTION_DEFAULT_CHARSET))) {
            charset = CS_ASCII;
        } else if ("ANSEL".equalsIgnoreCase(options.get(OPTION_DEFAULT_CHARSET))) {
            charset = CS_ANSEL;
        }
        int majorversion = 5;
        int line = 0;
        boolean flag_whitespace = options.containsKey(OPTION_TOLERATE_WHITESPCE);
        boolean flag_insertnewline = false;
        StringBuilder sb = new StringBuilder();
        int c = in.read();
        if (c == 0xEF) {
            if (in.read() != 0xBB && in.read() != 0xBF) {
                throw new IOException("Invalid initial bytes, not a BOM or level");
            } else {
                c = in.read();
            }
        }
        if (c == '[') {
            // Read JSON format
            sb.append('[');
            Reader r = new InputStreamReader(in, "UTF-8");
            while ((c = r.read()) >= 0) {
                sb.append((char)c);
            }
            @SuppressWarnings("unchecked") List<Map<String,Object>> l = (List<Map<String,Object>>)Stringify.parse(sb.toString());
            for (Map<String,Object> m : l) {
                records.add(jsonToRecord(m));
            }
            return;
        }
        Record prev = null;
        ByteArrayOutputStream value = new ByteArrayOutputStream();
        Set<String> seenid = new HashSet<String>();
        while (c >= 0) {
            int valueSize = value.size();
            int level = 0;
            String id = null, tag = null, idref = null;
            if (flag_whitespace || majorversion < 7) {  // not allowed in V7
                while (c == 0x20) {
                    c = in.read();
                }
                if (c == 0x0D) {
                    c = in.read();
                    if (c == 0x0A) {
                        c = in.read();
                    }
                    line++;
                    continue;
                } else if (c == 0x0A) {
                    c = in.read();
                    line++;
                    continue;
                }
            }
            // Read level
            if (c >= '0' && c <= '9') {
                level = c - '0';
                while ((c = in.read()) >= '0' && c <= '9') {
                    level = level * 10 + c - '0';
                }
                if (c != ' ') {
                    fail("Expected space after level", c, line);
                }
                c = in.read();
            } else {
                fail("Expected level", c, line);
            }
            // Read optional id
            if (c == '@') {
                while (((c = in.read()) >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '_') {
                    sb.append((char)c);
                }
                if (c == '@') {
                    id = sb.toString();
                    if (id.length() == 0) {
                        fail("Zero length id", Integer.MAX_VALUE, line);
                    } else if (id.equals("VOID")) {
                        fail("VOID id", Integer.MAX_VALUE, line);
                    } else if (!seenid.add(id)) {
                        warning("Duplicate id \"" + id + "\", keeping first", line);
                        id = null;
                    }
                    sb.setLength(0);
                    c = in.read();
                    if (c != ' ') {
                        fail("Expected space after id", c, line);
                    }
                    c = in.read();
                } else {
                    fail("Expected '@' after id", c, line);
                }
            }
            // Read tag
            if (c == '_' || (c >= 'A' && c <= 'Z')) {
                sb.append((char)c);
                while (((c = in.read()) >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '_') {
                    sb.append((char)c);
                }
                if (c == ' ') {
                    c = in.read();
                } else if (c != 0x0D && c != 0x0A && c >= 0) {
                    fail("Invalid tag character", c, line);
                }
                tag = sb.toString();
                sb.setLength(0);
                if (tag.equals("_")) {
                    fail("Zero length tag", Integer.MAX_VALUE, line);
                }
            } else {
                fail("Expected tag", c, line);
            }


            boolean append = prev != null && ((majorversion < 7 && "CONC".equals(tag)) || "CONT".equals(tag)) && level == prev.level() + 1 && prev.getIdRef() == null;
            if (append && ("CONT".equals(tag) || flag_insertnewline)) {
                value.write(0x0A);
                flag_insertnewline = false;
            }
            if (prev != null && prev.getIdRef() == null && !append) {
                prev.setValue(toValueString(value, line, charset, majorversion));
                value.reset();
            }

            if (c == 0x0D || c == 0x0A || c < 0) {
                // No value;
            } else if (c == '@') {
                c = in.read();
                if (c == '@') {
                    value.write(c);
                    while ((c = in.read()) == 0x09 || c >= 0x20) {
                        value.write(c);
                    }
                } else {
                    sb.append((char)c);
                    while (((c = in.read()) >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '_') {
                        sb.append((char)c);
                    }
                    if (c != '@') {
                        fail("Expected '@' after idref", c, line);
                    }
                    idref = sb.toString();
                    if (idref.length() == 0) {
                        fail("Zero length idref", Integer.MAX_VALUE, line);
                    }
                    sb.setLength(0);
                    c = in.read();
                }
            } else if (c == 0x09 || c >= 0x20) {
                value.write(c);
                while ((c = in.read()) == 0x09 || c >= 0x20) {
                    value.write(c);
                }
            } else {
                fail("Expected value", c, line);
            }
            if (c == 0x0D) {
                c = in.read();
                if (c == 0x0A) {
                    c = in.read();
                }
                line++;
            } else if (c == 0x0A) {
                c = in.read();
                line++;
            } else if (c >= 0) {
                fail("Invalid character in value", c, line);
            }
            if (append) {
                continue;
            }

            Record e = newRecord(tag, null);;
            e.setLineNumber(line);
            if (id != null) {
                e.setId(id);
            }
            if (idref != null) {
                e.setIdRef(idref);
            }
            if (level == 0) {
                getRecords().add(e);
            } else if (level > prev.level() + 1) {
                fail("Invalid nesting from level " + prev.level() + " to " + level, Integer.MAX_VALUE, line);
            } else {
                while (prev.level() >= level) {
                    prev = prev.owner();
                }
                prev.getRecords().add(e);
            }
            if ("CHAR".equals(tag) && level == 1 && e.owner().tag().equals("HEAD")) {
                String val = toValueString(value, line, charset, majorversion);
                if ("UTF-8".equalsIgnoreCase(val)) {
                    charset = CS_UTF8;
                } else if ("ASCII".equalsIgnoreCase(val)) {
                    charset = CS_ASCII;
                } else if ("ANSEL".equalsIgnoreCase(val)) {
                    charset = CS_ANSEL;
                } else {
                    warning("Ignoring unsupported charset \"" + charset + "\"", line);
                }
                if (majorversion >= 7 && charset != CS_UTF8) {
                    throw new IOException("Invalid charset in version 7 \"" + charset + "\"");
                }
            } else if ("VERS".equals(tag) && level == 2 && e.owner().tag().equals("GEDC") && e.owner().owner().tag().equals("HEAD")) {
                String val = toValueString(value, line, charset, majorversion);
                if (val.startsWith("7.")) {
                    majorversion = 7;
                    if (charset != CS_UTF8) {
                        throw new IOException("Invalid charset in version 7");
                    }
                } else if (val.startsWith("5.")) {
                    majorversion = 5;
                }
            } else if ("NOTE".equals(tag) && options.containsKey(OPTION_NL_AFTER_NOTE)) {
                flag_insertnewline = true;
            }
            prev = e;
        }
        if (prev != null && prev.getIdRef() == null) {
            prev.setValue(toValueString(value, line, charset, majorversion));
        }
    }

    private static final String ANSEL;
    static {
        char[] c = new char[256];
        for (int i=0;i<0x80;i++) {
            c[i] = (char)i;
        }
        String q = "\u0141\u00D8\u0110\u00DE\u00C6\u0152\u02B9\u00B7\u266D\u00AE\u00B1\u01A0\u01AF\u02BC\u0000\u02BB\u0142\u00F8\u0111\u00FE\u00E6\u0153\u02BA\u0131\u00A3\u00F0\u0000\u01A1\u01B0\u25A1\u25A0\u00B0\u2113\u2117\u00A9\u266F\u00BF\u00A1\u0000\u20AC\u0000\u0000\u0000\u0000\u0065\u006F\u00DF\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0309\u0300\u0301\u0302\u0303\u0304\u0306\u0307\u0308\u030C\u030A\uFE20\uFE21\u0315\u030B\u0310\u0327\u0328\u0323\u0324\u0325\u0333\u0332\u0326\u031C\u032E\uFE22\uFE23\u0338\u0000\u0313\u0000"; // starts at 0xa1
        for (int i=0xA1;i<0x100;i++) {
            c[i] = q.charAt(i - 0xA1);
        }
        ANSEL = new String(c);
    }

    private Record jsonToRecord(Map<String,Object> m) {
        String tag = (String)m.get("tag");
        String id = (String)m.get("id");
        String value = (String)m.get("value");
        String idref = (String)m.get("idref");
        @SuppressWarnings("unchecked") List<Map<String,Object>> kids = (List<Map<String,Object>>)m.get("records");
        Record r = newRecord(tag, null);
        if (id != null) {
            r.setId(id);
        }
        if (idref != null) {
            r.setIdRef(idref);
        } else if (value != null) {
            r.setValue(value);
        }
        if (kids != null) {
            for (Map<String,Object> m2 : kids) {
                r.getRecords().add(jsonToRecord(m2));
            }
        }
        return r;
    }

    private String toValueString(ByteArrayOutputStream in, int line, int charset, int majorversion) throws IOException {
        String s = new String(in.toByteArray(), charset == CS_UTF8 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1);
        if (charset == CS_ANSEL) {
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<s.length();i++) {
                int c0 = s.charAt(i);
                char c1 = ANSEL.charAt(c0);
                if (c1 == 0) {
                    fail("Invalid ANSEL codepoint 0x" + Integer.toHexString(c0), Integer.MAX_VALUE, line);
                }
                sb.append(c1);
            }
            s = sb.toString();
        }
        if (majorversion >= 7) {
            for (int i=0;i<s.length();) {
                int c = s.codePointAt(i);
                if (c == 0xFEFF || (c >= 0x7F && c <= 0x9F) || (c < 0x20 && c != 0x09 && c != 0x0A)) {
                    fail("Banned character in value", c, line);
                }
                i += c < 0x10000 ? 1 : 2;
            }
        } else {
            for (int i=0;i<s.length();) {
                int c = s.codePointAt(i);
                if ((c >= 0x7F && c <= 0x9F) || (c < 0x20 && c != 0x09 && c != 0x0A)) {
                    fail("Banned character in value", c, line);
                }
                i += c < 0x10000 ? 1 : 2;
            }
            s = s.replaceAll("@@", "@");
        }
        return s;
    }

    private void warning(String msg, int line) throws IOException {
        if (line >= 0) {
            msg += " (line " + line + ")";
        }
        System.out.println("WARNING: " + msg);
    }

    private void fail(String msg, int c, int line) throws IOException {
        if (c >= 0x20 && c <= 0x7e) {
            msg += " (got '" + ((char)c) + "')";
        } else if (c < 0) {
            msg += " (got EOF)";
        } else if (c < Integer.MAX_VALUE) {
            msg += " (got 0x" + Integer.toHexString(c) + ")";
        }
        throw new IOException(msg + " (line " + line + ")");
    }

    //-------------------------------------------------------------

    Record resolveIdRef(String id) {
        return idtable.get(id);
    }

    String nextId(String tag) {
        int i = 0;
        while (idtable.containsKey("R"+i)) {
            i++;
        }
        return "R" + i;
    }

    /**
     * Get the (mandatory) HEAD record
     */
    public Header getHeader() {
        if (records.size() > 0 && records.get(0) instanceof Header) {
            return (Header)records.get(0);
        }
        return null;
    }

    /**
     * Return a modifiable list of all records in this GEDCOM
     */
    public List<Record> getRecords() {
        return records;
    }

    /**
     * Create a new record which can be added to this GEDCOM or one of its records
     * @param tag the tag (required)
     * @param value the value (optional)
     */
    public Record newRecord(String tag, String value) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag is null");
        }
        Record r;
        switch (tag) {
            case "HEAD": r = new Header(this, tag); break;
            case "DATE": r = new GDate(this, tag); break;
            case "INDI": r = new Person(this, tag); break;
            case "FAM":  r = new Family(this, tag); break;
            case "OBJE": r = new Multimedia(this, tag); break;
            case "NOTE": r = new Note(this, tag); break;
            case "REPO": r = new Repository(this, tag); break;
            case "SOUR": r = new Source(this, tag); break;
            case "SUBM": r = new Submitter(this, tag); break;
            default: r = new Record(this, tag);
        }
        if (value != null) {
            r.setValue(value);
        }
        return r;
    }

    /**
     * Create a new record which is a reference to another record
     * @param tag the tag (required)
     * @param r the record to point to - must have had {@link Record#setId} called on it
     */
    public Record newReference(String tag, Record r) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag is null");
        }
        if (r == null) {
            throw new IllegalArgumentException("Record is null");
        }
        if (r.gedcom != this) {
            throw new IllegalArgumentException("Record is from another GEDCOM");
        }
        if (r.getId() == null) {
            throw new IllegalArgumentException("Record is not indirect");
        }
        Record out = new Record(this, tag);
        out.setIdRef(r.getId());
        return out;
    }

    /**
     * Write the GEDCOM to a stream
     * @param out the OutputStream to write to
     */
    public void write(OutputStream out) throws IOException {
        Header header = getHeader();
        if (header == null) {
            throw new IllegalStateException("No header");
        }
        Record r = header.getRecord("CHAR");
        if (r == null) {
            header.getRecords().add(newRecord("CHAR", "UTF-8"));
        } else if (!r.getValue().equals("UTF-8")) {
            header.getRecords().set(header.getRecords().indexOf(r), newRecord("CHAR", "UTF-8"));
        }
        int majorversion = header.getMajorVersion();
        final int maxlength = majorversion <= 5 ? 90 : Integer.MAX_VALUE;
        int[] line = new int[1];
        for (int i=0;i<getRecords().size();i++) {
            r = getRecords().get(i);
            if (i + 1 == getRecords().size() && r.tag().equals("TRLR")) {
                continue;
            }
            if (r instanceof Family) {
                Family f = (Family)r;
                if (f.getHusband() == null && f.getWife() == null && f.getChildren().isEmpty()) {
                    continue;
                }
            }
            write(out, r, line, maxlength);
        }
        out.write("0 TRLR\n".getBytes("UTF-8"));
        out.flush();
    }

    private void write(final OutputStream out, final Record r, final int[] line, final int maxlength) throws IOException {
        if (r.getIdRef() != null && !idtable.containsKey(r.getIdRef())) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(r.level());
        sb.append(' ');
        if (r.getId() != null) {
            sb.append('@');
            sb.append(r.getId());
            sb.append("@ ");
        }
        sb.append(r.tag());
        if (r.getIdRef() != null) {
            sb.append(" @");
            sb.append(r.getIdRef());
            sb.append("@");
            out.write(sb.toString().getBytes("UTF-8"));
        } else {
            byte[] b = sb.toString().getBytes("UTF-8");
            out.write(b);
            int len = b.length;
            b = r.getValue().getBytes("UTF-8");
            if (b.length > 0) {
                out.write(' ');
                len++;
            }
            for (int i=0;i<b.length;i++) {
                int v = b[i] & 0xFF;
                int charwidth = v < 0xC0 ? 1 : v < 0xDF ? 2 : v < 0xEF ? 3 : 4;
                if (v == 0x0D) {
                    continue;
                } else if (v == 0x0A) {
                    out.write(0x0A);
                    line[0]++;
                    sb.setLength(0);
                    sb.append(r.level() + 1);
                    sb.append(" CONT");
                    byte[] bt = sb.toString().getBytes("UTF-8");
                    out.write(bt);
                    if (!(i + 1 == b.length || (b[i + 1] & 0xFF) == 0x0A)) {
                        out.write(' ');
                    }
                    len = bt.length;
                } else if (len + charwidth >= maxlength) {
                    out.write(0x0A);
                    line[0]++;
                    sb.setLength(0);
                    sb.append(r.level() + 1);
                    sb.append(" CONC ");
                    byte[] bt = sb.toString().getBytes("UTF-8");
                    out.write(bt);
                    len = bt.length;
                    out.write(v);
                    len++;
                } else {
                    out.write(v);
                    len++;
                }
            }
        }
        out.write(0x0A);
        line[0]++;
        for (Record r2 : r.getRecords()) {
            write(out, r2, line, maxlength);
        }
    }

}
