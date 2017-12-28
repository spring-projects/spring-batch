/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.annotations.XmlPaths;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.utils.ClassUtils;
import org.springframework.util.Assert;

/**
 * Processor for the XmlPath annotations
 * <ul>
 * <li>The annotation of a class is transformed to an object fabric of type
 * {@link org.springframework.batch.item.xmlpathreader.value.ClassValue}</li>
 * <li>The static methods of a class are transformed to an object fabric of type
 * {@link org.springframework.batch.item.xmlpathreader.value.StaticMethodValue}</li>
 * <li>The annotation of a setter transforms to an implementation of the interface
 * {@link org.springframework.batch.item.xmlpathreader.attribute.Attribute}</li>
 * </ul>
 * The rules for the annotation of a setter depends on the type of the parameter of the setter Method. If the type of
 * the parameter is a
 * <ul>
 * <li>String then create a {@link org.springframework.batch.item.xmlpathreader.attribute.MethodAttribute}</li>
 * <li>is a class with a corresponding Adapter in {@link org.springframework.batch.item.xmlpathreader.adapters} then
 * create a {@link org.springframework.batch.item.xmlpathreader.attribute.AttributeWithAdapter}</li>
 * <li>if the class has a {@literal @}XmlPath annotation then create a
 * {@link org.springframework.batch.item.xmlpathreader.attribute.AttributeWithValue}</li>
 * </ul>
 *
 * @author Thomas Nill
 * @since 4.0.1
 * @see XmlPath
 *
 */
public class AnnotationProcessor {

	/**
	 * Constructor
	 */
	public AnnotationProcessor() {
		super();
	}

	/**
	 * Process a array of classes
	 * 
	 * @param container container that will be used and changed
	 * @param clazzes list of classes with {@link XmlPath} annotations
	 */
	public void processClasses(ValuesAndAttributesContainer container, Class<?>... clazzes) {
		Assert.notNull(container, "The Container should not be null");
		Assert.noNullElements(clazzes, "The classes in the array should not be null");

		for (Class<?> clazz : clazzes) {
			processClass(container, clazz);
		}
	}

	/**
	 * Process a single class
	 * 
	 * @param container container that will be used and changed
	 * @param clazz a class with {@link XmlPath} annotations
	 */
	public void processClass(ValuesAndAttributesContainer container, Class<?> clazz) {
		Assert.notNull(container, "The container should not be null");
		Assert.notNull(clazz, "The class should not be null");

		checkClass(clazz);
		processAllXmlPathAnnotations(container, clazz);
	}

	protected void processAllXmlPathAnnotations(ValuesAndAttributesContainer container, Class<?> clazz) {
		for (XmlPath cPath : clazz.getAnnotationsByType(XmlPath.class)) {
			container.addValue(new XmlElementPath(cPath.path()), clazz);
			processNonStaticMethods(container, clazz, cPath);
		}
		processStaticMethods(container, clazz);
	}

	private void processStaticMethods(ValuesAndAttributesContainer container, Class<?> clazz) {
		for (Method m : clazz.getMethods()) {
			processOneStaticMethod(container, clazz, m);
		}
	}

	private void processOneStaticMethod(ValuesAndAttributesContainer container, Class<?> clazz, Method method) {
		if (Modifier.isStatic(method.getModifiers())
				&& (method.isAnnotationPresent(XmlPath.class) || method.isAnnotationPresent(XmlPaths.class))) {
			for (XmlPath mPath : method.getAnnotationsByType(XmlPath.class)) {
				checkStaticMethod(clazz, method);
				container.addValue(new XmlElementPath(mPath.path()), clazz, method.getName());
				processNonStaticMethods(container, clazz, mPath);
			}
		}
	}

	private void checkStaticMethod(Class<?> clazz, Method method) {
		if (method.getParameterCount() != 0) {
			Messages.throwIllegalArgumentException(
					"AnnotationProcessor.NO_PARAMETER", method.getDeclaringClass().getName(), method.getName()); //$NON-NLS-1$
		}
		if (!ClassUtils.isThisClassOrASuperClass(method.getReturnType(), clazz)) {
			Messages.throwIllegalArgumentException(
					"AnnotationProcessor.WRONG_RETURN_TYPE", method.getDeclaringClass().getName(), method.getName(), clazz.getCanonicalName()); //$NON-NLS-1$

		}

	}

	private void processNonStaticMethods(ValuesAndAttributesContainer container, Class<?> clazz, XmlPath cPath) {
		for (Method m : clazz.getMethods()) {
			processOneNonStaticMethod(container, cPath, m);
		}
	}

	private void processOneNonStaticMethod(ValuesAndAttributesContainer container, XmlPath cPath, Method method) {
		if (!Modifier.isStatic(method.getModifiers())
				&& (method.isAnnotationPresent(XmlPath.class) || method.isAnnotationPresent(XmlPaths.class))) {
			checkMethod(method);
			processAllXmlPathAnnotations(container, cPath, method);
		}
	}

	protected void processAllXmlPathAnnotations(ValuesAndAttributesContainer container, XmlPath cPath, Method method) {
		for (XmlPath mPath : method.getAnnotationsByType(XmlPath.class)) {
			String path = mPath.path();
			String methodName = method.getName().substring(3);
			if (path.charAt(0) == '/') {
				container.addAttribute(new XmlElementPath(cPath.path()), new XmlElementPath(path), methodName);
			}
			else {
				container.addRelativAttribute(new XmlElementPath(cPath.path()), new XmlElementPath(path), methodName);
			}
		}
	}

	private void checkClass(Class<?> clazz) {

		// if the class have static methods witch are annotated it is allowed
		// too.
		for (Method method : clazz.getMethods()) {
			if (Modifier.isStatic(method.getModifiers())
					&& (method.isAnnotationPresent(XmlPath.class) || method.isAnnotationPresent(XmlPaths.class))) {
				return;
			}
		}

		if (!(clazz.isAnnotationPresent(XmlPath.class) || clazz.isAnnotationPresent(XmlPaths.class))) {
			Messages.throwIllegalArgumentException("AnnotationProcessor.WITH_ANNOTATION", clazz.getName()); //$NON-NLS-1$
		}
	}

	private void checkMethod(Method method) {

		if (method.getParameterCount() != 1) {
			Messages.throwIllegalArgumentException(
					"AnnotationProcessor.WRONG_PARAMETER_COUNT", method.getDeclaringClass().getName(), method.getName()); //$NON-NLS-1$
		}
		if (!method.getName().startsWith("set")) {
			Messages.throwIllegalArgumentException(
					"AnnotationProcessor.WRONG_FUNCTION_NAME", method.getDeclaringClass().getName(), method.getName()); //$NON-NLS-1$
		}
	}

}
