package gedcomj;

import java.text.*;
import java.util.*;

/**
 * The Dates test verifies that Dates are formated correctly
 */
public class TestDates extends Verifier.Test {
    
    @Override public void test(final Verifier verifier, final Record r, final List<Verifier.Fault> faults) {
        if (r instanceof GDate && (((GDate)r).getQualifier() == GDate.Qualifier.Invalid || !r.getValue().equals(r.getValue().toUpperCase()))) {
            String ov = r.getValue();
            String v = ov.toUpperCase();        // Values have to be UC
            v = v.replaceAll("  *", " ");
            v = v.replaceAll("(?i)JANUARY", "JAN");
            v = v.replaceAll("(?i)FEBRUARY", "FEB");
            v = v.replaceAll("(?i)MARCH", "MAR");
            v = v.replaceAll("(?i)APRIL", "APR");
            v = v.replaceAll("(?i)JUNE", "JUN");
            v = v.replaceAll("(?i)JULY", "JUL");
            v = v.replaceAll("(?i)AUGUST", "AUG");
            v = v.replaceAll("(?i)SEPTEMBER", "SEP");
            v = v.replaceAll("(?i)OCTOBER", "OCT");
            v = v.replaceAll("(?i)NOVEMBER", "NOV");
            v = v.replaceAll("(?i)DECEMBER", "DEC");
            v = v.replaceAll("(?i)(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC) ([0-9]+)[,]?  *([0-9]+)", "$2 $1 $3");
            v = v.replaceAll("^(?i)BEFORE ", "BEF ");
            v = v.replaceAll("^(?i)AFTER ", "AFT ");
            v = v.replaceAll("^(?i)ABOUT ", "ABT ");
            v = v.replaceAll("^(?i)BEF\\. *", "BEF ");
            v = v.replaceAll("^(?i)ABT\\. *", "ABT ");
            v = v.replaceAll("^(?i)EST\\. *", "EST ");
            v = v.replaceAll("^(?i)AFT\\. *", "AFT ");
            v = v.replaceAll("^(?i)BET\\. *", "BET ");
            v = v.replaceAll("\u2013", "-");
            v = v.replaceAll("^(?i)BET ([^ ]*) *- *(.*)", "BET $1 AND $2");
            v = v.replaceAll("^([^ ]*) *- *(.*)", "BET $1 AND $2");
            v = v.replaceAll("^(?i)CIRCA", "C");
            v = v.replaceAll("^(?i)C ", "ABT ");
            v = v.replaceAll("^(?i)C\\. *", "ABT ");
            v = v.replaceAll("^(?i)C([0-9])", "ABT $1");
            v = v.replaceAll("(.*) C$", "ABT $1");
            v = v.replaceAll("(.*) C\\.$", "ABT $1");

            try {
                v = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new SimpleDateFormat("yyyy-MM-dd").parse(v.replaceAll("\\.", "-"), new ParsePosition(0)));
            } catch (Exception e) {
                try {
                    v = new SimpleDateFormat("MMM yyyy", Locale.US).format(new SimpleDateFormat("yyyy-MM").parse(v.replaceAll("\\.", "-"), new ParsePosition(0)));
                } catch (Exception e2) { }
            }
            if (!v.equals(ov)) {
                GDate date = (GDate)r.gedcom.newRecord("DATE", v);
                if (date.getQualifier() != GDate.Qualifier.Invalid) {   // Don't apply change unless it fixes something
                    final String newv = v;
                    faults.add(new Verifier.Fault(r, this, "Invalid date", "Replace \"" + ov + "\" with \"" + v + "\"", Verifier.Severity.Trivial) {
                        @Override public boolean fix() {
                            r.setValue(newv);
                            return true;
                        }
                    });
                } else {
                    faults.add(new Verifier.Fault(r, this, "Invalid date", "Remove invalid date \"" + ov + "\"", Verifier.Severity.DataLoss) {
                        @Override public boolean fix() {
                            r.owner().getRecords().remove(r);
                            return true;
                        }
                    });
                }
            }
        }
        super.test(verifier, r, faults);
    }

}
