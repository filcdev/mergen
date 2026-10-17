package hu.petrik.filcapp.api.auth

class ApiKeyAuth(
    private val location: String,
    val paramName: String,
) : Authentication {
    var apiKey: String? = null
    var apiKeyPrefix: String? = null

    override fun apply(
        query: MutableMap<String, List<String>>,
        headers: MutableMap<String, String>,
    ) {
        val key: String = apiKey ?: return
        when (location) {
            "cookie" -> Unit
            "query" -> query[paramName] = listOf(key)
            "header" -> headers[paramName] = key
        }
    }
}
