// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import org.unbescape.html.HtmlEscape

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.{CompletableFuture, Executors}
import java.util.regex.Pattern
import java.util.{Collections, LinkedHashMap, Map}

import scala.util.Try
import scala.jdk.CollectionConverters.*

case class UrlInformation(title: String, description: Option[String])

class FormatUrl {
    private val cache = Collections.synchronizedMap(
        new LinkedHashMap[String, UrlInformation](16, 0.75f, true) {
            override protected def removeEldestEntry(
                eldest: Map.Entry[String, UrlInformation]
            ): Boolean = size() > 1000
        }
    )

    private val userAgent = Option(System.getProperty("glyphide.cache.user-agent"))
        .getOrElse("Mozilla/5.0 (X11; Linux x86_64; rv:138.0) Gecko/20100101 Firefox/138.0")

    private val requestorsExecutor = Executors.newWorkStealingPool(4)

    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(2))
        .executor(requestorsExecutor)
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
            CompletableFuture.runAsync(() => fetchAndCache(url), requestorsExecutor)
            None
        }
    }

    private def fetchAndCache(url: String): Unit = {
        Try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", userAgent)
                .timeout(java.time.Duration.ofSeconds(5))
                .build()

            val response = httpClient.send(request, BodyHandlers.ofInputStream())
            val bytes = response.body().readAllBytes()
            // get the first 8kb of the HTML code
            val html = new String(bytes.take(8192), StandardCharsets.UTF_8)

            val titleMatcher = titlePattern.matcher(html)
            val title = if (titleMatcher.find()) HtmlEscape.unescapeHtml(titleMatcher.group(1)) else ""

            val descMatcher = descPattern.matcher(html)
            val description = if (descMatcher.find()) {
                Some(HtmlEscape.unescapeHtml(descMatcher.group(1)))
            } else {
                None
            }

            UrlInformation(title, description)
        }.toOption.foreach(info => cache.put(url, info))
    }
}
