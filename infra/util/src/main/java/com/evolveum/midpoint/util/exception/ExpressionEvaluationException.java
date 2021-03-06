/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.util.exception;

/**
 * Error during evaluation of expression. The expressions are defined by system administrator.
 * 
 * @author Radovan Semancik
 *
 */
public class ExpressionEvaluationException extends CommonException {
	private static final long serialVersionUID = 5615419722362251191L;

	public ExpressionEvaluationException() {
	}

	public ExpressionEvaluationException(String message) {
		super(message);
	}

	public ExpressionEvaluationException(Throwable cause) {
		super(cause);
	}

	public ExpressionEvaluationException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getOperationResultMessage() {
		return "Expression error";
	}

}
