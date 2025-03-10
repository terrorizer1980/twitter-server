package com.twitter.server.util

import com.twitter.finagle.stats._
import com.twitter.finagle.stats.exp.ExpressionSchema
import org.scalatest.FunSuite

class MetricSchemaSourceTest extends FunSuite {

  private val schemaMap: Map[String, MetricSchema] = Map(
    "my/cool/counter" -> CounterSchema(
      MetricBuilder(
        keyIndicator = true,
        description = "Counts how many cools are seen",
        units = Requests,
        role = Server,
        verbosity = Verbosity.Default,
        sourceClass = Some("finagle.stats.cool"),
        name = Seq("my", "cool", "counter"),
        processPath = Some("dc/role/zone/service"),
        percentiles = IndexedSeq(0.5, 0.9, 0.95, 0.99, 0.999, 0.9999),
        statsReceiver = null
      )),
    "your/fine/gauge" -> GaugeSchema(
      MetricBuilder(
        keyIndicator = false,
        description = "Measures how fine the downstream system is",
        units = Percentage,
        role = Client,
        verbosity = Verbosity.Debug,
        sourceClass = Some("finagle.stats.your"),
        name = Seq("your", "fine", "gauge"),
        processPath = Some("dc/your_role/zone/your_service"),
        percentiles = IndexedSeq(0.5, 0.9, 0.95, 0.99, 0.999, 0.9999),
        statsReceiver = null
      )),
    "my/only/histo" -> HistogramSchema(
      MetricBuilder(
        name = Seq("my", "only", "histo"),
        percentiles = IndexedSeq(0.5, 0.9, 0.95, 0.99, 0.999, 0.9999),
        statsReceiver = null
      ))
  )

  private val latchedPopulatedRegistry: SchemaRegistry = new SchemaRegistry {
    def hasLatchedCounters: Boolean = true
    def schemas(): Map[String, MetricSchema] = schemaMap
    def expressions(): Map[String, ExpressionSchema] = Map.empty
  }

  private val unlatchedPopulatedRegistry: SchemaRegistry = new SchemaRegistry {
    def hasLatchedCounters: Boolean = false
    def schemas(): Map[String, MetricSchema] = schemaMap
    def expressions(): Map[String, ExpressionSchema] = Map.empty
  }

  private val metricSchemaSource = new MetricSchemaSource(Seq(latchedPopulatedRegistry))
  private val unlatchedMetricSchemaSource = new MetricSchemaSource(Seq(unlatchedPopulatedRegistry))
  private val emptyMetricSchemaSource = new MetricSchemaSource(Seq())

  test("hasLatchedCounters asserts if there is no SchemaRegistry") {
    assertThrows[AssertionError](emptyMetricSchemaSource.hasLatchedCounters)
  }

  test("hasLatchedCounters returns the underlying SchemaRegistry's hasLatchedCounters value") {
    assert(metricSchemaSource.hasLatchedCounters)
    assert(!unlatchedMetricSchemaSource.hasLatchedCounters)
  }

  test("getSchema returns the appropriate MetricSchema when there is one") {
    assert(metricSchemaSource.getSchema("my/cool/counter") == schemaMap.get("my/cool/counter"))
  }

  test("getSchema returns the None when absent") {
    assert(metricSchemaSource.getSchema("my/dull/counter") == None)
  }

  test("schemaList returns the full list of MetricSchemas") {
    assert(metricSchemaSource.schemaList() == schemaMap.values.toVector)
  }

  test("schemaList returns empty list if there is no registry") {
    assert(emptyMetricSchemaSource.schemaList() == Seq())
  }

  test(
    "contains accurately reflect the presence or absence of a Metric from the MetricSchema map") {
    assert(metricSchemaSource.contains("my/cool/counter"))
    assert(!metricSchemaSource.contains("my/dull/counter"))
    assert(metricSchemaSource.contains("my/only/histo"))
    assert(!emptyMetricSchemaSource.contains("my/cool/counter"))
    assert(!emptyMetricSchemaSource.contains("my/dull/counter"))
    assert(!emptyMetricSchemaSource.contains("my/only/histo"))
  }

  test("keySet returns Set of Metric names (key portion of the schema map)") {
    assert(metricSchemaSource.keySet == schemaMap.keySet)
  }

  test("keySet returns empty Set if there is no registry") {
    assert(emptyMetricSchemaSource.keySet == Set())
  }
}
