package com.nevakanezah.horseenhancer;

import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

// Handling those situations where your horse falls down some stairs... onto a pile of bullets.
public class FriendlyFireHandler implements Listener {
	
	@SuppressWarnings("unused")
	private final HorseEnhancerPlugin plugin;
	
	public FriendlyFireHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onFriendlyFire(EntityDamageByEntityEvent event) {
		// Filter for arrow collision events from a horse's rider
		if(event.isCancelled())
			return;
		if(!(event.getEntity() instanceof Vehicle))
			return;
		Vehicle mount = (Vehicle)event.getEntity();
		if(!(event.getCause().equals(DamageCause.PROJECTILE)))
			return;
		if((mount.isEmpty()))
			return;
		if(!(event.getDamager() instanceof Player && event.getEntity().getPassengers().contains(event.getDamager())))
			return;
		
		event.setDamage(0);
	}

}
