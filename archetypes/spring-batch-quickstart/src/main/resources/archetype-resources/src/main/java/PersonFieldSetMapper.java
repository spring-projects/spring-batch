package ${package};

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 * <p>
 * {@link FieldSetMapper} implementation to map a {@link FieldSet} to a {@link Person}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class PersonFieldSetMapper implements FieldSetMapper<Person> {
    @Override
    public Person mapFieldSet(final FieldSet fieldSet) throws BindException {
        final String firstName = fieldSet.readString("firstName");
        final String lastName = fieldSet.readString("lastName");

        return new Person(firstName, lastName);
    }
}
