package hu.petrik.filcapp.admin

import hu.petrik.filcapp.auth.AuthUser

/** Same capability names as @filcdev/api/permissions; roles are resolved by Chronos. */
object AdminPermissions {
    const val SUBSTITUTION_CREATE = "substitution:create"
    const val SUBSTITUTION_UPDATE = "substitution:update"
    const val SUBSTITUTION_DELETE = "substitution:delete"
}

fun List<String>?.can(permission: String): Boolean =
    this?.let { "*" in it || permission in it } == true

fun AuthUser.canManageSubstitutions(): Boolean =
    permissions.can(AdminPermissions.SUBSTITUTION_CREATE) ||
        permissions.can(AdminPermissions.SUBSTITUTION_UPDATE) ||
        permissions.can(AdminPermissions.SUBSTITUTION_DELETE)

/** Backend accepts a Date coerced from an ISO UTC timestamp, not a locale-specific date. */
fun substitutionIsoDate(localDate: String): String {
    val parsed = kotlinx.datetime.LocalDate.parse(localDate)
    require(parsed.toString() == localDate) { "Expected a date in YYYY-MM-DD format" }
    return "${parsed}T00:00:00.000Z"
}
