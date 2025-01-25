package gedcomj;

import java.io.*;
import java.util.*;
import me.zpath.*;

public class Main {

    static final String pkgname = Main.class.getPackage().getName();

    public static void main(String[] args) throws Exception {
        try {
            String infile = null, outfile = null, version = null, template = null;
            List<String> filters = new ArrayList<String>();
            List<String> testnames = new ArrayList<String>();
            List<String> notestnames = new ArrayList<String>();
            Collection<Verifier.Severity> fixes = new HashSet<Verifier.Severity>();
            Map<String,String> options = new HashMap<String,String>();
            boolean json = false, fixprompt = false, verify = false, quiet = false;

            for (int i=0;i<args.length;i++) {
                String s = args[i];
                if (s.equals("--input") && infile == null && i + 1 < args.length) {
                    infile = args[++i];
                } else if (s.equals("--version") && outfile == null && i + 1 < args.length) {
                    version = args[++i];
                } else if (s.equals("--output") && outfile == null && i + 1 < args.length) {
                    outfile = args[++i];
                } else if (s.startsWith("--opt-") && i + 1 < args.length) {
                    options.put(s.substring(6), args[++i]);
                } else if (s.equals("--filter") && i + 1 < args.length) {
                    filters.add(args[++i]);
                } else if (s.equals("--test") && i + 1 < args.length) {
                    testnames.add(args[++i]);
                } else if (s.equals("--notest") && i + 1 < args.length) {
                    notestnames.add(args[++i]);
                } else if (s.equals("--verify") && !verify) {
                    verify = true;
                } else if (s.equals("--quiet") && !quiet) {
                    quiet = true;
                } else if (s.equals("--fix-trivial")) {
                    fixes.add(Verifier.Severity.Trivial);
                } else if (s.equals("--fix-restructure")) {
                    fixes.add(Verifier.Severity.Restructure);
                } else if (s.equals("--fix-dataloss")) {
                    fixes.add(Verifier.Severity.DataLoss);
                } else if (s.equals("--fix-all")) {
                    fixes.addAll(Arrays.asList(Verifier.Severity.values()));
                } else if (s.equals("--fix-prompt") && !fixprompt) {
                    fixprompt = true;
                } else if (s.equals("--fix-prompt") && !quiet) {
                    quiet = true;
                } else if (s.equals("--json") && !json) {
                    json = true;
                } else if (s.equals("--ztemplate") && template == null && i + 1 < args.length) {
                    template = args[++i];
                } else {
                    help(s.equals("--help") ? null : "Invalid parameter\"" + s + "\"");
                }
            }
            if (infile == null) {
                help("No input specified");
            }
            if (fixprompt && infile.equals("-")) {
                help("Can't use --fixprompt and read from STDIN");
            }
            if (template != null && !filters.isEmpty()) {
                help("Can't use --filter and --ztemplate");
            }
            if (template != null && json) {
                help("Can't use --ztemplate and --json");
            }
            if (json && outfile == null) {
                help("--json requires --outfile");
            }
            if (!quiet) {
                System.err.print("Reading " + (infile.equals("-") ? "STDIN" : "\"" + infile + "\"") + "...");
            }
            InputStream in = null;
            try {
                in = infile.equals("-") ? System.in : new FileInputStream(infile);
            } catch (IOException e) {
                System.err.println("failed");
                e.printStackTrace(System.err);
                return;
            }

            GEDCOM gedcom = new GEDCOM();
            gedcom.getOptions().putAll(options);
            gedcom.read(in);
            System.err.println(" " + gedcom.getRecords().size() + " record");

            if (verify) {
                Verifier verifier = new Verifier();
                for (String name : testnames) {
                    try {
                        verifier.getTests().add((Verifier.Test)Class.forName(name).getDeclaredConstructor().newInstance());
                    } catch (Throwable e) {
                        try {
                            verifier.getTests().add((Verifier.Test)Class.forName(pkgname + "." + name).getDeclaredConstructor().newInstance());
                        } catch (Throwable e2) {
                            System.err.println("Can't instantiate test \"" + pkgname + "." + name + "\" or \"" + name + "\"");
                            return;
                        }
                    }
                }
                for (Iterator<Verifier.Test> i = verifier.getTests().iterator();i.hasNext();) {
                    Verifier.Test test = i.next();
                    String name = test.getClass().getName();
                    if (notestnames.contains(name) || notestnames.contains(name.substring(name.lastIndexOf(".") + 1))) {
                        i.remove();
                    }
                }
                boolean raw = rawmode(true);
                for (Verifier.Fault fault : verifier.verify(gedcom)) {
                    System.err.print(fault.getSeverity()+" error (line " + fault.getRecord().getLineNumber() + "): " + fault.getDescription()+" (" + fault.getRepairDescription() + ")");
                    boolean fix = fixes.contains(fault.getSeverity());
                    if (fix) {
                        System.err.print(": ");
                    } else if (fixprompt) {
                        System.err.print(" - enter 'y' to fix: ");
                        int c = System.in.read();
                        fix = c == 'y' || c == 'Y';
                        if (c == 3 || c < 0) {
                            return;
                        }
                    }
                    if (!fix) {
                        System.err.println("\r");
                    } else if (fault.fix()) {
                        System.err.println("fixed\r");
                    } else {
                        System.err.println("fix failed\r");
                    }
                }
                rawmode(false);
            }

            if (template != null) {
                Reader reader = new InputStreamReader(new FileInputStream(template), "UTF-8");
                Configuration config = new Configuration();
                config.getFactories().add(new GEDCOMEvalFactory());
                ZTemplate zt = ZTemplate.compile(reader, config);
                reader.close();
                Appendable out = System.out;
                if (outfile != null && !outfile.equals("-")) {
                    out = new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8");
                }
                zt.apply(gedcom, out);
                if (out instanceof Flushable) {
                    ((Flushable)out).flush();
                }
            } else {
                if (!filters.isEmpty()) {
                    StringBuilder query = new StringBuilder();
                    boolean first = true;
                    for (String s : filters) {
                        File f = new File(s);
                        if (f.canRead()) {
                            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                            while ((s=r.readLine()) != null) {
                                query.append(first ? "union(" : ", ");
                                first = false;
                                query.append(s);
                            }
                            r.close();
                        } else {
                            query.append(first ? "union(" : ", ");
                            first = false;
                            query.append(s);
                        }
                    }
                    query.append(")");
                    Configuration config = new Configuration();
                    config.getFactories().add(new GEDCOMEvalFactory());
                    ZPath path = ZPath.compile(query.toString(), config);
                    List<Object> match = path.eval(gedcom).all();
                    if (outfile != null) {
                        for (Iterator<Record> i = gedcom.getRecords().iterator();i.hasNext();) {
                            Record r = i.next();
                            if (r instanceof Person && !match.contains(r)) {
                                i.remove();
                            }
                        }
                    } else {
                        for (Object o : match) {
                            System.out.println(o);
                        }
                    }
                }
                if (outfile != null) {
                    int count = 0;
                    for (Iterator<Record> i = gedcom.getRecords().iterator();i.hasNext();) {
                        if (i.next() instanceof Person) {
                            count++;
                        }
                    }
                    if (version != null) {
                        gedcom.getHeader().setVersion(version);
                    }
                    if (!quiet) {
                        System.err.println("Writing " + count + " people records... ");
                    }
                    if (json && outfile.equals("-")) {
                        System.out.println(gedcom.toString());
                    } else {
                        OutputStream out = outfile.equals("-") ? System.out : new BufferedOutputStream(new FileOutputStream(outfile));
                        if (json) {
                            Writer w = new OutputStreamWriter(out, "UTF-8");
                            w.write(gedcom.toString());
                            w.close();
                        } else {
                            gedcom.write(out);
                            if (out != System.out) {
                                out.close();
                            }
                        }
                    }
                }
            }
        } finally {
            rawmode(false);
        }
    }

