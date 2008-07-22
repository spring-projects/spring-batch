package org.springframework.batch.sample;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for BatchSqlUpdateJob - checks that customer credit has been updated to expected value.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class BatchSqlUpdateJobFunctionalTests extends AbstractCustomerCreditIncreaseTests {

}
