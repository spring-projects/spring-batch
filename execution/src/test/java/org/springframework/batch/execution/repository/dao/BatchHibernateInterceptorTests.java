package org.springframework.batch.execution.repository.dao;

import junit.framework.TestCase;

import org.hibernate.type.Type;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.batch.repeat.ExitStatus;

public class BatchHibernateInterceptorTests extends TestCase {

	public static final String LONG_STRING = "A very long description: \n" +
	"Nov 16 20:10:42  util.JDBCExceptionReporter 78 - Data truncation: Data too long for column 'exit_message' at row 1\n" + 
	"Nov 16 20:10:43  def.AbstractFlushingEventListener 301 - Could not synchronize database state with session  \n" + 
	"org.hibernate.exception.DataException: could not update: [org.springframework.batch.core.domain.JobExecution#67]\n" + 
	"	at org.hibernate.exception.SQLStateConverter.convert(SQLStateConverter.java:77)\n";
	
	private BatchHibernateInterceptor interceptor = new BatchHibernateInterceptor();
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		interceptor.setEntityNameLocator(new EntityNameLocator() {
			public String locate(Class clz) {
				return "foo";
			}
		});
	}
	
	public void testAfterPropertiesSet() throws Exception {
		interceptor = new BatchHibernateInterceptor();
		try {
			interceptor.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testGetEntityName() {
		JobInstance job = new JobInstance(new ScheduledJobIdentifier("foo"));
		assertEquals("foo", interceptor.getEntityName(job));
	}
	
	public void testGetEntityNameForNonJobInstance() {
		Object job = new Object();
		assertEquals(null, interceptor.getEntityName(job));
	}
	
	public void testOnFlushDirtyWithJobInstance() throws Exception {
		JobInstance job = new JobInstance(new ScheduledJobIdentifier("foo"));
		assertFalse(interceptor.onFlushDirty(job, null, new Object[1], new Object[1], new String[] {"exitStatus"}, new Type[1]));
	}

	public void testOnFlushDirtyWithShortDescription() throws Exception {
		JobExecution execution = new JobExecution(new JobInstance(new ScheduledJobIdentifier("foo")));
		execution.setExitStatus(ExitStatus.UNKNOWN.addExitDescription("bar"));
		assertFalse(interceptor.onFlushDirty(execution, null, new Object[] {execution.getExitStatus()}, new Object[1], new String[] {"exitStatus"}, new Type[1]));
	}

	public void testOnFlushDirtyWithLongDescription() throws Exception {
		JobExecution execution = new JobExecution(new JobInstance(new ScheduledJobIdentifier("foo")));
		execution.setExitStatus(ExitStatus.UNKNOWN.addExitDescription(LONG_STRING));
		Object[] state = new Object[] {execution.getExitStatus()};
		assertTrue(interceptor.onFlushDirty(execution, null, state, new Object[1], new String[] {"exitStatus"}, new Type[1]));
		assertEquals(250, ((ExitStatus) state[0]).getExitDescription().length());
	}

	public void testOnFlushDirtyWithMissingExitStatusProperty() throws Exception {
		JobExecution execution = new JobExecution(new JobInstance(new ScheduledJobIdentifier("foo")));
		execution.setExitStatus(ExitStatus.UNKNOWN.addExitDescription("bar"));
		try {
			interceptor.onFlushDirty(execution, null, new Object[] {execution.getExitStatus()}, new Object[1], new String[1], new Type[1]);
			fail("Expected ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
			// expected;
		}
	}
}
