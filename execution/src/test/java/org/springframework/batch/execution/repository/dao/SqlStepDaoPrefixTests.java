package org.springframework.batch.execution.repository.dao;

import java.util.ArrayList;
import java.util.List;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import junit.framework.TestCase;

/**
 * Unit Test of SqlStepDao that only tests prefix matching. A
 * separate test is needed because all other tests hit hsql,
 * while this test needs to mock JdbcTemplate to analyze the
 * Sql passed in.
 * 
 * @author Lucas Ward
 *
 */
public class SqlStepDaoPrefixTests extends TestCase {
	
	private SqlStepDao stepDao;
	
	MockJdbcTemplate jdbcTemplate = new MockJdbcTemplate();
	
	StepExecution stepExecution = new StepExecution(new Long(1), new Long(2));
	StepInstance step = new StepInstance(new Long(1));
	JobInstance job = new JobInstance(new Long(1));
	
	MockControl stepExecutionIncrementerControl = MockControl.createControl(DataFieldMaxValueIncrementer.class);
	DataFieldMaxValueIncrementer stepExecutionIncrementer;
	MockControl stepIncrementerControl = MockControl.createControl(DataFieldMaxValueIncrementer.class);
	DataFieldMaxValueIncrementer stepIncrementer;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		stepDao = new SqlStepDao();
		stepExecutionIncrementer = (DataFieldMaxValueIncrementer)stepExecutionIncrementerControl.getMock();
		stepIncrementer = (DataFieldMaxValueIncrementer)stepIncrementerControl.getMock();
		
		stepDao.setJdbcTemplate(jdbcTemplate);
		stepDao.setStepExecutionIncrementer(stepExecutionIncrementer);
		stepDao.setStepIncrementer(stepIncrementer);
		stepExecution.setId(new Long(1));
		step.setStatus(BatchStatus.STARTED);
		
		job.addStep(step);
		
	}
	
	public void testModifiedUpdateStepExecution(){
		stepDao.setTablePrefix("FOO_");
		stepDao.update(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP_EXECUTION") != -1);
	}
	
	public void testModifiedSaveStepExecution(){
		stepDao.setTablePrefix("FOO_");
		stepExecutionIncrementer.nextLongValue();
		stepExecutionIncrementerControl.setReturnValue(1);
		stepExecutionIncrementerControl.replay();
		stepDao.save(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP_EXECUTION") != -1);
	}
	
	public void testModifiedFindStepExecutions(){
		stepDao.setTablePrefix("FOO_");
		stepDao.findStepExecutions(step);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP_EXECUTION") != -1);
	}
	
	public void testModifiedUpdateStep(){
		stepDao.setTablePrefix("FOO_");
		stepDao.update(step);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP") != -1);
	}
	
	public void testModifiedCreateStep(){
		stepDao.setTablePrefix("FOO_");
		stepIncrementer.nextLongValue();
		stepIncrementerControl.setReturnValue(1);
		stepIncrementerControl.replay();
		stepDao.createStep(job, "test");
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP") != -1);
	}
	
	public void testModifiedFindSteps(){
		stepDao.setTablePrefix("FOO_");
		stepDao.findSteps(new Long(1));
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("FOO_STEP") != -1);
	}
	
	public void testModifiedFindStep(){
		stepDao.setTablePrefix("FOO_");
		try{
			stepDao.findStep(job, "test");
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
			stepDao.findStep(job, "test");
		}
		catch(NullPointerException ex){
			//It's going to throw a NullPointerException because the MockJdbcTemplate
			//isn't returning anything, but that's okay because I'm only concerned
			//with the sql that was passed in.
		}
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
		
	}
	
	public void testDefaultFindSteps(){
		stepDao.findSteps(new Long(1));
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
	}
	
	public void testDefaultCreateStep(){
		stepIncrementer.nextLongValue();
		stepIncrementerControl.setReturnValue(1);
		stepIncrementerControl.replay();
		stepDao.createStep(job, "test");
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
	}
	
	public void testDefaultUpdateStep(){
		stepDao.update(step);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP") != -1);
	}
	
	public void testDefaultFindStepExecutions(){
		stepDao.findStepExecutions(step);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP_EXECUTION") != -1);
	}
	
	public void testDefaultSaveStepExecution(){
		stepExecutionIncrementer.nextLongValue();
		stepExecutionIncrementerControl.setReturnValue(1);
		stepExecutionIncrementerControl.replay();
		stepDao.save(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP_EXECUTION") != -1);
	}
	
	public void testDefaultUpdateStepExecution(){
		stepDao.update(stepExecution);
		assertTrue(jdbcTemplate.getSqlStatement().indexOf("BATCH_STEP_EXECUTION") != -1);
	}
}
