package ${package};

import org.junit.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Example test case mapping a {@link FieldSet} to a {@link Person} using the
 * {@link PersonFieldSetMapper}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class PersonFieldSetMapperTest {
    private static final String[] DEFAULT_TOKENS = new String[]{"Jane", "Doe"};
    private static final String[] DEFAULT_NAMES = new String[]{"firstName", "lastName"};

    @Test
    public void testValidFieldSet() throws BindException {
        final FieldSet fieldSet = new DefaultFieldSet(DEFAULT_TOKENS, DEFAULT_NAMES);

        final PersonFieldSetMapper personFieldSetMapper = new PersonFieldSetMapper();
        final Person person = personFieldSetMapper.mapFieldSet(fieldSet);

        assertEquals("Jane", person.getFirstName());
        assertEquals("Doe", person.getLastName());
    }
}
