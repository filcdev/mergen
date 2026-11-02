package hu.petrik.filcapp.admin

import hu.petrik.filcapp.auth.AuthUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminPermissionsTest {
    @Test
    fun permissionChecksMatchFilcWildcardAndExactNames() {
        assertFalse(null.can(AdminPermissions.SUBSTITUTION_CREATE))
        assertFalse(emptyList<String>().can(AdminPermissions.SUBSTITUTION_CREATE))
        assertTrue(listOf("*").can(AdminPermissions.SUBSTITUTION_DELETE))
        assertTrue(listOf("substitution:create").can(AdminPermissions.SUBSTITUTION_CREATE))
        assertFalse(listOf("substitution:create").can(AdminPermissions.SUBSTITUTION_UPDATE))
    }

    @Test
    fun adminMenuIncludesUsersWithAnyManagementCapability() {
        assertFalse(AuthUser().canManageSubstitutions())
        assertTrue(AuthUser(permissions = listOf("substitution:delete")).canManageSubstitutions())
    }

    @Test
    fun datesAreSerializedAtUtcMidnightWithoutShiftingLocalDay() {
        assertEquals("2026-11-03T00:00:00.000Z", substitutionIsoDate("2026-11-03"))
        assertFails { substitutionIsoDate("2026-02-30") }
    }
}
