package com.nevakanezah.horseenhancer.listeners;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class EquicideEventListener implements Listener {

	// Provide various functionality when right-clicking horses with certain items
	@EventHandler(ignoreCancelled = true)
	public void onDamageHorseEvent(EntityDamageByEntityEvent event) {
		final Entity eventEntity = event.getEntity();

		if(!(eventEntity instanceof AbstractHorse) 
				&& !(eventEntity instanceof Vehicle)
				&& !(event.getDamager() instanceof Player || event.getDamager() instanceof Projectile))
			return;

		if(event.getCause().equals(DamageCause.PROJECTILE) && ((Projectile)event.getDamager()).getShooter() instanceof Player){
			Player shooter = (Player)((Projectile)event.getDamager()).getShooter();

			if(eventEntity.getPassengers().contains(shooter)) {
				event.setDamage(0);
				event.setCancelled(true);
			}
		}
	}
}
