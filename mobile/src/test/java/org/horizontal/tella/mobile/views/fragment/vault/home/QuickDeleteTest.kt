package org.horizontal.tella.mobile.views.fragment.vault.home

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickDeleteTest {
    @Test
    fun `failure does not stop remaining deletion or uninstall`() {
        val calls = mutableListOf<String>()

        runQuickDelete(
            steps = listOf(
                { calls += "vault" },
                {
                    calls += "servers"
                    throw IllegalStateException()
                },
                { calls += "forms" },
            ),
            finalSteps = listOf(
                { calls += "exit" },
                { calls += "reset" },
                { calls += "uninstall" },
            ),
            onError = {},
        )

        assertEquals(
            listOf("vault", "servers", "forms", "exit", "reset", "uninstall"),
            calls,
        )
    }
}
