package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data

import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.domain.entity.MediaFile

object  MockVaultFiles {

    fun getListVaultFiles() : List<VaultFile>{
        return listOf(
            VaultFile(id = 1,type = MediaFile.newJpeg().type,path = "test/test",size = 1000L,mimeType = "/jpeg",created = 1000L,
                hash = "nkjevkjkfner",name ="image 1",duration = 100L,anonymous = true,metadata = null,thumb = null),
            VaultFile(id = 2,type = MediaFile.newMp4().type,path = "test/test",size = 1000L,mimeType = "/mp4",created = 1000L,
                hash = "nkjevkjkfner",name ="video 1",duration = 100L,anonymous = true,metadata = null,thumb = null),
            VaultFile(id = 3,type = MediaFile.newAac().type,path = "test/test",size = 1000L,mimeType = "/mp3",created = 1000L,
                hash = "nkjevkjkfner",name ="audio 1",duration = 100L,anonymous = true,metadata = null,thumb = null),
            VaultFile(id = 4,type = MediaFile.newJpeg().type,path = "test/test",size = 1000L,mimeType = "/jpeg",created = 1000L,
                hash = "nkjevkjkfner",name ="image 2",duration = 100L,anonymous = true,metadata = null,thumb = null)
        )
    }
    fun getRootFile() = VaultFile(id = 0,type = MediaFile.newJpeg().type,path = "test/test",size = 1000L,mimeType = "/jpeg",created = 1000L,
            hash = "nkjevkjkfner",name ="image 1",duration = 100L,anonymous = true,metadata = null,thumb = null)


    fun getListForms()  : List<XFormEntity> {
        val form = XFormEntity()
        form.formID = "11"
        form.name = "form 1"
        val form1 = XFormEntity()
        form1.formID = "12"
        form1.name = "form 2"
        val form3 = XFormEntity()
        form3.formID = "13"
        form3.name = "form 3"
        return listOf(form,form1,form3)
    }

}