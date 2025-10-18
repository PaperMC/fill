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
package io.papermc.fill.controller;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/graphql")
@NullMarked
public class GraphQLController {
  private final GraphQL graphQL;

  public GraphQLController(final GraphQL graphQL) {
    this.graphQL = graphQL;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> graphql(@RequestBody final GraphQLRequest request) {
    final ExecutionInput executionInput = ExecutionInput.newExecutionInput()
      .query(request.query())
      .operationName(request.operationName())
      .variables(request.variables() != null ? request.variables() : Map.of())
      .build();

    final ExecutionResult executionResult = this.graphQL.execute(executionInput);
    return executionResult.toSpecification();
  }

  public record GraphQLRequest(
    String query,
    @Nullable String operationName,
    @Nullable Map<String, Object> variables
  ) {}
}

