package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data

import rs.readahead.washington.mobile.domain.entity.MediaFile

object  MockVaultFiles {

    fun getListVaultFiles() : List<VaultFile>{
        return listOf(
            VaultFile(id = 0,type = MediaFile.newJpeg().type,path = "test/test",size = 1000L,mimeType = "/jpeg",created = 1000L,
                hash = "nkjevkjkfner",name ="image 1",duration = 100L,anonymous = true,metadata = null,thumb = null),
            VaultFile(id = 1,type = MediaFile.newMp4().type,path = "test/test",size = 1000L,mimeType = "/mp4",created = 1000L,
                hash = "nkjevkjkfner",name ="video 1",duration = 100L,anonymous = true,metadata = null,thumb = null),
            VaultFile(id = 3,type = MediaFile.newAac().type,path = "test/test",size = 1000L,mimeType = "/mp3",created = 1000L,
                hash = "nkjevkjkfner",name ="audio 1",duration = 100L,anonymous = true,metadata = null,thumb = null),
            VaultFile(id = 4,type = MediaFile.newJpeg().type,path = "test/test",size = 1000L,mimeType = "/jpeg",created = 1000L,
                hash = "nkjevkjkfner",name ="image 2",duration = 100L,anonymous = true,metadata = null,thumb = null)
        )
    }
}