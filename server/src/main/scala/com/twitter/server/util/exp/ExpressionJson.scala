package com.twitter.server.util.exp

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import com.twitter.finagle.stats.exp._

/**
 * A set of serializers, deserializers used to serve admin/metrics/expressions endpoint
 */
object ExpressionJson {

  /**
   * Serialize the ExpressionSchema to JSON
   */
  object ExpressionSchemaSerializer
      extends StdSerializer[ExpressionSchema](classOf[ExpressionSchema]) {

    def serialize(
      expressionSchema: ExpressionSchema,
      gen: JsonGenerator,
      provider: SerializerProvider
    ): Unit = {
      gen.writeStartObject()
      gen.writeStringField("name", expressionSchema.name)

      gen.writeObjectFieldStart("labels")
      gen.writeStringField(
        "process_path",
        expressionSchema.labels.processPath.getOrElse("Unspecified"))
      gen.writeStringField(
        "service_name",
        expressionSchema.labels.serviceName.getOrElse("Unspecified"))
      gen.writeStringField("role", expressionSchema.labels.role.toString)
      gen.writeEndObject()

      gen.writeStringField("expression", expressionSchema.exprQuery)

      provider.defaultSerializeField("bounds", expressionSchema.bounds, gen)

      gen.writeStringField("description", expressionSchema.description)
      gen.writeStringField("unit", expressionSchema.unit.toString)
      gen.writeEndObject()
    }
  }

  /**
   * Deserialize bounds/operator to the case object.
   * Serialization is handled by Json annotations.
   */
  object OperatorDeserializer extends StdDeserializer[Operator](classOf[Operator]) {
    override def deserialize(
      parser: JsonParser,
      ctx: DeserializationContext
    ): Operator = {
      parser.readValueAs(classOf[String]) match {
        case ">" => GreaterThan
        case "<" => LessThan
        case other =>
          throw ctx.instantiationException(classOf[Operator], s"Unknown operator: $other")
      }
    }
  }
}
