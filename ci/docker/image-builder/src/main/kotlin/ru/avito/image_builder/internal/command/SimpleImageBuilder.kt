package ru.avito.image_builder.internal.command

import ru.avito.image_builder.internal.docker.Docker
import ru.avito.image_builder.internal.docker.Image
import ru.avito.image_builder.internal.docker.ImageId
import java.io.File
import java.util.logging.Logger

internal class SimpleImageBuilder(
    private val docker: Docker,
    /**
     * Relative path to the Dockerfile inside context (buildDir)
     */
    private val dockerfilePath: String,
    /**
     * build path
     * https://docs.docker.com/engine/reference/commandline/build/#build-with-path
     */
    private val buildDir: File,
    private val login: RegistryLogin,
    private val tagger: ImageTagger,
    private val registry: String,
    /**
     * Image registry name which will be used in image tag name.
     * Use case: when we want to build image with internal registry dependencies, but then push image to other registry.
     * If null or empty then @param [registry] name will be used
     */
    private val imageRegistryTagName: String?,
    private val artifactoryUrl: String,
    private val imageName: String,
) : ImageBuilder {

    private val log: Logger = Logger.getLogger(this::class.java.simpleName)

    override fun build(): Image {
        login.login()

        val id = buildImage()

        return tag(id)
    }

    private fun buildImage(): ImageId {
        log.info("Building an image ...")

        val buildResult = docker.build(
            "--build-arg", "DOCKER_REGISTRY=$registry",
            "--build-arg", "ARTIFACTORY_URL=$artifactoryUrl",
            "--file", File(buildDir, dockerfilePath).canonicalPath,
            buildDir.canonicalPath,
        )
        check(buildResult.isSuccess) {
            "Failed to build the image: ${buildResult.exceptionOrNull()}"
        }
        val id = buildResult.getOrThrow()
        log.info("Built image: $id")
        return id
    }

    private fun tag(id: ImageId): Image {
        val registryName = if (!imageRegistryTagName.isNullOrEmpty()) {
            imageRegistryTagName
        } else {
            registry
        }
        return tagger.tag(id, "$registryName/$imageName")
    }
}
