package com.nevakanezah.horseenhancer.listeners;

import java.util.UUID;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.nevakanezah.horseenhancer.HorseEnhancerPlugin;
import com.nevakanezah.horseenhancer.data.HorseData;
import com.nevakanezah.horseenhancer.util.StorableHashMap;

public class HorseDeathEventListener implements Listener {
	
	private final HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;

	public HorseDeathEventListener(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	// We want to write horsedata to file semi-frequently, to prevent large data losses & minimize memory usage.
	// For now, it's written on server shutdown, and when a registered horse dies.
	@EventHandler(ignoreCancelled = true)
	public void onHorseDeathEvent(EntityDeathEvent event) {
		if(event.getEntity() instanceof AbstractHorse && horseList.containsKey(event.getEntity().getUniqueId()))
		{
			plugin.purgeInvalidHorses();
		}
	}

}
