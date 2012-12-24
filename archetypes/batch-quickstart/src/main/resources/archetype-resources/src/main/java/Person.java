package ${package};

/**
 * <p>
 * Domain object representing infomation about a person.
 * </p>
 *
 * @author Chris Schaefer
 */
public class Person {
    private final String firstName;
    private final String lastName;

    public Person(final String firstName, final String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String toString() {
        return "firstName: " + firstName + ", lastName: "  + lastName;
    }
}
