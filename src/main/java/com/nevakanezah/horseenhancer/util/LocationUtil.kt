package com.nevakanezah.horseenhancer.util

import org.bukkit.Location
import org.bukkit.entity.LivingEntity

object LocationUtil {
    infix fun Location.heightBetween(another: Location): Location = Location(this.world, this.x, (this.y + another.y) / 2, this.z, this.yaw, this.pitch)

    val LivingEntity.bodyLocation
        get() = this.location heightBetween this.eyeLocation
}
