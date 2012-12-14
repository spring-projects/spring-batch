package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JdbcPagingItemReaderConfigTests {

	@Autowired
	private JdbcPagingItemReader<Object> jdbcPagingItemReder;

	@Test
	public void testConfig() {
		assertNotNull(jdbcPagingItemReder);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = (NamedParameterJdbcTemplate)
                ReflectionTestUtils.getField(jdbcPagingItemReder, "namedParameterJdbcTemplate");
        JdbcTemplate jdbcTemplate = (JdbcTemplate) namedParameterJdbcTemplate.getJdbcOperations();
		assertEquals(1000, jdbcTemplate.getMaxRows());
		assertEquals(100, jdbcTemplate.getFetchSize());
	}

}
