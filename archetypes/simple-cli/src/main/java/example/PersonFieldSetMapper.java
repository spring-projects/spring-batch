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

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 * <p>
 * {@link org.springframework.batch.item.file.mapping.FieldSetMapper} implementation to map a
 * {@link org.springframework.batch.item.file.transform.FieldSet} to a {@link Person}.
 * </p>
 */
public class PersonFieldSetMapper implements FieldSetMapper<Person> {
    @Override
    public Person mapFieldSet(final FieldSet fieldSet) throws BindException {
        final String firstName = fieldSet.readString("firstName");
        final String lastName = fieldSet.readString("lastName");

        return new Person(firstName, lastName);
    }
}
