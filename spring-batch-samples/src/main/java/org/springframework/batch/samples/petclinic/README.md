# PetClinic Job

## About the sample

This sample uses the [PetClinic Spring application](https://github.com/spring-projects/spring-petclinic) to show how to use
Spring Batch to export data from a relational database table to a flat file.

The job in this sample is a single-step job that exports data from the `owners` table
to a flat file named `owners.csv`.

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=PetClinicJobFunctionalTests#testLaunchJobWithXmlConfiguration test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=PetClinicJobFunctionalTests#testLaunchJobWithJavaConfiguration test
```