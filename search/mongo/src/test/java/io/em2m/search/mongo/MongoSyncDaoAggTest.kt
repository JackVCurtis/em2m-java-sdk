/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________

 * Copyright (c) 2013-2016 Elastic M2M Incorporated, All Rights Reserved.

 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated

 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.search.mongo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Envelope
import io.em2m.search.core.model.*
import org.junit.Ignore
import org.junit.Test
import rx.observers.TestSubscriber


@Ignore
class MongoSyncDaoAggTest : FeaturesTestBase() {

    @Test
    fun testTerms() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(TermsAgg("properties.status", key = "statuses")))
        val result = syncDao.search(request)
        assertEquals(46, result.totalItems)
        assertEquals(0, result.items?.size)
        val statuses = result.aggs["statuses"] ?: error("statuses should not be null")
        val buckets = statuses.buckets ?: error("buckets should not be null")
        assertEquals(3, buckets.size)
        assertEquals("reviewed", buckets[0].key)
        assertEquals("REVIEWED", buckets[1].key)
        assertEquals("automatic", buckets[2].key)
        assertEquals(46, buckets[0].count + buckets[1].count + buckets[2].count)
    }

    @Test
    fun testMissingTerms() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(TermsAgg("properties.alert", key = "alerts", missing = "No Alert")))
        val result = syncDao.search(request)
        assertEquals(46, result.totalItems)
        assertEquals(0, result.items?.size)
        val alerts = result.aggs["alerts"] ?: error("alerts should not be null")
        val buckets = alerts.buckets ?: error("buckets should not be null")
        assertEquals(2, buckets.size)
        assertEquals("No Alert", buckets[0].key)
        assertEquals("green", buckets[1].key)
        assertEquals(46, buckets[0].count + buckets[1].count)
    }


    @Test
    fun testTermsWithQuery() {
        val request = SearchRequest(0, 0, RangeQuery("properties.mag", gte = 4), aggs = listOf(TermsAgg("properties.status", key = "statuses")))
        val result = syncDao.search(request)
        assertEquals(16, result.totalItems)
        assertEquals(0, result.items?.size)
        val statuses = result.aggs["statuses"] ?: error("statuses should not be null")
        val buckets = statuses.buckets ?: error("buckets should not be null")
        assertEquals(1, buckets.size)
        assertEquals("reviewed", buckets[0].key)
        assertEquals(16, buckets[0].count)

    }

    @Test
    fun testTermsWithSize() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(TermsAgg("properties.status", key = "statuses", size = 1)))
        val result = syncDao.search(request)
        assertEquals(46, result.totalItems)
        assertEquals(0, result.items?.size)
        val statuses = result.aggs["statuses"] ?: error("statuses should not be null")
        val buckets = statuses.buckets ?: error("buckets should not be null")
        assertEquals(1, buckets.size)
        assertEquals("reviewed", buckets[0].key)
        assertEquals(46, buckets[0].count + buckets[1].count + buckets[2].count)
    }


    @Test
    fun testFilters() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(FiltersAgg(mapOf("green_alerts" to TermQuery("properties.alert", "green")), "test")))
        val result = syncDao.search(request)
        val agg = result.aggs["test"] ?: error("agg should not be null")
        val buckets = agg.buckets ?: error("buckets should not be null")
        assertEquals(1, buckets.size)
        assertEquals(3, buckets[0].count)
    }

    @Test
    fun testHistogram() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(HistogramAgg("properties.mag", 1.0, key = "magnitude")))
        val result = syncDao.search(request)
        val missing = result.aggs["magnitude"] ?: error("agg should not be null")
        val buckets = missing.buckets ?: error("buckets should not be null")
        assertEquals(4, buckets.size)
        assertEquals(46, buckets.map { it.count }.sum())
    }

    @Test
    fun testMissing() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(MissingAgg("properties.alert", key = "missing")))
        val result = syncDao.search(request)
        val missing = result.aggs["missing"] ?: error("agg should not be null")
        val buckets = missing.buckets ?: error("buckets should not be null")
        assertEquals(1, buckets.size)
        assertEquals(43, buckets[0].count)
    }

    @Test
    fun testRange() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(RangeAgg("properties.mag", key = "magnitude",
                ranges = listOf(Range(from = 4.0, key = "4+")))))
        val result = syncDao.search(request)
        val agg = result.aggs["magnitude"] ?: error("agg should not be null")
        val buckets = agg.buckets ?: error("buckets should not be null")
        assertEquals(1, buckets.size)
        assertEquals(16, buckets.map { it.count }.sum())
        assertEquals("4+", buckets[0].key)
    }

    @Test
    fun testStats() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(StatsAgg("properties.mag", key = "magnitude")))
        val result = syncDao.search(request)
        val missing = result.aggs["magnitude"] ?: error("agg should not be null")
        val buckets = missing.buckets ?: error("buckets should not be null")
        assertEquals(1, buckets.size)
        assertEquals(2.5, buckets[0].stats?.min)
        assertEquals(46L, buckets[0].stats?.count)
    }

    @Test
    fun testDateRange() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(DateRangeAgg("properties.time", key = "magnitude",
                ranges = listOf(Range(from = "1408447319000", to = "now")))))
        val result = syncDao.search(request)
        val agg = result.aggs["magnitude"] ?: error("agg should not be null")
        val buckets = agg.buckets ?: error("buckets should not be null")
        assertEquals(1, buckets.size)
        assertEquals(20, buckets.map { it.count }.sum())
    }

    @Test
    fun testDateHistogram() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(DateHistogramAgg("properties.time", null, "hour", key = "time")))
        val result = syncDao.search(request)
        val agg = result.aggs["time"] ?: error("agg should not be null")
        val buckets = agg.buckets ?: error("buckets should not be null")
        assertEquals(23, buckets.size)
        assertEquals(46, buckets.map { it.count }.sum())
    }

    @Test
    fun testGeoDistance() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(GeoDistanceAgg("geometry.coordinates", key = "test",
                origin = Coordinate(-79.0, 38.0), ranges = listOf(Range(from = 0, to = 1000, key = "<1000miles"), Range(from = 1000)))))
        val result = syncDao.search(request)
        val agg = result.aggs["test"] ?: error("agg should not be null")
        val buckets = agg.buckets ?: error("buckets should not be null")
        assertEquals(2, buckets.size)
        assertEquals(46, buckets.map { it.count }.sum())
        assertEquals("<1000miles", buckets[0].key)
    }

    @Test
    fun testGeoHash() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(GeoHashAgg("geometry.coordinates", precision = 1, key = "geohash")))
        val result = syncDao.search(request)
        val agg = result.aggs["geohash"] ?: error("agg should not be null")
        val buckets = agg.buckets ?: error("buckets should not be null")
        assertEquals(11, buckets.size)
        assertEquals(46, buckets.map { it.count }.sum())
    }

    @Test
    fun testGeoBounds() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(GeoBoundsAgg("geometry.coordinates", key = "geo_bounds")))
        val result = syncDao.search(request)
        val agg = result.aggs["geo_bounds"] ?: error("agg should not be null")
        agg.value as Envelope? ?: error("Envelope should not be null")
    }

    @Test
    fun testGeoCentroid() {
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(GeoCentroidAgg("geometry.coordinates", key = "centroid")))
        val result = syncDao.search(request)
        val agg = result.aggs["centroid"] ?: error("agg should not be null")
        agg.value as Coordinate? ?: error("Coordinate should not be null")
    }

    @Test
    fun testNative() {
        val x = "$"
        val native = jacksonObjectMapper().readTree("""[{ "${x}unwind": "${x}type" },{ "${x}sortByCount": "${x}type" }]""")
        val request = SearchRequest(0, 0, MatchAllQuery(), aggs = listOf(NativeAgg(native, key = "test")))
        val result = syncDao.search(request)
        val agg = result.aggs["test"] ?: error("agg should not be null")
        val buckets = agg.buckets
        assertEquals(1, buckets?.size)
    }

}
