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

import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.VersionNotFoundException;
import io.papermc.fill.model.request.v3.AllocateRequest;
import io.papermc.fill.model.response.v3.AllocateBuildNumberResponse;
import io.papermc.fill.service.PublishingService;
import io.papermc.fill.util.http.Responses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.OptionalInt;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@NullMarked
@RestController
public class Publish3Controller {
  private final ProjectRepository projects;
  private final VersionRepository versions;
  private final PublishingService publishing;

  @Autowired
  public Publish3Controller(
    final ProjectRepository projects,
    final VersionRepository versions,
    final PublishingService publishing
  ) {
    this.projects = projects;
    this.versions = versions;
    this.publishing = publishing;
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @PostMapping("/v3/projects/{project}/versions/{version}/publishing")
  public ResponseEntity<?> allocateBuild(
    @Parameter(description = "The key of the project")
    @PathVariable("project")
    final String projectKey,
    @Parameter(description = "The key of the version")
    @PathVariable("version")
    final String versionKey,
    @RequestBody
    final AllocateRequest request
  ) {
    final ProjectEntity project = this.projects.findByKey(projectKey).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, versionKey).orElseThrow(VersionNotFoundException::new);
    final int number = this.publishing.allocateBuildNumber(request.session(), version, OptionalInt.empty());
    final AllocateBuildNumberResponse response = new AllocateBuildNumberResponse(number);
    return Responses.ok(response, CacheControl.noStore());
  }
}
