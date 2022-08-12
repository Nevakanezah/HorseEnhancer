package com.nevakanezah.horseenhancer.listeners;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nevakanezah.horseenhancer.HorseEnhancerPlugin;
import com.nevakanezah.horseenhancer.data.HorseData;
import com.nevakanezah.horseenhancer.util.StorableHashMap;

import net.md_5.bungee.api.ChatColor;

public class HorseInspectionListener implements Listener {
	
	private final HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;
	
	private static final String INSPECT_TOOL = "inspection-tool";
	private static final String GENDER_RATIO = "gender-ratio";

	public HorseInspectionListener(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	// Provide various functionality when right-clicking horses with certain items
	@EventHandler(ignoreCancelled = true)
	public void onDamageHorseEvent(EntityDamageByEntityEvent event) {
		final Entity eventEntity = event.getEntity();
		final Player player = (Player) event.getDamager();
		final AbstractHorse horse = (AbstractHorse)eventEntity;
		HorseData horseData = getOrRegisterData(horse);
		
		// No inspecting animals with riders.
		if(!(eventEntity instanceof AbstractHorse)
			|| !(event.getCause().equals(DamageCause.ENTITY_ATTACK))
			|| !eventEntity.isEmpty())
				return;
		
		// Only catch interactions with configured inspection item
		Material heldItem = player.getInventory().getItemInMainHand().getType();
		Material inspectItem = Material.matchMaterial(plugin.getConfig().getString(INSPECT_TOOL));
		if(!(inspectItem.equals(heldItem)) || player.isSneaking())
			return;
		
		// Only interactions with registered horses
		if(!plugin.getHorses().containsKey(eventEntity.getUniqueId()) && !((Tameable)eventEntity).isTamed()){
			player.sendMessage(ChatColor.RED + "You can't do that to a wild horse!");
			return;
		}
			
		handleInspection(player, horse, horseData, event);
	}
	
	private HorseData getOrRegisterData(AbstractHorse horse) {
		HorseData horseData = horseList.get(horse.getUniqueId());
		if(horseData == null){
			horseData = new HorseData(horse, null, null, plugin.getConfig().getDouble(GENDER_RATIO));
			horseList.put(horse.getUniqueId(), horseData);
		}
		return horseData;
	}
	
	private void handleInspection(Player player, AbstractHorse horse, HorseData horseData, EntityDamageByEntityEvent event) {
		final String INSPECTOR_TOGGLE = plugin.getConfig().getString("enable-inspector").toLowerCase();
		
		// In 'restrict' mode, a horse can only be inspected by its owner, and ops
		if(!player.isOp()
				&& INSPECTOR_TOGGLE.equals("restrict") 
				&& !(horse.getOwner().equals(player))) { 
			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "That's not your horse!");
			return;
		}

		// Cancel the triggering event
		event.setCancelled(true);
		
		ArrayList<String> msg = new ArrayList<>();
		
		// Collect & format horse data
		String speedFmt = new DecimalFormat("#.####").format(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
		String jumpFmt = new DecimalFormat("#.###").format(horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).getBaseValue());
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
