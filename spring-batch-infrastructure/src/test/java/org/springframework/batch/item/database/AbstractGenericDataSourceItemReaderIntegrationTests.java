package org.springframework.batch.item.database;



import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemReader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Generic configuration for testing {@link ItemReader} implementations which read
 * data from database. Uses a common test context and HSQLDB database.
 * 
 * @author Thomas Risberg
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
public abstract class AbstractGenericDataSourceItemReaderIntegrationTests 
		extends AbstractDataSourceItemReaderIntegrationTests {

}