    private static void help(String err) {
        if (err != null) {
            System.err.println("ERROR: " + err);
            System.err.println();
        }
        String version = Main.class.getPackage().getImplementationVersion();
        System.err.println("Java GEDCOM v" + version);
        System.err.println("Usage: java -jar gedcom-" + version + "-all.jar <args>");
        System.err.println();
        System.err.println("  --input <file|->        the source for the GEDCOM (filename or - for stdin). Required");
        System.err.println("  --output <file|->       the output to write to (filename or - for stdin)");
        System.err.println("  --quiet                 don't print information to stderr");
        System.err.println("  --json                  write to the output file in JSON format");
        System.err.println("  --template <ztemplate>  write to the output file using a ZTemplate");
        System.err.println("  --filter <file|zpath>   a ZPath expression to filter the output, or if the value is a filename");
        System.err.println("                          read the ZPath expressions from that file, one per line. Repeatable.");
        System.err.println("  --verify                verify the GEDCOM");
        System.err.println("  --test <testname>       add a test to the verify pass (testname is a class name). Repeatable"); 
        System.err.println("  --notest <testname>     remove a test from the verify pass (testname is a class name). Repeatable"); 
        System.err.println("  --fix-trivial           if verifying, always fix trivial errors");
        System.err.println("  --fix-reorder           if verifying, always fix errors that simply rearrange records");
        System.err.println("  --fix-dataloss          if verifying, always fix errors that remove invalid records");
        System.err.println("  --fix-prompt            if verifying, ask to fix errors that are not otherwise fixed");
        System.err.println("  --opt-NNN <value>       set option NNN = value before reading starts");
        System.err.println("  --version <value>       when writing, set the version number to the specified value");
        System.err.println();
        System.err.println("See https://github.com/faceless2/gedcom and https://zpath.me for more information");
        System.exit(err == null ? 0 : 1);
    }

    private static boolean rawmode(boolean raw) throws Exception {
        // So we can read the "y" or "n" for fixprompt without echo or needing a newline.
        // UNIX only obviously, will just fail in Windows
        return Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", "stty " + (raw ? "raw" : "sane") + " < /dev/tty"}).waitFor() == 0;
    }

}
