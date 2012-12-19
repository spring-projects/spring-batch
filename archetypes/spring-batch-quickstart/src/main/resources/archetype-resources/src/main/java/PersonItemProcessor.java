package ${package};

import org.springframework.batch.item.ItemProcessor;

/**
 * <p>
 * An example {@link ItemProcessor} implementation that uppercases fields on the provided {@link Person} object.
 * </p>
 *
 * @author Chris Schaefer
 */
public class PersonItemProcessor implements ItemProcessor<Person, Person> {
    @Override
    public Person process(final Person person) throws Exception {
        final String firstName = person.getFirstName().toUpperCase();
        final String lastName = person.getLastName().toUpperCase();

        return new Person(firstName, lastName);
    }
}
