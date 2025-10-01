package com.trackingapp.data.parser

import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpxGenerator @Inject constructor() {

    fun generateGpx(track: Track, trackPoints: List<TrackPoint>): String {
        val gpx = StringBuilder()
        
        gpx.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        gpx.appendLine("<gpx version=\"1.1\" creator=\"RouteTracker\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
        gpx.appendLine("  <trk>")
        gpx.appendLine("    <name>${escapeXml(track.name)}</name>")
        gpx.appendLine("    <trkseg>")
        
        trackPoints.forEach { point ->
            gpx.appendLine("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
            
            point.altitude?.let { altitude ->
                gpx.appendLine("        <ele>$altitude</ele>")
            }
            
            gpx.appendLine("        <time>${formatTime(point.timestamp)}</time>")
            
            point.speed?.let { speed ->
                gpx.appendLine("        <speed>$speed</speed>")
            }
            
            point.bearing?.let { bearing ->
                gpx.appendLine("        <course>$bearing</course>")
            }
            
            point.accuracy?.let { accuracy ->
                gpx.appendLine("        <hdop>$accuracy</hdop>")
            }
            
            gpx.appendLine("      </trkpt>")
        }
        
        gpx.appendLine("    </trkseg>")
        gpx.appendLine("  </trk>")
        gpx.appendLine("</gpx>")
        
        return gpx.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatTime(instant: Instant): String {
        return instant.toString()
    }
}



