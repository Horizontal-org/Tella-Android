package rs.readahead.washington.mobile.util;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.MatchResult;


public class StringUtils {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String hexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static String append(@NonNull Character separator, String base, @NonNull String appendage) {
        if (TextUtils.isEmpty(appendage)) {
            return base;
        }

        if (TextUtils.isEmpty(base)) {
            return appendage;
        }

        StringBuilder appended = new StringBuilder(base);
        if (base.charAt(base.length() - 1) != separator && appendage.charAt(0) != separator) {
            appended.append(separator);
        }
        appended.append(appendage);

        return appended.toString();
    }

    public static String getFileSize(long size) {
        if (size <= 0) {
            return "0";
        }

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };

        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String orDefault(final String str, final String def) {
        return TextUtils.isEmpty(str) ? def : str;
    }

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static String first(@NonNull String string, int n) {
        return string.substring(0, Math.min(string.length(), n));
    }

    public static boolean isTextEqual(@Nullable String str1, @Nullable String str2) {
        return TextUtils.isEmpty(str1) ? TextUtils.isEmpty(str2) : str1.equals(str2);
    }

    public static String join(String delimiter, List<String> strings) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }

            builder.append("[").append(strings.get(i)).append("]");
        }

        return builder.toString();
    }

    private static String markdownToHtml(String text) {
        text = text.replaceAll("<([^a-zA-Z/])", "&lt;$1");
        // https://github.com/enketo/enketo-transformer/blob/master/src/markdown.js

        // span - replaced &lt; and &gt; with <>
        text = ReplaceCallback.replace("(?s)<\\s?span([^\\/\n]*)>((?:(?!<\\/).)+)<\\/\\s?span\\s?>", text, createSpan);
        // strong
        text = text.replaceAll("(?s)__(.*?)__", "<strong>$1</strong>");
        text = text.replaceAll("(?s)\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        // emphasis
        text = text.replaceAll("(?s)_([^\\s][^_\n]*)_", "<em>$1</em>");
        text = text.replaceAll("(?s)\\*([^\\s][^\\*\n]*)\\*", "<em>$1</em>");
        // links
        text = text.replaceAll("(?s)\\[([^\\]]*)\\]\\(([^\\)]+)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");
        // headers - requires ^ or breaks <font color="#f58a1f">color</font>
        text = ReplaceCallback.replace("(?s)^(#+)([^\n]*)$", text, createHeader);
        // paragraphs
        text = ReplaceCallback.replace("(?s)([^\n]+)\n", text, createParagraph);

        return text;
    }


    public static CharSequence markdownToSpanned(String text) {
        if (text == null) {
            return null;
        }

        return Html.fromHtml(markdownToHtml(text));
    }

    private static ReplaceCallback.Callback createHeader = match -> {
        int level = match.group(1).length();
        return "<h" + level + ">" + match.group(2).replaceAll("#+$", "").trim() + "</h" + level
                + ">";
    };

    private static ReplaceCallback.Callback createParagraph = match -> {
        String trimmed = match.group(1).trim();
        if (trimmed.matches("(?i)^<\\/?(h|p|bl)")) {
            return match.group(1);
        }
        return "<p>" + trimmed + "</p>";
    };

    private static ReplaceCallback.Callback createSpan = new ReplaceCallback.Callback() {
        public String matchFound(MatchResult match) {
            String attributes = sanitizeAttributes(match.group(1));
            return "<font" + attributes + ">" + match.group(2).trim() + "</font>";
        }

        // throw away all styles except for color and font-family
        private String sanitizeAttributes(String attributes) {

            String stylesText = attributes.replaceAll("style=[\"'](.*?)[\"']", "$1");
            String[] styles = stylesText.trim().split(";");
            StringBuffer stylesOutput = new StringBuffer();

            for (String style : styles) {
                String[] stylesAttributes = style.trim().split(":");
                if (stylesAttributes[0].equals("color")) {
                    stylesOutput.append(" color=\"" + stylesAttributes[1] + "\"");
                }
                if (stylesAttributes[0].equals("font-family")) {
                    stylesOutput.append(" face=\"" + stylesAttributes[1] + "\"");
                }
            }

            return stylesOutput.toString();
        }
    };

    public static String capitalize(@NonNull String string, Locale locale) {
        return string.substring(0, 1).toUpperCase(locale) + string.substring(1);
    }

    private static class URLSpanNoUnderline extends URLSpan {
        URLSpanNoUnderline(String url) {
            super(url);
        }
        @Override public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

    public static void stripUnderlines(TextView textView) {
        Spannable s = (Spannable)textView.getText();
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span: spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
            s.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(s);
    }

    private StringUtils() {
    }
}
