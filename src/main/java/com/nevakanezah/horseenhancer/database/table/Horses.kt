package com.nevakanezah.horseenhancer.database.table

import com.nevakanezah.horseenhancer.database.SQLiteDatabase.Companion.flushChangesSuspend
import com.nevakanezah.horseenhancer.model.HorseGender
import com.nevakanezah.horseenhancer.util.NameConverter
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.varchar
import java.util.*

@Suppress("unused")
object Horses : Table<Horse>("horse") {
    val uid = varchar("uid").primaryKey().bindTo { it.uid }
    val fatherUid = varchar("father_uid").bindTo { it.fatherUid }
    val motherUid = varchar("mother_uid").bindTo { it.motherUid }
    val gender = enum<HorseGender>("gender").bindTo { it.gender }
}

interface Horse : Entity<Horse> {
    companion object : Entity.Factory<Horse>()

    var uid: String
    var fatherUid: String?
    var motherUid: String?
    var gender: HorseGender?

    val horseId: String
        get() = NameConverter.uint2quint(UUID.fromString(uid).leastSignificantBits.toUInt())
    val fatherId: String?
        get() = fatherUid?.let { NameConverter.uint2quint(UUID.fromString(it).leastSignificantBits.toUInt()) }
    val motherId: String?
        get() = motherUid?.let { NameConverter.uint2quint(UUID.fromString(it).leastSignificantBits.toUInt()) }

    suspend fun geld(): Boolean {
        if (gender != HorseGender.STALLION || gender != HorseGender.JACK || gender != HorseGender.HERDSIRE)
            return false

        gender = HorseGender.GELDING
        flushChangesSuspend()
        return true
    }

    fun genderCompatible(mate: Horse): Boolean {
        val mateGender = mate.gender

        return when (gender) {
            HorseGender.STALLION -> mateGender == HorseGender.MARE || mateGender == HorseGender.JENNY
            HorseGender.MARE -> mateGender == HorseGender.STALLION || mateGender == HorseGender.JACK
            HorseGender.JENNY -> mateGender == HorseGender.STALLION || mateGender == HorseGender.JACK
            HorseGender.JACK -> mateGender == HorseGender.MARE || mateGender == HorseGender.JENNY
            HorseGender.DAM -> mateGender == HorseGender.HERDSIRE
            HorseGender.HERDSIRE -> mateGender == HorseGender.DAM

            else -> false
        }
    }

    fun canSire(): Boolean = when (gender) {
        HorseGender.STALLION, HorseGender.JACK, HorseGender.HERDSIRE -> true
        else -> false
    }

    fun isRelated(partner: Horse): Boolean {
        if (partner.uid == fatherUid || partner.uid == motherUid)
            return true
        if (partner.fatherUid == uid || partner.fatherUid == uid)
            return true
        if (fatherUid == partner.fatherUid && motherUid == partner.motherUid)
            return true

        return false
    }
}

val Database.horses
    get() = this.sequenceOf(Horses)
