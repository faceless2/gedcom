package gedcomj;

import java.util.*;

class RecordList extends AbstractList<Record> {

    private final List<Record> list = new ArrayList<Record>();
    private final GEDCOM gedcom;
    private final Record owner;

    RecordList(GEDCOM gedcom, Record owner) {
        this.gedcom = gedcom;
        this.owner = owner;
    }

    @Override public Record get(int i) {
        return list.get(i);
    }

    @Override public int size() {
        return list.size();
    }

    @Override public Record set(int i, Record r) {
        if (r == null) {
            throw new IllegalArgumentException("Record is null");
        }
        if (r.gedcom != gedcom) {
            throw new IllegalArgumentException("Record from another GEDCOM");
        }
        Record old = list.get(i);
        if (old == r) {
            return r;
        }
        int oldindex = list.indexOf(r);
        if (oldindex >= 0) {
            list.remove(oldindex);
            if (oldindex < i) {
                i--;
            }
        } else if (r.owner() != null) {
            r.owner().getRecords().remove(r);
        }
        list.set(i, r);
        old.notifyRemoved();
        notifyRemoved(old);
        old.setOwner(null);
        if (oldindex < 0) {
            r.setOwner(owner);
            notifyAdded(r);
            r.notifyAdded();
        }
        return old;
    }

    @Override public void add(int i, Record r) {
        if (r == null) {
            throw new IllegalArgumentException("Record is null");
        }
        if (r.gedcom != gedcom) {
            throw new IllegalArgumentException("Record from another GEDCOM");
        }
        int oldindex = list.indexOf(r);
        if (oldindex >= 0) {
            list.remove(oldindex);
            if (oldindex < i) {
                i--;
            }
            list.add(i, r);
        } else {
            if (r.owner() != null) {
                r.owner().getRecords().remove(r);
            }
            list.add(i, r);
            r.setOwner(owner);
            notifyAdded(r);
            r.notifyAdded();
        }
    }

    @Override public Record remove(int i) {
        Record r = list.remove(i);
        r.notifyRemoved();
        notifyRemoved(r);
        r.setOwner(null);
        return r;
    }

    protected void notifyRemoved(Record r) {
    }

    protected void notifyAdded(Record r) {
    }

}
