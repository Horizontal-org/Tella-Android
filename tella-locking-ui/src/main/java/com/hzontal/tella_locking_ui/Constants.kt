package com.hzontal.tella_locking_ui

enum class ReturnActivity (val order: Int,  val activityName: String) {
    SETTINGS(1, "rs.readahead.washington.mobile.views.activity.onboarding.OnBoardingActivity"),
    CAMOUFLAGE(2,"rs.readahead.washington.mobile.views.activity.SettingsActivity");

    open fun getActivity(): String {
        return activityName
    }

    open fun getActivityOrder(): Int {
        return order
    }
}

const val RETURN_ACTIVITY = "RETURN_ACTIVITY"

const val IS_FROM_SETTINGS = "IS_FROM_SETTINGS"

const val IS_ONBOARD_LOCK_SET = "IS_ONBOARD_LOCK_SET"

const val IS_CAMOUFLAGE = "IS_CAMOUFLAGE"

const val CALCULATOR_ALIAS = "rs.readahead.washington.mobile.views.activity.AliasCalculator"

const val FINISH_ACTIVITY_REQUEST_CODE = 123
