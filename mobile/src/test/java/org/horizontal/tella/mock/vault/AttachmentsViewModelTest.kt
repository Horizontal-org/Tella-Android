package org.horizontal.tella.mock.vault

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import com.hzontal.tella_vault.rx.RxVault
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.views.fragment.vault.attachements.AttachmentsViewModel

class AttachmentsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AttachmentsViewModel

    @Mock
    private lateinit var keyDataSource: KeyDataSource

    @Mock
    private lateinit var filesDataObserver: Observer<List<VaultFile?>>

    @Mock
    private lateinit var filesSizeObserver: Observer<Int>

    @Mock
    private lateinit var deletedFilesObserver : Observer<Int>

    @Mock
    private lateinit var deletedFileObserver : Observer<VaultFile>

    @Mock
    lateinit var rxVault: RxVault

    @Mock
    private lateinit var firebaseCrashlytics: FirebaseCrashlytics

    @Before
    fun setUp() {
        openMocks(this)
        val application: Application = mock(Application::class.java)
        `when`(application.applicationContext).thenReturn(mock(Context::class.java))

        // Create a mock instance of RxVault
        MyApplication.rxVault = rxVault

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
        viewModel.filesSize.observeForever(filesSizeObserver)
        viewModel.deletedFiles.observeForever(deletedFilesObserver)
        viewModel.deletedFile.observeForever(deletedFileObserver)

    }

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setNewThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun tearDown() {
        RxAndroidPlugins.reset()
        viewModel.filesData.removeObserver(filesDataObserver)
        viewModel.filesSize.removeObserver(filesSizeObserver)
        viewModel.deletedFiles.removeObserver(deletedFilesObserver)
        viewModel.deletedFile.removeObserver(deletedFileObserver)
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
        val testObserver = object : Observer<List<VaultFile?>> {
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
    fun `test moveFiles when moveFile returns Single True`() {
        val parentId = "11223344-5566-4777-8899-aabbccddeeff"
        val mockVaultFile = mock(VaultFile::class.java)

        `when`(rxVault.move(mockVaultFile, parentId)).thenReturn(Single.just(true))

        // Act
        viewModel.moveFiles(parentId, listOf(mockVaultFile))

        // Assert
        // Verify that _filesSize has been set to 1
        verify(filesSizeObserver).onChanged(1)
    }


    @Test
    fun `test moveFiles when parentId is null`() {
        viewModel.moveFiles(null, listOf(VaultFile()))
        // Verify that no interactions with firebaseCrashlytics occurred
        Mockito.verifyNoInteractions(firebaseCrashlytics)
    }

    @Test
    fun `test moveFiles when vaultFiles is null`() {
        val parentId = "11223344-5566-4777-8899-aabbccddeeff"

        viewModel.moveFiles(parentId, null)
        // Verify that no interactions with firebaseCrashlytics occurred
        Mockito.verifyNoInteractions(firebaseCrashlytics)
    }

    @Test
    fun `should post value to _deletedFile LiveData when deleteVaultFile is called and deletion is successful`(){
       val mockVaultFile = mock(VaultFile::class.java)
       `when`(rxVault.delete(mockVaultFile)).thenReturn(Single.just(true))

        //Act
        viewModel.deleteVaultFile(mockVaultFile)

        // Assert
        verify(deletedFileObserver).onChanged(mockVaultFile)

    }

}