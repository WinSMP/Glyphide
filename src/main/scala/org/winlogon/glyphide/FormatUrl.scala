package org.winlogon.glyphide

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap, Executors, TimeUnit}
import java.util.regex.Pattern
import java.util.Map

import scala.util.Try
import scala.jdk.CollectionConverters._

case class UrlInformation(title: String, description: Option[String])

class FormatUrl {
    private val cache = Collections.synchronizedMap(
        new java.util.LinkedHashMap[String, UrlInformation](16, 0.75f, true) {
            override protected def removeEldestEntry(
                eldest: Map.Entry[String, UrlInformation]
            ): Boolean = size() > 1000
        }
    )
    
    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(java.time.Duration.ofSeconds(5))
        .executor(Executors.newWorkStealingPool(4))
        .build()

    private val titlePattern = Pattern.compile(
        "<title>\\s*(.*?)\\s*</title>",
        Pattern.CASE_INSENSITIVE
    )
    private val descPattern = Pattern.compile(
        "<meta\\s+name=\"description\"\\s+content=\"(.*?)\"",
        Pattern.CASE_INSENSITIVE
    )

    def getUrlInformation(url: String): Option[UrlInformation] = {
        Option(cache.get(url)) orElse {
            CompletableFuture.runAsync(() => fetchAndCache(url))
            None
        }
    }

    private def fetchAndCache(url: String): Unit = {
        Try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:138.0) Gecko/20100101 Firefox/138.0")
                .timeout(java.time.Duration.ofSeconds(5))
                .build()

            val response = httpClient.send(request, BodyHandlers.ofInputStream())
            val bytes = response.body().readAllBytes()
            // get the first 8kb of the html code
            val html = new String(bytes.take(8192), StandardCharsets.UTF_8)
            
            val titleMatcher = titlePattern.matcher(html)
            val title = if (titleMatcher.find()) titleMatcher.group(1) else ""
            
            val descMatcher = descPattern.matcher(html)
            val description = if (descMatcher.find()) Some(descMatcher.group(1)) else None
            
            UrlInformation(title, description)
        }.toOption.foreach(info => cache.put(url, info))
    }
}
