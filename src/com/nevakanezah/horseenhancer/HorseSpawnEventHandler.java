package com.nevakanezah.horseenhancer;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.nevakanezah.horseenhancer.util.StorableHashMap;

import net.md_5.bungee.api.ChatColor;

public class HorseSpawnEventHandler implements Listener {
	private final HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;

	public HorseSpawnEventHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}

	// Register horses that spawn tame. Includes skeleton traps and admin-spawned horses.
	@EventHandler
	public void onHorseSpawn(CreatureSpawnEvent event) {
		if(!(event.getEntity() instanceof AbstractHorse))
			return;
		
		AbstractHorse horse = (AbstractHorse)event.getEntity();
		
		if(!horse.isTamed() && !(horse instanceof SkeletonHorse))
			return;
		
		HorseData horseData = horseList.get(horse.getUniqueId());
		if(horseData == null){
			horseData = new HorseData(horse, null, null, plugin.getConfig().getDouble("gender-ratio"));
			if(event.getEntity() instanceof SkeletonHorse)
			{
				horseData.setFatherName(ChatColor.DARK_RED + "THUNDER");
				horseData.setMotherName(ChatColor.DARK_RED + "DEATH");
			}
			horseList.put(horse.getUniqueId(), horseData);
			if(!plugin.getHorses().containsKey(horse.getUniqueId()))
				plugin.getLogger().log(Level.WARNING, "Failed to register horse [" + horse.getEntityId() + "], spawned from [" + event.getSpawnReason() + "]");
		}		
		
	}
	
}
