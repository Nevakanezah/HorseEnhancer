package com.nevakanezah.horseenhancer.model

enum class HorseGender {
    STALLION, // Male horse capable of breeding
    GELDING, // Male animal that is unable to breed
    MULE, // I doubt anyone cares what gender their mule is
    MARE, // Female horse
    JENNY, // Female donkey
    JACK, // Male donkey capable of breeding
    DAM, // Female llama
    HERDSIRE, // Male llama capable of breeding
    UNDEAD, // Zombie or skeleton
    INBRED, // Child of related parents
    CREATIVE, // Creative-mode spawned horses with unique breeding behaviour
    UNIQUE, // Special case, or custom behaviour
    UNKNOWN, // Used when all cases failed, the horse gender is unknown
}
