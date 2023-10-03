### Json Input Output Job

## About

The goal of this sample is to show how to read and write JSON files:

```json
[
  {"isin":"123","quantity":5,"price":10.5,"customer":"foo","id":1,"version":0},
  {"isin":"456","quantity":10,"price":20.5,"customer":"bar","id":2,"version":0},
  {"isin":"789","quantity":15,"price":30.5,"customer":"baz","id":3,"version":0}
]
```

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
$>../mvnw -Dtest=JsonFunctionalTests#testJsonReadingAndWriting test
```

