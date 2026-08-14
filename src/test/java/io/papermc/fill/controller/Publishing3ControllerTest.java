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
import io.papermc.fill.exception.PublishFailedException;
import io.papermc.fill.exception.StorageWriteException;
import io.papermc.fill.model.BuildChannel;
import io.papermc.fill.model.Checksums;
import io.papermc.fill.model.Commit;
import io.papermc.fill.model.Download;
import io.papermc.fill.model.Java;
import io.papermc.fill.model.JavaFlags;
import io.papermc.fill.model.JavaVersion;
import io.papermc.fill.model.Support;
import io.papermc.fill.model.request.v3.PublishRequest;
import io.papermc.fill.notification.BuildListener;
import io.papermc.fill.service.StorageService;
import io.papermc.fill.util.discord.DiscordNotificationChannel;
import io.papermc.fill.util.git.GitRepository;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@NullMarked
public class Publishing3ControllerTest {
  private static final Instant CREATED_AT = Instant.parse("2026-07-28T00:00:00Z");
  private static final UUID UPLOAD_ID = UUID.fromString("9d42dfd6-6b0f-4eb5-ac5f-45efcdfead7e");
  private static final ProjectEntity PROJECT = ProjectEntity.create(
    new ObjectId("000000000000000000000001"),
    "paper",
    "Paper",
    new GitRepository("PaperMC", "Paper"),
    URI.create("https://example.invalid/logo.png"),
    List.<DiscordNotificationChannel>of(),
    "server:default"
  );
  private static final FamilyEntity FAMILY = FamilyEntity.create(
    new ObjectId("000000000000000000000002"),
    CREATED_AT,
    PROJECT,
    "1.21",
    new Java(new JavaVersion(21), new JavaFlags(List.of()))
  );
  private static final VersionEntity VERSION = VersionEntity.create(
    new ObjectId("000000000000000000000003"),
    CREATED_AT,
    PROJECT,
    FAMILY,
    "1.21.8",
    null,
    Support.SUPPORTED,
    null
  );

  private ProjectRepository projects;
  private FamilyRepository families;
  private VersionRepository versions;
  private BuildRepository builds;
  private StorageService storage;
  private BuildListener listener;
  private Publishing3Controller controller;

  @BeforeEach
  void setup() {
    this.projects = mock(ProjectRepository.class);
    this.families = mock(FamilyRepository.class);
    this.versions = mock(VersionRepository.class);
    this.builds = mock(BuildRepository.class);
    this.storage = mock(StorageService.class);
    this.listener = mock(BuildListener.class);
    this.controller = new Publishing3Controller(
      this.projects,
      this.families,
      this.versions,
      this.builds,
      this.storage,
      Set.of(this.listener)
    );

    when(this.projects.findByKey(PROJECT.key())).thenReturn(Optional.of(PROJECT));
    when(this.families.findByProjectAndKey(PROJECT, FAMILY.key())).thenReturn(Optional.of(FAMILY));
    when(this.versions.findByProjectAndKey(PROJECT, VERSION.key())).thenReturn(Optional.of(VERSION));
  }

  @Test
  void publishesOnlyAfterAllObjectsAreVerifiedAndPromoted() throws Exception {
    final PublishRequest request = request();
    final List<Download> downloads = List.copyOf(request.downloads().values());
    when(this.builds.findByVersionAndNumber(VERSION, request.build())).thenReturn(Optional.empty());
    when(this.builds.save(any(BuildEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    final ResponseEntity<?> response = this.controller.publish(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    final InOrder order = inOrder(this.storage, this.builds, this.listener);
    for (final Download download : downloads) {
      order.verify(this.storage).verifyStagedObject(UPLOAD_ID, download);
    }
    for (final Download download : downloads) {
      order.verify(this.storage).promoteStagedObject(eq(UPLOAD_ID), eq(PROJECT), eq(VERSION), any(BuildEntity.class), eq(download));
    }
    order.verify(this.builds).save(any(BuildEntity.class));
    for (final Download download : downloads) {
      order.verify(this.storage).deleteStagedObject(UPLOAD_ID, download.name());
    }
    order.verify(this.listener).onBuildPublished(eq(PROJECT), eq(VERSION), any(BuildEntity.class));
  }

  @Test
  void doesNotPublishOrCleanUpWhenPromotionFails() throws Exception {
    final PublishRequest request = request();
    final List<Download> downloads = List.copyOf(request.downloads().values());
    when(this.builds.findByVersionAndNumber(VERSION, request.build())).thenReturn(Optional.empty());
    doThrow(new StorageWriteException("copy failed", new IllegalStateException()))
      .when(this.storage).promoteStagedObject(eq(UPLOAD_ID), eq(PROJECT), eq(VERSION), any(BuildEntity.class), eq(downloads.getFirst()));

    assertThrows(PublishFailedException.class, () -> this.controller.publish(request));

    verify(this.builds, never()).save(any(BuildEntity.class));
    for (final Download download : downloads) {
      verify(this.storage, never()).deleteStagedObject(UPLOAD_ID, download.name());
    }
    verifyNoInteractions(this.listener);
  }

  @Test
  void treatsAnIdenticalExistingBuildAsAnIdempotentRetry() throws Exception {
    final PublishRequest request = request();
    final BuildEntity existing = BuildEntity.create(
      new ObjectId("000000000000000000000004"),
      request.time(),
      PROJECT,
      VERSION,
      request.build(),
      request.channel(),
      request.commits().reversed(),
      request.downloads()
    );
    when(this.builds.findByVersionAndNumber(VERSION, request.build())).thenReturn(Optional.of(existing));

    final ResponseEntity<?> response = this.controller.publish(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    for (final Download download : request.downloads().values()) {
      verify(this.storage).deleteStagedObject(UPLOAD_ID, download.name());
    }
    verifyNoMoreInteractions(this.storage);
    verify(this.builds, never()).save(any(BuildEntity.class));
    verifyNoInteractions(this.listener);
  }

  private static PublishRequest request() {
    final Map<String, Download> downloads = new LinkedHashMap<>();
    downloads.put("server:default", new Download("paper.jar", new Checksums("a".repeat(64)), 100));
    downloads.put("server:mojang", new Download("paper-mojang.jar", new Checksums("b".repeat(64)), 200));
    return new PublishRequest(
      UPLOAD_ID,
      PROJECT.key(),
      FAMILY.key(),
      VERSION.key(),
      42,
      CREATED_AT,
      BuildChannel.STABLE,
      List.of(new Commit("c".repeat(40), CREATED_AT, "Test commit")),
      downloads
    );
  }
}
