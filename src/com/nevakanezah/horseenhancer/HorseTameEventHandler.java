package com.nevakanezah.horseenhancer;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

//import net.md_5.bungee.api.ChatColor;

public class HorseTameEventHandler implements Listener {

	private final HorseEnhancerPlugin plugin;
	
	public HorseTameEventHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onHorseTame(EntityTameEvent event) {

		if(event.isCancelled())
			return;
		if(!(event.getEntity() instanceof AbstractHorse && event.getOwner() instanceof Player))
			return;
		if(plugin.getHorses().containsKey(event.getEntity().getUniqueId()))
			return;
		
		Entity horse = event.getEntity();
		
		HorseData horseData = new HorseData(event.getEntity(), null, null, plugin.getConfig().getDouble("gender-ratio"));
		
		plugin.getHorses().put(horse.getUniqueId(), horseData);
		
//		if(plugin.getHorses().containsKey(horse.getUniqueId()))
//			((Player) event.getOwner()).sendMessage(ChatColor.GREEN + "Horse Registered!");
//		else
//			((Player) event.getOwner()).sendMessage(ChatColor.RED + "Failed to register horse!");
	}
}
