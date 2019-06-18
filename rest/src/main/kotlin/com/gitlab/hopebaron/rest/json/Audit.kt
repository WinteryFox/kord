package com.gitlab.hopebaron.rest.json

import com.gitlab.hopebaron.common.entity.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.IntDescriptor
import kotlinx.serialization.internal.NullableSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import mu.KotlinLogging

private val auditLogger = KotlinLogging.logger { }

@Serializable
data class AuditLog(
        val webhooks: List<Webhook>,
        val users: List<User>,
        @SerialName("audit_log_entries")
        val auditLogEntries: List<AuditLogEntry>
)

@Serializable
data class AuditLogEntry(
        @SerialName("target_id")
        val targetId: String? = null,
        val changes: List<AuditLogChange<*>>? = null,
        @SerialName("user_id")
        val userId: Snowflake,
        val id: Snowflake,
        @SerialName("action_type")
        val actionType: AuditLogEvent,
        val options: AuditEntryInfo? = null,
        val reason: String? = null
)

@Serializable(with = AuditLogChange.AuditLogChangeSerializer::class)
data class AuditLogChange<T>(
        val newValue: T?,
        val oldValue: T?,
        val key: String
) {

    @Serializer(forClass = AuditLogChange::class)
    companion object AuditLogChangeSerializer : KSerializer<AuditLogChange<*>> {
        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("AuditLogChange") {
            init {
                addElement("new_value", true)
                addElement("old_value", true)
                addElement("key", true)
            }
        }

        @UnstableDefault
        override fun deserialize(decoder: Decoder): AuditLogChange<*> {
            var newValue: JsonElement? = null
            var oldValue: JsonElement? = null
            lateinit var key: String

            with(decoder.beginStructure(descriptor)) {
                loop@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        CompositeDecoder.READ_DONE -> break@loop
                        0 -> newValue = decodeSerializableElement(descriptor, index, JsonElement.serializer())
                        1 -> oldValue = decodeSerializableElement(descriptor, index, JsonElement.serializer())
                        3 -> key = decodeStringElement(descriptor, index)
                    }
                }
                val serializer = key.asSerializer()

                val actualNewValue: Any? = serializer?.let {
                    if (newValue != null) Json.nonstrict.fromJson(NullableSerializer(it), newValue as JsonElement)
                } ?: newValue

                val actualOldValue: Any? = serializer?.let {
                    if (oldValue != null) Json.nonstrict.fromJson(NullableSerializer(it), oldValue as JsonElement)

                } ?: oldValue

                endStructure(descriptor)

                return AuditLogChange(actualNewValue, actualOldValue, key)
            }
        }

        override fun serialize(encoder: Encoder, obj: AuditLogChange<*>) {
            TODO("not implemented")
        }

        private fun String.asSerializer(): KSerializer<out Any>? = when (this) {
            "name", "icon_hash", "splash_hash" -> String.serializer()
            "owner_id" -> Snowflake.Companion
            "region" -> String.serializer()
            "afk_channel_id" -> Snowflake.serializer()
            "afk_timeout" -> Int.serializer()
            "mfa_level" -> MFALevel.MFALevelSerializer
            "verification_level" -> VerificationLevel.VerificationLevelSerializer
            "explicit_content_filter" -> ExplicitContentFilter.ExplicitContentFilterSerializer
            "default_message_notifications" -> DefaultMessageNotificationLevel.DefaultMessageNotificationLevelSerializer
            "vanity_url_code" -> String.serializer()
            "\$add", "\$remove" -> ArrayListSerializer(Role.serializer())
            "prune_delete_days" -> Int.serializer()
            "widget_enabled" -> Boolean.serializer()
            "widget_channel_id" -> Snowflake.serializer()
            "position" -> Int.serializer()
            "topic" -> String.serializer()
            "bitrate" -> Int.serializer()
            "permission_overwrites" -> ArrayListSerializer(Overwrite.serializer())
            "nsfw" -> Boolean.serializer()
            "application_id" -> Snowflake.serializer()
            "permissions" -> Permissions.serializer()
            "color" -> Int.serializer()
            "hoist" -> Boolean.serializer()
            "mentionable" -> Boolean.serializer()
            "allow" -> Permissions.serializer()
            "deny" -> Permissions.serializer()
            "code" -> String.serializer()
            "channel_id", "inviter_id" -> Snowflake.serializer()
            "max_uses", "uses", "max_age" -> Int.serializer()
            "temporary", "deaf", "mute" -> Boolean.serializer()
            "nick" -> String.serializer()
            "avatar_hash" -> String.serializer()
            "id" -> Snowflake.serializer()
            "type" -> null // TODO fix mixed type int|string

            else -> {
                auditLogger.warn { "unknown audit log key $this" }
                null
            }
        }

    }

}

@Serializable(with = AuditLogEvent.AuditLogEventSerializer::class)
enum class AuditLogEvent(val code: Int) {
    GuildUpdate(1),
    ChannelCreate(10),
    ChannelUpdate(11),
    ChannelDelete(12),
    ChannelOverwriteCreate(13),
    ChannelOverwriteUpdate(14),
    ChannelOverwriteDelete(15),

    MemberKick(15),
    MemberPrune(21),
    MemberBanAdd(22),
    MemberBanRemove(23),
    MemberUpdate(24),
    MemberRoleUpdate(25),

    RoleCreate(30),
    RoleUpdate(31),
    RoleDelete(32),

    InviteCreate(40),
    InviteUpdate(41),
    InviteDelete(42),

    WebhookCreate(50),
    WebhookDelete(51),

    EmojiCreate(60),
    EmojiUpdate(61),
    EmojiDelete(62),

    MessageDelete(72);

    @Serializer(forClass = AuditLogEvent::class)
    companion object AuditLogEventSerializer : KSerializer<AuditLogEvent> {
        override val descriptor: SerialDescriptor = IntDescriptor.withName("AuditLogEvent")

        override fun deserialize(decoder: Decoder): AuditLogEvent {
            val code = decoder.decodeInt()

            return values().first { it.code == code }
        }

        override fun serialize(encoder: Encoder, obj: AuditLogEvent) {
            encoder.encodeInt(obj.code)
        }

    }
}

@Serializable
data class AuditEntryInfo(
        @SerialName("delete_member_days")
        val deleteMemberDays: String? = null,
        @SerialName("members_removed")
        val membersRemoved: String? = null,
        @SerialName("channel_id")
        val channelId: Snowflake? = null,
        val count: String? = null,
        val id: Snowflake? = null,
        val type: String? = null,
        @SerialName("role_name")
        val roleName: String? = null
)