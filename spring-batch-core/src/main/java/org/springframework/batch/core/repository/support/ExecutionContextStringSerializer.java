package org.springframework.batch.core.repository.support;

import java.util.Map;

/**
 * @author trisberg
 */
public interface ExecutionContextStringSerializer {

	String serialize(Map<String, Object> context);

	Map<String, Object> deserialize(String context);
	
}
