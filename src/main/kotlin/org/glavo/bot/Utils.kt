package org.glavo.bot

import java.util.regex.Matcher
import java.util.regex.Pattern

inline fun Pattern.tryMatch(str: CharSequence, action: (Matcher) -> Unit) {
    val m = this.matcher(str)
    if (m.matches()) {
        action(m)
    }
}