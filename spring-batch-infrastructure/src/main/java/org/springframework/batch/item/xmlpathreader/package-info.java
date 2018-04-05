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

/**
 * Here there a defined the ItemReader.
 * 
 * <p>
 * The reason for this project was the need to read a complicated XML structure into a 
 * set of objects in a ItemReader. I immediately generalized this to the task of 
 * extending the naming attribute of the annotation  
 * <a href="https://docs.oracle.com/javaee/5/api/javax/xml/bind/annotation/XmlElement.html">XmlElement</a> 
 * to a path of XML elements.
 * <p>
 * What do i mean with the word path? For example if you go line for line throw a XXML 
 * document like this: 
 *<pre>
 *{@code
 *  <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.003.03"
 * 	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *	xsi:schemaLocation="urn:iso:std:iso:20022:tech:xsd:pain.001.003.03 pain.001.003.03.xsd">
 * <CstmrCdtTrfInitn>
 *		<GrpHdr>
 *			<MsgId>Message-ID-1</MsgId>
 *			<CreDtTm>2011-11-11T09:30:47.000Z</CreDtTm>
 *			<NbOfTxs>2</NbOfTxs>
 *			<InitgPty>
 *				<Nm>Initiator Name</Nm>
 *			</InitgPty>
 *		</GrpHdr>
 *		<PmtInf>
 *			<PmtInfId>Payment-Information-ID-1</PmtInfId>
 *			<PmtMtd>TRF</PmtMtd>
 *			<BtchBookg>true</BtchBookg>
 *			<NbOfTxs>2</NbOfTxs>
 *			<CtrlSum>1000.00</CtrlSum>
 *			<PmtTpInf>
 *				<SvcLvl>
 *					<Cd>SEPA</Cd>
 *				</SvcLvl>
 *			</PmtTpInf>
 *			<ReqdExctnDt>2016-12-25</ReqdExctnDt>
 *			<Dbtr>
 *				<Nm>Debtor Name</Nm>
 *			</Dbtr>
 *			<DbtrAcct>
 *				<Id>
 *					<IBAN>DE99999999999999999999</IBAN>
 *				</Id>
 * 			</DbtrAcct>
 * }
 * </pre>
 * 
 * At the start is the path like {@literal /}Document. Then at the second element the 
 * path is like {@literal /}Document{@literal /}CstmrCdtTrfInitn, then 
 * {@literal /}Document{@literal /}CstmrCdtTrfInitn{@literal /}GrpHdr
 * and so on.
 * <p>
 * Now the idea is to associate XML elements with actions when an element starts and when 
 * it ends.  The action at the start creates an Object of a class. The action at the end 
 * write an object to the ItemReader. This object is then the next item in the series 
 * of items of the ItemReader. Also there are actions for an attribute of the XML 
 * element or for the text of an element. These actions call setters of the objects.
 * <p>
 * The association of the actions to the elements is with the help of paths. The paths 
 * are defined in the package {@link org.springframework.batch.item.xmlpathreader.path}. 
 * Some paths has an action associated. There are different actions, first create 
 * objects and then set object attributes. For the creation of objects, there are 
 * the classes in the sub package 
 * {@link org.springframework.batch.item.xmlpathreader.value}
 * For the actions that set attributes, there is the package 
 * {@link org.springframework.batch.item.xmlpathreader.attribute}  
 * In these packages there are containers too. These containers associate the path in 
 * the xml to actions. They extend 
 * {@link org.springframework.batch.item.xmlpathreader.path.PathEntryContainer}
 * <p>
 * The different containers for the 
 * {@link org.springframework.batch.item.xmlpathreader.value.Value} and 
 * {@link org.springframework.batch.item.xmlpathreader.attribute.Attribute} actions are 
 * put together in a 
 * {@link org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesBag}.
 * For the creation of the 
 * {@link org.springframework.batch.item.xmlpathreader.value.Value} and 
 * {@link org.springframework.batch.item.xmlpathreader.attribute.Attribute} 
 * this class is extended to 
 * {@link org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesContainer}
 * <p>
 * The {@link org.springframework.batch.item.xmlpathreader.StaxXmlPathReader} uses such a 
 * {@link org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesContainer}. 
 * The Stax-Events are converted to the call of the actions.
 * <p>
 * The definition of the association are made with two different kinds. With an 
 * annotation  
 * {@link org.springframework.batch.item.xmlpathreader.annotations.XmlPath}
 * or the call to an api. The annotation is processed in 
 * {@link org.springframework.batch.item.xmlpathreader.core.AnnotationProcessor}
 * <p>
 * The classes in the following packages are helpers for national language support 
 * {@link org.springframework.batch.item.xmlpathreader.nls}, for specific Attributes
 * {@link org.springframework.batch.item.xmlpathreader.adapters}, for class questions 
 * {@link org.springframework.batch.item.xmlpathreader.utils} and for
 * the definitions of exceptions 
 * {@link org.springframework.batch.item.xmlpathreader.exceptions}.
 * <p>
 * <h2>Using the JAR with annotations</h2>
 * In this part, we use annotations to describe the connections between the objects and 
 * the XML structure.<p>
 * There are one annotation 
 * {@link org.springframework.batch.item.xmlpathreader.annotations.XmlPath}.
 * You can annotate classes, setter methods and static methods, that return a instance 
 * of the class.
 * You can have multiple annotations at each of this positions.
 * <p>
 * For example If you want to read in:
 *<pre>
 *{@code
 * <Document>
 *	<child>
 *		<name>Hans</name>
 *		<child>
 *			<name>Thomas</name>
 *			<child>
 *				<name>Vera</name>
 *			</child>
 *		</child>
 *  </child>
 * </Document>
 * }
 * </pre>
 * and create for each child element a object of the Class Child
 * you can do this with annotations like
 * <code><br><br>
 * {@literal @}XmlPath(path = "child")<br>
 * public class Child {<br>
 *	private String name;<br>
 * <br>
 *	public String getName() {<br>
 *		return name;<br>
 *	}<br>
 * <br>
 *	{@literal @}XmlPath(path = "name")<br>
 *	public void setName(String name) {<br>
 *		this.name = name;<br>
 *	}<br>
 *<br>
 * }<br>
 *<br>
 * </code> 
 * The {@literal @}XmlPath(path = "child") connect the generation of an instance of the 
 * class Child with the tag child of the XML element.<p>
 * The {@literal @}XmlPath(path = "name") connect the call of the Function setName with 
 * the tag name. The parameter for setName is the text of the element name. If the 
 * parameter should be a attribute of the element, use {@literal @}attributname.<p>
 * You can use {@literal @}XmlPath(path = "child/deep") if the elements are nested, like
 *<pre>
 *{@code
 * <Document>
 *	<child>
 *		<deep>
 *			<name>Hans</name>
 *			<city>
 *				<city>
 * }
 * </pre>
 * If you annotate a static method of the class, then the objects for the XML elements 
 * are created with this static method. As an example:
 * <code><br><br>
 * {@literal @}XmlPath(path = "D")<br>
 *	public static TStaticAnnotated namedThomas() {<br>
 *		TStaticAnnotated o = new TStaticAnnotated();<br>
 *		o.setVorName("Thomas");<br>
 *		return o;<br>
 *	}<br>
 * </code>
 * create a object for each D element. If the path has a / as first character, then the
 * path is absolute. /D is  * the document root D element<p> and the path 
 * /Document/BkToCstmrDbtCdtNtfctn/Ntfctn/Ntry is the Ntry element 
 * with has a parent Ntfctn with has a parent BkToCstmrDbtCdtNtfctn with has as a parent 
 * a root Document element.   
 * <p>
 * You describe the connection between the class and the XML structure with the 
 * {@link org.springframework.batch.item.xmlpathreader.annotations.XmlPath} 
 * annotation.<p>
 * You read in the XML with the 
 * {@link org.springframework.batch.item.xmlpathreader.StaxXmlPathReader} like<p>
 * <code><br><br>
 *		StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(Child.class);<br>
 *		staxXmlPathReader.read("child.xml"); // start the reading of the child.xml<br>
 *		Object o = staxXmlPathReader.read(); // read one item from the item reader<br>
 *	}<br>
 * </code>
 * Where Cild.class is annotated with 
 * {@link org.springframework.batch.item.xmlpathreader.annotations.XmlPath}.<p>
 * You can have multiple classes as arguments to the constructor of 
 * {@link org.springframework.batch.item.xmlpathreader.StaxXmlPathReader} like 
 * <code><br><br>
 * StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(LinkChild.class, City.class);<br>
 * <br>
 * </code>
 * <p>
 * <h2>Finer points of the annotation of a setXXX method</h2>
 * You can annotate setXXX methods, if the have the following parameter types. 
 * <ul>
 * <li>String then the parameter is the text value of the XML element or XML attribute.
 * <li>A type to with can converted from a String with unmarshal of an 
 * {@link javax.xml.bind.annotation.adapters.XmlAdapter}.
 * You can add an adapter for a class with the method addAdapter of the class 
 * {@link org.springframework.batch.item.xmlpathreader.adapters.AdapterMap}
 * <li>A type which itself is annotated with a 
 * {@link org.springframework.batch.item.xmlpathreader.annotations.XmlPath} 
 * annotation. In this case the paths of the object that is set, the path of the
 *  attribute and the path to the object 
 * that is the parameter of the setXXX method are concatenated. 
 * </ul>
 * <p>
 * <h2>Using the JAR with 
 * {@link org.springframework.batch.item.xmlpathreader.value.Creator} and 
 * {@link org.springframework.batch.item.xmlpathreader.attribute.Setter}</h2>
 * You can describe the connection between java objects and XML file in a second form.
 * <code><br><br>
 * 		StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();<br>
 *		staxXmlPathReader.addValue("child", Child.class, Child::new);
 * // connect child element with the creation of a Child<br>
 *		staxXmlPathReader.addAttribute("child", "name", Child::setName);
 * // connect child/name element with the Setter setName<br>
 * <br>
 * </code>
 * This two different kinds of connecting java and xml can be mixed.
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
package org.springframework.batch.item.xmlpathreader;

