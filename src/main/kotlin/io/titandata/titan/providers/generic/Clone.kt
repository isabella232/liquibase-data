/*
 * Copyright (c) 2021 by Titan Project Contributors.. All rights reserved.
 */

package io.titandata.titan.providers.generic

import io.titandata.client.apis.RemotesApi
import io.titandata.client.apis.RepositoriesApi
import io.titandata.models.Commit
import io.titandata.models.Repository
import io.titandata.serialization.RemoteUtil
import io.titandata.titan.clients.Docker
import io.titandata.titan.exceptions.CommandException
import io.titandata.titan.providers.Metadata
import io.titandata.titan.utils.CommandExecutor
import java.net.URI
import kotlin.system.exitProcess
import liquibase.Scope
import liquibase.ui.UIService
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class Clone(
    private val remoteAdd: (container: String, uri: String, remoteName: String?, params: Map<String, String>) -> Unit,
    private val pull: (container: String, commit: String?, remoteName: String?, tags: List<String>, metadataOnly: Boolean) -> Unit,
    private val checkout: (container: String, hash: String?, tags: List<String>) -> Unit,
    private val run: (container: String, repository: String?, environments: List<String>, arguments: List<String>, disablePortMapping: Boolean, createRepo: Boolean) -> Unit,
    private val remove: (container: String, force: Boolean) -> Unit,
    private val commandExecutor: CommandExecutor = CommandExecutor(),
    private val docker: Docker = Docker(commandExecutor),
    private val remotesApi: RemotesApi = RemotesApi(),
    private val repositoriesApi: RepositoriesApi = RepositoriesApi(),
    private val remoteUtil: RemoteUtil = RemoteUtil(),
    private val ui: UIService? = Scope.getCurrentScope().ui
) {
    fun clone(
        uri: String,
        container: String?,
        guid: String?,
        params: Map<String, String>,
        arguments: List<String>,
        disablePortMapping: Boolean,
        tags: List<String>
    ) {
        val parsedUri = URI(uri)
        val repoName = when (container) {
            null -> parsedUri.path.split("/").last()
            else -> container
        }
        val commitId = when {
            guid.isNullOrEmpty() && parsedUri.fragment != null -> parsedUri.fragment
            else -> guid
        }
        val repository = Repository(repoName, emptyMap())
        val plainUri = "${parsedUri.scheme}://${parsedUri.authority}${parsedUri.path}"
        val allTags = tags.toMutableList()
        if (parsedUri.query != null) {
            allTags.addAll(("http://host?${parsedUri.query}".toHttpUrlOrNull()?.queryParameterValues("tag") ?: emptyList()) as Collection<String>)
        }
        var cleanup = false
        try {
            repositoriesApi.createRepository(repository)
            cleanup = true
            remoteAdd(repoName, plainUri, null, params)
            val remote = remotesApi.getRemote(repoName, "origin")
            var commit = Commit("id", emptyMap())
            if (commitId.isNullOrEmpty()) {
                val remoteCommits = remotesApi.listRemoteCommits(repoName, remote.name, remoteUtil.getParameters(remote), allTags)
                if (remoteCommits.isEmpty()) {
                    error("unable to find any matching commits in remote repository")
                }
                commit = remoteCommits.first()
            } else {
                if (!tags.isEmpty()) {
                    error("tags cannot be specified with commit ID")
                }
                commit = remotesApi.getRemoteCommit(repoName, remote.name, commitId, remoteUtil.getParameters(remote))
            }
            val metadata = Metadata.load(commit.properties)
            try {
                docker.inspectImage(metadata.image.digest)
            } catch (e: CommandException) {
                try {
                    docker.pull(metadata.image.digest)
                } catch (e: CommandException) {
                    throw CommandException(
                            "Unable to find image ${metadata.image.digest} for ${metadata.image.image}",
                            e.exitCode,
                            e.output
                    )
                }
                docker.pull(metadata.image.digest)
            }
            run(metadata.image.digest, repoName, metadata.environment, arguments, disablePortMapping, false)
            pull(repoName, commit.id, null, listOf(), false)
            checkout(repoName, commit.id, listOf())
            cleanup = false
        } catch (t: Throwable) {
            // We explicitly handle the exception so that the error message appears before the remove messages
            ui?.sendMessage(t.message)
            if (t is CommandException) {
                ui?.sendMessage(t.output)
            }
            if (cleanup) {
                try {
                    remove(repository.name, true)
                } catch (t: Throwable) {
                    // Ignore
                }
            }
            exitProcess(1)
        }
    }
}
