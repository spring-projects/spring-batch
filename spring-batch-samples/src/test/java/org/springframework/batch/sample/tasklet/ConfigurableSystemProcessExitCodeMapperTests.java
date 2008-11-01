package org.springframework.batch.sample.tasklet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.sample.tasklet.ConfigurableSystemProcessExitCodeMapper;

/**
 * Tests for {@link ConfigurableSystemProcessExitCodeMapper}
 */
public class ConfigurableSystemProcessExitCodeMapperTests extends TestCase {

	private ConfigurableSystemProcessExitCodeMapper mapper = new ConfigurableSystemProcessExitCodeMapper();
	
	/**
	 * Regular usage scenario - mapping adheres to injected values
	 */
	public void testMapping() {
		Map mappings = new HashMap() {{
			put(new Integer(0), ExitStatus.FINISHED);
			put(new Integer(1), ExitStatus.FAILED);
			put(new Integer(2), ExitStatus.CONTINUABLE);
			put(new Integer(3), ExitStatus.NOOP);
			put(new Integer(4), ExitStatus.UNKNOWN);
			put(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY, ExitStatus.UNKNOWN);
		}};
		
		mapper.setMappings(mappings);
		
		//check explicitly defined values
		for (Iterator iterator = mappings.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey().equals(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY)) continue;
			
			int exitCode = ((Integer)entry.getKey()).intValue();
			assertSame(entry.getValue(), mapper.getExitStatus(exitCode));
		}
		
		//check the else clause
		assertSame(mappings.get(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY),
				mapper.getExitStatus(5));
	}
	
	/**
	 * Else clause is required in the injected map - setter checks its presence.
	 */
	public void testSetMappingsMissingElseClause() {
		Map missingElse = Collections.EMPTY_MAP;
		try {
			mapper.setMappings(missingElse);
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		
		Map containsElse = new HashMap() {{
			put(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY, ExitStatus.FAILED);
		}};
		// no error expected now
		mapper.setMappings(containsElse);
	}
	
}
