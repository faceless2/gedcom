package gedcomj;

import java.util.*;

/**
 * Represents a Individual record.
 */
public class Person extends Record {

    private List<Connection> connections;
    private Person father, mother;

    Person(GEDCOM gedcom, String tag) {
        super(gedcom, tag);
    }

    @Override protected void notifyRemoved() {
        for (Connection c : getConnections()) {
            c.getObject().resetConnections();
        }
        resetConnections();
    }

    @Override protected void notifyAdded() {
        resetConnections();
    }

    /**
     * Return the Birth date for this person, or null if it can't be parsed.
     * For a more complete answer query the BIRT/DATE record directly
     */
    public Date getBirthDate() {
        Record r = getRecord("BIRT");
        if (r != null) {
            r = getRecord("DATE");
            if (r instanceof GDate) {
                Date d = ((GDate)r).getStart();
                if (d == null) {
                    d = ((GDate)r).getEnd();
                }
                return d;
            }
        }
        return null;
    }

    /**
     * Return the Father of this person (the HUSB of any FAMC record), or null if unset
     */
    public Person getFather() {
        getConnections();
        return father;
    }

    /**
     * Return the Father of this person (the WIFE of any FAMC record), or null if unset
     */
    public Person getMother() {
        getConnections();
        return mother;
    }

    void resetConnections() {
        mother = father = null;
        connections = null;
    }

    /**
     * Return a list of Connections for this person. The returned list is fixed, but
     * calling this methood again after changes to the GEDCOM will return an updated list
     */
    public List<Connection> getConnections() {
        if (connections == null) {
            List<Connection> l = new ArrayList<Connection>();
            for (Record r : getRecords()) {
                if (r.tag().equals("BIRT")) {
                    Record famc = r.getRecord("FAMC");
                    if (famc != null) {
                        famc = famc.dereference();
                        if (famc != null) {
                            processFAMC(l, famc, "birth");
                        }
                    }
                } else if (r.tag().equals("FAMC")) {
                    r = r.dereference();
                    if (r != null) {
                        processFAMC(l, r, null);
                    }
                } else if (r.tag().equals("FAMS")) {
                    r = r.dereference();
                    if (r != null) {
                        Person husband = null, wife = null;
                        Record marr = null;
                        for (Record r2 : r.getRecords()) {
                            if (marr == null && r2.tag().equals("MARR")) {
                                marr = r2.dereference();;
                            }
                        }
                        for (Record r2 : r.getRecords()) {
                            if (r2.tag().equals("HUSB")) {
                                Record r3 = r2.dereference();
                                if (r3 instanceof Person && r3 != this) {
                                    husband = (Person)r3;
                                    Connection c = new Connection(this, marr != null ? Connection.Role.Husband : Connection.Role.CoParent, husband, r2);
                                    if (!l.contains(c)) {
                                        l.add(c);
                                    }
                                }
                            } else if (r2.tag().equals("WIFE")) {
                                Record r3 = r2.dereference();
                                if (r3 instanceof Person && r3 != this) {
                                    wife = (Person)r3;
                                    Connection c = new Connection(this, marr != null ? Connection.Role.Wife : Connection.Role.CoParent, wife, r2);
                                    if (!l.contains(c)) {
                                        l.add(c);
                                    }
                                }
                            }
                        }
                        for (Record r2 : r.getRecords()) {
                            Record r3 = r2.dereference();
                            if (r3 instanceof Person) {
                                if (r2.tag().equals("CHIL")) {
                                    Connection c = new Connection(this, Connection.Role.Child, (Person)r3, r2);
                                    if (!l.contains(c)) {
                                        l.add(c);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Collections.sort(l);
            connections = l;
        }
        return connections;
    }

    private void processFAMC(final List<Connection> l, final Record famc, final String type) {
        for (Record r2 : famc.getRecords()) {
            if (r2.tag().equals("HUSB")) {
                Record r3 = r2.dereference();
                if (r3 instanceof Person) {
                    Connection.Role role = Connection.Role.Father;
                    if ("birth".equals(type)) {
                        role = Connection.Role.BiologicalFather;
                    } else if (type == null && father == null) {
                        father = (Person)r3;
                    }
                    Connection c = new Connection(this, role, (Person)r3, r2);
                    if (!l.contains(c)) {
                        l.add(c);
                    }
                }
            } else if (r2.tag().equals("WIFE")) {
                Record r3 = r2.dereference();
                if (r3 instanceof Person) {
                    Connection.Role role = Connection.Role.Mother;
                    if ("birth".equals(type)) {
                        role = Connection.Role.BiologicalMother;
                    } else if (type == null && mother == null) {
                        mother = (Person)r3;
                    }
                    Connection c = new Connection(this, role, (Person)r3, r2);
                    if (!l.contains(c)) {
                        l.add(c);
                    }
                }
            }
        }
        for (Record r2 : famc.getRecords()) {
            Record r3 = r2.dereference();
            if (r3 instanceof Person && r3 != this) {
                if (r2.tag().equals("CHIL")) {
                    Connection.Role role = null;
                    if ("bioligical".equals(type)) {
                        role = Connection.Role.BiologicalSibling;
                    } else if (father != null && mother != null) {
                        role = Connection.Role.FullSibling;
                    } else {
                        role = Connection.Role.HalfSibling;
                    }
                    Connection c = new Connection(this, role, (Person)r3, r2);
                    if (!l.contains(c)) {
                        l.add(c);
                    }
                }
            }
        }
    }

}
