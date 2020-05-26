/*
 * Copyright 2013 the original author or authors.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Support class for parsing JSR-352 expressions. The JSR-352 expression syntax, for
 * example conditional/elvis statements need to be transformed a bit to be valid SPeL expressions.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class JsrExpressionParser {
	private static final String QUOTE = "'";
	private static final String NULL = "null";
	private static final String ELVIS_RHS = ":";
	private static final String ELVIS_LHS = "\\?";
	private static final String ELVIS_OPERATOR = "?:";
	private static final String EXPRESSION_SUFFIX = "}";
	private static final String EXPRESSION_PREFIX = "#{";
	private static final String DEFAULT_VALUE_SEPARATOR = ";";
	private static final Pattern CONDITIONAL_EXPRESSION = Pattern.compile("(((\\bnull\\b)|(#\\{\\w))[^;]+)");

	private BeanExpressionContext beanExpressionContext;
	private BeanExpressionResolver beanExpressionResolver;

	/**
	 * <p>
	 * Creates a new instance of this expression parser without and expression resolver. Creating
	 * an instance via this constructor will still parse expressions but no resolution of operators
	 * will occur as its expected the caller will.
	 * </p>
	 */
	public JsrExpressionParser() { }

	/**
	 * <p>
	 * Creates a new instances of this expression parser with the provided expression resolver and context to evaluate
	 * against.
	 * </p>
	 *
	 * @param beanExpressionResolver the expression resolver to use when resolving expressions
	 * @param beanExpressionContext the expression context to resolve expressions against
	 */
	public JsrExpressionParser(BeanExpressionResolver beanExpressionResolver, BeanExpressionContext beanExpressionContext) {
		this.beanExpressionContext = beanExpressionContext;
		this.beanExpressionResolver = beanExpressionResolver;
	}

	/**
	 * <p>
	 * Parses the provided expression, applying any transformations needed to evaluate as a SPeL expression.
	 * </p>
	 *
	 * @param expression the expression to parse and transform
	 * @return a JSR-352 transformed expression that can be evaluated by a SPeL parser
	 */
	public String parseExpression(String expression) {
		String expressionToParse = expression;

		if (StringUtils.countOccurrencesOf(expressionToParse, ELVIS_OPERATOR) > 0) {
			expressionToParse = parseConditionalExpressions(expressionToParse);
		}

		return evaluateExpression(expressionToParse);
	}

	private String parseConditionalExpressions(String expression) {
		String expressionToParse = expression;

		Matcher conditionalExpressionMatcher = CONDITIONAL_EXPRESSION.matcher(expressionToParse);

		while (conditionalExpressionMatcher.find()) {
			String conditionalExpression = conditionalExpressionMatcher.group(1);

			String value = conditionalExpression.split(ELVIS_LHS)[0];
			String defaultValue = conditionalExpression.split(ELVIS_RHS)[1];

			StringBuilder parsedExpression = new StringBuilder();

			if(beanExpressionResolver != null) {
						parsedExpression.append(EXPRESSION_PREFIX)
						.append(evaluateExpression(value))
						.append(ELVIS_OPERATOR)
						.append(QUOTE)
						.append(evaluateExpression(defaultValue))
						.append(QUOTE)
						.append(EXPRESSION_SUFFIX);
			} else {
				if(NULL.equals(value)) {
					parsedExpression.append(defaultValue);
				} else {
					parsedExpression.append(value);
				}
			}

			expressionToParse = expressionToParse.replace(conditionalExpression, parsedExpression);
		}

		return expressionToParse.replace(DEFAULT_VALUE_SEPARATOR, "");
	}

	private String evaluateExpression(String expression) {
		if(beanExpressionResolver != null) {
			return (String) beanExpressionResolver.evaluate(expression, beanExpressionContext);
		}

		return expression;
	}
}
