package hu.petrik.filcapp.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Announcement (
	val author: Author? = null,
	val authorId: String,
	val cohortIds: List<String> = emptyList(),
	val content: List<JsonElement> = emptyList(),
	val createdAt: String,
	val id: String,
	val title: String,
	val updatedAt: String,
	val validFrom: String,
	val validUntil: String,
)

@Serializable
data class AuditLog (
	val buttonPressed: Boolean,
	val cardData: String? = null,
	val cardId: String? = null,
	val deviceId: String,
	val id: Int,
	val result: Boolean,
	val timestamp: String,
	val userId: String? = null,
)

@Serializable
data class Author (
	val id: String,
	val image: String? = null,
	val name: String,
)

@Serializable
data class BlogPost (
	val author: Author? = null,
	val authorId: String,
	val content: List<JsonElement> = emptyList(),
	val createdAt: String,
	val id: String,
	val publishedAt: String? = null,
	val slug: String,
	val status: String,
	val title: String,
	val updatedAt: String,
)

@Serializable
data class Card (
	val authorizedDevices: List<Card_authorizedDevices> = emptyList(),
	val cardData: String,
	val createdAt: String,
	val enabled: Boolean,
	val frozen: Boolean,
	val id: String,
	val name: String,
	val owner: Card_owner? = null,
	val updatedAt: String,
	val userId: String? = null,
)

@Serializable
data class CardListResponse (
	val cards: List<Card> = emptyList(),
)

@Serializable
data class CardResponse (
	val card: Card,
)

@Serializable
data class Card_authorizedDevices (
	val id: String,
	val name: String,
)

@Serializable
data class Card_owner (
	val email: String,
	val id: String,
	val name: String,
	val nickname: String? = null,
)

@Serializable
data class Classroom (
	val buildingId: String? = null,
	val capacity: Int? = null,
	val id: String,
	val name: String,
	val short: String,
)

@Serializable
data class Cohort (
	val classroomIds: List<String> = emptyList(),
	val id: String,
	val name: String,
	val short: String,
	val teacherId: String? = null,
	val timetableId: String,
)

@Serializable
data class DayDefinition (
	val createdAt: String,
	val days: List<String> = emptyList(),
	val id: String,
	val name: String,
	val short: String,
	val updatedAt: String,
)

@Serializable
data class Device (
	val apiToken: String,
	val createdAt: String,
	val id: String,
	val lastResetReason: String? = null,
	val location: String? = null,
	val name: String,
	val updatedAt: String,
)

@Serializable
data class DeviceHealthStat (
	val debug: DeviceHealthStat_debug,
	val fwVersion: String,
	val id: Double,
	val ramFree: Double,
	val storage: DeviceHealthStat_storage,
	val timestamp: String,
	val uptime: Double,
)

@Serializable
data class DeviceHealthStat_debug (
	val deviceState: String,
	val errors: DeviceHealthStat_debug_errors,
	val lastResetReason: String,
)

@Serializable
data class DeviceHealthStat_debug_errors (
	val db: Boolean,
	val nfc: Boolean,
	val ota: Boolean,
	val sd: Boolean,
	val wifi: Boolean,
)

@Serializable
data class DeviceHealthStat_storage (
	val total: Double,
	val used: Double,
)

@Serializable
data class DeviceListResponse (
	val devices: List<Device> = emptyList(),
)

@Serializable
data class DeviceResponse (
	val device: Device,
)

@Serializable
data class DeviceStatsResponse (
	val stats: List<DeviceHealthStat> = emptyList(),
)

@Serializable
data class DoorlockActivationResponse (
	val log: AuditLog,
)

@Serializable
data class DoorlockLogEntry (
	val buttonPressed: Boolean,
	val card: DoorlockLogEntry_card? = null,
	val cardData: String? = null,
	val cardId: String? = null,
	val device: DoorlockLogEntry_device? = null,
	val deviceId: String,
	val id: Int,
	val owner: DoorlockLogEntry_owner? = null,
	val result: Boolean,
	val timestamp: String,
	val userId: String? = null,
)

@Serializable
data class DoorlockLogEntry_card (
	val id: String,
	val name: String,
)

