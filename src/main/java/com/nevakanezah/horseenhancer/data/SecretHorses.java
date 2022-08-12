package com.nevakanezah.horseenhancer.data;

import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;

public class SecretHorses {
	
	private SecretHorses() {
		super();
	}
	
	/**
	 * Spawn a horse whose parents were maybe a little too close.
	 * @param loc Location to spawn the new entity
	 * @param horseData Data container to be attached to the new entity
	 * @return the newly-spawned horse
	 */
	public static AbstractHorse spawnInbred(Location loc, HorseData horseData) {
		AbstractHorse horse = (AbstractHorse)loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE_HORSE);
		horseData.setUniqueID(horse.getUniqueId());
		horseData.setType(EntityType.ZOMBIE_HORSE);
		
		horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
		horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10);
		horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(0.3);
		horse.setTamed(true);
		horse.setAge(-9999999);
		horse.setAgeLock(true);
		horse.setBreed(false);
		
		horseData.setGender("INBRED");
		
		return horse;
	}
	
	public static AbstractHorse spawnMaximule(Location loc, HorseData horseData) {
		AbstractHorse horse = (AbstractHorse)loc.getWorld().spawnEntity(loc, EntityType.MULE);
		horseData.setUniqueID(horse.getUniqueId());
		horseData.setType(EntityType.MULE);
		
		horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
		horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(35);
		horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(1.18);
		horse.setCustomName(ChatColor.DARK_RED + "MA"+ ChatColor.GOLD + "XI" + ChatColor.DARK_BLUE + "MU" + ChatColor.DARK_GREEN + "LE");
		horse.setBreed(false);
		horse.setAge(0);
		horse.setAgeLock(true);
		horse.playEffect(EntityEffect.FIREWORK_EXPLODE);
		
		horseData.setGender("UNIQUE");
		
		return horse;
	}
	
	public static AbstractHorse spawnInvincible(Location loc, HorseData horseData) {
		AbstractHorse horse = (AbstractHorse)loc.getWorld().spawnEntity(loc, EntityType.HORSE);
		horseData.setUniqueID(horse.getUniqueId());
		horseData.setType(EntityType.HORSE);
		
		horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.38);
		horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(15);
		horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(0.565);
		horse.setCustomName(ChatColor.DARK_BLUE + "Invincible");
		horse.setAge(0);
		horse.setAgeLock(true);
		horse.setTamed(true);
		horse.setBreed(false);
		horse.playEffect(EntityEffect.FIREWORK_EXPLODE);
		PotionEffect invis = new PotionEffect(PotionEffectType.INVISIBILITY, 2147483000, 1, false, false);
		horse.addPotionEffect(invis, true);
		horseData.setGender("UNIQUE");
		
		return horse;
	}

}
