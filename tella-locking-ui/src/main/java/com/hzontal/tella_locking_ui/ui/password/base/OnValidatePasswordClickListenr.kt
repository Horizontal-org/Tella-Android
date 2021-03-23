package com.hzontal.tella_locking_ui.ui.password.base

interface OnValidatePasswordClickListener {
    fun onSuccessSetPassword(password : String)
    fun onFailureSetPassword(error : String)
}