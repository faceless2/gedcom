package gedcomj;

import java.text.*;
import java.util.*;

/**
 * The Header tests verifies that things in the Header are generally correct
 */
public class TestHeader extends Verifier.Test {

    @Override public void test(final Verifier verifier, final GEDCOM gedcom, final List<Verifier.Fault> faults) {
        String fault = null;
        Header header = gedcom.getHeader();
        if (header == null) {
            faults.add(new Verifier.Fault(null, this, "Header: missing", "Add header", Verifier.Severity.Trivial) {
               @Override public boolean fix() {
                   gedcom.getRecords().add(0, gedcom.newRecord("HEAD", null));
                   return true;
               }
            });
            header = (Header)gedcom.newRecord("HEAD", null);   // Assume this will be fixed.
            header.setVersion("7.0.0");
        }
        final String version = header.getVersion();
        final int mv = header.getMajorVersion();
        if (mv != 5 && mv != 7) {
            faults.add(new Verifier.Fault(header, this, "Header: unsupported version " + version, "Set version to 5.5.1", mv == 0 ? Verifier.Severity.Trivial : Verifier.Severity.DataLoss) {
                @Override public boolean fix() {
                    gedcom.getHeader().setVersion("5.5.1");
                    return true;
                }
            });
        }
    }

}
