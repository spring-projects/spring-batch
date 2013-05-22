package org.springframework.batch.core.step.tasklet;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;

/**
 * basic mock test
 * @author Will Schipp
 *
 */
public class StoppableTaskletTest {

	@Test
	public void testStop() {
		StoppableTasklet stoppableTasklet = mock(StoppableTasklet.class);
		try {
			stoppableTasklet.stop();
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
