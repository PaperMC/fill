package io.papermc.fill.graph

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.Scalars
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLType
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Component
class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {
  override fun willGenerateGraphQLType(type: KType): GraphQLType? {
    return when (type.classifier as? KClass<*>) {
      Instant::class -> ExtendedScalars.DateTime
      LocalDate::class -> ExtendedScalars.Date
      ZonedDateTime::class -> ExtendedScalars.DateTime
      ObjectId::class -> Scalars.GraphQLID
      URI::class -> Scalars.GraphQLString
      else -> super.willGenerateGraphQLType(type)
    }
  }
}
