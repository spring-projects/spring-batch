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

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Example test case verifying data written by the {@link PersonItemWriter}.
 * </p>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:test-context.xml")
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
