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
package io.papermc.fill.database;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@NullMarked
@Repository
public interface BuildRepository extends MongoRepository<BuildEntity, ObjectId> {
  Stream<BuildEntity> findAllByProjectAndVersion(
    final ProjectEntity project,
    final VersionEntity version
  );

  Page<BuildEntity> findAllByProjectAndVersion(
    final ProjectEntity project,
    final VersionEntity version,
    final Pageable pageable
  );

  Stream<BuildEntity> findAllByProjectAndVersionIn(
    final ProjectEntity project,
    final Collection<VersionEntity> version
  );

  Optional<BuildEntity> findByProjectAndVersionAndNumber(
    final ProjectEntity project,
    final VersionEntity version,
    final int number
  );
}
