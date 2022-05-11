package ch.hippmann.localizer.plugin.logging

internal inline fun verbose(t: Throwable? = null, message: () -> String) = log(LogPrefix.VERBOSE, t, message())
internal inline fun debug(t: Throwable? = null, message: () -> String) = log(LogPrefix.DEBUG, t, message())
internal inline fun info(t: Throwable? = null, message: () -> String) = log(LogPrefix.INFO, t, message())
internal inline fun warn(t: Throwable? = null, message: () -> String) = log(LogPrefix.WARN, t, message())
internal inline fun err(t: Throwable? = null, message: () -> String) = log(LogPrefix.ERR, t, message())
internal inline fun wtf(t: Throwable? = null, message: () -> String) = log(LogPrefix.WTF, t, message())


internal enum class LogPrefix {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERR,
    WTF,
}

private fun log(prefix: LogPrefix, t: Throwable? = null, message: String) {
    if (prefix >= LogPrefix.ERR) {
        System.err.println("${prefix.name}: $message")
    } else {
        println("${prefix.name}: $message")
    }

    t?.printStackTrace()
}
