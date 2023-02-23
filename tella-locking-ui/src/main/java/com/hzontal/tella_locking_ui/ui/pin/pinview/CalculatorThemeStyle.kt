package com.hzontal.tella_locking_ui.ui.pin.pinview

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class CalculatorThemeStyle(@ColorRes val calculatorOperatorsBackgroundColor: Int = -1,
                                @ColorRes val calculatorNumbersBackgroundColor: Int = -1,
                                @DrawableRes val calculatorBackgroundDrawable: Int = -1,
                                @ColorRes val calculatorOkBtnBackgroundColor: Int = -1,
                                @ColorRes val calculatorNumbersTextColor: Int = -1,
                                @ColorRes val calculatorOperatorsTextColor: Int = -1,
                                @DrawableRes val calculatorOkBtnTextBackgroundDrawable: Int = -1,
                                @ColorRes val calculatorOkBtnTextColor: Int = -1


)
