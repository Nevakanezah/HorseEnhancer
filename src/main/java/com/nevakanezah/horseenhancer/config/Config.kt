package com.nevakanezah.horseenhancer.config

import org.bukkit.Material
import space.arim.dazzleconf.annote.*
import space.arim.dazzleconf.annote.ConfDefault.*

@ConfHeader(
    "HorseEnhancer Configuration file",
    "Version 2.0 for MC 1.18.2",
    "HorseEnhancer written by Nevakanezah",
)
interface Config {
    @get:ConfKey("enable-inspector")
    @get:DefaultString("RESTRICT")
    @get:ConfComments(
        "Toggle the ability to inspect horses. Ops will always have this ability.",
        "TRUE - All players can inspect any horse.",
        "RESTRICT - Only a horse's owner can inspect it.",
        "FALSE - Disable horse inspection.",
    )
    val enableInspector: InspectorMode
    enum class InspectorMode {
        TRUE,
        RESTRICT,
        FALSE,
    }

    @get:ConfKey("enable-inspector-attributes")
    @get:DefaultBoolean(true)
    @get:ConfComments("When inspecting a horse, players see attribute info like speed & jump height.",)
    val enableInspectorAttributes: Boolean

    @get:ConfKey("enable-equicide-protection")
    @get:DefaultBoolean(true)
    @get:ConfComments("Whether you can shoot an animal you're currently riding. Mostly applies to pigs.")
    val enableEquicideProtection: Boolean

    @get:ConfKey("enable-secret-horses")
    @get:DefaultBoolean(true)
    @get:ConfComments("Enable Easter egg horses with unique breeding conditions, and custom stats.")
    val enableSecretHorses: Boolean

    @get:SubSection
    @get:ConfKey("tools")
    @get:ConfComments("Tools")
    val tools: Tools
    interface Tools {
        @get:ConfKey("gelding")
        @get:DefaultString("SHEARS")
        val gelding: Material

        @get:ConfKey("inspection")
        @get:DefaultString("CLOCK")
        val inspection: Material
    }

    @get:SubSection
    @get:ConfKey("breeding")
    @get:ConfComments("Breeding")
    val breeding: Breeding
    interface Breeding {
        @get:SubSection
        @get:ConfKey("child-skew")
        @get:ConfComments(
            "Horse stats are determined by selecting a random point between the stats of both parents.",
            "From that point, the child's stats can skew up or down, potentially beyond their parents' values.",
            "These values set how much a child can skew up or down, as a percentage value between -1.0 and 1.0.",
        )
        val childSkew: ChildSkew
        interface ChildSkew {
            @get:ConfKey("upper")
            @get:DefaultDouble(0.05)
            @get:NumericRange(min = -1.0, max = 1.0)
            val upper: Double

            @get:ConfKey("lower")
            @get:DefaultDouble(-0.1)
            @get:NumericRange(min = -1.0, max = 1.0)
            val lower: Double
        }

        @get:ConfKey("gender-ratio")
        @get:DefaultDouble(0.125)
        @get:NumericRange(min = 0.0, max = 1.0)
        @get:ConfComments("Male-Female ratio for horse population, from 0.0 - 1.0. A lower value makes males less common.")
        val genderRatio: Double
    }
}
