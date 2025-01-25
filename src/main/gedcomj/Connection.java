package gedcomj;

import java.util.*;

/**
 * A Connection represents a link between two people - a subject and an object.
 * For example, if John is Mary's father then Subject=John, Role=Father, Object=Mary
 */
public class Connection implements Comparable<Connection> {

    private final Person subject, object;
    private final Role role;
    private final Record r;

    Connection(Person subject, Role role, Person object, Record r) {
        this.subject = subject;
        this.role = role;
        this.object = object;
        this.r = r;
    }

    public int hashCode() {
        return subject.hashCode() ^ object.hashCode() ^ role.hashCode() ^ r.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof Connection) {
            Connection c = (Connection)o;
            return c.subject == subject && c.object == object && c.role == role && c.r == r;
        }
        return false;
    }

    public int compareTo(Connection con) {
        Date d0 = getDate();
        Date d1 = con.getDate();
        if (d0 != null && d1 != null) {
            long diff = d0.getTime() - d1.getTime();
            return diff < 0 ? -1 : diff == 0 ? 0 : 1;
        } else {
            Record p = r.owner();
            if (con.r.owner() == p) {
                return p.getRecords().indexOf(r) - p.getRecords().indexOf(con.r);
            } else {
                return r.gedcom.getRecords().indexOf(r) - con.r.gedcom.getRecords().indexOf(con.r);
            }
        }
    }

    /**
     * Get the date that applies to this Connection (a birth or marriage datea)
     */
    public Date getDate() {
        // r is the relationship record within FAM, eg "HUSB, WIFE, CHIL"
        // Date for spouses is marriage date
        // Date for parents/children/siblings is birthdate of child
        // Date for coparent is birthdate of first child
        if (role.isSpouse()) {
            Record r = this.r.owner().getRecord("MARR").getRecord("DATE");
            if (r instanceof GDate) {
                Date date = ((GDate)r).getStart();
                if (date == null) {
                    date = ((GDate)r).getEnd();
                }
                return date;
            }
        } else if (role.isChild() ) {
            return subject.getBirthDate();
        } else {
            return object.getBirthDate();
        }
        return null;
    }

    public enum Role {
        Father, Mother, Child, FullSibling, HalfSibling, BiologicalFather, BiologicalMother, BiologicalSibling, Husband, Wife, CoParent;

        public boolean isParent() {
            return this == Father || this == Mother;
        }

        public boolean isSpouse() {
            return this == Husband || this == Wife;
        }

        public boolean isCoParent() {
            return this == CoParent;
        }

        public boolean isChild() {
            return this == Child;
        }

        public boolean isSibling() {
            return this == FullSibling || this == HalfSibling;
        }

    }

    /**
     * Get the subject of this connection
     */
    public Person getSubject() {
        return subject;
    }

    /**
     * Get the object of this connection
     */
    public Person getObject() {
        return object;
    }

    /**
     * Get the role of this connection
     */
    public Role getRole() {
        return role;
    }

    /**
     * Get the Record that this connection was extracted from, eg a CHIL or HUSB
     */
    public Record getRecord() {
        return r;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"role\":");
        sb.append("\"" + role + "\"");
        sb.append("\"subject\":");
        sb.append(subject);
        sb.append(",\"object\":");
        sb.append(object);
        sb.append("}");
        return sb.toString();
    }

}
