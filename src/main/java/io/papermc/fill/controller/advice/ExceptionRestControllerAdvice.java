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

import io.papermc.fill.api.ApiRoute;
import io.papermc.fill.api.ApiVersion;
import io.papermc.fill.exception.BuildNotFoundException;
import io.papermc.fill.exception.ChecksumMismatchException;
import io.papermc.fill.exception.CommitOrderValidationException;
import io.papermc.fill.exception.DownloadFailedException;
import io.papermc.fill.exception.DownloadNotFoundException;
import io.papermc.fill.exception.DuplicateBuildException;
import io.papermc.fill.exception.DuplicateFamilyException;
import io.papermc.fill.exception.DuplicateVersionException;
import io.papermc.fill.exception.FamilyNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.PublishFailedException;
import io.papermc.fill.exception.SunsetException;
import io.papermc.fill.exception.VersionNotFoundException;
import io.papermc.fill.model.response.ErrorResponse;
import io.papermc.fill.model.response.LegacyErrorResponse;
import io.papermc.fill.util.http.Responses;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

@NullMarked
@RestControllerAdvice
public class ExceptionRestControllerAdvice {
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<?> onNoHandlerFoundException(final NoHandlerFoundException exception) {
    return Responses.notFound(new ErrorResponse("unknown_method", exception.getMessage()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<?> onAuthorizationDeniedException(final AuthorizationDeniedException exception) {
    return Responses.forbidden(new ErrorResponse("unauthorized", exception.getMessage()));
  }

  @ExceptionHandler({
    ChecksumMismatchException.class,
    CommitOrderValidationException.class
  })
  public ResponseEntity<?> on400BadRequest(final Throwable throwable) {
    return Responses.badRequest(new ErrorResponse(
      switch (throwable) {
        case final ChecksumMismatchException _ -> "checksum_mismatch";
        case final CommitOrderValidationException _ -> "commit_order_validation";
        default -> throw new IllegalStateException("Unexpected value: " + throwable);
      },
      throwable.getMessage()
    ));
  }

  @ExceptionHandler({
    BuildNotFoundException.class,
    DownloadNotFoundException.class,
    FamilyNotFoundException.class,
    ProjectNotFoundException.class,
    VersionNotFoundException.class
  })
  public ResponseEntity<?> on404NotFound(final WebRequest request, final Throwable throwable) {
    if (ApiRoute.isApiRoute(request, ApiVersion.V2)) {
      return Responses.notFound(new LegacyErrorResponse(
        switch (throwable) {
          case final BuildNotFoundException _ -> "Build not found.";
          case final DownloadNotFoundException _ -> "Download not found.";
          case final FamilyNotFoundException _ -> "Family not found.";
          case final ProjectNotFoundException _ -> "Project not found.";
          case final VersionNotFoundException _ -> "Version not found.";
          default -> throw new IllegalStateException("Unexpected value: " + throwable);
        }
      ));
    }
    return Responses.notFound(new ErrorResponse(
      switch (throwable) {
        case final BuildNotFoundException _ -> "build_not_found";
        case final DownloadNotFoundException _ -> "download_not_found";
        case final FamilyNotFoundException _ -> "family_not_found";
        case final ProjectNotFoundException _ -> "project_not_found";
        case final VersionNotFoundException _ -> "version_not_found";
        default -> throw new IllegalStateException("Unexpected value: " + throwable);
      },
      throwable.getMessage()
    ));
  }

  @ExceptionHandler
  public ResponseEntity<?> on405MethodNotAllowed(final HttpRequestMethodNotSupportedException exception) {
    return Responses.create(HttpStatus.METHOD_NOT_ALLOWED, new ErrorResponse("method_not_allowed", exception.getMessage()));
  }

  @ExceptionHandler({
    DuplicateBuildException.class,
    DuplicateFamilyException.class,
    DuplicateVersionException.class
  })
  public ResponseEntity<?> on409Conflict(final Throwable throwable) {
    return Responses.conflict(new ErrorResponse(
      switch (throwable) {
        case final DuplicateBuildException _ -> "build_already_exists";
        case final DuplicateFamilyException _ -> "family_already_exists";
        case final DuplicateVersionException _ -> "version_already_exists";
        default -> throw new IllegalStateException("Unexpected value: " + throwable);
      },
      throwable.getMessage()
    ));
  }

  @ExceptionHandler({
    SunsetException.class
  })
  public ResponseEntity<?> on410Gone(final Throwable throwable) {
    return Responses.gone(new ErrorResponse(
      switch (throwable) {
        case final SunsetException _ -> "sunset";
        default -> throw new IllegalStateException("Unexpected value: " + throwable);
      },
      throwable.getMessage()
    ));
  }

  @ExceptionHandler({
    DownloadFailedException.class,
    PublishFailedException.class
  })
  public ResponseEntity<?> on500InternalServerError(final Throwable throwable) {
    return Responses.error(new ErrorResponse(
      switch (throwable) {
        case final DownloadFailedException _ -> "download_failed";
        case final PublishFailedException _ -> "publish_failed";
        default -> throw new IllegalStateException("Unexpected value: " + throwable);
      },
      throwable.getMessage()
    ));
  }
}
