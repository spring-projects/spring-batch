## Multiline Aggregate Sample

### About

The goal of this sample is to show some common tricks with multiline
records in file input jobs.

The input file in this case consists of two groups of trades
delimited by special lines in a file (BEGIN and END):

```
BEGIN
UK21341EAH4597898.34customer1
UK21341EAH4611218.12customer2
END
BEGIN
UK21341EAH4724512.78customer2
UK21341EAH4810809.25customer3
UK21341EAH4985423.39customer4
END
```

The goal of the job is to operate on the two groups, so the item
type is naturally `List<Trade`>.  To get these items delivered
from an item reader we employ two components from Spring Batch: the
`AggregateItemReader` and the
`PrefixMatchingCompositeLineTokenizer`.  The latter is
responsible for recognising the difference between the trade data
and the delimiter records.  The former is responsible for
aggregating the trades from each group into a `List` and handing
out the list from its `read()` method.  To help these components
perform their responsibilities we also provide some business
knowledge about the data in the form of a `FieldSetMapper`
(`TradeFieldSetMapper`).  The `TradeFieldSetMapper` checks
its input for the delimiter fields (BEGIN, END) and if it detects
them, returns the special tokens that `AggregateItemReader`
needs.  Otherwise it maps the input into a `Trade` object.

### Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=MultilineAggregateJobFunctionalTests#testJobLaunch test
```