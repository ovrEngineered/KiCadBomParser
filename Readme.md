
# KiCAD Bom Parser
This is a simple java program which will post-process the Comma-Separated Values (CSV) Bill of Materials (BOM) output from KiCAD produced by the `bom2csv` plugin.

## Usage
There are a couple different usages for this program listed below.

### Part Concatenation
By default `bom2csv` will generate a single line for every component. This will create many duplicate lines for things like resistors, capacitors, etc. To
concatenate those multiple duplicate lines into a single line, use this command:

```
java KiCadBomParser -i <yourInputCsv> -o <yourOutputCsv> -c <idFieldName> -q <quantityFieldName> -r <referenceFieldName>
```

This example will:
 * read from the path indicated by `<yourInputCsv>`
 * concatenate parts by matching identical fields in the <idFieldName> (usually something like `"Supplier Part Num."` for matching by supplier part number)
 * create a `Quantity` field for each part (even unaffected parts) that will be updated with part quantities
 * update the `Reference` field for each affected part with a space-delimited list of all identical parts
 * output to a file at the path indicated by `<yourOutputCsv>`
 
 
### Supplier Separation
If you have multiple suppliers, it's often useful to separate components into separate BOMs for ordering...use this command:
 
```
java KiCadBomParser -i <yourInputCsv> -o <yourOutputCsv> -s <idFieldName>
```
 
This example will:
 * read from the path indicated by `<yourInputCsv>`
 * split the parts based upon the field specified in <idFieldName> (usually something like `"Supplier"`)
 * output to multiple files at the path indicated by `<yourOutputCsv>`
   * eg. if you specified `foo.csv` and you have parts from 'DigiKey' and 'Mouser' will create two files:
     * `foo-DigiKey.csv`
     * `foo-Mouser.csv`