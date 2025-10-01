package com.trackingapp.data.parser

import com.trackingapp.data.model.GpxTrack
import com.trackingapp.data.model.GpxTrackPoint
import kotlinx.datetime.Instant
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpxParser @Inject constructor() {

    fun parseGpx(inputStream: InputStream): GpxTrack? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var trackName = ""
            val trackPoints = mutableListOf<GpxTrackPoint>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "name" -> {
                                if (parser.depth == 3) { // Track name
                                    trackName = parser.nextText()
                                }
                            }
                            "trkpt" -> {
                                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                                
                                if (lat != null && lon != null) {
                                    val trackPoint = parseTrackPoint(parser, lat, lon)
                                    trackPoints.add(trackPoint)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            GpxTrack(trackName, trackPoints)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTrackPoint(parser: XmlPullParser, lat: Double, lon: Double): GpxTrackPoint {
        var elevation: Double? = null
        var time: Instant? = null
        var speed: Float? = null
        var course: Float? = null
        var hdop: Float? = null

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_TAG || parser.name != "trkpt") {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "ele" -> {
                        elevation = parser.nextText().toDoubleOrNull()
                    }
                    "time" -> {
                        val timeStr = parser.nextText()
                        time = try {
                            Instant.parse(timeStr)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    "speed" -> {
                        speed = parser.nextText().toFloatOrNull()
                    }
                    "course" -> {
                        course = parser.nextText().toFloatOrNull()
                    }
                    "hdop" -> {
                        hdop = parser.nextText().toFloatOrNull()
                    }
                }
            }
            eventType = parser.next()
        }

        return GpxTrackPoint(lat, lon, elevation, time, speed, course, hdop)
    }
}



