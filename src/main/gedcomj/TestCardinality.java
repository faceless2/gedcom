package gedcomj;

import java.text.*;
import java.util.*;
import java.io.*;

/**
 * The Cardinality Test verifies that the children of each node is allowed and that
 * there are not too many or too few of them.
 */
public class TestCardinality extends Verifier.Test {

    // No apologies for this class, was in a hurry

    private static class Structure {
        private Map<String,RecordDef> definitions = new HashMap<String,RecordDef>();
        private BufferedReader in;
        private int line;
        RecordDef getRecordDef(String name) {
            return definitions.get(name);
        }
        void parse(BufferedReader in) throws IOException {
            this.in = in;
            RecordDef def;
            while ((def = parseRecordDef()) != null) {
//                System.out.println();
//                System.out.println(def);
//                System.out.println();
                definitions.put(def.name, def);
            }
        }

        RecordDef parseRecordDef() throws IOException {
            String ss;
            do {
                ss = in.readLine();
                line++;
                if (ss == null) {
                    return null;
                }
                ss = ss.replaceAll("#.*", "").trim();
            } while (ss.length() == 0);
            String[] s = ss.split("  *");
//            System.out.println("S0: " + Arrays.toString(s));
            if (s.length == 2 && s[1].equals(":=")) {
                RecordDef def = new RecordDef();
                def.name = s[0];
                s = in.readLine().replaceAll("#.*", "").trim().split("  *");
                line++;
//                System.out.println("S1: " + Arrays.toString(s));
                if (s.length == 1 && s[0].equals("[")) {
                    while (true) {
                        s = in.readLine().replaceAll("#.*", "").trim().split("  *");
                        line++;
//                        System.out.println("S2: " + Arrays.toString(s));
                        if (s.length == 1 && s[0].equals("|")) {
                            while (def.parent != null) {
                                def = def.parent;
                            }
                            def.nextAlt();
                        } else if (s.length == 1 && s[0].equals("]")) {
                            break;
                        } else {
                            def = parseRecordSubDef(def, s);
                        }
                    }
                    s = in.readLine().replaceAll("#.*", "").trim().split("  *");
                    line++;
                } else {
                    do {
                        def = parseRecordSubDef(def, s);
                        s = in.readLine().replaceAll("#.*", "").trim().split("  *");
                        line++;
//                        System.out.println("S3: " + Arrays.toString(s)+"/"+s.length);
                    } while (s.length > 1);
                }
                while (def.parent != null) {
                    def = def.parent;
                }
                return def;
            } else {
                throw new IOException(Arrays.toString(s)+" (line "+line+")");
            }
        }

        RecordDef parseRecordSubDef(RecordDef def, String[] s) throws IOException {
            int depth;
//            System.out.println("P" + Arrays.toString(s));

            RecordDef subdef = new RecordDef();
            if (s[0].equals("n") || s[0].equals("0")) {
                depth = 0;
            } else if (s[0].length() > 1 && s[0].charAt(0) == '+') {
                depth = Integer.parseInt(s[0].substring(1));
            } else {
                throw new IOException(Arrays.toString(s));
            }
            int i = 1;
            if (s[i].startsWith("@") && s[i].endsWith("@")) {
                subdef.needxref = true;
                i++;
            }
            if (s[i].startsWith("<<") && s[i].endsWith(">>")) {
                subdef.pointer = true;
                subdef.name = s[i].substring(2, s[i].length() - 2);
                i++;
            } else if (s[i].equals("[")) {
                i++;
                subdef.tags = new ArrayList<String>();
                while (!s[i].equals("]")) {
                    subdef.tags.add(s[i++]);
                    if (s[i].equals("]")) {
                        break;
                    } else if (!s[i].equals("|")) {
                        throw new IOException(Arrays.toString(s)+" i="+i);
                    }
                    i++;
                }
                i++;
            } else {
                subdef.tags = Collections.<String>singletonList(s[i]);
                i++;
            }
            if (s[i].startsWith("@<") && s[i].endsWith(">@")) {
                subdef.type = s[i].substring(2, s[i].length() - 2);
                subdef.needpointer = true;
                i++;
            } else if (s[i].startsWith("<") && s[i].endsWith(">")) {
                subdef.type = s[i].substring(1, s[i].length() - 1);
                i++;
            }
            if (s[i].charAt(0) == '{' && s[i].charAt(s[i].length() - 1) == '}') {
                int ix = s[i].indexOf(":");
                subdef.min = Integer.parseInt(s[i].substring(1, ix));
                subdef.max = s[i].endsWith(":M}") ? Integer.MAX_VALUE : Integer.parseInt(s[i].substring(ix + 1, s[i].length() - 1));
            } else {
                throw new IOException(Arrays.toString(s)+" i="+i+" (line " + line + ")");
            }
            while (def.parent != null && depth < def.depth()) {
//                System.out.println("D="+def+" ID="+depth);
                def = def.parent;
            }
            def.add(subdef);
            return subdef;
        }
    }

