import java.time.Instant

buildscript {
  dependencies {
    classpath("com.google.cloud.tools:jib-spring-boot-extension-gradle:0.1.0")
  }
}

plugins {
  alias(libs.plugins.gradleGitProperties)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.indra.git)
  alias(libs.plugins.jib)
  alias(libs.plugins.sentry)
  alias(libs.plugins.spotless)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.deps)
}

indra {
  apache2License()

  javaVersions {
    target(23)
  }
}

jib {
  pluginExtensions {
    pluginExtension {
      implementation = "com.google.cloud.tools.jib.gradle.extension.springboot.JibSpringBootExtension"
    }
  }

  container {
    mainClass = "io.papermc.fill.FillApplication"
    args = listOf("--spring.config.additional-location=optional:file:/config/")
    ports = listOf("8080")
  }

  from {
    image = "azul/zulu-openjdk-alpine:${indra.javaVersions().target().get()}-jre"
    platforms {
      // We can only build multi-arch images when pushing to a registry, not when building locally
      val requestedTasks = gradle.startParameter.taskNames
      if ("jibBuildTar" in requestedTasks || "jibDockerBuild" in requestedTasks) {
        platform {
          // todo: better logic
          architecture = when (System.getProperty("os.arch")) {
            "aarch64" -> "arm64"
            else -> "amd64"
          }
          os = "linux"
        }
      } else {
        platform {
          architecture = "amd64"
          os = "linux"
        }
        platform {
          architecture = "arm64"
          os = "linux"
        }
      }
    }
  }

  to {
    image = "ghcr.io/papermc/fill"
    tags = setOf(
      "latest",
      "${indraGit.branchName()}-${indraGit.commit()?.name()?.take(7)}-${Instant.now().epochSecond}"
    )
  }
}

spotless {
  java {
    licenseHeaderFile(rootProject.file("license_header.txt"))
    targetExclude("build/generated/**/*.java")
  }
}

tasks.named("sourcesJar") {
  dependsOn(tasks.named("collectExternalDependenciesForSentry"))
  dependsOn(tasks.named("generateSentryDebugMetaPropertiesjava"))
}

repositories {
  mavenCentral()
  maven("https://central.sonatype.com/repository/maven-snapshots/") {
    content {
      includeGroup("com.discord4j")
    }
  }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  compileOnlyApi("org.jspecify:jspecify:1.0.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  implementation("com.bucket4j:bucket4j_jdk17-caffeine:8.15.0")
  implementation("com.bucket4j:bucket4j_jdk17-core:8.15.0")
  implementation("com.discord4j:discord4j-core:3.3.0")
  implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
  implementation("com.google.guava:guava:33.5.0-jre")
  implementation("com.graphql-java:graphql-java-extended-scalars:24.0")
  implementation("io.jsonwebtoken:jjwt-api:0.13.0")
  implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("org.apache.commons:commons-text:1.15.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
  implementation("org.springframework.boot:spring-boot-starter-graphql")
  implementation("org.springframework.boot:spring-boot-starter-json")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("software.amazon.awssdk:s3:2.40.15")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.graphql:spring-graphql-test")
  testImplementation("org.springframework.security:spring-security-test")
}
