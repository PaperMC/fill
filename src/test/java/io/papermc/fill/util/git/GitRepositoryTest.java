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
package io.papermc.fill.util.git;

import org.bson.Document;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class GitRepositoryTest {
  @Test
  public void testDefaultGitHub() {
    final GitRepository repository = new GitRepository("PaperMC/Paper");
    assertEquals(GitForge.GITHUB, repository.forge());
    assertEquals("PaperMC/Paper", repository.name());
    assertEquals("https://github.com/PaperMC/Paper", repository.url());
    assertEquals("https://github.com/PaperMC/Paper/commit/abc1234", repository.commitUrl("abc1234"));
    assertEquals("https://github.com/PaperMC/Paper/compare/abc1234...def5678", repository.compareUrl("abc1234", "def5678"));
  }

  @Test
  public void testExplicitForge() {
    final GitRepository repository = new GitRepository(GitForge.GITHUB, "PaperMC/Paper");
    assertEquals(GitForge.GITHUB, repository.forge());
    assertEquals("PaperMC/Paper", repository.name());
    assertEquals("https://github.com/PaperMC/Paper", repository.url());
  }

  @Test
  public void testLegacyOwnerNameConstructor() {
    final GitRepository repository = new GitRepository("PaperMC", "Paper");
    assertEquals(GitForge.GITHUB, repository.forge());
    assertEquals("PaperMC/Paper", repository.name());
    assertEquals("https://github.com/PaperMC/Paper", repository.url());
  }

  @Test
  public void testPersistenceCreator() {
    final GitRepository fromSplit = new GitRepository(GitForge.GITHUB, "PaperMC", "Paper");
    assertEquals("PaperMC/Paper", fromSplit.name());

    final GitRepository fromCombined = new GitRepository(GitForge.GITHUB, null, "PaperMC/Paper");
    assertEquals("PaperMC/Paper", fromCombined.name());
  }

  @Test
  public void testReadLegacyDocument() throws Exception {
    final GitRepository repository = converter().read(
      GitRepository.class,
      Document.parse("{\"owner\":\"PaperMC\",\"name\":\"Paper\"}")
    );

    assertEquals(GitForge.GITHUB, repository.forge());
    assertEquals("PaperMC/Paper", repository.name());
  }

  @Test
  public void testReadCurrentDocument() throws Exception {
    final GitRepository repository = converter().read(
      GitRepository.class,
      Document.parse("{\"forge\":\"GITHUB\",\"name\":\"PaperMC/Paper\"}")
    );

    assertEquals(GitForge.GITHUB, repository.forge());
    assertEquals("PaperMC/Paper", repository.name());
  }

  private static MappingMongoConverter converter() throws Exception {
    final MongoCustomConversions conversions = MongoCustomConversions.create(adapter -> {});
    final MongoMappingContext mappingContext = new MongoMappingContext();
    mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
    mappingContext.afterPropertiesSet();

    final MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
    converter.setCustomConversions(conversions);
    converter.afterPropertiesSet();
    return converter;
  }
}
