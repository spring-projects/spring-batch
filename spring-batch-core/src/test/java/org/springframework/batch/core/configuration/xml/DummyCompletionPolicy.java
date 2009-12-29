package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyCompletionPolicy implements CompletionPolicy {

	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		return false;
	}

	public boolean isComplete(RepeatContext context) {
		return false;
	}

	public RepeatContext start(RepeatContext parent) {
		return null;
	}

	public void update(RepeatContext context) {
	}

}
