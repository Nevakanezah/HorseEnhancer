package com.nevakanezah.horseenhancer;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.nevakanezah.horseenhancer.util.StorableHashMap;

public class HorseDeathEventHandler implements Listener {
	
	private final HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;

	public HorseDeathEventHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	// We want to write horsedata to file semi-frequently, to prevent large data losses & minimize memory usage.
	// For now, it's written on server shutdown, and when a registered horse dies.
	@EventHandler(ignoreCancelled = true)
	public void onHorseDeathEvent(EntityDeathEvent event) {
		if(event.getEntity() instanceof AbstractHorse && horseList.containsKey(event.getEntity().getUniqueId()))
		{
			horseList.remove(event.getEntity().getUniqueId());
			try { horseList.saveToFile(); } 
			catch (IOException e)
			{
				plugin.getLogger().log(Level.WARNING, "Error: Failed to save horse data!", e);
			}
		}
	}

}
