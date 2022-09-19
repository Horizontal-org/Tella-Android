package org.cleaninsights.sdk

import java.net.URL

interface DebugHandler {
    fun debug(message: String)
}

interface DoneHandler {
    fun done(e: Exception?)
}

/**
 * The store holds the user's consents to the different `Feature`s and `Campaign`s, and their `Visit`
 * and `Event` measurements.
 *
 * If you want to implement your own persistence of the store (e.g. because you want to write it in a
 * database instead of the file system) and your own implementation of the transmission to the
 * Matomo/CIMP backend (e.g. because you want to tunnel the requests through a proxy or add your own
 * encryption layer), then create a subclass of this class and and implement the `#init`, `#persist`
 * and `#send` methods.
 *
 * If you only want to change either one or the other, you can use `DefaultStore` as a base and work
 * from there.
 *
 * This is a more Java-friendly implementation of `Store`.
 *
 * @param args
 *      Optional arguments your implementation might need for loading the store.
 * @param debug
 *      Optional callback handler to output debug messages.
 */
abstract class JavaStore @JvmOverloads constructor(args: Map<String, Any> = HashMap(), debug: DebugHandler? = null)
    : Store(args, fun(message){ debug?.debug(message); }) {

    /**
     * This method gets called, when the SDK is of the opinion, that persisting
     * the `Store` is in order.
     *
     * This is partly controlled by the `Configuration.persistEveryNTimes` configuration
     * option, and by calls to `CleanInsights#persist`.
     *
     * If possible, try to honor the `async` flag:
     * If it's true, it is set so, because the SDK wants to reduce impact on user
     * responsivity as much as possible.
     * If it's false, the SDK wants to make sure, that the call finishes before
     * the app gets killed.
     *
     * @param async
     *      Indicates, if the persistence should be done asynchronously or synchronously.
     *      E.g. a persist call during the exit of an app should be done synchronously,
     *      otherwise the operation might never get finished because the OS kills the
     *      server too early.
     * @param done
     *      Callback, when the operation is finished, either successfully or not.
     *      Be aware that this might get called on different threads.
     *      If no error is returned, the operation is considered successful and the
     *      internal counter will be set back again.
     */
    abstract fun persist(async: Boolean, done: DoneHandler)

    override fun persist(async: Boolean, done: (e: Exception?) -> Unit) {
        persist(async, object : DoneHandler {
            override fun done(e: Exception?) {
                done(e)
            }
        })
    }

    /**
     * This method gets called, when the SDK gathered enough data for a
     * time period and is ready to send the data to a CIMP (CleanInsights Matomo Proxy).
     *
     * @param data
     *      The serialized JSON for a POST request to a CIMP.
     * @param server
     *      The server URL from `Configuration.server`.
     * @param timeout
     *      The timeout in seconds from `Configuration.timeout`.
     * @param done
     *      Callback, when the operation is finished, either successfully or not.
     *      Be aware that this might get called on different threads.
     *      If no error is returned, the data sent will be removed from the
     *      store and the store persisted.
     */
    abstract fun send(data: String, server: URL, timeout: Double, done: DoneHandler)

    override fun send(data: String, server: URL, timeout: Double, done: (e: Exception?) -> Unit) {
        send(data, server, timeout, object : DoneHandler {
            override fun done(e: Exception?) {
                done(e)
            }
        })
    }
}