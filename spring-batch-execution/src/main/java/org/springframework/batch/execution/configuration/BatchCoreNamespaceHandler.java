package org.springframework.batch.execution.configuration;

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * <code>NamespaceHandler</code> for the <code>batch</code> namespace.
 * 
 * <p>Provides a {@link BeanDefinitionParser} for the <code>&lt;batch:job&gt;</code> tag. A <code>job</code>
 * tag can include nested <code>chunked-step</code> and <code>tasklet-step</code> tags.
 * 
 * @author Ben Hale
 */
public class BatchCoreNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * Register the {@link BeanDefinitionParser BeanDefinitionParser} for the
	 * '<code>job</code>', tag.
	 */
	public void init() {
		registerBeanDefinitionParser("job", new JobBeanDefinitionParser());
	}

}
