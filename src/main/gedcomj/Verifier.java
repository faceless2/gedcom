package gedcomj;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * The Verifier runs one or more Tests on the GEDCOM, reporting a list of faults
 * some of which may be repairable
 * <pre>
 * for (Verifier.Fault fault : new Verifier().verify(gedcom)) {
 *     System.out.print(fault.getSeverity()+" Repair " + fault.getType()+" at line " + fault.getRecord().getLineNumber()+" (" + fault.getDetail() + ") = ");
 *     if (fault.fix()) {
 *         System.out.println("Fixed");
 *     } else {
 *         System.out.println("Not Fixed");
 *     }
 * }
 * </pre>
 */
public class Verifier {

     private List<Test> tests;

     public Verifier() {
         tests = new ArrayList<Test>();
         tests.addAll(DEFAULTS);
     }

     /**
      * Return the list of tests that will be run, which can be modified
      */
     public List<Test> getTests() {
         return tests;
     }

     /**
      * Verify the GEDCom and return a list of faults
      */
     public List<Fault> verify(GEDCOM gedcom) {
         List<Fault> faults = new ArrayList<Fault>();
         Set<Test> seen = new HashSet<Test>();
         for (Test test : tests) {
             if (seen.add(test)) {
                 test.test(this, gedcom, faults);
             }
         }
         return faults;
     }

     /**
      * Represents a Test on the file
      */
     public static abstract class Test {
         public void test(Verifier verifier, GEDCOM gedcom, List<Fault> faults) {
             for (Record r : gedcom.getRecords()) {
                 test(verifier, r, faults);
             }
         }
         public void test(Verifier verifier, Record rec, List<Fault> faults) {
             for (Record r : rec.getRecords()) {
                 test(verifier, r, faults);
             }
         }
     }

     /**
      * Represents the severity of the proposed fix: trivial, one involving some restructuring but not dataloss, or one that involves dataloss
      */
     public enum Severity {
         Trivial, Restructure, DataLoss
     }

     /**
      * Represents a fault found by a test
      */
     public static class Fault {
         private final Record r;
         private final Test test;
         private final String type, detail;
         private final Severity severity;
         public Fault(Record r, Test test, String type, String detail, Severity severity) {
             this.r = r;
             this.test = test;
             this.type = type;
             this.detail = detail;
             this.severity = severity;
         }
         /**
          * Return the record this fault applies to
          */
         public Record getRecord() {
             return r;
         }
         /**
          * Return the Test that found this Fault
          */
         public Test getTest() {
             return test;
         }
         /**
          * Return the description of the fault
          */
         public String getDescription() {
             return type;
         }
         /**
          * Return the description of the proposed fix
          */
         public String getRepairDescription() {
             return detail;
         }
         /**
          * Return the severity of the propossed fix
          */
         public Severity getSeverity() {
             return severity;
         }
         /**
          * Attempt to fix the fault
          * @return true if the fault was fixed
          */
         public boolean fix() {
             return false;
         }
     }


     private static List<Test> DEFAULTS;
     static {
        List<Test> list = new ArrayList<Test>();
        Set<String> seen = new HashSet<String>();
        final String clname = Test.class.getName();
        try {
            for (Enumeration<URL> e = Verifier.class.getClassLoader().getResources("META-INF/services/" + clname);e.hasMoreElements();) {
                URL url = (URL)e.nextElement();
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "ISO-8859-1"));
                String classname;
                while ((classname=reader.readLine()) != null) {
                    classname = classname.replaceAll("#.*", "").trim();
                    if (classname.length() > 0 && seen.add(classname)) {
                        try {
                            list.add((Test)Class.forName(classname).getDeclaredConstructor().newInstance());
                        } catch (Throwable x) { }
                    }
                }
                reader.close();
            }
        } catch (IOException x) {
            new Exception("Error parsing META-INF/services/" + clname, x).printStackTrace();
        }
        DEFAULTS = Collections.<Test>unmodifiableList(list);
    }
}
