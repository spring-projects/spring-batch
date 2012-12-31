package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyCompletionPolicy implements CompletionPolicy {

	@Override
	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		return false;
	}

	@Override
	public boolean isComplete(RepeatContext context) {
		return false;
	}

	@Override
	public RepeatContext start(RepeatContext parent) {
		return null;
	}

	@Override
	public void update(RepeatContext context) {
	}

}
