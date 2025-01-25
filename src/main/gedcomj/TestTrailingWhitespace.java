package gedcomj;

import java.text.*;
import java.util.*;

/**
 * The Trailing Whitespace test verifies there is no trailing whitespace
 * on any text before a newline. This isn't a requirement, it's just a nice thing to remove
 */
public class TestTrailingWhitespace extends Verifier.Test {
    
    @Override public void test(final Verifier verifier, final Record r, final List<Verifier.Fault> faults) {
        String v = r.getValue();
        if (v != null && v.contains("\n ")) {
            faults.add(new Verifier.Fault(r, this, "Trailing whitespace", "Trim trailing whitespace from value", Verifier.Severity.DataLoss) {
                @Override public boolean fix() {
                    r.setValue(r.getValue().replaceAll("\n  *", "\n"));
                    return true;
                }
            });
        }
        super.test(verifier, r, faults);
    }

}
