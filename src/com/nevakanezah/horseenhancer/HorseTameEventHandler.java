package com.nevakanezah.horseenhancer;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class HorseTameEventHandler implements Listener {

	private final HorseEnhancerPlugin plugin;
	
	public HorseTameEventHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onHorseTame(EntityTameEvent event) {
		if(!(event.getEntity() instanceof AbstractHorse && event.getOwner() instanceof Player)
				|| plugin.getHorses().containsKey(event.getEntity().getUniqueId()))
			return;
		
		Entity horse = event.getEntity();
		
		HorseData horseData = new HorseData(event.getEntity(), null, null, plugin.getConfig().getDouble("gender-ratio"));
		
		plugin.getHorses().put(horse.getUniqueId(), horseData);
	}
}
