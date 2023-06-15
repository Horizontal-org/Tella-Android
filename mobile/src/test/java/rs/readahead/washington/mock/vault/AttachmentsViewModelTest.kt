package rs.readahead.washington.mock.vault

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.tella_vault.rx.RxVault
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import junit.framework.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.openMocks
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.views.fragment.vault.attachements.AttachmentsViewModel
import java.util.concurrent.CountDownLatch
import io.reactivex.rxjava3.observers.TestObserver

class AttachmentsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    private lateinit var viewModel: AttachmentsViewModel

    @Mock
    private lateinit var keyDataSource: KeyDataSource

    @Mock
    private lateinit var filesDataObserver: androidx.lifecycle.Observer<List<VaultFile?>>

    @Mock
    private lateinit var rxVault: RxVault


    @Before
    fun setUp() {
        openMocks(this)
        val application: Application = mock(Application::class.java)
        `when`(application.applicationContext).thenReturn(mock(Context::class.java))

        // Create a mock instance of RxVault
        val rxVault: RxVault = mock(RxVault::class.java)

        // Mock the behavior of rxVault.get() and rxVault.list()
        `when`(rxVault.get(anyString())).thenReturn(Single.just(mock(VaultFile::class.java)))
        `when`(rxVault.list(any(), any(), any(), any())).thenReturn(
            Single.just(
                listOf(
                    mock(
                        VaultFile::class.java
                    )
                )
            )
        )

        // Create an instance of AttachmentsViewModel with the mocked dependencies
        viewModel = AttachmentsViewModel(application, keyDataSource, rxVault)

        // Set the filesDataObserver on the LiveData to capture the emitted data
        viewModel.filesData.observeForever(filesDataObserver)

        // Initialize RxAndroid schedulers
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }


    @After
    fun tearDown() {
        RxAndroidPlugins.reset()
        viewModel.filesData.removeObserver(filesDataObserver)
    }

    @Test
    fun `getFiles should update filesData with the expected value`() {
        // Arrange
        val parent = "11223344-5566-4777-8899-aabbccddeeff"
        val sort = Sort().apply {
            direction = Sort.Direction.ASC
            type = Sort.Type.NAME
        }
        val expectedFilesData = listOf(mock(VaultFile::class.java))

        // Act
        val testObserver = object : androidx.lifecycle.Observer<List<VaultFile?>> {
            override fun onChanged(filesData: List<VaultFile?>) {
                // Assert
                assertEquals(expectedFilesData, filesData)
                assertEquals(expectedFilesData, viewModel.filesData.value)

                // Clean up
                viewModel.filesData.removeObserver(this)
            }
        }
        viewModel.filesData.observeForever(testObserver)
        viewModel.getFiles(parent, FilterType.ALL, sort)

        // Clean up
        viewModel.filesData.removeObserver(testObserver)
    }

    @Test
    fun `getFiles should sort files in ascending order`() {
        // Arrange
        val parent = "11223344-5566-4777-8899-aabbccddeeff"
        val sort = Sort().apply {
            direction = Sort.Direction.ASC
            type = Sort.Type.NAME
        }

        // Create a list of VaultFiles in an ascending order
        val ascendingFiles = listOf(
            mock(VaultFile::class.java),
            mock(VaultFile::class.java),
            mock(VaultFile::class.java)
        ).apply {
            // Set different names to ensure ordering
            get(0).name = "File A"
            get(1).name = "File B"
            get(2).name = "File C"
        }

        // Mock the RxVault.list() call to return the ascendingFiles
        `when`(rxVault.get(anyString())).thenReturn(Single.just(mock(VaultFile::class.java)))
        `when`(rxVault.list(any(), any(), any(), any())).thenReturn(Single.just(ascendingFiles))

        // Create a new instance of AttachmentsViewModel

        // Create a CountDownLatch with a count of 1
        val latch = CountDownLatch(1)

        // Set up the filesDataObserver to capture the emitted data
        val filesDataObserver =
            androidx.lifecycle.Observer<List<VaultFile?>> { filesData -> // Assert
                assertNotNull(filesData)
                assertEquals(3, filesData.size)
                assertEquals("File A", filesData[0]?.name)
                assertEquals("File B", filesData[1]?.name)
                assertEquals("File C", filesData[2]?.name)

                // Release the latch to signal that the data has been emitted
                latch.countDown()
            }
        viewModel.filesData.observeForever(filesDataObserver)

        // Act
        val testObserver = viewModel.filesData.test()
        viewModel.getFiles(parent, FilterType.ALL, sort)
        testObserver.assertValueCount(3)

        // Clean up
        viewModel.filesData.removeObserver(filesDataObserver)
    }


}