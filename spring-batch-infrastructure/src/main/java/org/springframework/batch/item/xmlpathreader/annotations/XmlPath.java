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

package org.springframework.batch.item.xmlpathreader.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * XmlPath annotation to mark the connection between xml paths and properties of classes. 
 * <p>
 * With this annotation you can annotate 
 * <ul>
 *  <li>a class</li> 
 *  <li>a static method of a class without parameters</li>
 *  <li>a setMethod of a property</li>
 * </ul>
 * <p>
 * Examples:
 * <ul>
 *  <li>a class <code><br><br>  
 *  {@literal @}XmlPath(path = "child")<br>
 * public class Child {<br><br>
 *               </code> </li> 
 *  <li>a static method of a class without parameters
 *    <code> <br><br>
 * 
 *  {@literal @}XmlPath(path = "/D")<br>
 *	public static TStaticAnnotated namedThomas() {<br>
 *	    TStaticAnnotated o = new TStaticAnnotated();<br>
 *	    o.setVorName("Thomas");<br>
 *	    return o;<br>
 * 	} <br><br>

 *     </code></li>
 *  <li>a setMethod of a property<code><br><br>
 *  {@literal @}XmlPath(path = "name")<br>
 *  public void setName(String name) {<br>
 *      this.name = name;<br>
 *  }<br>
 *  <br></code>
 *  </li>
 * </ul>
 * 
 * <p>
 * The annotation is processed with {@link org.springframework.batch.item.xmlpathreader.core.AnnotationProcessor}
 * 
 * <ul>
 * <li>The annotation of a class is transformed to an object fabric of type {@link org.springframework.batch.item.xmlpathreader.value.ClassValue}</li> 
 * <li>The static methods of a class are transformed to an object fabric of type {@link org.springframework.batch.item.xmlpathreader.value.StaticMethodValue}</li>
 * <li>The annotation of a setter transforms to an implementation of the interface {@link org.springframework.batch.item.xmlpathreader.attribute.Attribute}</li>
 * </ul>
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Repeatable(XmlPaths.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlPath {

	/**
	 * the String representation of the path
	 * 
	 * @return the path as a String
	 */
	String path();
}
