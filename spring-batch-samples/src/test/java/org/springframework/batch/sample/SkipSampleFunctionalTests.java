package org.springframework.batch.sample;

import org.springframework.batch.sample.item.writer.ItemTrackingItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Error is encountered during writing - transaction is rolled back and the
 * error item is skipped on second attempt to process the chunk.
 * 
 * @author Robert Kasanicky
 */
public class SkipSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	int before = -1;

	JdbcTemplate jdbcTemplate;

	ItemTrackingItemWriter writer;

	// auto-injection
	public void setWriter(ItemTrackingItemWriter writer) {
		this.writer = writer;
	}

	// auto-injection
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	protected void onSetUp() throws Exception {
		before = jdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
	}

	protected void validatePostConditions() throws Exception {
		int after = jdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
		// 5 input records, 1 skipped => 4 written to output
		assertEquals(before + 4, after);
		
		// no item was processed twice (no rollback occurred despite error on write)
		assertEquals(after, writer.getItems().size());
	}

}
