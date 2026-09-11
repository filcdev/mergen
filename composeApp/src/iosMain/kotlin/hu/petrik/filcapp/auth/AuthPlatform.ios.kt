package hu.petrik.filcapp.auth

import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication

actual fun openExternalUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

actual fun loadStoredAuthCookie(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("mergen_session_cookie")

actual fun saveStoredAuthCookie(cookie: String?) {
    if (cookie == null) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey("mergen_session_cookie")
    } else {
        NSUserDefaults.standardUserDefaults.setObject(cookie, forKey = "mergen_session_cookie")
    }
}
