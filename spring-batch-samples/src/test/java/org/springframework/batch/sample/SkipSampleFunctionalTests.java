package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.support.ItemTrackingItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Error is encountered during writing - transaction is rolled back and the
 * error item is skipped on second attempt to process the chunk.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class SkipSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	int before = -1;

	SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	ItemTrackingItemWriter<?> writer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUp() throws Exception {
		before = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
	}

	protected void validatePostConditions() throws Exception {

		int after = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
		// 5 input records, 1 skipped => 4 written to output
		assertEquals(before + 4, after);
		
		// no item was processed twice (no rollback occurred despite error on write)
		assertEquals(after, writer.getItems().size());
	}

}
