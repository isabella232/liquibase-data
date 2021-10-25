/*
 * Copyright (c) 2021 by Titan Project Contributors.. All rights reserved.
 */

package io.titandata.titan.providers.generic

import io.titandata.client.apis.CommitsApi

class Tag(
    private val commitsApi: CommitsApi = CommitsApi()
) {
    fun tagCommit(repository: String, commit: String, tags: List<String>) {
        val commit = commitsApi.getCommit(repository, commit)
        val commitTags = if (commit.properties.containsKey("tags")) {
            (commit.properties.get("tags") as Map<String, String>).toMutableMap()
        } else {
            mutableMapOf()
        }

        for (t in tags) {
            if (t.contains("=")) {
                val key = t.substringBefore("=")
                val value = t.substringAfter("=")
                commitTags[key] = value
            } else {
                commitTags[t] = ""
            }
        }

        val metadata = commit.properties.toMutableMap()
        metadata["tags"] = commitTags

        commitsApi.updateCommit(repository, io.titandata.models.Commit(id = commit.id, properties = metadata))
    }
}
