// SPDX-License-Identifier: MPL-2.0
package org.winlogon.glyphide

import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import io.papermc.paper.plugin.loader.{PluginClasspathBuilder, PluginLoader}

import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class GlyphideLoader extends PluginLoader {
    override def classloader(classpathBuilder: PluginClasspathBuilder): Unit = {
        val resolver = MavenLibraryResolver();

        resolver.addRepository(
            RemoteRepository.Builder(
                "central", 
                "default", 
                "https://repo.maven.apache.org/maven2/"
            ).build()
        );

        resolver.addDependency(
            Dependency(
                DefaultArtifact("org.unbescape:unbescape:1.1.6.RELEASE"),
                null
            )
        );

        classpathBuilder.addLibrary(resolver);
    }

}
