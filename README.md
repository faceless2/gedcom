= GEDCOM Java API

A Java API for reading/writing/validating GEDCOM files, and for
selecting a subset of records.

There are a few Java GEDCOM APIs available which can read, write and validate, eg
* https://github.com/FamilySearch/gedcom5-java
* https://github.com/frizbog/gedcom4j

This API can do that, but also allows a subset of GEDCOM to be queried using
[ZPath](https://zpath.me), a generic query language similar to xpath (for XML)
or jmespath (for JSON).

Features:
* Read GEDCOM (handling split UTF-8 sequences, invalid structures etc.
* Write GEDCOM (UTF-8 only)
* Write/Read GEDCOM as JSON (using a simple serialization of the GEDCOM structure)
* Verify and optionally repair GEDCOM against various tests, including
  * Repair invalid dates
  * Identify invalid structures by comparing against a model built from the spec structures.
* Select a subset of the individuals in the GEDCOM using a query language

Examples:
```java
// Simple read
InputStream in = new FileInputStream("file.gedcom");
GEDCOM gedcom = new GEDCOM();
gedcom.read(in);
for (Record r : gedcom.getRecords()) {
    if (r instanceof Person) {
        System.out.println(r);
        for (Connection c : r.getConnections()) {
            Connection.Role role = c.getRole(); // eg father, sibling, child
            Person other = c.getObject(); the other person
            System.out.println(" * " + role + " " + other.getRecord("NAME"));
        }
    }
}
```

```
// Verify and repair trivial errors
InputStream in = new FileInputStream("file.gedcom");
GEDCOM gedcom = new GEDCOM();
gedcom.read(in);
Verification v = new Verifier();
for (Verification.Fault f : v.verify(gedcom)) {
   String line = f.getRecord().getLineNumber();
   String desc = f.getDescription();
   Verification.Severity severity = f.getSeverity();
   if (severity == Verification.Severity.Trivial) {
       f.fix();
   }
}
OutputStream out = new FileOutputStream("out.gedcom");
gedcom.write(out);
out.close();
```

```
// Select a subset of records
InputStream in = new FileInputStream("file.gedcom");
GEDCOM gedcom = new GEDCOM();
gedcom.read(in);
// This ZPath query selectors all descendants of all ancestors of anyone called "John Smith"
// unless they have a "wikitree.privacy" value of less than 60
String query = """
'*[name/givn == "John" && name/surn == "Smith"]/ancestors/descendants[!(refn[type == "wikitree.privacy"] < 60)]'
""";
ZPath zpath = ZPath.compile(query);
List<Object> match = path.eval(gedcom).all();
for (Iterator<Record> i = gedcom.getRecords().iterator();i.hasNext();) {
    Record r = i.next();
    if (r instanceof Person && !match.contains(r)) {
        i.remove();
    }
}
OutputStream out = new FileOutputStream("out.gedcom");
gedcom.write(out);
out.close();
```

There is also a Main class which can be executed to run most of these operations directly
```
java -jar dist/gedcom-0.1-all.jar --help
```

## ZPath
The API comes with a ZPath implementation (the first based on a Model that is not XML or JSON; it was a good test).
Queries are made against the GEDCOM file itself and match only INDI records. Some examples
```
|Query|Result
|-|-
|*|Select all records (remember only INDI records are selected)
|*/name/surn|Select the NAME.SURN from all records
|*[name/surn == "Smith"]|Select all records that ... have NAME.SURN of "Smith"
|*[birt/date >= "JAN 1900" && birt/date <= "MAR 1900"]|... have a birthdate between 1 Jan and 31 Mar 1900
|*[www == "https://www.WikiTree.com/wiki/Smith-1"]|... have a WWW child of this value
|*[name/surn == "Smith"]/parents|... are the parent of anyone with NAME.SURN of "Smith"
|*[name/surn == "Smith"]/children|... are the children of anyone with NAME.SURN of "Smith"
|*[name/surn == "Smith"]/spouses|... are the spouses of anyone with NAME.SURN of "Smith"
|*[name/surn == "Smith"]/ancestors|... are the ancestor of anyone with NAME.SURN of "Smith"
|*[name/surn == "Smith"]/descendants|... are the descendant of anyone with NAME.SURN of "Smith"
|*[name/surn == "Smith"]/ancestors/descendants|... are the descendant of an ancestors of anyone with NAME.SURN of "Smith"

