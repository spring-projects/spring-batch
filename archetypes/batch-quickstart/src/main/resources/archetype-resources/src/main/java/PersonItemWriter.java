package ${package};

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * <p>
 * An example {@link ItemWriter} implementation that writes data using the provided {@link JdbcTemplate}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class PersonItemWriter implements ItemWriter<Person> {
    private static final Log LOG = LogFactory.getLog(PersonItemWriter.class);

    private final String sql;
    private final JdbcTemplate jdbcTemplate;

    public PersonItemWriter(final JdbcTemplate jdbcTemplate, final String sql) {
        this.sql = sql;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(final List<? extends Person> people) throws Exception {
        for (final Person person : people) {
            LOG.info("Writing person: " + person);
            jdbcTemplate.update(sql, person.getFirstName(), person.getLastName());
        }
    }
}
