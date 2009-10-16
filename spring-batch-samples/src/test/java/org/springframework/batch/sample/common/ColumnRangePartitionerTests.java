package org.springframework.batch.sample.common;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ColumnRangePartitionerTests {
	
	private DataSource dataSource;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private ColumnRangePartitioner partitioner = new ColumnRangePartitioner();

	@Test
	public void testPartition() {
		partitioner.setDataSource(dataSource);
		partitioner.setTable("CUSTOMER");
		partitioner.setColumn("ID");
		Map<String, ExecutionContext> partition = partitioner.partition(2);
		assertEquals(2, partition.size());
	}

}
