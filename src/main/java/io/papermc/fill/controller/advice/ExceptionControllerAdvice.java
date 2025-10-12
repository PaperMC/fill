/*
 * Copyright 2024 PaperMC
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
package io.papermc.fill.controller.advice;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.papermc.fill.exception.AppException;
import org.jspecify.annotations.NullMarked;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import reactor.core.publisher.Mono;

@ControllerAdvice
@NullMarked
public class ExceptionControllerAdvice {
  @GraphQlExceptionHandler(AppException.class)
  public Mono<GraphQLError> on(final AppException exception, final DataFetchingEnvironment environment) {
    ErrorClassification classification = exception.getGraphErrorClassification();
    if (classification == null) {
      classification = ErrorType.BAD_REQUEST;
    }
    return Mono.just(
      GraphqlErrorBuilder.newError(environment)
        .errorType(classification)
        .message(exception.getMessage())
        .build()
    );
  }
}
