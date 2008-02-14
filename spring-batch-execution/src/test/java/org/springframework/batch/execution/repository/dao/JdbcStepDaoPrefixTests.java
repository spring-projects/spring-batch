package org.springframework.batch.execution.repository.dao;

import java.util.List;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * Unit Test of SqlStepDao that only tests prefix matching. A
 * separate test is needed because all other tests hit hsql,
 * while this test needs to mock JdbcTemplate to analyze the
 * Sql passed in.
 * 
 * @author Lucas Ward
 *
 */
public class JdbcStepDaoPrefixTests extends TestCase {
	
	private JdbcStepInstanceDao stepInstanceDao;
	
	private JdbcStepExecutionDao stepExecutionDao;
	
	MockJdbcTemplate jdbcTemplate = new MockJdbcTemplate();
	
	JobInstance job = new JobInstance(new Long(1), new JobParameters());
	StepInstance step = new StepInstance(job, "foo", new Long(1));
	StepExecution stepExecution = new StepExecution(step, new JobExecution(job), null);
	
	MockControl stepExecutionIncrementerControl = MockControl.createControl(DataFieldMaxValueIncrementer.class);
	DataFieldMaxValueIncrementer stepExecutionIncrementer;
	MockControl stepIncrementerControl = MockControl.createControl(DataFieldMaxValueIncrementer.class);
	DataFieldMaxValueIncrementer stepIncrementer;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		stepInstanceDao = new JdbcStepInstanceDao();
		stepExecutionDao = new JdbcStepExecutionDao();
		stepExecutionIncrementer = (DataFieldMaxValueIncrementer)stepExecutionIncrementerControl.getMock();
		stepIncrementer = (DataFieldMaxValueIncrementer)stepIncrementerControl.getMock();
		
		stepInstanceDao.setJdbcTemplate(jdbcTemplate);
		stepExecutionDao.setJdbcTemplate(jdbcTemplate);
		stepExecutionDao.setStepExecutionIncrementer(stepExecutionIncrementer);
		stepInstanceDao.setStepIncrementer(stepIncrementer);
		stepExecution.setId(new Long(1));
		stepExecution.incrementVersion();
		step.setLastExecution(stepExecution);
		
		job.addStepInstance(step);
		
	}
	
	public void testModifiedUpdateStepExecution(){
		stepExecutionDao.setTablePrefix("FOO_");
		stepExecutionDao.updateStepExecution(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP_EXECUTION") != -1);
	}
	
	public void testModifiedSaveStepExecution(){
		stepExecutionDao.setTablePrefix("FOO_");
		stepExecutionIncrementer.nextLongValue();
		stepExecutionIncrementerControl.setReturnValue(1);
		stepExecutionIncrementerControl.replay();
		stepExecutionDao.saveStepExecution(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP_EXECUTION") != -1);
	}
	
	public void testModifiedFindStepExecutions(){
		stepExecutionDao.setTablePrefix("FOO_");
		stepExecutionDao.findStepExecutions(step);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP_EXECUTION") != -1);
	}
	
//	public void testModifiedUpdateStep(){
//		stepInstanceDao.setTablePrefix("FOO_");
//		stepInstanceDao.updateStepInstance(step);
//		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP") != -1);
//	}
	
	public void testModifiedCreateStep(){
		stepInstanceDao.setTablePrefix("FOO_");
		stepIncrementer.nextLongValue();
		stepIncrementerControl.setReturnValue(1);
		stepIncrementerControl.replay();
		stepInstanceDao.createStepInstance(job, "test");
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP") != -1);
	}
	
	public void testModifiedFindSteps(){
		stepInstanceDao.setTablePrefix("FOO_");
		stepInstanceDao.findStepInstances(new JobInstance(new Long(1), new JobParameters()));
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP") != -1);
	}
	
	public void testModifiedFindStep(){
		stepInstanceDao.setTablePrefix("FOO_");
		try{
			stepInstanceDao.findStepInstance(job, "test");
		}
		catch(NullPointerException ex){
			//It's going to throw a NullPointerException because the MockJdbcTemplate
			//isn't returning anything, but that's okay because I'm only concerned
			//with the sql that was passed in.
		}
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP") != -1);
	}
	
	public void testDefaultFindStep(){
		try{
			stepInstanceDao.findStepInstance(job, "test");
		}
		catch(NullPointerException ex){
			//It's going to throw a NullPointerException because the MockJdbcTemplate
			//isn't returning anything, but that's okay because I'm only concerned
			//with the sql that was passed in.
		}
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
		
	}
	
	public void testDefaultFindSteps(){
		stepInstanceDao.findStepInstances(new JobInstance(new Long(1), new JobParameters()));
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
	}
	
	public void testDefaultCreateStep(){
		stepIncrementer.nextLongValue();
		stepIncrementerControl.setReturnValue(1);
		stepIncrementerControl.replay();
		stepInstanceDao.createStepInstance(job, "test");
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
	}
	
//	public void testDefaultUpdateStep(){
//		stepInstanceDao.updateStepInstance(step);
//		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
//	}
	
	public void testDefaultFindStepExecutions(){
		stepExecutionDao.findStepExecutions(step);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP_EXECUTION") != -1);
	}
	
	public void testDefaultSaveStepExecution(){
		stepExecutionIncrementer.nextLongValue();
		stepExecutionIncrementerControl.setReturnValue(1);
		stepExecutionIncrementerControl.replay();
		stepExecutionDao.saveStepExecution(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP_EXECUTION") != -1);
	}	

	public void testDefaultUpdateStepExecution(){
		stepExecutionDao.updateStepExecution(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP_EXECUTION") != -1);
	}
	
	private class MockJdbcTemplate extends JdbcTemplate {

		private String sql;
		
		public int update(String sql, Object[] args) throws DataAccessException {
			this.sql = sql;
			return 1;
		}

		public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
			this.sql = sql;
			return 1;
		}

		public List query(String sql, Object[] args, RowMapper rowMapper) throws DataAccessException {
			this.sql = sql;
			return null;
		}
		
		public String getSqlStatement() {
			return sql;
		}
		
	}
	
}
