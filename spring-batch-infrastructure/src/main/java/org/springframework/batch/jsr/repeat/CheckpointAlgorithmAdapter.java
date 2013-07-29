package org.springframework.batch.jsr.repeat;

import javax.batch.api.chunk.CheckpointAlgorithm;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.Assert;

public class CheckpointAlgorithmAdapter implements CompletionPolicy {

	private CheckpointAlgorithm policy;

	public CheckpointAlgorithmAdapter(CheckpointAlgorithm policy) {
		Assert.notNull(policy, "A CheckpointAlgorithm is required");

		this.policy = policy;
	}

	@Override
	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		try {
			return policy.isReadyToCheckpoint();
		} catch (Exception e) {
			//TODO: do something here
		}

		return false;
	}

	@Override
	public boolean isComplete(RepeatContext context) {
		try {
			return policy.isReadyToCheckpoint();
		} catch (Exception e) {
			//TODO: do something here
		}

		return false;
	}

	@Override
	public RepeatContext start(RepeatContext parent) {
		try {
			policy.beginCheckpoint();
		} catch (Exception e) {
			//TODO: do something here
		}

		return null;
	}

	@Override
	public void update(RepeatContext context) {
		try {
			if(policy.isReadyToCheckpoint()) {
				policy.endCheckpoint();
			}
		} catch (Exception e) {
			//TODO: do something here
		}
	}
}
