# Football Job

## About the sample

This is a (American) Football statistics loading job. We gave it the
id of `footballJob` in our configuration file. Before diving
into the batch job, we'll examine the two input files that need to
be loaded. First is `player.csv`, which can be found in the
samples project under `src/main/resources/org/springframework/batch/samples/football/data`.
Each line within this file represents a player, with a unique id, the player’s name, position, etc:

```
AbduKa00,Abdul-Jabbar,Karim,rb,1974,1996
AbduRa00,Abdullah,Rabih,rb,1975,1999
AberWa00,Abercrombie,Walter,rb,1959,1982
AbraDa00,Abramowicz,Danny,wr,1945,1967
AdamBo00,Adams,Bob,te,1946,1969
AdamCh00,Adams,Charlie,wr,1979,2003
...
```

One of the first noticeable characteristics of the file is that each
data element is separated by a comma, a format most are familiar
with known as 'CSV'. Other separators such as pipes or semicolons
could just as easily be used to delineate between unique
elements. In general, it falls into one of two types of flat file
formats: delimited or fixed length.  (The fixed length case was
covered in the `fixedLengthImportJob`).

The second file, 'games.csv' is formatted the same as the previous
example, and resides in the same directory:

```
AbduKa00,1996,mia,10,nwe,0,0,0,0,0,29,104,,16,2
AbduKa00,1996,mia,11,clt,0,0,0,0,0,18,70,,11,2
AbduKa00,1996,mia,12,oti,0,0,0,0,0,18,59,,0,0
AbduKa00,1996,mia,13,pit,0,0,0,0,0,16,57,,0,0
AbduKa00,1996,mia,14,rai,0,0,0,0,0,18,39,,7,0
AbduKa00,1996,mia,15,nyg,0,0,0,0,0,17,96,,14,0
...
```

Each line in the file represents an individual player's performance
in a particular game, containing such statistics as passing yards,
receptions, rushes, and total touchdowns.

Our example batch job is going to load both files into a database,
and then combine each to summarise how each player performed for a
particular year. Although this example is fairly trivial, it shows
multiple types of input, and the general style is a common batch
scenario.  That is, summarising a very large dataset so that it can
be more easily manipulated or viewed by an online web-based
application. In an enterprise solution the third step, the reporting
step, could be implemented through the use of Eclipse BIRT or one of
the many Java Reporting Engines. Given this description, we can then
easily divide our batch job up into 3 'steps': one to load the
player data, one to load the game data, and one to produce a summary
report:

```mermaid
graph LR
    A(playerLoad) --> B(gameLoad)
    B --> C(playerSummarization)
```

This corresponds exactly with the `footballJob.xml` job configuration file which can be found in
`src/main/resources/org/springframework/batch/samples/football/job`.
When you drill down into the football job you will see that the configuration has a list of steps:

```xml
<property name="steps">
  <list>
    <bean id="playerload" parent="simpleStep" .../>
    <bean id="gameLoad" parent="simpleStep" .../>
    <bean id="playerSummarization" parent="simpleStep" .../>
  </list>
</property>
```

A step is run until there is no more input to process, which in
this case would mean that each file has been completely
processed. To describe it in a more narrative form: the first step,
playerLoad, begins executing by grabbing one line of input from the
file, and parsing it into a domain object. That domain object is
then passed to a dao, which writes it out to the PLAYERS table. This
action is repeated until there are no more lines in the file,
causing the playerLoad step to finish. Next, the gameLoad step does
the same for the games input file, inserting into the GAMES
table. Once finished, the playerSummarization step can begin. Unlike
the first two steps, playerSummarization input comes from the
database, using a Sql statement to combine the GAMES and PLAYERS
table. Each returned row is packaged into a domain object and
written out to the PLAYER_SUMMARY table.

Now that we've discussed the entire flow of the batch job, we can
dive deeper into the first step: playerLoad:

```xml
<bean id="playerload" parent="simpleStep">
  <property name="commitInterval" value="${job.commit.interval}" />
  <property name="startLimit" value="100" />
  <property name="itemReader"
    ref="playerFileItemReader" />
  <property name="itemWriter">
    <bean
      class="org.springframework.batch.samples.football.internal.internal.PlayerItemWriter">
      <property name="playerDao">
        <bean
          class="org.springframework.batch.samples.football.internal.internal.JdbcPlayerDao">
          <property name="dataSource"
            ref="dataSource" />
        </bean>
      </property>
    </bean>
  </property>
</bean>
```

The root bean in this case is a `SimpleStepFactoryBean`, which
can be considered a 'blueprint' of sorts that tells the execution
environment basic details about how the batch job should be
executed. It contains four properties: (others have been removed for
greater clarity) commitInterval, startLimit, itemReader and
itemWriter . After performing all necessary startup, the framework
will periodically delegate to the reader and writer. In this way,
the developer can remain solely concerned with their business
logic.

* *ItemReader* – the item reader is the source of the information
  pipe. At the most basic level input is read in from an input
  source, parsed into a domain object and returned. In this way, the
  good batch architecture practice of ensuring all data has been
  read before beginning processing can be enforced, along with
  providing a possible avenue for reuse.

* *ItemWriter* – this is the business logic. At a high level,
  the item writer takes the item returned from the reader
  and 'processes' it. In our case it's a data access object that is
  simply responsible for inserting a record into the PLAYERS
  table. As you can see the developer does very little.

The application developer simply provides a job configuration with a
configured number of steps, an ItemReader associated to some type
of input source, and ItemWriter associated to some type of
output source and a little mapping of data from flat records to
objects and the pipe is ready wired for processing.

