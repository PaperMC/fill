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

import java.util.Optional;
import java.util.stream.Stream;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@NullMarked
@Repository
public interface FamilyRepository extends MongoRepository<FamilyEntity, ObjectId> {
  default Stream<FamilyEntity> findAllByProject(final ProjectEntity project) {
    return this.findAllByProject(project._id());
  }

  Stream<FamilyEntity> findAllByProject(final ObjectId project);

  default Optional<FamilyEntity> findByProjectAndKey(
    final ProjectEntity project,
    final String key
  ) {
    return this.findByProjectAndKey(project._id(), key);
  }

  Optional<FamilyEntity> findByProjectAndKey(
    final ObjectId project,
    final String key
  );
}
