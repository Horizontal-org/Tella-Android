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
const val CALCULATOR_ALIAS_BLUE_SKIN = "rs.readahead.washington.mobile.views.activity.AliasCalculatorBlueSkin"
const val CALCULATOR_ALIAS_ORANGE_SKIN = "rs.readahead.washington.mobile.views.activity.AliasCalculatorOrangeSkin"
const val CALCULATOR_ALIAS_YELLOW_SKIN = "rs.readahead.washington.mobile.views.activity.AliasCalculatorYellowSkin"

const val CALCULATOR_YELLOW_SKIN = "YELLOW_SKIN"
const val CALCULATOR_ORANGE_SKIN = "ORANGE_SKIN"
const val CALCULATOR_BLUE_SKIN = "BLUE_SKIN"

const val CALC_ALIAS_GREEN_SKIN = "Calculator"
const val CALC_ALIAS_YELLOW_SKIN = "CalculatorYellowSkin"
const val CALC_ALIAS_ORANGE_SKIN = "CalculatorOrangeSkin"
const val CALC_ALIAS_BLUE_SKIN = "CalculatorBlueSkin"

const val FINISH_ACTIVITY_REQUEST_CODE = 123
