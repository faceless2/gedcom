package gedcomj;

import me.zpath.*;
import java.util.*;

class GEDCOMEvalContext implements EvalContext {

    private final GEDCOM gedcom;
    private final Record ctx;
    private final Configuration config;
    private final LinkedHashMap<String,GDate> datecache = new LinkedHashMap<String,GDate>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String,GDate> eldest) {
            return size() > 16;
        }
    };
    private int index;
    private List<Object> nodeset;

    public GEDCOMEvalContext(Record o, Configuration config) {
        this.gedcom = o.gedcom;
        this.ctx = o;
        this.config = config;
    }

    public GEDCOMEvalContext(GEDCOM o, Configuration config) {
        this.gedcom = o;
        this.ctx = null;
        this.config = config;
    }

    @Override public Object parent(Object o) {
        if (o instanceof Record) {
            Record r = (Record)o;
            if (r.owner() != null) {
                return r.owner();
            } else {
                return r.gedcom;
            }
        } else if (o instanceof Connection) {
            return ((Connection)o).getSubject();
        }
        return null;
    }

    @Override public String stringValue(Object o) {
        if (o instanceof Record) {
            Record r = (Record)o;
            r = r.dereference();
            if (r != null) {
                return r.getValue().isEmpty() ? null : r.getValue();
            }
        }
        return null;
    }

    @Override public Number numberValue(Object o) {
        String s = stringValue(o);
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                try {
                    return Double.parseDouble(s);
                } catch (Exception e2) { }
            }
        }
        return null;
    }

    @Override public Boolean booleanValue(Object o) {
        String s = stringValue(o);
        if (s != null) {
            return true;
        }
        return null;
    }

    @Override public Iterable<? extends Record> get(Object o, Object key) {
        if (o instanceof GEDCOM) {
            GEDCOM r = (GEDCOM)o;
            return new MyIterable(r.getRecords(), key) {
                boolean matches(int i, Object key) {
                    if (key == WILDCARD) {
                        return l.get(i) instanceof Person;
                    } else if (key instanceof Integer) {
                        int ix = ((Integer)key).intValue();
                        while (ix >= 0 && i >= 0) {
                            if (l.get(i) instanceof Person) {
                                ix--;
                            }
                            i--;
                        }
                        return i == -1 && ix == -1;
                    }
                    return false;
                }
            };
        } else if (o instanceof Record) {
            Record r = (Record)o;
            r = r.dereference();
            if (r != null) {
                String skey = key instanceof String ? ((String)key).toLowerCase() : null;
                if (r instanceof Person && ("parents".equals(skey) || "children".equals(skey) || "coparents".equals(skey) || "spouses".equals(skey) || "ancestors".equals(skey) || "descendants".equals(skey) || "family".equals(skey) || "connections".equals(skey))) {
                    List<Person> l = new ArrayList<Person>();
                    List<Person> input = Collections.<Person>singletonList((Person)r);
                    if ("ancestors".equals(skey)) {
                        skey = "parents";
                        input = l;
                    } else if ("descendants".equals(skey)) {
                        skey = "children";
                        input = l;
                    } else if ("connections".equals(skey)) {
                        skey = "family";
                        input = l;
                    }
                    for (int i=input.size() - 1;i<input.size();i++) {
                        final Person p = i < 0 ? (Person)r : input.get(i);
                        for (Connection c : p.getConnections()) {
                            boolean ok = false;
                            switch (skey) {
                                case "parents": ok = c.getRole().isParent(); break;
                                case "children": ok = c.getRole().isChild(); break;
                                case "siblings": ok = c.getRole().isSibling(); break;
                                case "spouses": ok = c.getRole().isSpouse(); break;
                                case "coparents": ok = c.getRole().isCoParent(); break;
                                case "family": ok = true;
                            }
                            if (ok) {
                                l.add(c.getObject());
                            }
                        }
                    }
                    return new MyIterable(l, key) {
                        boolean matches(int i, Object key) {
                            return true;
                        }
                    };
                } else {
                    return new MyIterable(r.getRecords(), key) {
                        boolean matches(int i, Object key) {
                            if (key == WILDCARD) {
                                return true;
                            } else if (key instanceof Integer) {
                                return ((Integer)key) == i;
                            } else if (key instanceof String) {
                                return ((String)key).equalsIgnoreCase(l.get(i).tag());
                            } else {
                                return false;
                            }
                        }
                    };
                }
            }
        }
        return Collections.<Record>emptyList();
    }

    @Override public String type(Object o) {
        if (o instanceof GEDCOM) {
            return "gedcom";
        } else if (o instanceof Record) {
            Record r = (Record)o;
            if (r instanceof GDate) {
                return "date";
            } else if (r.getRecords().isEmpty() && r.getValue().isEmpty()) {
                return "null";
            } else if (r.getRecords().isEmpty()) {
                return "text";
            } else {
                return "record";
            }
        }
        return "unknown";
    }

    @Override public String key(Object o) {
        if (o instanceof Record) {
            Record r = (Record)o;
            Record p = r.owner();
            if (p != null) {
                for (int i=0;i<p.getRecords().size();i++) {
                    Record t = p.getRecords().get(i);
                    if (t == r) {
                        return r.tag();
                    } else if (t.tag().equals(r.tag())) {
                        break;
                    }
                }
            }
        }
        return null;
    }

    @Override public int index(Object o) {
        if (o instanceof Record) {
            Record r = (Record)o;
            Record p = r.owner();
            if (p != null) {
                return p.getRecords().indexOf(r);
            } else {
                int i = 0;
                for (Record t : r.gedcom.getRecords()) {
                    if (t == o) {
                        return i;
                    } else if (t instanceof Person) {
                        i++;
                    }
                }
            }
        }
        return -1;
    }

    @Override public Object value(Object o) {
        if (o instanceof Record) {
            Record r = (Record)o;
            String type = type(o);
            if ("string".equals(type)) {
                return stringValue(r);
            } else if ("date".equals(type)) {
                return ((GDate)r).getStart();
            }
        }
        return o;
    }

    @Override public boolean isUnique(Object o) {
        return o instanceof GEDCOM || o instanceof Record;
    }

    private GDate parseDate(String value) {
        GDate date = datecache.get(value);
        if (date == null) {
            date = (GDate)gedcom.newRecord("DATE", value);
            if (date.getStart() != null || date.getEnd() != null) {
                datecache.put(value, date);
            }
        }
        return date;
    }

    @Override public Integer compare(Object a, Object b, String op) {
        if (a instanceof String || a instanceof Integer) {
            a = parseDate(a.toString());
        }
        if (b instanceof String || b instanceof Integer) {
            b = parseDate(b.toString());
        }
        if (a instanceof GDate && b instanceof GDate) {
            GDate ad = (GDate)a;
            GDate bd = (GDate)b;
            long a0 = ad.getStart() == null ? Long.MIN_VALUE : ad.getStart().getTime();
            long a1 = ad.getEnd() == null ? Long.MAX_VALUE : ad.getEnd().getTime();
            long b0 = bd.getStart() == null ? Long.MIN_VALUE : bd.getStart().getTime();
            long b1 = bd.getEnd() == null ? Long.MAX_VALUE : bd.getEnd().getTime();
            if (a1 < b0) {
                return -1;
            } else if (a0 > b1) {
                return 1;
            }  else {
                return 0;
            }
        }
        return null;
    }

    @Override public Function getFunction(String name) {
        return null;
    }

    @Override public Configuration getConfiguration() {
        return config;
    }

    @Override public Configuration.Logger getLogger() {
        return null;
    }

    @Override public void setContext(int index, List<Object> nodeset) {
        this.index = index;
        this.nodeset = nodeset;
    }

    @Override public int getContextIndex() {
        return index;
    }

    @Override public List<Object> getContext() {
        return nodeset;
    }

    private static abstract class MyIterable implements Iterable<Record> {
        final List<? extends Record> l;
        final Object key;

        MyIterable(List<? extends Record> l, Object key) {
            this.l = l;
            this.key = key;
        }

        abstract boolean matches(int i, Object key);

        public Iterator<Record> iterator() {
            return new Iterator<Record>() {
                int i;
                {
                    while (i < l.size() && !matches(i, key)) {
                        i++;
                    }
                }
                public boolean hasNext() {
                    return i < l.size();
                }
                public Record next() {
                    if (i == l.size()) {
                        throw new NoSuchElementException();
                    }
                    Record r = l.get(i++);
                    while (i < l.size() && !matches(i, key)) {
                        i++;
                    }
                    return r;
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
