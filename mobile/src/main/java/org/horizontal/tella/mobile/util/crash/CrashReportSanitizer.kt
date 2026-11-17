package org.horizontal.tella.mobile.util.crash

import org.horizontal.tella.mobile.domain.exception.NoConnectivityException
import retrofit2.HttpException
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException

/**
 * Removes user data (servers, files, emails, locations...) from exceptions before they go
 * to Crashlytics. Network errors are skipped: they come from the user's connection or
 * server, not from bugs in Tella.
 */
object CrashReportSanitizer {

    private const val MAX_CAUSE_DEPTH = 10
    private const val MAX_MESSAGE_LENGTH = 300

    private val expectedNetworkErrors = listOf(
        UnknownHostException::class.java,
        SocketException::class.java,
        SocketTimeoutException::class.java,
        SSLException::class.java,
        MalformedURLException::class.java,
        ProtocolException::class.java,
        UnknownServiceException::class.java,
        HttpException::class.java,
        NoConnectivityException::class.java,
    )

    // SQLite adds the full query, with its values, after "while compiling:"
    private val SQL = Regex("""(?is)(,\s*)?while compiling:.*""")
    private val URL = Regex("""\b[a-zA-Z][a-zA-Z0-9+.\-]*://[^\s"'<>]*""")
    private val EMAIL = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val DOUBLE_QUOTED = Regex(""""[^"]*"""")
    private val SINGLE_QUOTED = Regex("""'([^']*)'""")

    // Method/field names Android quotes in NPE messages, e.g. 'int java.lang.String.length()'
    private val CODE_SIGNATURE = Regex(
        """(?:void|boolean|byte|char|short|int|long|float|double|[\w$]+(?:\.[\w$]+)+)(?:\[])*""" +
            """ (?:[\w$]+\.)+[\w$<>]+(?:\([\w$.\[\], ]*\))?"""
    )

    private val PATH = Regex("""(?<![\w.:])/(?:[^\s/:'"]+/)+[^\s:'",)]*""")
    private val UUID = Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b""")
    private val IPV6 = Regex("""(?<![\w:])[0-9a-fA-F]{0,4}(?::[0-9a-fA-F]{0,4}){2,7}(?![\w:])""")
    private val IPV4 = Regex("""\b\d{1,3}(?:\.\d{1,3}){3}\b""")

    // GPS coordinates
    private val PRECISE_DECIMAL = Regex("""-?\b\d{1,3}\.\d{4,}\b""")
    private val FILE_NAME = Regex(
        """[^\s/'"<>:]+\.(?i:jpe?g|png|gif|bmp|heic|heif|webp|mp4|3gp|mov|mkv|webm|avi|mp3|m4a|aac|ogg|opus|wav|amr|flac|pdf|docx?|xlsx?|pptx?|odt|csv|txt|rtf|zip|apk|xml|json|tmp|enc)\b"""
    )

    // Lowercase domains; class names like java.lang.String are kept
    private val HOST = Regex("""(?<![\w.$-])(?:[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}(?![\w$-]|\.[\w$])""")

    private val LONG_NUMBER = Regex("""\b\d{9,}\b""")
    private val TOKEN = Regex("""(?<![\w+/=\-])[A-Za-z0-9+/=_\-]{32,}(?![\w+/=\-])""")

    fun isExpectedNetworkError(throwable: Throwable): Boolean =
        causeChain(throwable).any { t -> expectedNetworkErrors.any { it.isInstance(t) } }

    /** Returns null when the error should not be reported. */
    fun sanitizeNonFatal(throwable: Throwable): Throwable? =
        if (isExpectedNetworkError(throwable)) null else sanitize(throwable)

    /** Returns the original if there is nothing to remove, so Crashlytics keeps grouping it the same way. */
    fun sanitize(throwable: Throwable): Throwable {
        val chain = causeChain(throwable).toList()
        val unchanged = chain.all { t ->
            t.suppressed.isEmpty() && (t.message == null || scrubMessage(t.message!!) == t.message)
        }
        return if (unchanged && chain.last().cause == null) throwable else redactChain(chain)
    }

    fun scrubMessage(message: String): String {
        var text = message
        text = SQL.replace(text) { "${it.groupValues[1]}while compiling: <sql>" }
        text = URL.replace(text, "<url>")
        text = EMAIL.replace(text, "<email>")
        text = DOUBLE_QUOTED.replace(text, "\"<redacted>\"")
        text = SINGLE_QUOTED.replace(text) {
            if (CODE_SIGNATURE.matches(it.groupValues[1])) it.value else "'<redacted>'"
        }
        text = PATH.replace(text, "<path>")
        text = UUID.replace(text, "<id>")
        text = IPV6.replace(text, "<ip>")
        text = IPV4.replace(text, "<ip>")
        text = PRECISE_DECIMAL.replace(text, "<number>")
        text = FILE_NAME.replace(text, "<file>")
        text = HOST.replace(text, "<host>")
        text = LONG_NUMBER.replace(text, "<number>")
        // Needs a digit so long class names aren't treated as tokens
        text = TOKEN.replace(text) { if (it.value.any(Char::isDigit)) "<token>" else it.value }
        if (text.length > MAX_MESSAGE_LENGTH) text = text.take(MAX_MESSAGE_LENGTH) + "…"
        return text
    }

    private fun redactChain(chain: List<Throwable>): Throwable {
        var cause: Throwable? = null
        for (t in chain.asReversed()) {
            val message = t.message?.let { scrubMessage(it) }
            val description = if (message.isNullOrEmpty()) t.javaClass.name else "${t.javaClass.name}: $message"
            cause = RedactedException(description, cause).also { it.stackTrace = t.stackTrace }
        }
        return cause!!
    }

    private fun causeChain(throwable: Throwable): Sequence<Throwable> {
        val seen = HashSet<Throwable>()
        return generateSequence(throwable) { it.cause }
            .take(MAX_CAUSE_DEPTH + 1)
            .takeWhile { seen.add(it) }
    }
}

/** Copy of an exception with its message cleaned. The message starts with the original class name. */
class RedactedException(description: String, cause: Throwable?) : Exception(description, cause)
