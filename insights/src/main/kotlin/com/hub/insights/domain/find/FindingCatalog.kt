package com.hub.insights.domain.find

import java.time.Instant

object FindingCatalog {

    fun evaluate(
        ctx: FindingContext,
        sleepPolicy: SleepPolicy = SleepPolicy(),
        carePolicy: CarePolicy = CarePolicy(),
        bathroomPolicy: BathroomPolicy = BathroomPolicy(),
    ): List<Finding> {
        if (!ctx.baseline.ready) {
            return listOf(baselineForming(ctx))
        }
        val out = mutableListOf<Finding>()
        if (sleepPolicy.dawnClusterEnabled) {
            val cluster = dawnCluster(ctx, sleepPolicy)
            when {
                cluster != null && policyGap(ctx) -> out += policyDawn(ctx, cluster)
                cluster != null -> out += clusterOnly(ctx, cluster)
            }
        }
        if (sleepPolicy.exitsRisingEnabled) exitsRising(ctx, sleepPolicy)?.let { out += it }
        if (sleepPolicy.restlessHighEnabled) restlessHigh(ctx, sleepPolicy)?.let { out += it }
        if (bathroomPolicy.bathroomNightEnabled) bathroomNight(ctx, bathroomPolicy)?.let { out += it }
        if (carePolicy.careThinEnabled) careThin(ctx, carePolicy)?.let { out += it }
        briefing(ctx)?.let { out += it }
        if (sleepPolicy.sleepInRangeEnabled) sleepInRange(ctx, sleepPolicy)?.let { out += it }
        return out
    }

    private fun baselineForming(ctx: FindingContext) = Finding(
        code = "BASELINE_FORMING",
        kind = FindingKind.WATCH,
        polarity = Polarity.NEUTRAL,
        severity = "info",
        headline = "Línea base en formación",
        body = "Alta hace ${ctx.baseline.observedDays} día${if (ctx.baseline.observedDays == 1) "" else "s"}. " +
            "Hacen falta ${7} días para hablar de tendencias. No evaluar umbrales todavía.",
        windowDays = ctx.windowDays,
        residentId = ctx.residentId,
        residentName = ctx.residentName,
    )

    private data class DawnStats(
        val exits: List<Instant>,
        val dawn: List<Instant>,
    ) {
        val alwaysDawn: Boolean get() = exits.isNotEmpty() && dawn.size == exits.size
    }

    private fun dawnCluster(ctx: FindingContext, policy: SleepPolicy): DawnStats? {
        val exits = ctx.exitsLast7d
        if (exits.size < policy.dawnMinCount) return null
        val dawn = exits.filter { BedExits.isDawn(it, ctx.zone, policy) }
        if (dawn.size.toDouble() / exits.size < policy.dawnRatio) return null
        return DawnStats(exits, dawn)
    }

    private fun policyGap(ctx: FindingContext): Boolean {
        val warn = ctx.bedEdgeWarningMinutes ?: return false
        return warn >= 1
    }

    private fun clusterHeadline() = "Salidas de cama concentradas entre 5 y 6 de la mañana"

    private fun clusterBody(ctx: FindingContext, stats: DawnStats): String {
        val name = CopyFormat.firstName(ctx.residentName) ?: "El residente"
        val n = stats.exits.size
        val times = stats.dawn.map { it.atZone(ctx.zone).toLocalTime() }.sorted()
        val from = CopyFormat.clockTime(times.firstOrNull() ?: BedExits.DAWN_FROM)
        val to = CopyFormat.clockTime(times.lastOrNull() ?: BedExits.DAWN_TO)
        val whenPhrase = if (stats.alwaysDawn) {
            "siempre entre las $from y las $to"
        } else {
            "concentradas entre las $from y las $to"
        }
        return "En los últimos siete días $name salió de la cama ${CopyFormat.veces(n)}, $whenPhrase."
    }

    private fun clusterOnly(ctx: FindingContext, stats: DawnStats) = Finding(
        code = "BED_EXIT_DAWN_CLUSTER",
        kind = FindingKind.CLUSTER,
        polarity = Polarity.CONCERN,
        severity = "warning",
        headline = clusterHeadline(),
        body = clusterBody(ctx, stats),
        windowDays = 7,
        residentId = ctx.residentId,
        residentName = ctx.residentName,
        evidence = clusterEvidence(stats),
    )

    private fun policyDawn(ctx: FindingContext, stats: DawnStats): Finding {
        val warn = ctx.bedEdgeWarningMinutes ?: 1
        val parts = mutableListOf(clusterBody(ctx, stats))
        staffSentence(ctx)?.let { parts += it }
        PolicyCopy.levelLabel(ctx.riskLevel)?.let { label ->
            parts += "Su nivel actual es $label, que avisa igual de día que de noche."
        }
        val minuto = if (warn == 1) "minuto" else "minutos"
        parts += "El aviso en el borde de la cama espera $warn $minuto; en esa franja el retraso importa."
        return Finding(
            code = "POLICY_BED_EDGE_DAWN",
            kind = FindingKind.POLICY,
            polarity = Polarity.CONCERN,
            severity = "decision",
            headline = clusterHeadline(),
            body = parts.joinToString(" "),
            windowDays = 7,
            residentId = ctx.residentId,
            residentName = ctx.residentName,
            awaitingDecision = true,
            evidence = clusterEvidence(stats) + mapOf(
                "bedEdgeWarningMinutes" to warn,
                "episodeIds" to ctx.relatedEpisodeIds,
            ),
            proposal = FindingProposal(
                action = "SET_BED_EDGE_WARNING_IMMEDIATE",
                text = "Avisar apenas se detecte el borde de la cama, en lugar de esperar $warn $minuto.",
            ),
        )
    }

