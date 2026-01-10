// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import io.papermc.paper.plugin.loader.{PluginClasspathBuilder, PluginLoader}

import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

import java.util.List as JavaList

class GlyphideLoader extends PluginLoader {
    override def classloader(classpathBuilder: PluginClasspathBuilder): Unit = {
        val resolver = MavenLibraryResolver();

        // See https://mvnrepository.com/artifact/org.scala-lang/scala3-library
        val scala = "3.7.4"

        resolver.addRepository(
            RemoteRepository.Builder(
                "central",
                "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
            ).build()
        );

        resolver.addRepository(
            RemoteRepository.Builder(
                "winlogon",
                "default",
                "https://maven.winlogon.org/releases"
            ).build()
        );

        val dependencies = JavaList.of(
            dependency("org.unbescape:unbescape:1.1.6.RELEASE"),
            dependency("org.winlogon:retrohue:0.1.0"),
            dependency("org.scala-lang:scala3-library_3:".concat(scala))
        )

        dependencies.forEach((d: Dependency) => resolver.addDependency(d))

        classpathBuilder.addLibrary(resolver);
    }

    private def dependency(dep: String): Dependency = {
        Dependency(DefaultArtifact(dep), null)
    }
}
