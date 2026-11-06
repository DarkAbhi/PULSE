package dev.abhishekan.pulse.core.domain.usecase

object AccountValidation {
    // Go strings.TrimSpace uses Unicode White_Space, which differs from Java's defaults.
    fun trimmedName(value: String) =
        value.trim {
            it in '\u0009'..'\u000D' ||
                it in " \u0085\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u2028\u2029\u202F\u205F\u3000"
        }

    fun validName(value: String): Boolean {
        val name = trimmedName(value)
        return name.codePointCount(0, name.length) in 1..120
    }

    fun validPassword(
        current: String,
        next: String,
        confirmation: String,
    ) = current.isNotEmpty() && next.toByteArray(Charsets.UTF_8).size in 6..72 && next == confirmation
}
