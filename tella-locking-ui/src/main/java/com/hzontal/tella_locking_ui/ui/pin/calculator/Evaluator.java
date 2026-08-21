/* Inspired by Boann answer https://stackoverflow.com/a/26227947 */

package com.hzontal.tella_locking_ui.ui.pin.calculator;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.TextUtils.substring;
import static java.lang.String.format;


public class Evaluator {

    public static String calculateResult(final String input) {
        String entry = handlePercentage(input);
        return evaluateResult(entry);
    }

    public static String evaluateResult(final String input) {
        String entry = input;
        entry = entry.replace('x', '*');
        entry = entry.replace('÷', '/');
        entry = entry.replace(',', '.');
        entry = entry.replace(" ", "");
        Double evaluation = eval(entry);
        String evaluationString = format("%s", evaluation);
        if (substring(
                evaluationString,
                evaluationString.length() - 2,
                evaluationString.length()
        ).equals(".0")
        ) {
            evaluationString = substring(evaluationString, 0, evaluationString.length() - 2);
        }
        if (evaluationString.length() > 10) {
            DecimalFormat sf = new DecimalFormat("0.#####E0");
            evaluationString = sf.format(evaluation);
        }
        return evaluationString;
    }

    public static String handlePercentage(final String input) {
        String inputS = input.replace(" ", "");
        inputS = inputS.replace('x', '*');
        //regular expression to find decimal

        Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?%)");
        Matcher matcher = regex.matcher(inputS);

            while (matcher.find()) {
                //last decimal in string up to char %
                String occurrence = matcher.group(1);
                // replacement for x% = x/100
                if (occurrence != null) {
                    String pctNum = occurrence.replace("%", "");
                    String pctReplace = evaluateResult(pctNum + "/100");
                    pctReplace = pctReplace.concat("*");
                    inputS = inputS.replace(occurrence, pctReplace);
                    inputS = inputS.replace("**", "*");
                } else {
                    break;
                }
            }

        return inputS;
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    switch (func) {
                        case "sqrt":
                            x = Math.sqrt(x);
                            break;
                        case "sin":
                            x = Math.sin(Math.toRadians(x));
                            break;
                        case "cos":
                            x = Math.cos(Math.toRadians(x));
                            break;
                        case "tan":
                            x = Math.tan(Math.toRadians(x));
                            break;
                        default:
                            throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}
