package io.titandata.titan.providers.generic

import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class CloneTest {
    private fun remoteAdd(container: String, uri: String, remoteName: String?, params: Map<String, String>) {}
    private fun pull(container: String, commit: String?, remoteName: String?, tags: List<String>, metadataOnly: Boolean) {}
    private fun checkout(container: String, hash: String?, tags: List<String>) {}
    private fun run(container: String, repository: String?, environments: List<String>, parameters: List<String>, disablePortMapping: Boolean, createRepo: Boolean) {}
    private fun remove(container: String, force: Boolean) {}
    private val command = Clone(::remoteAdd, ::pull, ::checkout, ::run, ::remove)

    @Test
    fun `can instantiate`() {
        assertThat(command, instanceOf(Clone::class.java))
    }
}
