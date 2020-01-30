package com.nevakanezah.horseenhancer;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
//import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class HorseBreedEventHandler implements Listener {
	
	private final HorseEnhancerPlugin plugin;

	public HorseBreedEventHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onHorseBreed(EntityBreedEvent event) {
		
		if(event.isCancelled())
			return;
		
		// Only catch horses bred by players
		if(!(event.getBreeder() instanceof Player && event.getEntity() instanceof AbstractHorse)) { return; }
		
		// EntityBreedEvent is called by both parents, so we don't want to duplicate our work
		if(plugin.getHorses().containsKey(event.getEntity().getUniqueId()))
			return;
		
	    // Declare the horses & data containers involved
		AbstractHorse child = (AbstractHorse) event.getEntity();
		AbstractHorse father = (AbstractHorse) event.getFather();
		AbstractHorse mother = (AbstractHorse) event.getMother();
		
		HorseData fatherData = plugin.getHorses().get(father.getUniqueId());
		HorseData motherData = plugin.getHorses().get(mother.getUniqueId());
		
		// Only horses of opposite gender can breed. If I can figure out how to override target selection in love mode, I will.
		if(!fatherData.genderCompatible(motherData) && child instanceof AbstractHorse && !child.equals(father) && !child.equals(mother)){
			// Refund the items used in breeding, and cancel the event
			// Deprecated until needed.
//			Location loc = child.getLocation();
//			ItemStack breedItem = event.getBredWith();
//			breedItem.setAmount(2);
//			Item dropitem = loc.getWorld().dropItem(loc.clone().add(0.5, 1.2, 0.5), breedItem);
//			dropitem.setVelocity(new Vector());
			
			child.remove();
			event.setCancelled(true);
			return;
			}
		
		double genderRatio = plugin.getConfig().getDouble("gender-ratio");			
		
		HorseData childData = new HorseData(event, genderRatio);
		
		// Minecraft seems to determine father/mother based on the order you fed animals, 
		// so we want to swap if the genders are reversed.
		if(!fatherData.canSire() && motherData.canSire())
		{
			childData.setMother(mother);
			childData.setFather(father);
		}
		
		// Inbreeding results in unhealthy children
		if(fatherData.isRelated(motherData)){
			Location loc = child.getLocation();
			event.setCancelled(true);
			child.remove();
			
			child = (AbstractHorse)loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE_HORSE);
			childData.setUniqueID(child.getUniqueId());
			
			child.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
			child.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10);
			child.setJumpStrength(0.3);
			child.setAgeLock(true);
			child.setBreed(false);
			
			childData.setGender("INBRED");
					
			return;
			}
		
		//Apply modified stats to the child
		child.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(
				Math.max( 0.1125, Math.min( 0.3375, getAttributeFromParents(father, mother, "GENERIC_MOVEMENT_SPEED" ))));
		
		child.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(
				Math.max( 15, Math.min( 30, getAttributeFromParents(father, mother, "GENERIC_MAX_HEALTH" ))));
		
		child.setJumpStrength( Math.max( 0.4, Math.min( 1.0, getAttributeFromParents(father, mother, "JUMP_STRENGTH" ) )));
		
		// Coat colouration handling
		if(child.getType().equals(EntityType.HORSE)) {
			((Horse)child).setColor(
					horseColourSelector(((Horse)father).getColor(), ((Horse)mother).getColor()));
		}
		
		// Write the finalized horseData to the list.
		plugin.getHorses().put(child.getUniqueId(), childData);
		
		if(plugin.getHorses().containsKey(child.getUniqueId()))
			((Player) event.getBreeder()).sendMessage(ChatColor.GREEN + "Horse Registered!");
		else
			((Player) event.getBreeder()).sendMessage(ChatColor.RED + "Failed to register horse!");
	}
	
	/**
	 * Determine the colour of the child based loosely on the colours of the parents,
	 * instead of randomly.
	 * @param father The colour of the father
	 * @param mother The colour of the mother
	 * @return The colour decided for the child
	 */
	private Horse.Color horseColourSelector(Horse.Color father, Horse.Color mother){
		int i, min, max;
		
		// We're going to use the colour's hashcode to figure out likely child colours
		List<Color> ha = Arrays.asList(Horse.Color.values());
		// Minecraft almost numbers colours from light-to-dark, so we're swapping BLACK and DARK_BROWN
		ha.set(4, ha.set(6, ha.get(4)));
		
		// There's probably a more efficient way to do this
		min = Math.min(ha.indexOf(father), ha.indexOf(mother));
		max = Math.max(ha.indexOf(father), ha.indexOf(mother));
		
		// Determine the range of likely colours, then skew it by -1, 0, or 1
		i = (int) Math.max( 0, Math.min( 6
				, ThreadLocalRandom.current().nextInt(min, max+1)
				+ (int)(Math.round(Math.random() * 2) - 1)));
		
		return ha.get(i);
	}
	
	/**
	 * Calculate the stat value the child should have using the following formula:
	 * (Father * r) + (Mother * (1-r)) + k
	 * where r is a random value representing which parent the foal takes after most
	 * And k is a bounded random percentage by which to skew the result, so children can differ from their parents
	 * @param father The father horse object
	 * @param mother The mother horse object
	 * @param attr A string representation of the attribute value to collect.
	 * @return The calculated attribute value for the child, unbounded.
	 */
	protected double getAttributeFromParents(AbstractHorse father, AbstractHorse mother, String attr) {
		// Child stats are set within the range defined by their parents' stats, then skewed by a % value
		double skew = ThreadLocalRandom.current().nextDouble(plugin.getConfig().getDouble("childskew-lower"), plugin.getConfig().getDouble("childskew-upper")+0.001);
		// Defines which parent the child most takes after
		double bias = Math.random(); 
		double result = 0;
		
		if(attr.contentEquals("GENERIC_MOVEMENT_SPEED")
			|| attr.contentEquals("GENERIC_MAX_HEALTH")) {
			result = (father.getAttribute(Attribute.valueOf(attr)).getBaseValue() * bias)
			+ ((mother.getAttribute(Attribute.valueOf(attr)).getBaseValue() * (1-bias)));
		}
		else if(attr.contentEquals("JUMP_STRENGTH")) {
			result = (father.getJumpStrength() * bias)
			+ (mother.getJumpStrength() * (1-bias));
		}
		
		// Convert skew to its value as a % of result, then apply that value to the result
		skew *= result;
		result += skew;
		
		// Health is rounded to the nearest 0.5
		if(attr.contentEquals("GENERIC_MAX_HEALTH"))
			result = Math.round(result * 2)/2;
		
		return result;
	}
}
