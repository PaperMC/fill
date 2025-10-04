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

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.papermc.fill.exception.AppException;
import io.papermc.fill.exception.BuildNotFoundException;
import io.papermc.fill.exception.DownloadNotFoundException;
import io.papermc.fill.exception.DuplicateBuildException;
import io.papermc.fill.exception.DuplicateFamilyException;
import io.papermc.fill.exception.DuplicateVersionException;
import io.papermc.fill.exception.FamilyInUseException;
import io.papermc.fill.exception.FamilyNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.VersionInUseException;
import io.papermc.fill.exception.VersionNotFoundException;
import org.jspecify.annotations.NullMarked;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import reactor.core.publisher.Mono;

@ControllerAdvice
@NullMarked
public class ExceptionControllerAdvice {
  @GraphQlExceptionHandler({
    BuildNotFoundException.class,
    DownloadNotFoundException.class,
    FamilyNotFoundException.class,
    ProjectNotFoundException.class,
    VersionNotFoundException.class
  })
  public Mono<GraphQLError> onNotFound(final AppException exception, final DataFetchingEnvironment environment) {
    return Mono.just(
      GraphqlErrorBuilder.newError(environment)
        .errorType(ErrorType.NOT_FOUND)
        .message(exception.getMessage())
        .build()
    );
  }

  @GraphQlExceptionHandler({
    DuplicateBuildException.class,
    DuplicateFamilyException.class,
    FamilyInUseException.class,
    DuplicateVersionException.class,
    VersionInUseException.class
  })
  public Mono<GraphQLError> onBadRequest(final AppException exception, final DataFetchingEnvironment environment) {
    return Mono.just(
      GraphqlErrorBuilder.newError(environment)
        .errorType(ErrorType.BAD_REQUEST)
        .message(exception.getMessage())
        .build()
    );
  }
}
