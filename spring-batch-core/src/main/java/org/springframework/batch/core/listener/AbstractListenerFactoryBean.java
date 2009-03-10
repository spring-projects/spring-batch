/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.batch.core.listener;

import static org.springframework.batch.support.MethodInvokerUtils.getMethodInvokerForInterface;
import static org.springframework.batch.support.MethodInvokerUtils.getParamTypesString;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.batch.support.MethodInvoker;
import org.springframework.batch.support.MethodInvokerUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link FactoryBean} implementation that builds a listener based on the
 * various lifecycle methods or annotations that are provided. There are three
 * possible ways of having a method called as part of a listener lifecycle:
 * 
 * <ul>
 * <li>Interface implementation: By implementing any of the subclasses of a
 * listener interface, methods on said interface will be called
 * <li>Annotations: Annotating a method will result in registration.
 * <li>String name of the method to be called, which is tied to a
 * {@link ListenerMetaData} value in the metaDataMap.
 * </ul>
 * 
 * It should be noted that methods obtained by name or annotation that don't
 * match the listener method signatures to which they belong will cause errors.
 * However, it is acceptable to have no parameters at all. If the same method is
 * marked in more than one way. (i.e. the method name is given and it is
 * annotated) the method will only be called once. However, if the same class
 * has multiple methods tied to a particular listener, each method will be
 * called. Also note that the same annotations cannot be applied to two separate
 * methods in a single class.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 * @see ListenerMetaData
 */
public abstract class AbstractListenerFactoryBean implements FactoryBean, InitializingBean {

	private static Log logger = LogFactory.getLog(AbstractListenerFactoryBean.class);

	private Object delegate;

	private Map<String, String> metaDataMap;

	public Object getObject() {

		if (metaDataMap == null) {
			metaDataMap = new HashMap<String, String>();
		}
		// Because all annotations and interfaces should be checked for, make
		// sure that each meta data
		// entry is represented.
		for (ListenerMetaData metaData : this.getMetaDataValues()) {
			if (!metaDataMap.containsKey(metaData.getPropertyName())) {
				// put null so that the annotation and interface is checked
				metaDataMap.put(metaData.getPropertyName(), null);
			}
		}

		Set<Class<?>> listenerInterfaces = new HashSet<Class<?>>();

		// For every entry in the map, try and find a method by interface, name,
		// or annotation. If the same
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		for (Entry<String, String> entry : metaDataMap.entrySet()) {
			final ListenerMetaData metaData = this.getMetaDataFromPropertyName(entry.getKey());
			Set<MethodInvoker> invokers = new NullIgnoringSet<MethodInvoker>();
			invokers.add(getMethodInvokerByName(entry.getValue(), delegate, metaData.getParamTypes()));
			invokers.add(getMethodInvokerForInterface(metaData.getListenerInterface(), metaData.getMethodName(),
					delegate, metaData.getParamTypes()));
			invokers.add(getMethodInvokerByAnnotation(metaData));
			if (!invokers.isEmpty()) {
				invokerMap.put(metaData.getMethodName(), invokers);
				listenerInterfaces.add(metaData.getListenerInterface());
			}
		}

		if (listenerInterfaces.isEmpty()) {
			listenerInterfaces.add(this.getDefaultListenerClass());
		}

		boolean ordered = false;
		if (delegate instanceof Ordered) {
			ordered = true;
			listenerInterfaces.add(Ordered.class);
		}

		// create a proxy listener for only the interfaces that have methods to
		// be called
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(delegate);
		proxyFactory.setInterfaces(listenerInterfaces.toArray(new Class[0]));
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(new MethodInvokerMethodInterceptor(invokerMap, ordered)));
		return proxyFactory.getProxy();

	}

	protected abstract ListenerMetaData getMetaDataFromPropertyName(String propertyName);
	
	protected abstract ListenerMetaData[] getMetaDataValues();
	
	protected abstract Class<?> getDefaultListenerClass();

	/**
	 * Create a MethodInvoker from the delegate based on the annotationType.
	 * Ensure that the annotated method has a valid set of parameters.
	 * 
	 * @param metaData
	 * @return a MethodInvoker
	 */
	protected MethodInvoker getMethodInvokerByAnnotation(final ListenerMetaData metaData) {
		final Class<? extends Annotation> annotationType = metaData.getAnnotation();
		MethodInvoker mi = MethodInvokerUtils.getMethodInvokerByAnnotation(annotationType, delegate);
		if (mi != null) {
			ReflectionUtils.doWithMethods(delegate.getClass(), new ReflectionUtils.MethodCallback() {
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
					if (annotation != null) {
						Class<?>[] paramTypes = method.getParameterTypes();
						if (paramTypes.length > 0) {
							Class<?>[] expectedParamTypes = metaData.getParamTypes();

							String errorMsg = "The method [" + method.getName() + "] on target class ["
									+ delegate.getClass().getSimpleName() + "] is incompatable with the signature ["
									+ getParamTypesString(expectedParamTypes) + "] expected for the annotation ["
									+ metaData.getAnnotation().getSimpleName() + "].";

							Assert.isTrue(paramTypes.length == expectedParamTypes.length, errorMsg);
							for (int i = 0; i < paramTypes.length; i++) {
								Assert.isTrue(paramTypes[i].equals(expectedParamTypes[i]), errorMsg);
							}
						}
					}
				}
			});
		}
		return mi;
	}

	protected MethodInvoker getMethodInvokerByName(String methodName, Object candidate, Class<?>... params) {
		if (methodName != null) {
			return MethodInvokerUtils.createMethodInvokerByName(candidate, methodName, false, params);
		}
		else {
			return null;
		}
	}

	public boolean isSingleton() {
		return false;
	}

	public void setDelegate(Object delegate) {
		if (delegate instanceof Advised) {
			try {
				setDelegate(((Advised) delegate).getTargetSource().getTarget());
			} catch (Exception e) {
				throw new IllegalStateException("Cannot generate listener for proxy with no target", e);
			}
		}
		else {
			this.delegate = delegate;
		}
	}

	public void setMetaDataMap(Map<String, String> metaDataMap) {
		this.metaDataMap = metaDataMap;
	}

	/*
	 * Extension of HashSet that ignores nulls, rather than putting them into
	 * the set.
	 */
	protected static class NullIgnoringSet<E> extends HashSet<E> {

		@Override
		public boolean add(E e) {
			if (e == null) {
				return false;
			}
			else {
				return super.add(e);
			}
		};
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "Delegate must not be null");
	}

	/**
	 * Convenience method to check whether the given object is or can be made
	 * into a listener.
	 * 
	 * @param delegate the object to check
	 * @return true if the delegate is an instance of any of the listener
	 *         interface, or contains the marker annotations
	 */
	public static boolean isListener(Object delegate, Class<?> listenerType, ListenerMetaData[] metaDataValues) {
		if (listenerType.isInstance(delegate)) {
			return true;
		}
		if (delegate instanceof Advised) {
			try {
				return isListener(((Advised) delegate).getTargetSource().getTarget(), listenerType, metaDataValues);
			} catch (Exception e) {
				logger.debug("Error obtaining target for Proxy.  Assume not a listener.", e);
				return false;
			}
		}
		for (ListenerMetaData metaData : metaDataValues) {
			if (MethodInvokerUtils.getMethodInvokerByAnnotation(metaData.getAnnotation(), delegate) != null) {
				return true;
			}
		}
		return false;
	}
}
