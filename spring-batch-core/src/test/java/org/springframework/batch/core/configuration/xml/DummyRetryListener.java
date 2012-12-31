package org.springframework.batch.core.configuration.xml;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyRetryListener implements RetryListener {

	@Override
	public <T> boolean open(RetryContext context, RetryCallback<T> callback) {
		return false;
	}

	@Override
	public <T> void close(RetryContext context, RetryCallback<T> callback, Throwable throwable) {
	}

	@Override
	public <T> void onError(RetryContext context, RetryCallback<T> callback, Throwable throwable) {
	}

}
