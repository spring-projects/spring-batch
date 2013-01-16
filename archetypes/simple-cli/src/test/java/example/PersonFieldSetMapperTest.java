/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example;

import org.junit.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;
import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Example test case mapping a {@link org.springframework.batch.item.file.transform.FieldSet} to a {@link Person} using the
 * {@link PersonFieldSetMapper}.
 * </p>
 */
public class PersonFieldSetMapperTest {
    private static final String[] DEFAULT_TOKENS = new String[] {"Jane", "Doe"};
    private static final String[] DEFAULT_NAMES = new String[] {"firstName", "lastName"};

    @Test
    public void testValidFieldSet() throws BindException {
        final FieldSet fieldSet = new DefaultFieldSet(DEFAULT_TOKENS, DEFAULT_NAMES);

        final PersonFieldSetMapper personFieldSetMapper = new PersonFieldSetMapper();
        final Person person = personFieldSetMapper.mapFieldSet(fieldSet);

        assertEquals("Jane", person.getFirstName());
        assertEquals("Doe", person.getLastName());
    }
}