Another property in the step configuration, the commitInterval,
gives the framework vital information about how to control
transactions during the batch run. Due to the large amount of data
involved in batch processing, it is often advantageous to 'batch'
together multiple logical units of work into one transaction, since
starting and committing a transaction is extremely expensive. For
example, in the playerLoad step, the framework calls read() on the
item reader. The item reader reads one record from the file, and
returns a domain object representation which is passed to the
processor. The writer then writes the one record to the database. It
can then be said that one iteration = one call to
`ItemReader.read()` = one line of the file. Therefore, setting
your commitInterval to 5 would result in the framework committing a
transaction after 5 lines have been read from the file, with 5
resultant entries in the PLAYERS table.

Following the general flow of the batch job, the next step is to
describe how each line of the file will be parsed from its string
representation into a domain object. The first thing the provider
will need is an `ItemReader`, which is provided as part of the Spring
Batch infrastructure. Because the input is flat-file based, a
`FlatFileItemReader` is used:

```xml

<bean id="playerFileItemReader"
      class="org.springframework.batch.infrastructure.item.file.FlatFileItemReader">
    <property name="resource"
              value="classpath:data/footballjob/input/${player.file.name}"/>
    <property name="lineTokenizer">
        <bean
                class="org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer">
            <property name="names"
                      value="ID,lastName,firstName,position,birthYear,debutYear"/>
        </bean>
    </property>
    <property name="fieldSetMapper">
        <bean
                class="org.springframework.batch.samples.football.internal.internal.PlayerFieldSetMapper"/>
    </property>
</bean>
```

There are three required dependencies of the item reader; the first
is a resource to read in, which is the file to process. The second
dependency is a `LineTokenizer`. The interface for a
`LineTokenizer` is very simple, given a string; it will return a
`FieldSet` that wraps the results from splitting the provided
string. A `FieldSet` is Spring Batch's abstraction for flat file
data. It allows developers to work with file input in much the same
way as they would work with database input. All the developers need
to provide is a `FieldSetMapper` (similar to a Spring
`RowMapper`) that will map the provided `FieldSet` into an
`Object`. Simply by providing the names of each token to the
`LineTokenizer`, the `ItemReader` can pass the
`FieldSet` into our `PlayerMapper`, which implements the
`FieldSetMapper` interface. There is a single method,
`mapLine()`, which maps `FieldSet`s the same way that
developers are comfortable mapping `ResultSet`s into Java
`Object`s, either by index or field name. This behaviour is by
intention and design similar to the `RowMapper` passed into a
`JdbcTemplate`.  You can see this below:

```java
public class PlayerMapper implements FieldSetMapper {

    public Object mapLine(FieldSet fs) {

        if(fs == null){
            return null;
        }

        Player player = new Player();
        player.setID(fs.readString("ID"));
        player.setLastName(fs.readString("lastName"));
        player.setFirstName(fs.readString("firstName"));
        player.setPosition(fs.readString("position"));
        player.setDebutYear(fs.readInt("debutYear"));
        player.setBirthYear(fs.readInt("birthYear"));

        return player;
    }
}
```

The flow of the `ItemReader`, in this case, starts with a call
to read the next line from the file. This is passed into the
provided `LineTokenizer`. The `LineTokenizer` splits the
line at every comma, and creates a `FieldSet` using the created
`String` array and the array of names passed in.

**Note:** it is only necessary to provide the names to create the
`FieldSet` if you wish to access the field by name, rather
than by index.

Once the domain representation of the data has been returned by the
provider, (i.e. a `Player` object in this case) it is passed to
the `ItemWriter`, which is essentially a Dao that uses a Spring
`JdbcTemplate` to insert a new row in the PLAYERS table.

The next step, gameLoad, works almost exactly the same as the
playerLoad step, except the games file is used.

The final step, playerSummarization, is much like the previous two
steps, in that it reads from a reader and returns a domain object to
a writer. However, in this case, the input source is the database,
not a file:

```xml

<bean id="playerSummarizationSource"
      class="org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader">
    <property name="dataSource" ref="dataSource"/>
    <property name="mapper">
        <bean
                class="org.springframework.batch.samples.football.internal.internal.PlayerSummaryMapper"/>
    </property>
    <property name="sql">
        <value>
            SELECT games.player_id, games.year_no, SUM(COMPLETES),
            SUM(ATTEMPTS), SUM(PASSING_YARDS), SUM(PASSING_TD),
            SUM(INTERCEPTIONS), SUM(RUSHES), SUM(RUSH_YARDS),
            SUM(RECEPTIONS), SUM(RECEPTIONS_YARDS), SUM(TOTAL_TD)
            from games, players where players.player_id =
            games.player_id group by games.player_id, games.year_no
        </value>
    </property>
</bean>
```

The `JdbcCursorItemReader` has three dependencies:

* A `DataSource`
* The `RowMapper` to use for each row.
* The Sql statement used to create the cursor.

When the step is first started, a query will be run against the
database to open a cursor, and each call to `itemReader.read()`
will move the cursor to the next row, using the provided
`RowMapper` to return the correct object. As with the previous
two steps, each record returned by the provider will be written out
to the database in the PLAYER_SUMMARY table.

The equivalent Java configuration of the football job can be found in
`org/springframework/batch/samples/football/FootballJobConfiguration.java`.

## Run the sample

You can run the sample from the command line as following:

```
$>cd spring-batch-samples
# Launch the sample using the XML configuration
$>../mvnw -Dtest=FootballJobFunctionalTests#testLaunchJobWithXmlConfiguration test
# Launch the sample using the Java configuration
$>../mvnw -Dtest=FootballJobFunctionalTests#testLaunchJobWithJavaConfiguration test
```