package org.cyoda.example.webserver.conf

data class CookieConfig(
    val cookieEncryptKey:String?,
    val cookieSignKey:String?
)