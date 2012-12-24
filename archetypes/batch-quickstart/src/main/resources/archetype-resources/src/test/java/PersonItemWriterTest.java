package ${package};

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Example test case verifying data written by the {@link PersonItemWriter}.
 * </p>
 *
 * @author Chris Schaefer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/${package.replace('.','/')}/ExampleItemWriterTest-context.xml")
public class PersonItemWriterTest {
    private static final String COUNT_SQL = "SELECT count(*) from people";
    private static final String INSERT_SQL = "INSERT INTO people (first_name, last_name) VALUES (?,?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testWrittenPersonRecordsCount() throws Exception {
        final List<Person> people = new ArrayList<Person>();
        people.add(new Person("John", "Doe"));
        people.add(new Person("Jane", "Doe"));

        final PersonItemWriter personItemWriter = new PersonItemWriter(jdbcTemplate, INSERT_SQL);
        personItemWriter.write(people);

        final int count = jdbcTemplate.queryForInt(COUNT_SQL);
        assertEquals(2, count);
    }
}
