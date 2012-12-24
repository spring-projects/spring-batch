package ${package};

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Example test case processing a {@link Person} using the {@link PersonItemProcessor}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class PersonItemProcessorTest {
    @Test
    public void testProcessedPersonRecord() throws Exception {
        final Person person = new Person("Jane", "Doe");
        final Person processedPerson = new PersonItemProcessor().process(person);

        assertEquals("JANE", processedPerson.getFirstName());
        assertEquals("DOE", processedPerson.getLastName());
    }
}