@Serializable
data class DoorlockLogEntry_device (
	val id: String,
	val name: String,
)

@Serializable
data class DoorlockLogEntry_owner (
	val email: String,
	val id: String,
	val name: String,
	val nickname: String? = null,
)

@Serializable
data class DoorlockLogListResponse (
	val logs: List<DoorlockLogEntry> = emptyList(),
)

@Serializable
data class DoorlockStats (
	val doorOpenSeries: List<DoorlockStats_doorOpenSeries> = emptyList(),
	val topUsers: List<DoorlockStats_topUsers> = emptyList(),
	val totalCards: Int,
	val totalDevices: Int,
	val totalSuccessfulOpens: Int,
)

@Serializable
data class DoorlockStatsResponse (
	val stats: DoorlockStats,
)

@Serializable
data class DoorlockStats_doorOpenSeries (
	val count: Int,
	val date: String,
)

@Serializable
data class DoorlockStats_topUsers (
	val count: Int,
	val id: String,
	val name: String? = null,
	val nickname: String? = null,
)

@Serializable
data class DoorlockUser (
	val email: String,
	val id: String,
	val name: String,
	val nickname: String? = null,
)

@Serializable
data class DoorlockUserListResponse (
	val users: List<DoorlockUser> = emptyList(),
)

@Serializable
data class EnrichedLesson (
	val classrooms: List<Classroom> = emptyList(),
	val day: DayDefinition,
	val id: String,
	val period: Period? = null,
	val periodsPerWeek: Double,
	val subject: Subject? = null,
	val teachers: List<TeacherSummary> = emptyList(),
	val termDefinitionId: String? = null,
	val weeksDefinitionId: String,
)

@Serializable
data class MovedLesson (
	val date: String,
	val id: String,
	val room: String? = null,
	val startingDay: String? = null,
	val startingPeriod: String? = null,
)

@Serializable
data class MovedLessonWithRelations (
	val classroom: Classroom? = null,
	val dayDefinition: DayDefinition? = null,
	val lessons: List<String> = emptyList(),
	val movedLesson: MovedLesson,
	val period: Period? = null,
)

@Serializable
data class Period (
	val endTime: String,
	val id: String,
	val period: Double,
	val startTime: String,
)

@Serializable
data class PingResponse (
	val message: String,
)

@Serializable
data class Subject (
	val id: String,
	val name: String,
	val short: String,
)

@Serializable
data class Substitution (
	val date: String,
	val id: String,
	val substituter: String? = null,
)

@Serializable
data class SubstitutionWithRelations (
	val lessons: List<String> = emptyList(),
	val substitution: Substitution,
	val teacher: Teacher? = null,
)

@Serializable
data class SubstitutionsByCohort (
	val cohortId: String,
	val substitutions: List<SubstitutionWithRelations> = emptyList(),
)

@Serializable
data class SystemMessage (
	val author: Author? = null,
	val authorId: String,
	val cohortIds: List<String> = emptyList(),
	val content: List<JsonElement> = emptyList(),
	val createdAt: String,
	val id: String,
	val title: String,
	val updatedAt: String,
	val validFrom: String,
	val validUntil: String,
)

@Serializable
data class Teacher (
	val firstName: String,
	val gender: String? = null,
	val id: String,
	val lastName: String,
	val short: String,
	val userId: String? = null,
)

@Serializable
data class TeacherSummary (
	val id: String,
	val name: String,
	val short: String,
)

@Serializable
data class Timetable (
	val createdAt: String,
	val id: String,
	val name: String,
	val updatedAt: String,
	val validFrom: String? = null,
)

@Serializable
data class UptimeResponse (
	val pretty: String,
	val uptime_ms: Double,
)

@Serializable
data class User (
	val cohortId: String? = null,
	val createdAt: String,
	val displayName: String,
	val email: String,
	val emailVerified: Boolean,
	val id: String,
	val image: String? = null,
	val name: String,
	val nickname: String? = null,
	val permissions: List<String> = emptyList(),
	val roles: List<String> = emptyList(),
	val updatedAt: String,
)

@Serializable
data class UserListResponse (
	val total: Int,
	val users: List<User> = emptyList(),
)