    private static class RecordDef {
        RecordDef parent;
        String name, type;
        List<String> tags;
        List<List<RecordDef>> kids;
        int min, max, alt;
        boolean needxref, needpointer, pointer;
        
        RecordDef() {
            kids = new ArrayList<List<RecordDef>>();
        }

        int depth() {
            return parent == null ? 0 : parent.depth() + 1;
        }

        void nextAlt() {
            alt++;
        }

        void add(RecordDef def) {
            while (kids.size() <= alt) {
                kids.add(new ArrayList<RecordDef>());
            }
            kids.get(alt).add(def);
            def.parent = this;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"depth\":" + depth());
            if (pointer) {
                sb.append(",\"def\":\"" + name + "\"");
            } else if (tags != null) {
                if (tags.size() == 1) {
                    sb.append(",\"tag\":\"" + tags.get(0) + "\"");
                } else {
                    sb.append(",\"tag\":[");
                    for (int i=0;i<tags.size();i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        sb.append("\"" + tags.get(i) + "\"");
                    }
                    sb.append("]");
                }
            }
            if (needxref) {
                sb.append(",\"indirect\":true");
            }
            if (type != null) {
                sb.append(",\"type\":\"" + type + "\"");
            }
            if (min > 0) {
                sb.append(",\"min\":" + min);
            }
            if (max < Integer.MAX_VALUE) {
                sb.append(",\"max\":" + max);
            }
            if (!kids.isEmpty()) {
                sb.append(",\"kids\":[");
                for (int i=0;i<kids.size();i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append("[");
                    List<RecordDef> l = kids.get(i);
                    for (int j=0;j<l.size();j++) {
                        if (j > 0) {
                            sb.append(",");
                        }
                        sb.append(l.get(j).toString());
                    }
                    sb.append("]");
                }
                sb.append("]");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    private static Structure structure7, structure5;

    synchronized static Structure getStructure(int version) {
        version = 5;
        Structure s = version == 7 ? structure7 : structure7;
        if (s == null) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(GEDCOM.class.getResourceAsStream("v" + version + ".dat"), "UTF-8"));
                s = new Structure();
                s.parse(r);
                r.close();
                if (version == 7) {
                    structure7 = s;
                } else {
                    structure5 = s;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return s;
    }

    @Override public void test(final Verifier verifier, final GEDCOM gedcom, final List<Verifier.Fault> faults) {
        Structure s = getStructure(gedcom.getHeader().getMajorVersion());
        RecordDef def = match(s, gedcom.getHeader(), s.getRecordDef("HEADER"), null);
        test(verifier, gedcom, s, gedcom.getHeader(), def, faults);
        for (int i=1;i<gedcom.getRecords().size() - 1;i++) {
            final Record record = gedcom.getRecords().get(i);
            def =  match(s, record, s.definitions.get("RECORD"), null);
            if (def == null) {
                faults.add(new Verifier.Fault(record, this, "Structure: \"" + record.tag() + "\" doesn't match <RECORD>", "Remove this record", Verifier.Severity.DataLoss) {
                    public boolean fix() {
                        gedcom.getRecords().remove(record);
                        return true;
                    }
                });
            } else {
                test(verifier, gedcom, s, record, def, faults);
            }
        }
    }

    /**
     * If record matches a construction within the supplied definition,
     * return the matching definition (which will be direct)
     * @param structure the structure
     * @param record the record to match
     * @param def the initial recorddef
     * @param steps if not null, will be populated with a list of steps to get to the solution
     * @return the matching RecordDef or null if none found
     */
    private RecordDef match(final Structure structure, final Record record, final RecordDef def, List<RecordDef> steps) {
        final int size = steps == null ? 0 : steps.size();
        if (steps != null) {
            steps.add(def);
        }
        if (def.parent == null) {
            // Go through all the alts and see which one matches
            for (int alt=0;alt<def.kids.size();alt++) {
                for (int i=0;i<def.kids.get(alt).size();i++) {
                    final RecordDef def2 = def.kids.get(alt).get(i);
                    if (def2.name != null) {
                        if (steps != null) {
                            steps.add(def2);
                        }
                        final RecordDef def3 = structure.getRecordDef(def2.name);
                        final RecordDef def4 = match(structure, record, def3, steps);
                        if (def4 != null) {
                            return def4;
                        } 
                        if (steps != null) {
                            steps.subList(size + 1, steps.size()).clear();
                        }
                    } else if (def2.tags.contains(record.tag()) && (def2.needpointer ? (record.getIdRef() != null || record.owner() == null) : true)) {
                        return def2;
                    }
                }
            }
        } else if (def.pointer) { 
            RecordDef def2 = structure.getRecordDef(def.name);
            if (def2 != null) {
                return match(structure, record, def2, steps);
            }
        } else if (def.tags != null && def.tags.contains(record.tag())) {
            return def;
        }
        if (steps != null) {
            steps.subList(size, steps.size()).clear();
        }
        return null;
    }

    private void test(final Verifier verifier, final GEDCOM gedcom, final Structure structure, final Record record, final RecordDef def, final List<Verifier.Fault> faults) {
//        System.out.println("-- DI" + def + " match " + record);
        Map<String,List<Record>> count = new HashMap<String,List<Record>>();
        for (Record subrecord : record.getRecords()) {
            if (!def.kids.isEmpty()) {
                List<RecordDef> steps = new ArrayList<RecordDef>();
                for (int i=0;i<def.kids.get(0).size();i++) {
                    steps.clear();
                    RecordDef def2 = def.kids.get(0).get(i);
                    RecordDef def3 = match(structure, subrecord, def2, steps);
                    if (def3 != null) {
                        List<Record> l = count.get(subrecord.tag());
                        if (l == null) {
                            count.put(subrecord.tag(), l = new ArrayList<Record>());
                        }
                        l.add(subrecord);
                        int max = def3.max;    // Give up attempts to do this properly
                        for (RecordDef d : steps) {
                            if (d.depth() > 0) {
                                max = Math.max(max, d.max);
                            }
                        }

                        if (l.size() > max) {
                            faults.add(new Verifier.Fault(subrecord, this, "Structure: \"" + subrecord.tag() + "\" occurs " + l.size() + " times in \"" + record.tag() + "\"", "Remove this record", Verifier.Severity.DataLoss) {
                                public boolean fix() {
                                    getRecord().owner().getRecords().remove(getRecord());
                                    return true;
                                }
                            });
                        } else {
                            test(verifier, gedcom, structure, subrecord, def3, faults);
                        }
                        subrecord = null;
                        break;
                    }
                }
            }
            if (subrecord != null && !subrecord.tag().startsWith("_")) {
                final Record fsubrecord = subrecord;
                if (subrecord.tag().equals("FORM") && subrecord.owner().getRecord("FILE") != null) {
                    faults.add(new Verifier.Fault(subrecord, this, "Structure: \"" + subrecord.tag() + "\" not allowed in \"" + record.tag() + "\"", "Move record to FILE sibling", Verifier.Severity.Restructure) {
                        public boolean fix() {
                            getRecord().owner().getRecord("FILE").getRecords().add(getRecord());
                            return true;
                        }
                    });
                } else {
                    faults.add(new Verifier.Fault(subrecord, this, "Structure: \"" + subrecord.tag() + "\" not allowed in \"" + record.tag() + "\"", "Remove this record", Verifier.Severity.DataLoss) {
                        public boolean fix() {
                            getRecord().owner().getRecords().remove(getRecord());
                            return true;
                        }
                    });
                }
            }
        }
    }

    /*
    public static void main(String[] args) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(GEDCOM.class.getResourceAsStream("v7.dat"), "UTF-8"));
        Structure s = new Structure();
        s.parse(r);
        r.close();
        for (Map.Entry<String,RecordDef> e : s.definitions.entrySet()) {
            System.out.println(e.getKey());
            System.out.println(e.getValue());
            System.out.println();
        }
    }
    */

}
