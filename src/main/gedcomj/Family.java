package gedcomj;

import java.util.*;

/**
 * Represents a "FAM" record which (in the world of GEDCOM) is an optional Husband, optional Wife
 * and zero or more Children. 
 */
public class Family extends Record {

    Family(GEDCOM gedcom, String tag) {
        super(gedcom, tag);
    }

    @Override protected void notifyRemoved() {
        for (Record r : getRecords()) {
            r = r.dereference();
            if (r instanceof Person) {
                ((Person)r).resetConnections();
            }
        }
    }

    @Override protected void notifyAdded() {
        for (Record r : getRecords()) {
            r = r.dereference();
            if (r instanceof Person) {
                ((Person)r).resetConnections();
            }
        }
    }

    /** 
     * Return the "HUSB" from this Family record, or null if none exists
     */
    public Person getHusband() {
        Record r = getRecord("HUSB");
        if (r != null) {
            r = r.dereference();
            if (r instanceof Person) {
                return (Person)r;
            }
        }
        return null;
    }

    /** 
     * Return the "WIFE" from this Family record, or null if none exists
     */
    public Person getWife() {
        Record r = getRecord("WIFE");
        if (r != null) {
            r = r.dereference();
            if (r instanceof Person) {
                return (Person)r;
            }
        }
        return null;
    }

    /** 
     * Return the list of "CHIL" records from this Family record, or an empty list.
     * The returned list is a copy and read-only, any modifications should be done
     * directly to the {@link #getRecords} list
     */
    public List<Person> getChildren() {
        List<Person> l = new ArrayList<Person>();
        for (Record r : getRecords()) {
            if (r.tag().equals("CHIL")) {
                r = r.dereference();
                if (r instanceof Person) {
                    l.add((Person)r);
                }
            }
        }
        return Collections.<Person>unmodifiableList(l);
    }


}
