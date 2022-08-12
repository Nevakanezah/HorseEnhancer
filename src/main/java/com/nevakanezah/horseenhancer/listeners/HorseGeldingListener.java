package com.nevakanezah.horseenhancer.listeners;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nevakanezah.horseenhancer.HorseEnhancerPlugin;
import com.nevakanezah.horseenhancer.data.HorseData;
import com.nevakanezah.horseenhancer.util.StorableHashMap;

import net.md_5.bungee.api.ChatColor;

public class HorseGeldingListener implements Listener {
	private final HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;
	
	private static final String GELD_TOOL = "gelding-tool";
	private static final String GENDER_RATIO = "gender-ratio";

	public HorseGeldingListener(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	// Provide various functionality when right-clicking horses with certain items
	@EventHandler(ignoreCancelled = true)
	public void onDamageHorseEvent(EntityDamageByEntityEvent event) {
		final Entity eventEntity = event.getEntity();
		final AbstractHorse horse = (AbstractHorse)eventEntity;
		
		// No gelding animals with riders.
		if(!(eventEntity instanceof AbstractHorse)
			|| !(event.getCause().equals(DamageCause.ENTITY_ATTACK))
			|| !eventEntity.isEmpty())
				return;

		final Player player = (Player) event.getDamager();
		
		// Only catch interactions with configured gelding item
		Material heldItem = player.getInventory().getItemInMainHand().getType();
		Material geldingItem = Material.matchMaterial(plugin.getConfig().getString(GELD_TOOL));
		if(!(geldingItem.equals(heldItem)) || !(player.isSneaking()))
			return;
		
		// Only interactions with registered horses
		if(!(plugin.getHorses().containsKey(eventEntity.getUniqueId())) 
				|| !(horse.isTamed()) 
				|| horse.getOwner() == null){
			player.sendMessage(ChatColor.RED + "You can't do that to a wild horse!");
			return;
		}
		
		event.setCancelled(true);
		HorseData horseData = getOrRegisterData(horse);
		String horseName = horseData.getHorseID();
		
		if(horse.getOwner().equals(player)){
			if(horseData.geld()) {
				player.sendMessage(ChatColor.GREEN + "Successfully gelded " + horseName + "!");
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1, 1);
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, 0.3f, 1.3f);
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1, 1);
				short durability = player.getInventory().getItemInMainHand().getDurability();
				if(player.getInventory().getItemInMainHand().getType().getMaxDurability() > 0) 
					durability++;
				player.getInventory().getItemInMainHand().setDurability(durability);
				player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 100, 80);
			}
			else
				player.sendMessage(ChatColor.RED + "Failed to geld " + horseName + ChatColor.RED + " - Target is not male.");
		}
		else
			player.sendMessage(ChatColor.RED + "That's not your horse!");
	}
	
	private HorseData getOrRegisterData(AbstractHorse horse) {
		HorseData horseData = horseList.get(horse.getUniqueId());
		if(horseData == null){
			horseData = new HorseData(horse, null, null, plugin.getConfig().getDouble(GENDER_RATIO));
			horseList.put(horse.getUniqueId(), horseData);
		}
		return horseData;
	}
}
