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

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <p>
 * An example {@link org.springframework.batch.item.ItemWriter} implementation that writes data using the provided
 * {@link org.springframework.jdbc.core.JdbcTemplate}.
 * </p>
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
