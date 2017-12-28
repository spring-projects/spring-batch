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
 * The reason for this project was the need to read a complicated XML structure into a set of objects in a ItemReader.
 * I immediately generalized this to the task of extending the naming attribute of the annotation 
 * <a href="https://docs.oracle.com/javaee/5/api/javax/xml/bind/annotation/XmlElement.html">XmlElement</a> to a path of XML elements.
 * <p>
 * What do i mean with the word path? For example if you go line for line throw a XXML document like this: 
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
 * At the start is the path like {@literal /}Document. Then at the second element the path is like {@literal /}Document{@literal /}CstmrCdtTrfInitn, then {@literal /}Document{@literal /}CstmrCdtTrfInitn{@literal /}GrpHdr
 * and so on.
 * <p>
 * Now the idea is to associate elements with actions when an element starts and when it ends.
 * The action at the start creates an Object of a class. The action at the end write an object to the ItemReader.
 * This object is then the next item in the series of items of the ItemReader.
 * Also there are actions for an attribute of the XML element or for the text of an element. These actions call setters of the objects.
 * <p>
 * The association of the actions to the elements is with the help of paths. The paths are defined in the package {@link org.springframework.batch.item.xmlpathreader.path}. 
 * Some paths has an action associated. There are different actions, first create objects and then set object attributes.
 * For the creation of objects, there are the classes in the sub package {@link org.springframework.batch.item.xmlpathreader.value}
 * For the actions that set attributes, there is the package {@link org.springframework.batch.item.xmlpathreader.attribute}  
 * In these packages there are containers to. These containers associate the path in the xml to actions.
 * <p>
 * The different containers for the {@link org.springframework.batch.item.xmlpathreader.value.Value} and {@link org.springframework.batch.item.xmlpathreader.attribute.Attribute} actions are put together in a {@link org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesBag}.
 * For the creation of the {@link org.springframework.batch.item.xmlpathreader.value.Value} and {@link org.springframework.batch.item.xmlpathreader.attribute.Attribute} this class is extended to {@link org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesContainer}
 * <p>
 * The {@link org.springframework.batch.item.xmlpathreader.StaxXmlPathReader} uses such a {@link org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesContainer}. The Stax-Events are converted to the call of the actions.
 * <p>
 * The definition of the association are made with two different kinds. With an annotation {@link org.springframework.batch.item.xmlpathreader.annotations.XmlPath}
 * or the call to an api. The annotation is processed in {@link org.springframework.batch.item.xmlpathreader.core.AnnotationProcessor}
 * <p>
 * The classes in the following packages are helpers for national language support {@link org.springframework.batch.item.xmlpathreader.nls}, for specific Attributes
 * {@link org.springframework.batch.item.xmlpathreader.adapters}, for class questions {@link org.springframework.batch.item.xmlpathreader.utils} and for
 * the definitions of exceptions {@link org.springframework.batch.item.xmlpathreader.exceptions}.
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
package org.springframework.batch.item.xmlpathreader;