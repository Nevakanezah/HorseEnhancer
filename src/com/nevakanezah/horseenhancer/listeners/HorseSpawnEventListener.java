package com.nevakanezah.horseenhancer.listeners;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityBreedEvent;

import com.nevakanezah.horseenhancer.HorseEnhancerPlugin;
import com.nevakanezah.horseenhancer.data.HorseData;
import com.nevakanezah.horseenhancer.data.SecretHorses;
import com.nevakanezah.horseenhancer.util.StorableHashMap;

import net.md_5.bungee.api.ChatColor;

public class HorseSpawnEventListener implements Listener {
	private final HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;
	
	static final String MOVE_SPEED = "GENERIC_MOVEMENT_SPEED";
	static final String MAX_HEALTH = "GENERIC_MAX_HEALTH";
	static final String JUMP_STRENGTH = "HORSE_JUMP_STRENGTH";

	public HorseSpawnEventListener(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	// Register the parent information from breeding events; remaining data will be generated when and if a horse spawns.
	@EventHandler(ignoreCancelled = true)
	public void onHorseBreed(EntityBreedEvent event) {
		if(!(event.getEntity() instanceof AbstractHorse))
			return;
		if(!((event).getBreeder() instanceof Player))
			return;	
		
		AbstractHorse horse = (AbstractHorse) event.getEntity();
		AbstractHorse father = (AbstractHorse) event.getFather();
		AbstractHorse mother = (AbstractHorse) event.getMother();
		
		HorseData horseData = horseList.get(horse.getUniqueId());
		
		if(horseData == null){
			horseData = new HorseData(horse, father, mother, plugin.getConfig().getDouble("gender-ratio"));
		} 
		else 
			return;
	    
	    registerHorse(horse.getUniqueId(), horseData);
	}

	// Register horses that spawn tame. Includes skeleton traps and admin-spawned horses.
	@EventHandler(ignoreCancelled = true)
	public void onHorseSpawn(CreatureSpawnEvent event) {
		if(!(event.getEntity() instanceof AbstractHorse))
			return;
		if(!(event.getSpawnReason().equals(SpawnReason.BREEDING))
			&& !(event.getSpawnReason().equals(SpawnReason.TRAP))
			&& !(event.getSpawnReason().equals(SpawnReason.NATURAL))
			&& !(event.getSpawnReason().equals(SpawnReason.DEFAULT)))
			return;
		if(event.getSpawnReason().equals(SpawnReason.NATURAL) && !(event.getEntity() instanceof SkeletonHorse))
			return;
		
		AbstractHorse horse = (AbstractHorse)event.getEntity();
		HorseData horseData = horseList.get(horse.getUniqueId());
		
		if(horseData == null){
			horseData = new HorseData(horse, null, null, plugin.getConfig().getDouble("gender-ratio"));
		}
		
		if(event.getEntity() instanceof SkeletonHorse && !(event.getSpawnReason().equals(SpawnReason.NATURAL)))
		{
			horseData.setFatherName(ChatColor.DARK_RED + "THUNDER");
			horseData.setMotherName(ChatColor.DARK_RED + "DEATH");
		}
		
		if(event.getSpawnReason().equals(SpawnReason.BREEDING))
		{
			horseData = handleBreedingEvent(event, horseData);
			if(horseData == null)
				return;
		}
		registerHorse(horse.getUniqueId(), horseData);
	}
	
	/**
	 * Handles breeding-specific operations like assigning attributes and parents, as well as inbreeding.
	 * @param event The event that spawned this horse, with spawnReason.BREEDING
	 * @param childData The data for the child that we will operate on
	 * @return The modified childData
	 */
	private HorseData handleBreedingEvent(CreatureSpawnEvent event, HorseData childData) {
		final boolean secretHorsesEnabled = plugin.getConfig().getBoolean("enable-secret-horses");
		// Declare the horses & data containers involved
		AbstractHorse child = (AbstractHorse) event.getEntity();
		AbstractHorse father = (AbstractHorse) Bukkit.getEntity(childData.getFatherID());
		AbstractHorse mother = (AbstractHorse) Bukkit.getEntity(childData.getMotherID());
		HorseData fatherData = plugin.getHorses().get(childData.getFatherID());
		HorseData motherData = plugin.getHorses().get(childData.getMotherID());
		String fatherName = ChatColor.BLUE + "#" + fatherData.getHorseID();
		String motherName = ChatColor.BLUE + "#" + motherData.getHorseID();
		
		// Only horses of opposite gender can breed. If I can figure out how to override target selection in love mode, I will.
		if(!fatherData.genderCompatible(motherData) && child instanceof AbstractHorse){
			event.setCancelled(true);
			return null;
			}
		
		if(father.getCustomName() != null)
			fatherName = ChatColor.GREEN + father.getCustomName() + " " + fatherName;
		if(mother.getCustomName() != null)
			motherName = ChatColor.GREEN + mother.getCustomName() + " " + motherName;
		
		// Minecraft seems to determine father/mother based on the order you fed animals, 
		// so we want to swap if the genders are reversed.
		if(!fatherData.canSire() && motherData.canSire())
		{
			childData.setMotherID(father.getUniqueId());
			childData.setFatherID(mother.getUniqueId());
			childData.setFatherName(motherName);
			childData.setMotherName(fatherName);
		}
		else {
			childData.setFatherName(fatherName);
			childData.setMotherName(motherName);
		}
		
		// Inbreeding results in unhealthy children
		if(fatherData.isRelated(motherData)){
			Location loc = mother.getLocation();
			child.remove();
			
			AbstractHorse newChild = SecretHorses.spawnInbred(loc, childData);
			
			registerHorse(newChild.getUniqueId(), childData);
			return null;
			}
		
		if(secretHorsesEnabled && handleSecretHorses(child, father, mother, childData, fatherData, motherData))
			return childData;
		
		//Apply modified stats to the child
		child.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(
				Math.max( 0.1125, Math.min( 0.3375, getAttributeFromParents(father, mother, MOVE_SPEED ))));
		child.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(
				Math.max( 15, Math.min( 30, getAttributeFromParents(father, mother, MAX_HEALTH ))));
		child.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue( 
				Math.max( 0.4, Math.min( 1.0, getAttributeFromParents(father, mother, JUMP_STRENGTH ) )));
		
		// Coat colouration handling
		if(child.getType().equals(EntityType.HORSE)) {
			((Horse)child).setColor(
					horseColourSelector(((Horse)father).getColor(), ((Horse)mother).getColor()));
		}
		
		return childData;
	}

	/**
	 * Determine the colour of the child based loosely on the colours of the parents,
	 * instead of randomly.
	 * @param father The colour of the father
	 * @param mother The colour of the mother
	 * @return The colour decided for the child
	 */
	private Horse.Color horseColourSelector(Horse.Color father, Horse.Color mother){
		int i;
		int min;
		int max;
		
		// We're going to use the colour's hashcode to figure out likely child colours
		List<Color> ha = Arrays.asList(Horse.Color.values());
		// Minecraft almost numbers colours from light-to-dark, so we're swapping BLACK and DARK_BROWN
		ha.set(4, ha.set(6, ha.get(4)));
		
		// There's probably a more efficient way to do this
		min = Math.min(ha.indexOf(father), ha.indexOf(mother));
		max = Math.max(ha.indexOf(father), ha.indexOf(mother));
		
		// Determine the range of likely colours, then skew it by -1, 0, or 1
		i = Math.max( 0, Math.min( 6
				, ThreadLocalRandom.current().nextInt(min, max+1)
				+ ThreadLocalRandom.current().nextInt(-1, 1)));
		
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
		double skew = ThreadLocalRandom.current().nextDouble(plugin.getConfig().getDouble("childskew-lower"), plugin.getConfig().getDouble("childskew-upper")+0.00001);
		// Defines which parent the child most takes after
		double bias = Math.random(); 
		double result = 0;
		
		result = (father.getAttribute(Attribute.valueOf(attr)).getBaseValue() * bias)
		+ (mother.getAttribute(Attribute.valueOf(attr)).getBaseValue() * (1-bias));
	
		// Convert skew to its value as a % of result, then apply that value to the result
		skew *= result;
		result += skew;
		
		// Health is rounded to the nearest 0.5
		if(attr.contentEquals(MAX_HEALTH))
			result = Math.round(result * 2)/2.0;
		
		return result;
	}
	
	private String registerHorse(UUID uuid, HorseData horseData) {
		String output;
		plugin.getHorses().put(uuid, horseData);
		
		if(plugin.getHorses().containsKey(uuid))
			output = ChatColor.GREEN + "Horse Registered!";
		else
			output = ChatColor.RED + "Failed to register horse!";
		return output;
	}
	
	/**
	 * Evaluates whether to spawn one of the easter egg horse varieties, and handles
	 * spawning and attribute assignment. Secret horses are acquired through breeding
	 * parents with particular circumstances, and only one secret horse may exist 
	 * at a time on the server.
	 * @param child The new horse entity to update or replace
	 * @param father The horse's father
	 * @param mother The horse's mother
	 * @param childData The horse's data container to be updated
	 * @param fatherData The father's data container, for evaluating conditions
	 * @param motherData The mother's data container, for evaluating conditions
	 * @return true if a secret horse was born, false otherwise.
	 */
	private boolean handleSecretHorses(AbstractHorse child, AbstractHorse father, AbstractHorse mother,
			HorseData childData, HorseData fatherData, HorseData motherData) {	
		Boolean specialHorseSpawned = false;
		Location loc = child.getLocation();
		double fSpeed = father.getAttribute(Attribute.valueOf(MOVE_SPEED)).getBaseValue();
		double fHealth = father.getAttribute(Attribute.valueOf(MAX_HEALTH)).getBaseValue();
		double fJump = father.getAttribute(Attribute.valueOf(JUMP_STRENGTH)).getBaseValue();
		double mSpeed = mother.getAttribute(Attribute.valueOf(MOVE_SPEED)).getBaseValue();
		double mHealth = mother.getAttribute(Attribute.valueOf(MAX_HEALTH)).getBaseValue();
		double mJump = mother.getAttribute(Attribute.valueOf(JUMP_STRENGTH)).getBaseValue();
		
		// A better-than-vanilla horse born to an unlikely couple
		boolean maximule = false;
		
		// Why is it called invincible if I can still see it? Well not anymore!
		boolean invincible = false;
		
		for(HorseData horseData : horseList.values()) {
			if(horseData.getGenderName().equalsIgnoreCase("UNIQUE"))
				return false;
		}
		
		if(((father instanceof Horse && mother instanceof Donkey)
				&& (0.1125 <= fSpeed && fSpeed <= 0.135)
				&& (0.4 <= fJump && fJump <= 0.46)
				&& (15 <= fHealth && fHealth < 17)
				&& (28.5 <= mHealth && mHealth <= 30))
		  || ((father instanceof Donkey && mother instanceof Horse)
				&& (0.1125 <= mSpeed && mSpeed <= 0.135)
				&& (0.4 <= mJump && mJump <= 0.46)
				&& (15 <= mHealth && mHealth < 17)
				&& (28.5 <= fHealth && fHealth <= 30))) {
			maximule = true;
		}
		else if(father.hasPotionEffect(PotionEffectType.INVISIBILITY) 
				&& ((Horse)father).getInventory().getArmor().getType().equals(Material.GOLD_BARDING)
				&& mother.hasPotionEffect(PotionEffectType.INVISIBILITY)
				&& ((Horse)mother).getInventory().getArmor().getType().equals(Material.GOLD_BARDING)) {
			invincible = true;
		}
				
		if(maximule || invincible)
			child.remove();
		
		if(maximule) {
			SecretHorses.spawnMaximule(loc, childData);
			specialHorseSpawned = true;
		}
		
		if(invincible) {
			SecretHorses.spawnInvincible(loc, childData);
			specialHorseSpawned = true;
		}

		
		return specialHorseSpawned;
	}
	
}
