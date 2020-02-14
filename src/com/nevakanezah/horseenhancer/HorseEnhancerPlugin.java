package com.nevakanezah.horseenhancer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.nevakanezah.horseenhancer.util.StorableHashMap;

/**
 * 
 * @author Nevakanezah
 * 
 * HorseEnhancer:
 * All-natural enhancements to vanilla horses
 * Much reference made to https://github.com/AnomalyTea/Horse-Inspector/tree/master/src/com/anomalytea/HorseInspector,
 * whose work was vital in learning how these parts should fit together.
 *
 */

public class HorseEnhancerPlugin extends JavaPlugin {
	
	// Global list of custom horse data
	private StorableHashMap<UUID, HorseData> horses = null;
	
	private Logger logger = this.getLogger();

	@Override
	public void onDisable() {
		ArrayList<UUID> invalidHorses = new ArrayList<>();
		
		horses.forEach((k,v) -> checkInvalid(k,invalidHorses));
		if(!invalidHorses.isEmpty()) {
			String msg = "Unloading [" + horses.size() +"] invalid horses.";
			getLogger().log(Level.INFO, msg);
		}
		invalidHorses.forEach(k -> horses.remove(k));
		
	    try { horses.saveToFile(); } 
	    catch (IOException e)
	    {
	    	getLogger().log(Level.WARNING, "Error: Failed to save horse data!", e);
	    }
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		loadConfig();
		
		try { horses = new StorableHashMap<>(this.getDataFolder(), "Horses"); }
		catch (IOException e)
		{
			getLogger().log(Level.WARNING, "Error: Failed to create horse save file: ", e);
		}
		
	    try{ horses.loadFromFile(); } 
	    catch (ClassNotFoundException | IOException e)
	    {
	        getLogger().log(Level.WARNING, "Error: Failed to load horse data: ", e);
	    }
	    
	    String msg = "Successfully loaded [" + horses.size() +"] horses.";
	    getLogger().log(Level.INFO, msg);
	    
		getServer().getPluginManager().registerEvents(new PlayerAttackHorseEventHandler(this), this);
		getServer().getPluginManager().registerEvents(new HorseTameEventHandler(this), this);
		getServer().getPluginManager().registerEvents(new HorseDeathEventHandler(this), this);
		getServer().getPluginManager().registerEvents(new HorseSpawnEventHandler(this), this);
		
		this.getCommand("he").setExecutor(new CommandHandler(this));
		this.getCommand("horseenhancer").setExecutor(new CommandHandler(this));
		this.getCommand("he").setTabCompleter(new TabComplete());
		this.getCommand("horseenhancer").setTabCompleter(new TabComplete());
	}

	public ArrayList<String> loadConfig() {
		
		ArrayList<String> msg = new ArrayList<>();
		
		this.reloadConfig();
		
		// Populate any options missing from config
		ArrayList<String> configNames = new ArrayList<>();
		configNames.add("gelding-tool");
		configNames.add("inspection-tool");
		configNames.add("childskew-upper");
		configNames.add("childskew-lower");
		configNames.add("gender-ratio");
		configNames.add("enable-inspector");
		configNames.add("enable-inspector-attributes");
		configNames.add("enable-equicide-protection");
		
		for(String item : configNames) {
			if(!this.getConfig().isSet(item)) {
				logger.log(Level.INFO, "Setting config: [" + item + "]");
				this.getConfig().set(
						item, this.getConfig().getDefaults().getString(item));
				this.saveConfig();
			}
		}
		
		// Clamp the skew values between -1 and 1, and ensure that upper is >= lower.
		double upper = this.getConfig().getDouble("childskew-upper");
		double lower = this.getConfig().getDouble("childskew-lower");
		
		if(upper > 1.0)
			this.getConfig().set("childskew-upper", Math.min( 1.0, upper));
		
		if(lower < -1.0)
			this.getConfig().set("childskew-lower", Math.max( -1.0, lower));
		
		if(lower > upper) {
			if(lower <= 0)
			   upper = lower;
			else
			  lower = upper;
		}
		
		logger.log(Level.INFO, "Loaded stat skew: [" + lower + " - " + upper + "]");
		
		double genderRatio = Math.max( 0.0, Math.min( 1.0, this.getConfig().getDouble("gender-ratio")));
		this.getConfig().set("gender-ratio", genderRatio);
		
		logger.log(Level.INFO, "Loaded gender ratio with value of [" + genderRatio + "]");
		logger.log(Level.INFO, "Gelding tool is [" + this.getConfig().getString("gelding-tool") + "]");
		logger.log(Level.INFO, "Inspection tool is [" + this.getConfig().getString("inspection-tool") + "]");
		
		msg.add("[" + this.getDescription().getName() + "] configuration loaded.");
		
		return msg;
	}
	
	
	public StorableHashMap<UUID, HorseData> getHorses(){
		return horses;
	}
	
	private void checkInvalid(UUID id, ArrayList<UUID> invalidHorses)
	{
		
		if(this.getServer().getEntity(id) == null || this.getServer().getEntity(id).isDead())
			invalidHorses.add(id);
	}
}
