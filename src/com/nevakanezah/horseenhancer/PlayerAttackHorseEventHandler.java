package com.nevakanezah.horseenhancer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;


import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nevakanezah.horseenhancer.util.StorableHashMap;

import net.md_5.bungee.api.ChatColor;

public class PlayerAttackHorseEventHandler implements Listener {

	private final HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;

	public PlayerAttackHorseEventHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	// Provide various functionality when right-clicking horses with certain items
	@EventHandler
	public void onDamageHorseEvent(EntityDamageByEntityEvent event) {
		if(event.isCancelled())
			return;
		if(!(event.getEntity() instanceof AbstractHorse) && !(event.getEntity() instanceof Vehicle))
			return;
	    if(!(event.getDamager() instanceof Player) && !(event.getDamager() instanceof Projectile))
	      return;
		
	    // Cancel friendly fire on your own mount
		if(event.getCause().equals(DamageCause.PROJECTILE))
		{
			Player shooter = (Player)((Projectile)event.getDamager()).getShooter();
			if(event.getEntity().getPassengers().contains(shooter))
			{
				event.setDamage(0);
				return;
			}
		}
		
		// Pigs get out
		if(!(event.getEntity() instanceof AbstractHorse))
			return;
	    if(!(event.getCause().equals(DamageCause.ENTITY_ATTACK)))
	    	return;
	    if(!event.getEntity().isEmpty())
	    	return;

		// Only catch interactions with configured interaction items
		HashMap<String, Material> items = new HashMap<>();
		Player player = (Player) event.getDamager();
		
		items.put("gelding-tool", Material.matchMaterial(plugin.getConfig().getString("gelding-tool")));
		items.put("inspection-tool", Material.matchMaterial(plugin.getConfig().getString("inspection-tool")));
		
		Material heldItem = player.getInventory().getItemInMainHand().getType();
		
		if(!(items.containsValue(heldItem))) {
			return;
		}
		
		// Only interactions with registered horses
		if(!plugin.getHorses().containsKey(event.getEntity().getUniqueId()) && !((Tameable)event.getEntity()).isTamed()){
			player.sendMessage(ChatColor.RED + "You can't do that to a wild horse!");
			return;
		}
		
		// Register tamed horses upon inspection
		AbstractHorse horse = (AbstractHorse)event.getEntity();
		HorseData horseData = horseList.get(horse.getUniqueId());
		if(horseData == null){
			horseData = new HorseData(horse, null, null, plugin.getConfig().getDouble("gender-ratio"));
			horseList.put(horse.getUniqueId(), horseData);
			if(plugin.getHorses().containsKey(horse.getUniqueId()))
				player.sendMessage(ChatColor.GREEN + "Horse Registered!");
			else
				player.sendMessage(ChatColor.RED + "Failed to register horse!");
		}
		
		String horseName = (horse.getCustomName() == null) ? ChatColor.GREEN + "" + horse.getUniqueId() : ChatColor.GREEN + horse.getCustomName();
		
		// When holding the gelding tool, attempt gelding
		if(heldItem.equals(items.get("gelding-tool")) && ((Player)event.getDamager()).isSneaking()) {
			
			// Cancel the triggering event
			event.setCancelled(true);
			
			// Cancel gelding for untamed, registered horses like foals
			if(!((Tameable)event.getEntity()).isTamed()){
				player.sendMessage(ChatColor.RED + "You can't do that to a wild horse!");
				return;
			}
			
			if(horse.getOwner().getUniqueId().equals(event.getDamager().getUniqueId()) 
				|| horse.getOwner().getName().equals(event.getDamager().getName()) ){
				if(horseData.geld()) {
					player.sendMessage(ChatColor.GREEN + "Successfully gelded " + horseName + "!");
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1, 1);
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, 0.3f, 1.3f);
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1, 1);
					short durability = player.getInventory().getItemInMainHand().getDurability();
					player.getInventory().getItemInMainHand().setDurability(durability);
					player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 100, 80);
				}
				else
					player.sendMessage(ChatColor.RED + "Failed to geld " + horseName + ChatColor.RED + " - Target is not male.");
			}
			else
				player.sendMessage(ChatColor.RED + "That's not your horse!");
		}
		
		// When holding the inspection tool, report horse stats to the player
		if(heldItem.equals(items.get("inspection-tool")) && !((Player)event.getDamager()).isSneaking()) {
			ArrayList<String> msg = new ArrayList<String>();
			
			// Cancel the triggering event
			event.setCancelled(true);
			
			// Collect & format horse data
			String speedFmt = new DecimalFormat("#.####").format(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
			String jumpFmt = new DecimalFormat("#.###").format(horse.getJumpStrength());
			boolean ownerless = horse.getOwner() == null;
			
			String health = "" + ChatColor.GREEN + horse.getHealth() + "/30";
			String speed = "" + ChatColor.GREEN + speedFmt + "/0.3375";
			String jump = "" + ChatColor.GREEN + jumpFmt + "/1.0";
			String strength = (horse instanceof Llama) ? "" + ChatColor.GREEN + ((Llama)horse).getStrength() + "/5" : null;
			String tamer = "" + (ownerless ? ChatColor.BLUE + "Wild" : ChatColor.GREEN + horse.getOwner().getName());
			tamer = "" + (horse.getAge() < 0 ? ChatColor.BLUE + "Foal" : tamer);
			String gender = "" + ChatColor.GREEN + horseData.getGenderName();
			String sire = "" + ChatColor.GREEN + horseData.getFatherName();
			String dam = "" + ChatColor.GREEN + horseData.getMotherName();
			
			msg.add(ChatColor.DARK_PURPLE + "-------");
			msg.add(ChatColor.DARK_PURPLE + "Stats for " + gender + ChatColor.DARK_PURPLE + ": " + horseName);
			msg.add(ChatColor.DARK_PURPLE + "Tamer: " + tamer);
			msg.add(ChatColor.DARK_PURPLE + "Sire: " + sire);
			msg.add(ChatColor.DARK_PURPLE + "Dam: " + dam);
			msg.add(ChatColor.DARK_PURPLE + "Health: " + health);
			msg.add(ChatColor.DARK_PURPLE + "Speed: " + speed);
			msg.add(ChatColor.DARK_PURPLE + "Jump: " + jump);
			if(strength != null)
				msg.add(ChatColor.DARK_PURPLE + "Strength: " + strength);  
			msg.add(ChatColor.DARK_PURPLE + "-------");
			
		    // Send message to player
		    for (String m : msg) {
		      player.sendMessage(m);
		    }
		}
	}
	
}
