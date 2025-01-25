package gedcomj;

import java.util.*;

/**
 * A generic GEDCOM record
 */
public class Record {

    final GEDCOM gedcom;
    private final String tag;
    private final RecordList records;
    private String id, idref;
    private Record owner;
    private String value;
    private int line;

    Record(GEDCOM gedcom, String tag) {
        if (gedcom == null) {
            throw new IllegalArgumentException("GEDCOM is null");
        }
        if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException("Tag is missing");
        }
        this.gedcom = gedcom;
        this.records = new RecordList(gedcom, this);
        this.tag = tag;
    }

    /**
     * Set the line number this record was read from.
     */
    public void setLineNumber(int line) {
        this.line = line;
    }

    /**
     * Return the line number this record was read from, or 0 if it wasn't read from a file.
     */
    public int getLineNumber() {
        return line;
    }

    /**
     * Set this record to be an indirect record. Once a record
     * is made indirect, it can't be changed
     * @param id the ID to use, or null to choose one
     */
    public void setId(String id) {
        if (this.id != null) {
            throw new IllegalStateException("Already indirect");
        }
        if (id == null) {
            id = gedcom.nextId(id);
        }
        this.id = id;
    }

    /**
     * If this record is indirect, return the id, or null if its direct
     */
    public String getId() {
        return this.id;
    }

    void setIdRef(String idref) {
        if (idref == null) {
            throw new IllegalStateException("Null value");
        }
        if (this.idref != null || this.value != null) {
            throw new IllegalStateException("Already indirect");
        }
        this.idref = idref;
    }

    /**
     * If this record is a idref to an indirect records, return the
     * id of the record it points to, otherwise null
     */
    public String getIdRef() {
        return idref;
    }

    void setOwner(Record owner) {
        this.owner = owner;
    }

    /**
     * Notify this record it is about to be removed from a parent or from the GEDCOM.
     * This method is called immediately before removal.
     */
    protected void notifyRemoved() {
    }

    /**
     * Notify this record it is has been added to a parent or to the GEDCOM.
     * This method is called immediately after removal.
     */
    protected void notifyAdded() {
    }

    /**
     * Return the Record that contains this Record, or null if it is a top-level record
     */
    public Record owner() {
        return owner;
    }

    /**
     * Return the Tag for this record, eg INDI or BIRT
     */
    public String tag() {
        return tag;
    }

    /**
     * Set the value on this record. The value must not null and this record cannot be a {@link #getIdRef idref}
     */
    public void setValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value is null");
        }
        if (this.idref != null) {
            throw new IllegalStateException("IdRef record: " + this);
        }
        this.value = value;
    }

    /**
     * Return the value from this record, which will be null if it is a {@link #getIdRef idref}, otherwise
     * will always be at least an empty string
     */
    public String getValue() {
        if (idref != null) {
            return null;
        }
        if (value == null) {
            value = "";
        }
        return value;
    }

    int level() {
        int level = 0;
        Record r = this;
        while (r.owner != null) {
            level++;
            r = r.owner;
        }
        return level;
    }

    /**
     * If this record is an idref record, return the record it points to (which will be null if the idref is invalid).
     * For non-idref records, returns this
     */
    public Record dereference() {
        if (idref != null) {
            return gedcom.resolveIdRef(idref);
        }
        return this;
    }

    /**
     * Return the modifiable list of sub-records for this Record, which may be empty but is never null.
     */
    public List<Record> getRecords() {
        return records;
    }

    /**
     * Return the first record in {@link #getRecords} that has a tag of "name", or null if none exists
     */
    public Record getRecord(String name) {
        for (Record r : getRecords()) {
            if (r.tag.equals(name)) {
                return r;
            }
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"tag\":");
        sb.append(Stringify.toString(tag));
        if (id != null) {
            sb.append(",\"id\":");
            sb.append(Stringify.toString(id));
        }
        if (idref != null) {
            sb.append(",\"idref\":");
            sb.append(Stringify.toString(idref));
        } else if (value != null && !value.isEmpty()) {
            sb.append(",\"value\":");
            sb.append(Stringify.toString(getValue()));
        }
        if (!getRecords().isEmpty()) {
            sb.append(",\"records\":");
            sb.append(Stringify.toString(getRecords()));
        }
        sb.append("}");
        return sb.toString();
    }

}