    private fun staffSentence(ctx: FindingContext): String? {
        val n = ctx.staffAfterExitCount
        if (n <= 0) return null
        val veces = CopyFormat.veces(n).replaceFirstChar { it.uppercase() }
        return "$veces necesitó que fuera una enfermera."
    }

    private fun clusterEvidence(stats: DawnStats) = mapOf(
        "exitCount" to stats.exits.size,
        "dawnCount" to stats.dawn.size,
        "windowFrom" to BedExits.DAWN_FROM.toString(),
        "windowTo" to BedExits.DAWN_TO.toString(),
        "exitAt" to stats.exits.map { it.toString() },
    )

    private fun exitsRising(ctx: FindingContext, policy: SleepPolicy): Finding? {
        if (!SleepBriefing.exitsRising(ctx.sleepDays, policy)) return null
        val (last7, prev7) = SleepBriefing.weeks(ctx.sleepDays)
        val last = last7.map { it.bedExitCount }.average()
        val prev = prev7.map { it.bedExitCount }.average()
        return Finding(
            code = "BED_EXITS_RISING",
            kind = FindingKind.TREND,
            polarity = Polarity.CONCERN,
            severity = "warning",
            headline = "Las salidas de cama vienen aumentando",
            body = "En la última semana hubo ${CopyFormat.oneDecimal(last)} salidas por noche, " +
                "frente a ${CopyFormat.oneDecimal(prev)} la semana anterior.",
            windowDays = 14,
            residentId = ctx.residentId,
            residentName = ctx.residentName,
            evidence = mapOf("last7" to last, "prev7" to prev),
        )
    }

    private fun restlessHigh(ctx: FindingContext, policy: SleepPolicy): Finding? {
        val share = ctx.sleep.restlessShare ?: return null
        if (share <= policy.restlessHighThreshold) return null
        val minutes = ctx.sleep.avgRestlessMinutes7d?.let { CopyFormat.clock(it) } ?: return null
        return Finding(
            code = "SLEEP_RESTLESS_HIGH",
            kind = FindingKind.TREND,
            polarity = Polarity.CONCERN,
            severity = "warning",
            headline = "Sueño inquieto por encima de lo habitual",
            body = "Tiempo en sueño inquieto $minutes, ${CopyFormat.percent(share)} del total dormido. " +
                "Conviene mirar salidas de cama y ventanas cortadas antes de cambiar alarmas.",
            windowDays = 7,
            residentId = ctx.residentId,
            residentName = ctx.residentName,
            evidence = mapOf("restlessShare" to share),
        )
    }

    private fun sleepInRange(ctx: FindingContext, policy: SleepPolicy): Finding? {
        val share = ctx.sleep.restlessShare ?: return null
        if (share > policy.sleepInRangeThreshold) return null
        return Finding(
            code = "SLEEP_IN_RANGE",
            kind = FindingKind.WATCH,
            polarity = Polarity.POSITIVE,
            severity = "info",
            headline = "Sueño dentro de su rango",
            body = "Comparado contra su propia línea base, no contra un estándar: en su rango. " +
                "Un promedio de sueño sólo significa algo al lado de cuánto duerme habitualmente esta persona.",
            windowDays = ctx.windowDays,
            residentId = ctx.residentId,
            residentName = ctx.residentName,
        )
    }

    private fun briefing(ctx: FindingContext): Finding? {
        val text = SleepBriefing.narrative(ctx.sleep, ctx.sleepDays) ?: return null
        return Finding(
            code = "SLEEP_14D_BRIEFING",
            kind = FindingKind.BRIEFING,
            polarity = Polarity.NEUTRAL,
            severity = "info",
            headline = "Insight · ${ctx.windowDays} días",
            body = text,
            windowDays = ctx.windowDays,
            residentId = ctx.residentId,
            residentName = ctx.residentName,
        )
    }

    private fun bathroomNight(ctx: FindingContext, policy: BathroomPolicy): Finding? {
        val ordered = ctx.bathroomDays.filter { it.measured }.sortedBy { it.day }
        val last7 = ordered.takeLast(7)
        val prev7 = ordered.dropLast(7).takeLast(7)
        if (last7.isEmpty() || prev7.isEmpty()) return null
        val last = last7.map { it.nightVisitCount }.average()
        val prev = prev7.map { it.nightVisitCount }.average()
        if (last < policy.nightMinAvg || last < prev * policy.nightRiseFactor) return null
        return Finding(
            code = "BATHROOM_NIGHT_UP",
            kind = FindingKind.TREND,
            polarity = Polarity.CONCERN,
            severity = "warning",
            headline = "Las visitas al baño de noche vienen aumentando",
            body = "En la última semana hubo ${CopyFormat.oneDecimal(last)} visitas nocturnas por día, " +
                "frente a ${CopyFormat.oneDecimal(prev)} la semana anterior.",
            windowDays = 14,
            residentId = ctx.residentId,
            residentName = ctx.residentName,
            evidence = mapOf("last7" to last, "prev7" to prev),
        )
    }

    private fun careThin(ctx: FindingContext, policy: CarePolicy): Finding? {
        val avg = ctx.careAvgMinutes ?: return null
        if (avg >= policy.careThinMinutes) return null
        return Finding(
            code = "CARE_THIN",
            kind = FindingKind.WATCH,
            polarity = Polarity.CONCERN,
            severity = "info",
            headline = "Poco cuidado medido en la ventana",
            body = "El tiempo de cuidado medido ronda ${CopyFormat.oneDecimal(avg)} min por día. " +
                "Cero o poco medido no es una caída inventada: es lo que hay en el cubo.",
            windowDays = ctx.windowDays,
            residentId = ctx.residentId,
            residentName = ctx.residentName,
            evidence = mapOf("avgMinutesPerDay" to avg),
        )
    }
}
