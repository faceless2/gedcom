package gedcomj;

import me.zpath.*;

/**
 * The GEDCOMEvalFactory allows GEDCOM structures to be queried with <a href="https://zpath.me">ZPath</a>
 */
public class GEDCOMEvalFactory implements EvalFactory {

    /**
     * Create a new EvalContext, or return <code>null</code> if this factory doesn't apply to this type of object
     * @param o the object that will be parsed by the returned {@link EvalContext}
     * @param config the Configuration
     * @return the EvalContext or null if the factory doesn't apply
     */
    public EvalContext create(Object o, Configuration config) {
        if (o instanceof Record) {
            return new GEDCOMEvalContext((Record)o, config);
        } else if (o instanceof GEDCOM) {
            return new GEDCOMEvalContext((GEDCOM)o, config);
        }
        return null;
    }

}
