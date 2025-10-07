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

import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.FamilyEntity;
import io.papermc.fill.database.FamilyRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.BuildNotFoundException;
import io.papermc.fill.exception.DuplicateFamilyException;
import io.papermc.fill.exception.DuplicateVersionException;
import io.papermc.fill.exception.FamilyInUseException;
import io.papermc.fill.exception.FamilyNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.VersionInUseException;
import io.papermc.fill.exception.VersionNotFoundException;
import io.papermc.fill.graphql.input.CreateFamilyInput;
import io.papermc.fill.graphql.input.CreateVersionInput;
import io.papermc.fill.graphql.input.DeleteFamilyInput;
import io.papermc.fill.graphql.input.DeleteVersionInput;
import io.papermc.fill.graphql.input.PromoteBuildInput;
import io.papermc.fill.graphql.input.UpdateFamilyInput;
import io.papermc.fill.graphql.input.UpdateVersionInput;
import io.papermc.fill.graphql.payload.CreateFamilyPayload;
import io.papermc.fill.graphql.payload.CreateVersionPayload;
import io.papermc.fill.graphql.payload.DeleteFamilyPayload;
import io.papermc.fill.graphql.payload.DeleteVersionPayload;
import io.papermc.fill.graphql.payload.PromoteBuildPayload;
import io.papermc.fill.graphql.payload.UpdateFamilyPayload;
import io.papermc.fill.graphql.payload.UpdateVersionPayload;
import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.Java;
import io.papermc.fill.model.Support;
import io.papermc.fill.model.SupportStatus;
import java.time.Instant;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@NullMarked
public class GraphMutationController {
  private final ProjectRepository projects;
  private final FamilyRepository families;
  private final VersionRepository versions;
  private final BuildRepository builds;

  @Autowired
  public GraphMutationController(
    final ProjectRepository projects,
    final FamilyRepository families,
    final VersionRepository versions,
    final BuildRepository builds
  ) {
    this.projects = projects;
    this.families = families;
    this.versions = versions;
    this.builds = builds;
  }

  @MutationMapping("createFamily")
  @PreAuthorize("hasRole('API_MANAGE')")
  public CreateFamilyPayload createFamily(
    @Argument
    final CreateFamilyInput input
  ) {
    final ProjectEntity project = this.projects.findByName(input.project()).orElseThrow(ProjectNotFoundException::new);
    if (this.families.findByProjectAndName(project, input.id()).isPresent()) {
      throw new DuplicateFamilyException();
    }
    final FamilyEntity entity = this.families.save(FamilyEntity.create(
      new ObjectId(),
      Instant.now(),
      project,
      input.id(),
      input.java()
    ));
    return new CreateFamilyPayload(entity);
  }

  @MutationMapping("updateFamily")
  @PreAuthorize("hasRole('API_MANAGE')")
  public UpdateFamilyPayload updateFamily(
    @Argument
    final UpdateFamilyInput input
  ) {
    final ProjectEntity project = this.projects.findByName(input.project()).orElseThrow(ProjectNotFoundException::new);
    FamilyEntity family = this.families.findByProjectAndName(project, input.id()).orElseThrow(FamilyNotFoundException::new);
    final Java java = input.java();
    if (java != null) {
      family.setJava(java);
    }
    family = this.families.save(family);
    return new UpdateFamilyPayload(family);
  }

  @MutationMapping("deleteFamily")
  @PreAuthorize("hasRole('API_MANAGE')")
  public DeleteFamilyPayload deleteFamily(
    @Argument
    final DeleteFamilyInput input
  ) {
    final ProjectEntity project = this.projects.findByName(input.project()).orElseThrow(ProjectNotFoundException::new);
    final FamilyEntity family = this.families.findByProjectAndName(project, input.id()).orElseThrow(FamilyNotFoundException::new);
    if (this.versions.findAllByFamily(family).findAny().isPresent()) {
      throw new FamilyInUseException("Cannot delete this family because one or more versions are still associated with it.");
    }
    return new DeleteFamilyPayload(true);
  }

  @MutationMapping("createVersion")
  @PreAuthorize("hasRole('API_MANAGE')")
  public CreateVersionPayload createVersion(
    @Argument
    final CreateVersionInput input
  ) {
    final ProjectEntity project = this.projects.findByName(input.project()).orElseThrow(ProjectNotFoundException::new);
    final FamilyEntity family = this.families.findByProjectAndName(project, input.family()).orElseThrow(FamilyNotFoundException::new);
    if (this.versions.findByProjectAndName(project, input.id()).isPresent()) {
      throw new DuplicateVersionException();
    }
    final VersionEntity entity = this.versions.save(VersionEntity.create(
      new ObjectId(),
      Instant.now(),
      project,
      family,
      input.id(),
      null,
      new Support(SupportStatus.SUPPORTED, null),
      input.java()
    ));
    return new CreateVersionPayload(entity);
  }

  @MutationMapping("updateVersion")
  @PreAuthorize("hasRole('API_MANAGE')")
  public UpdateVersionPayload updateVersion(
    @Argument
    final UpdateVersionInput input
  ) {
    final ProjectEntity project = this.projects.findByName(input.project()).orElseThrow(ProjectNotFoundException::new);
    VersionEntity version = this.versions.findByProjectAndName(project, input.id()).orElseThrow(VersionNotFoundException::new);
    final Support newSupport = input.support();
    if (newSupport != null) {
      version.setSupport(newSupport);
    }
    version.setJava(input.java());
    version = this.versions.save(version);
    return new UpdateVersionPayload(version);
  }

  @MutationMapping("deleteVersion")
  @PreAuthorize("hasRole('API_MANAGE')")
  public DeleteVersionPayload deleteVersion(
    @Argument
    final DeleteVersionInput input
  ) {
    final ProjectEntity project = this.projects.findByName(input.project()).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndName(project, input.id()).orElseThrow(VersionNotFoundException::new);
    if (this.builds.findAllByVersion(version).findAny().isPresent()) {
      throw new VersionInUseException("Cannot delete this version because one or more builds are still associated with it.");
    }
    return new DeleteVersionPayload(true);
  }

  @MutationMapping("promoteBuild")
  @PreAuthorize("hasRole('API_MANAGE')")
  public PromoteBuildPayload promoteBuild(
    @Argument
    final PromoteBuildInput input
  ) {
    final ProjectEntity project = this.projects.findByName(input.project()).orElseThrow(ProjectNotFoundException::new);
    VersionEntity version = this.versions.findByProjectAndName(project, input.version()).orElseThrow(VersionNotFoundException::new);
    BuildEntity build = this.builds.findByVersionAndNumber(version, input.id()).orElseThrow(BuildNotFoundException::new);

    build.setChannel(BuildChannel.RECOMMENDED);
    build = this.builds.save(build);

    version.setMostRecentPromotedBuild(build);
    version = this.versions.save(version);

    return new PromoteBuildPayload(version);
  }
}
