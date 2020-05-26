/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr.configuration.support;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * <p>This is a copy of AutowiredAnnotationBeanPostProcessor with modifications allow a subclass to
 * do additional checks on other field annotations before processing injection annotations.</p>
 *
 * <p>This class is considered a quick work around and needs to be refactored / removed.</p>
 *
 * <p>The in addition to making this class package private, the following methods were modified to be protected:</p>
 * <ul>
 * <li>findAutowiringMetadata(Class&lt;?&gt; clazz)</li>
 * <li>buildAutowiringMetadata(Class&lt;?&gt; clazz)</li>
 * <li>findAutowiredAnnotation(AccessibleObject ao)</li>
 * </ul>
 */
class SpringAutowiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

    protected final Log logger = LogFactory.getLog(getClass());

    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes =
            new LinkedHashSet<>();

    private String requiredParameterName = "required";

    private boolean requiredParameterValue = true;

    private int order = Ordered.LOWEST_PRECEDENCE - 2;

    private ConfigurableListableBeanFactory beanFactory;

    private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache =
            new ConcurrentHashMap<>(64);

    private final Map<Class<?>, InjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<>(64);


    /**
     * Create a new AutowiredAnnotationBeanPostProcessor
     * for Spring's standard {@link org.springframework.beans.factory.annotation.Autowired} annotation.
     * <p>Also supports JSR-330's {@link javax.inject.Inject} annotation, if available.
     */
    @SuppressWarnings("unchecked")
    public SpringAutowiredAnnotationBeanPostProcessor() {
        this.autowiredAnnotationTypes.add(Autowired.class);
        this.autowiredAnnotationTypes.add(Value.class);
        ClassLoader cl = SpringAutowiredAnnotationBeanPostProcessor.class.getClassLoader();
        try {
            this.autowiredAnnotationTypes.add((Class<? extends Annotation>) cl.loadClass("javax.inject.Inject"));
            logger.info("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
        }
        catch (ClassNotFoundException ex) {
            // JSR-330 API not available - simply skip.
        }
    }


    /**
     * Set the 'autowired' annotation type, to be used on constructors, fields,
     * setter methods and arbitrary config methods.
     * <p>The default autowired annotation type is the Spring-provided
     * {@link Autowired} annotation, as well as {@link Value}.
     * <p>This setter property exists so that developers can provide their own
     * (non-Spring-specific) annotation type to indicate that a member is
     * supposed to be autowired.
     *
     * @param autowiredAnnotationType type to be used by constructors, fields and methods.
     */
    public void setAutowiredAnnotationType(Class<? extends Annotation> autowiredAnnotationType) {
        Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' must not be null");
        this.autowiredAnnotationTypes.clear();
        this.autowiredAnnotationTypes.add(autowiredAnnotationType);
    }

    /**
     * Set the 'autowired' annotation types, to be used on constructors, fields,
     * setter methods and arbitrary config methods.
     * <p>The default autowired annotation type is the Spring-provided
     * {@link Autowired} annotation, as well as {@link Value}.
     * <p>This setter property exists so that developers can provide their own
     * (non-Spring-specific) annotation types to indicate that a member is
     * supposed to be autowired.

     * @param autowiredAnnotationTypes set of types to be used by constructors, fields and methods.
     */
    public void setAutowiredAnnotationTypes(Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
        Assert.notEmpty(autowiredAnnotationTypes, "'autowiredAnnotationTypes' must not be empty");
        this.autowiredAnnotationTypes.clear();
        this.autowiredAnnotationTypes.addAll(autowiredAnnotationTypes);
    }

    /**
     * Set the name of a parameter of the annotation that specifies
     * whether it is required.
     *
     * @param requiredParameterName the name of the parameter.
     *
     * @see #setRequiredParameterValue(boolean)
     */
    public void setRequiredParameterName(String requiredParameterName) {
        this.requiredParameterName = requiredParameterName;
    }

    /**
     * Set the boolean value that marks a dependency as required
     * <p>For example if using 'required=true' (the default),
     * this value should be <code>true</code>; but if using
     * 'optional=false', this value should be <code>false</code>.
     *
     * @param requiredParameterValue true if dependency is required.
     *
     * @see #setRequiredParameterName(String)
     */
    public void setRequiredParameterValue(boolean requiredParameterValue) {
        this.requiredParameterValue = requiredParameterValue;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
	public int getOrder() {
        return this.order;
    }

    @Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory");
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }


    @Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType != null) {
            InjectionMetadata metadata = findAutowiringMetadata(beanType);
            metadata.checkConfigMembers(beanDefinition);
        }
    }

    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
        // Quick check on the concurrent map first, with minimal locking.
        Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
        if (candidateConstructors == null) {
            synchronized (this.candidateConstructorsCache) {
                candidateConstructors = this.candidateConstructorsCache.get(beanClass);
                if (candidateConstructors == null) {
                    Constructor<?>[] rawCandidates = beanClass.getDeclaredConstructors();
                    List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
                    Constructor<?> requiredConstructor = null;
                    Constructor<?> defaultConstructor = null;
                    for (Constructor<?> candidate : rawCandidates) {
                        Annotation annotation = findAutowiredAnnotation(candidate);
                        if (annotation != null) {
                            if (requiredConstructor != null) {
                                throw new BeanCreationException("Invalid autowire-marked constructor: " + candidate +
                                        ". Found another constructor with 'required' Autowired annotation: " +
                                        requiredConstructor);
                            }
                            if (candidate.getParameterTypes().length == 0) {
                                throw new IllegalStateException(
                                        "Autowired annotation requires at least one argument: " + candidate);
                            }
                            boolean required = determineRequiredStatus(annotation);
                            if (required) {
                                if (!candidates.isEmpty()) {
                                    throw new BeanCreationException(
                                            "Invalid autowire-marked constructors: " + candidates +
                                                    ". Found another constructor with 'required' Autowired annotation: " +
                                                    requiredConstructor);
                                }
                                requiredConstructor = candidate;
                            }
                            candidates.add(candidate);
                        }
                        else if (candidate.getParameterTypes().length == 0) {
                            defaultConstructor = candidate;
                        }
                    }
                    if (!candidates.isEmpty()) {
                        // Add default constructor to list of optional constructors, as fallback.
                        if (requiredConstructor == null && defaultConstructor != null) {
                            candidates.add(defaultConstructor);
                        }
                        candidateConstructors = candidates.toArray(new Constructor<?>[candidates.size()]);
                    }
                    else {
                        candidateConstructors = new Constructor<?>[0];
                    }
                    this.candidateConstructorsCache.put(beanClass, candidateConstructors);
                }
            }
        }
        return (candidateConstructors.length > 0 ? candidateConstructors : null);
    }

    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

        InjectionMetadata metadata = findAutowiringMetadata(bean.getClass());
        try {
            metadata.inject(bean, beanName, pvs);
        }
        catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
        }
        return pvs;
    }

    /**
     * 'Native' processing method for direct calls with an arbitrary target instance,
     * resolving all of its fields and methods which are annotated with <code>@Autowired</code>.
     * @param bean the target instance to process
     * @throws BeansException if autowiring failed
     */
    public void processInjection(Object bean) throws BeansException {
        Class<?> clazz = bean.getClass();
        InjectionMetadata metadata = findAutowiringMetadata(clazz);
        try {
            metadata.inject(bean, null, null);
        }
        catch (Throwable ex) {
            throw new BeanCreationException("Injection of autowired dependencies failed for class [" + clazz + "]", ex);
        }
    }


    protected InjectionMetadata findAutowiringMetadata(Class<?> clazz) {
        // Quick check on the concurrent map first, with minimal locking.
        InjectionMetadata metadata = this.injectionMetadataCache.get(clazz);
        if (metadata == null) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(clazz);
                if (metadata == null) {
                    metadata = buildAutowiringMetadata(clazz);
                    this.injectionMetadataCache.put(clazz, metadata);
                }
            }
        }
        return metadata;
    }

    protected InjectionMetadata buildAutowiringMetadata(Class<?> clazz) {
        LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<>();
        Class<?> targetClass = clazz;

        do {
            LinkedList<InjectionMetadata.InjectedElement> currElements = new LinkedList<>();
            for (Field field : targetClass.getDeclaredFields()) {
                Annotation annotation = findAutowiredAnnotation(field);
                if (annotation != null) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Autowired annotation is not supported on static fields: " + field);
                        }
                        continue;
                    }
                    boolean required = determineRequiredStatus(annotation);
                    currElements.add(new AutowiredFieldElement(field, required));
                }
            }
            for (Method method : targetClass.getDeclaredMethods()) {
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
                Annotation annotation = BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod) ?
                        findAutowiredAnnotation(bridgedMethod) : findAutowiredAnnotation(method);
                if (annotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Autowired annotation is not supported on static methods: " + method);
                        }
                        continue;
                    }
                    if (method.getParameterTypes().length == 0) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Autowired annotation should be used on methods with actual parameters: " + method);
                        }
                    }
                    boolean required = determineRequiredStatus(annotation);
                    PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
                    currElements.add(new AutowiredMethodElement(method, required, pd));
                }
            }
            elements.addAll(0, currElements);
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);

        return new InjectionMetadata(clazz, elements);
    }

    protected Annotation findAutowiredAnnotation(AccessibleObject ao) {
        for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
            Annotation annotation = AnnotationUtils.getAnnotation(ao, type);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Obtain all beans of the given type as autowire candidates.
     *
     * @param type the type of the bean.
     * @param <T> the type of the bean.
     * @return the target beans, or an empty Collection if no bean of this type is found
     *
     * @throws BeansException if bean retrieval failed
     */
    protected <T> Map<String, T> findAutowireCandidates(Class<T> type) throws BeansException {
        if (this.beanFactory == null) {
            throw new IllegalStateException("No BeanFactory configured - " +
                    "override the getBeanOfType method or specify the 'beanFactory' property");
        }
        return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type);
    }

    /**
     * Determine if the annotated field or method requires its dependency.
     * <p>A 'required' dependency means that autowiring should fail when no beans
     * are found. Otherwise, the autowiring process will simply bypass the field
     * or method when no beans are found.
     * @param annotation the Autowired annotation
     * @return whether the annotation indicates that a dependency is required
     */
    protected boolean determineRequiredStatus(Annotation annotation) {
        try {
            Method method = ReflectionUtils.findMethod(annotation.annotationType(), this.requiredParameterName);
            if (method == null) {
                // annotations like @Inject and @Value don't have a method (attribute) named "required"
                // -> default to required status
                return true;
            }
            return (this.requiredParameterValue == (Boolean) ReflectionUtils.invokeMethod(method, annotation));
        }
        catch (Exception ex) {
            // an exception was thrown during reflective invocation of the required attribute
            // -> default to required status
            return true;
        }
    }

    /**
     * Register the specified bean as dependent on the autowired beans.
     */
    private void registerDependentBeans(String beanName, Set<String> autowiredBeanNames) {
        if (beanName != null) {
            for (String autowiredBeanName : autowiredBeanNames) {
                if (this.beanFactory.containsBean(autowiredBeanName)) {
                    this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Autowiring by type from bean name '" + beanName +
                            "' to bean named '" + autowiredBeanName + "'");
                }
            }
        }
    }

    /**
     * Resolve the specified cached method argument or field value.
     */
    private Object resolvedCachedArgument(String beanName, Object cachedArgument) {
        if (cachedArgument instanceof DependencyDescriptor) {
            DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
            TypeConverter typeConverter = this.beanFactory.getTypeConverter();
            return this.beanFactory.resolveDependency(descriptor, beanName, null, typeConverter);
        }
        else if (cachedArgument instanceof RuntimeBeanReference) {
            return this.beanFactory.getBean(((RuntimeBeanReference) cachedArgument).getBeanName());
        }
        else {
            return cachedArgument;
        }
    }


    /**
     * Class representing injection information about an annotated field.
     */
    private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

        private final boolean required;

        private volatile boolean cached = false;

        private volatile Object cachedFieldValue;

        public AutowiredFieldElement(Field field, boolean required) {
            super(field, null);
            this.required = required;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            Field field = (Field) this.member;
            try {
                Object value;
                if (this.cached) {
                    value = resolvedCachedArgument(beanName, this.cachedFieldValue);
                }
                else {
                    DependencyDescriptor descriptor = new DependencyDescriptor(field, this.required);
                    Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
                    TypeConverter typeConverter = beanFactory.getTypeConverter();
                    value = beanFactory.resolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
                    synchronized (this) {
                        if (!this.cached) {
                            if (value != null || this.required) {
                                this.cachedFieldValue = descriptor;
                                registerDependentBeans(beanName, autowiredBeanNames);
                                if (autowiredBeanNames.size() == 1) {
                                    String autowiredBeanName = autowiredBeanNames.iterator().next();
                                    if (beanFactory.containsBean(autowiredBeanName)) {
                                        if (beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                                            this.cachedFieldValue = new RuntimeBeanReference(autowiredBeanName);
                                        }
                                    }
                                }
                            }
                            else {
                                this.cachedFieldValue = null;
                            }
                            this.cached = true;
                        }
                    }
                }
                if (value != null) {
                    ReflectionUtils.makeAccessible(field);
                    field.set(bean, value);
                }
            }
            catch (Throwable ex) {
                throw new BeanCreationException("Could not autowire field: " + field, ex);
            }
        }
    }


    /**
     * Class representing injection information about an annotated method.
     */
    private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

        private final boolean required;

        private volatile boolean cached = false;

        private volatile Object[] cachedMethodArguments;

        public AutowiredMethodElement(Method method, boolean required, PropertyDescriptor pd) {
            super(method, pd);
            this.required = required;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            if (checkPropertySkipping(pvs)) {
                return;
            }
            Method method = (Method) this.member;
            try {
                Object[] arguments;
                if (this.cached) {
                    // Shortcut for avoiding synchronization...
                    arguments = resolveCachedArguments(beanName);
                }
                else {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    arguments = new Object[paramTypes.length];
                    DependencyDescriptor[] descriptors = new DependencyDescriptor[paramTypes.length];
                    Set<String> autowiredBeanNames = new LinkedHashSet<>(paramTypes.length);
                    TypeConverter typeConverter = beanFactory.getTypeConverter();
                    for (int i = 0; i < arguments.length; i++) {
                        MethodParameter methodParam = new MethodParameter(method, i);
                        GenericTypeResolver.resolveParameterType(methodParam, bean.getClass());
                        descriptors[i] = new DependencyDescriptor(methodParam, this.required);
                        arguments[i] = beanFactory.resolveDependency(
                                descriptors[i], beanName, autowiredBeanNames, typeConverter);
                        if (arguments[i] == null && !this.required) {
                            arguments = null;
                            break;
                        }
                    }
                    synchronized (this) {
                        if (!this.cached) {
                            if (arguments != null) {
                                this.cachedMethodArguments = new Object[arguments.length];
                                for (int i = 0; i < arguments.length; i++) {
                                    this.cachedMethodArguments[i] = descriptors[i];
                                }
                                registerDependentBeans(beanName, autowiredBeanNames);
                                if (autowiredBeanNames.size() == paramTypes.length) {
                                    Iterator<String> it = autowiredBeanNames.iterator();
                                    for (int i = 0; i < paramTypes.length; i++) {
                                        String autowiredBeanName = it.next();
                                        if (beanFactory.containsBean(autowiredBeanName)) {
                                            if (beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
                                                this.cachedMethodArguments[i] = new RuntimeBeanReference(autowiredBeanName);
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                this.cachedMethodArguments = null;
                            }
                            this.cached = true;
                        }
                    }
                }
                if (arguments != null) {
                    ReflectionUtils.makeAccessible(method);
                    method.invoke(bean, arguments);
                }
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
            catch (Throwable ex) {
                throw new BeanCreationException("Could not autowire method: " + method, ex);
            }
        }

        private Object[] resolveCachedArguments(String beanName) {
            if (this.cachedMethodArguments == null) {
                return null;
            }
            Object[] arguments = new Object[this.cachedMethodArguments.length];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = resolvedCachedArgument(beanName, this.cachedMethodArguments[i]);
            }
            return arguments;
        }
    }
}
