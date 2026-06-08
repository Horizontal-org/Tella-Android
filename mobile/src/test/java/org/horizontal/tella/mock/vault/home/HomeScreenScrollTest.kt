package org.horizontal.tella.mock.vault.home

import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.views.fragment.vault.home.HomeScreenScroll
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class HomeScreenScrollTest {

    @Mock
    private lateinit var recyclerView: RecyclerView

    @Test
    fun homeScrollPosition_isAlwaysZero() {
        assertEquals(0, HomeScreenScroll.HOME_SCROLL_POSITION)
    }

    @Test
    fun scrollToTop_postsRunnableThatScrollsToHomePosition() {
        val runnableCaptor = ArgumentCaptor.forClass(Runnable::class.java)

        HomeScreenScroll.scrollToTop(recyclerView)

        verify(recyclerView).post(runnableCaptor.capture())
        runnableCaptor.value.run()
        verify(recyclerView).scrollToPosition(HomeScreenScroll.HOME_SCROLL_POSITION)
    }

    @Test
    fun scrollToTop_doesNothingWhenRecyclerViewIsNull() {
        HomeScreenScroll.scrollToTop(null)
        verify(recyclerView, never()).post(any())
    }
}
