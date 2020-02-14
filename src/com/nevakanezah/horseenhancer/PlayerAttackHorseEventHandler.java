package com.nevakanezah.horseenhancer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
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
	
	private static final String GELD_TOOL = "gelding-tool";
	private static final String INSPECT_TOOL = "inspection-tool";
	private static final String GENDER_RATIO = "gender-ratio";

	public PlayerAttackHorseEventHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	// Provide various functionality when right-clicking horses with certain items
	@EventHandler(ignoreCancelled = true)
	public void onDamageHorseEvent(EntityDamageByEntityEvent event) {
		final Entity eventEntity = event.getEntity();
		
		if(!(eventEntity instanceof AbstractHorse) 
			&& !(eventEntity instanceof Vehicle)
			&& !(event.getDamager() instanceof Player || event.getDamager() instanceof Projectile))
	      return;
	    
		boolean equicideProtection = plugin.getConfig().getBoolean("enable-equicide-protection");
	    if(event.getCause().equals(DamageCause.PROJECTILE) && equicideProtection)
	    	handleFriendlyFire(event);
		
		// Pigs get out. Also, no inspecting animals with riders.
		if(!(eventEntity instanceof AbstractHorse)
			|| !(event.getCause().equals(DamageCause.ENTITY_ATTACK))
			|| !eventEntity.isEmpty())
				return;

		final Player player = (Player) event.getDamager();
		
		// Only catch interactions with configured interaction items
		HashMap<String, Material> items = new HashMap<>();
		items.put(GELD_TOOL, Material.matchMaterial(plugin.getConfig().getString(GELD_TOOL)));
		items.put(INSPECT_TOOL, Material.matchMaterial(plugin.getConfig().getString(INSPECT_TOOL)));
		
		Material heldItem = player.getInventory().getItemInMainHand().getType();
		
		if(!(items.containsValue(heldItem)))
			return;
		
		// Only interactions with registered horses
		if(!plugin.getHorses().containsKey(eventEntity.getUniqueId()) && !((Tameable)eventEntity).isTamed()){
			player.sendMessage(ChatColor.RED + "You can't do that to a wild horse!");
			return;
		}
		
		final AbstractHorse horse = (AbstractHorse)eventEntity;
		HorseData horseData = getOrRegisterData(horse);
		
		// When holding the gelding tool, attempt gelding
		if(heldItem.equals(items.get(GELD_TOOL)) && (player.isSneaking())) {
			event.setCancelled(true);
			handleGelding(player, horse, horseData);
		}
			
		// When holding the inspection tool, report horse stats to the player
		if(heldItem.equals(items.get(INSPECT_TOOL)) && !(player.isSneaking())) {
			handleInspection(player, horse, horseData, event);
		}
	}
	
	private HorseData getOrRegisterData(AbstractHorse horse) {
		HorseData horseData = horseList.get(horse.getUniqueId());
		if(horseData == null){
			horseData = new HorseData(horse, null, null, plugin.getConfig().getDouble(GENDER_RATIO));
			horseList.put(horse.getUniqueId(), horseData);
		}
		return horseData;
	}
	
	private void handleFriendlyFire(EntityDamageByEntityEvent event){
		final Entity eventEntity = event.getEntity();
		Player shooter = (Player)((Projectile)event.getDamager()).getShooter();
		
		if(eventEntity.getPassengers().contains(shooter))
			event.setCancelled(true);
	}
	
	private void handleGelding(Player player, AbstractHorse horse, HorseData horseData) {
		String horseName = horseData.getHorseID();
		// Cancel gelding for untamed, registered horses like foals
		if(!(horse.isTamed()) || horse.getOwner() == null){
			player.sendMessage(ChatColor.RED + "You can't do that to a wild horse!");
			return;
		}
		
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
	
	private void handleInspection(Player player, AbstractHorse horse, HorseData horseData, EntityDamageByEntityEvent event) {
		final String INSPECTOR_TOGGLE = plugin.getConfig().getString("enable-inspector").toLowerCase();
		
		if(!player.isOp()) 
			switch(INSPECTOR_TOGGLE) {
			case "false":
				return;
			case "restrict":
				if(!(horse.getOwner().equals(player))) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "That's not your horse!");
				}
				return;
			default:
				return;
			}

		// Cancel the triggering event
		event.setCancelled(true);
		
		ArrayList<String> msg = new ArrayList<>();
		
		// Collect & format horse data
		String speedFmt = new DecimalFormat("#.####").format(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
		String jumpFmt = new DecimalFormat("#.###").format(horse.getJumpStrength());
		boolean ownerless = horse.getOwner() == null;
		
		String health = "" + ChatColor.GREEN + (int)horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() + "/30";
		String speed = "" + ChatColor.GREEN + speedFmt + "/0.3375";
		String jump = "" + ChatColor.GREEN + jumpFmt + "/1.0";
		String strength = (horse instanceof Llama) ? "" + ChatColor.GREEN + ((Llama)horse).getStrength() + "/5" : null;
		String tamer = "" + (ownerless ? ChatColor.BLUE + "Wild" : ChatColor.GREEN + horse.getOwner().getName());
		tamer = "" + (horse.getAge() < 0 ? ChatColor.BLUE + "Foal" : tamer);
		String gender = "" + ChatColor.GREEN + horseData.getGenderName();
		String sire = "" + ChatColor.GREEN + horseData.getFatherName();
		String dam = "" + ChatColor.GREEN + horseData.getMotherName();
		
		String horseName = ChatColor.BLUE + "#" + horseData.getHorseID();
		if(horse.getCustomName() != null)
			horseName = ChatColor.GREEN + horse.getCustomName() + " " + horseName;
		

		msg.add(ChatColor.DARK_PURPLE + "-------");
		msg.add(ChatColor.DARK_PURPLE + "Stats for " + gender + ChatColor.DARK_PURPLE + ": " + horseName);
		msg.add(ChatColor.DARK_PURPLE + "Tamer: " + tamer);
		msg.add(ChatColor.DARK_PURPLE + "Sire: " + sire);
		msg.add(ChatColor.DARK_PURPLE + "Dam: " + dam);
			if(plugin.getConfig().getBoolean("enable-inspector-attributes") || player.isOp())
			{
			msg.add(ChatColor.DARK_PURPLE + "Health: " + health);
			msg.add(ChatColor.DARK_PURPLE + "Speed: " + speed);
			msg.add(ChatColor.DARK_PURPLE + "Jump: " + jump);
			if(strength != null)
				msg.add(ChatColor.DARK_PURPLE + "Strength: " + strength);  
		}
		msg.add(ChatColor.DARK_PURPLE + "-------");
		
	    // Send message to player
	    for (String m : msg) {
	      player.sendMessage(m);
	    }
	}
	
}
