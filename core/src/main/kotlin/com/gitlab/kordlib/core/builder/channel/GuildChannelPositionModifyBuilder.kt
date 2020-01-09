package com.gitlab.kordlib.core.builder.channel

import com.gitlab.kordlib.core.builder.AuditRequestBuilder
import com.gitlab.kordlib.core.builder.KordDsl
import com.gitlab.kordlib.core.entity.Snowflake
import com.gitlab.kordlib.rest.json.request.GuildChannelPositionModifyRequest

@KordDsl
class GuildChannelPositionModifyBuilder: AuditRequestBuilder<GuildChannelPositionModifyRequest>  {
    override var reason: String? = null
    private val swaps: MutableList<Pair<String, Int>> = mutableListOf()

    fun move(pair: Pair<Snowflake, Int>) {
        swaps += pair.first.value to pair.second
    }

    fun move(vararg pairs: Pair<Snowflake, Int>) {
        swaps += pairs.map { it.first.value to it.second }
    }

    override fun toRequest(): GuildChannelPositionModifyRequest =
            GuildChannelPositionModifyRequest(swaps)
}