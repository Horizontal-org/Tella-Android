package com.hzontal.tella_locking_ui.ui.pin.base

interface OnSetPinClickListener {
    fun onSuccessSetPin(pin : String?)
    fun onFailureSetPin(error : String)
}