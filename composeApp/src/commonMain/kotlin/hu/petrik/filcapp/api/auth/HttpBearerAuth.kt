package hu.petrik.filcapp.api.auth

class HttpBearerAuth(
    private val scheme: String?,
) : Authentication {
    var bearerToken: String? = null

    override fun apply(
        query: MutableMap<String, List<String>>,
        headers: MutableMap<String, String>,
    ) {
        val token: String = bearerToken ?: return
        headers["Authorization"] = (if (scheme != null) upperCaseBearer(scheme)!! + " " else "") + token
    }

    private fun upperCaseBearer(scheme: String): String? = if ("bearer".equals(scheme, ignoreCase = true)) "Bearer" else scheme
}
