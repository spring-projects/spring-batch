/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.classify;

import org.springframework.batch.support.MethodInvoker;
import org.springframework.batch.support.MethodInvokerUtils;
import org.springframework.util.Assert;

/**
 * Wrapper for an object to adapt it to the {@link Classifier} interface.
 * 
 * @author Dave Syer
 * 
 */
public class ClassifierAdapter<C, T> implements Classifier<C, T> {

	private MethodInvoker invoker;

	private Classifier<C, T> classifier;

	/**
	 * Default constructor for use with setter injection.
	 */
	public ClassifierAdapter() {
		super();
	}

	/**
	 * Create a new {@link Classifier} from the delegate provided. Use the
	 * constructor as an alternative to the {@link #setDelegate(Object)} method.
	 * 
	 * @param delegate
	 */
	public ClassifierAdapter(Object delegate) {
		setDelegate(delegate);
	}

	/**
	 * Create a new {@link Classifier} from the delegate provided. Use the
	 * constructor as an alternative to the {@link #setDelegate(Classifier)}
	 * method.
	 * 
	 * @param delegate
	 */
	public ClassifierAdapter(Classifier<C, T> delegate) {
		classifier = delegate;
	}

	public void setDelegate(Classifier<C, T> delegate) {
		classifier = delegate;
		invoker = null;
	}

	/**
	 * Search for the
	 * {@link org.springframework.batch.support.annotation.Classifier
	 * Classifier} annotation on a method in the supplied delegate and use that
	 * to create a {@link Classifier} from the parameter type to the return
	 * type. If the annotation is not found a unique non-void method with a
	 * single parameter will be used, if it exists. The signature of the method
	 * cannot be checked here, so might be a runtime exception when the method
	 * is invoked if the signature doesn't match the classifier types.
	 * 
	 * @param delegate an object with an annotated method
	 */
	public final void setDelegate(Object delegate) {
		classifier = null;
		invoker = MethodInvokerUtils.getMethodInvokerByAnnotation(
				org.springframework.batch.support.annotation.Classifier.class, delegate);
		if (invoker == null) {
			invoker = MethodInvokerUtils.<C, T> getMethodInvokerForSingleArgument(delegate);
		}
		Assert.state(invoker != null, "No single argument public method with or without "
				+ "@Classifier was found in delegate of type " + delegate.getClass());
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public T classify(C classifiable) {
		if (classifier != null) {
			return classifier.classify(classifiable);
		}
		return (T) invoker.invokeMethod(classifiable);
	}

}
