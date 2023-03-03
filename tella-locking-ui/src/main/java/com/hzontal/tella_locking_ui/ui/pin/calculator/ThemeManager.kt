package com.hzontal.tella_locking_ui.ui.pin.calculator

import android.content.Context
import com.hzontal.tella_locking_ui.R
import org.hzontal.shared_ui.pinview.CalculatorThemeStyle
import org.hzontal.shared_ui.utils.CalculatorTheme

object ThemeManager {

    fun getTheme(calculatorTheme: CalculatorTheme): CalculatorThemeStyle {
        return when (calculatorTheme) {
            CalculatorTheme.BLUE_SKIN -> {
                CalculatorThemeStyle(calculatorOperatorsBackgroundColor = R.color.calc_blue_skin_dark_blue,
                        calculatorNumbersBackgroundColor = R.color.calc_blue_skin_grey,
                        calculatorOkBtnBackgroundColor = R.color.calc_blue_skin_petrol_blue,
                        calculatorNumbersTextColor = R.color.wa_white,
                        calculatorOperatorsTextColor = R.color.wa_white)
            }
            CalculatorTheme.ORANGE_SKIN -> {
                CalculatorThemeStyle(
                        calculatorBackgroundDrawable = R.drawable.rounded_button_background,
                        calculatorOkBtnBackgroundColor = R.color.calc_orange_skin_orange,
                        calculatorNumbersTextColor = R.color.wa_black,
                        calculatorOperatorsTextColor = R.color.calc_orange_skin_orange,
                        calculatorOkBtnTextColor = R.color.wa_white,
                        calculatorOkBtnTextBackgroundDrawable = R.drawable.rounded_ok_button_background)

            }
            CalculatorTheme.YELLOW_SKIN -> {
                CalculatorThemeStyle(calculatorOperatorsBackgroundColor = R.color.wa_white,
                        calculatorNumbersBackgroundColor = R.color.wa_white,
                        calculatorBackgroundDrawable = R.drawable.light_button_background,
                        calculatorNumbersTextColor = R.color.wa_black,
                        calculatorOperatorsTextColor = R.color.calc_yellow_skin_yellow,
                        calculatorOkBtnTextBackgroundDrawable = R.drawable.yellow_button_background,
                        calculatorOkBtnTextColor = R.color.wa_black)


            }

            CalculatorTheme.GREEN_SKIN -> {
                CalculatorThemeStyle()
            }
            else -> {
                CalculatorThemeStyle()
            }
        }
    }
}