package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data

import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.data.entity.XFormEntity

object  MockVaultFiles {

    fun getListVaultFiles() : List<VaultFile>{
        return listOf()
    }
    fun getRootFile() = VaultFile()


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