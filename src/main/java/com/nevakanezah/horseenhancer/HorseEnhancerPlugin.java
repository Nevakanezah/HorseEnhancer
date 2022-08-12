package com.nevakanezah.horseenhancer;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.nevakanezah.horseenhancer.data.HorseData;
import com.nevakanezah.horseenhancer.listeners.HorseDeathEventListener;
import com.nevakanezah.horseenhancer.listeners.HorseGeldingListener;
import com.nevakanezah.horseenhancer.listeners.HorseInspectionListener;
import com.nevakanezah.horseenhancer.listeners.HorseSpawnEventListener;
import com.nevakanezah.horseenhancer.listeners.HorseTameEventListener;
import com.nevakanezah.horseenhancer.util.StorableHashMap;

/**
 * HorseEnhancer:
 * All-natural enhancements to equine husbandry in Minecraft.
 * 
 * @author Nevakanezah
 *
 */

public class HorseEnhancerPlugin extends JavaPlugin {
	
	// Global list of custom horse data
	private StorableHashMap<UUID, HorseData> horses = null;
	
	private Logger logger = this.getLogger();

	@Override
	public void onDisable() {
		purgeInvalidHorses(true);
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		loadConfig();
		
		FileConfiguration config = this.getConfig();
		
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
	    
		getServer().getPluginManager().registerEvents(new HorseTameEventListener(this), this);
		getServer().getPluginManager().registerEvents(new HorseDeathEventListener(this), this);
		getServer().getPluginManager().registerEvents(new HorseSpawnEventListener(this), this);
		getServer().getPluginManager().registerEvents(new HorseGeldingListener(this), this);
		
		if(!(config.getString("enable-inspector").toLowerCase().equalsIgnoreCase("false")))
			getServer().getPluginManager().registerEvents(new HorseInspectionListener(this), this);
		if(config.getBoolean("enable-equicide-protection"))
			getServer().getPluginManager().registerEvents(new HorseSpawnEventListener(this), this);
		
		this.getCommand("he").setExecutor(new CommandHandler(this));
		this.getCommand("horseenhancer").setExecutor(new CommandHandler(this));
		this.getCommand("he").setTabCompleter(new TabComplete());
		this.getCommand("horseenhancer").setTabCompleter(new TabComplete());
	}

	public String loadConfig() {
		
		FileConfiguration config = this.getConfig();
		
		config.options().header("HorseEnhancer by Nevakanezah\n\n For info on the configs, see https://pastebin.com/Q623SBwY");
		
		config.addDefault("gelding-tool", "shears");
		config.addDefault("inspection-tool", "watch");
		config.addDefault("childskew-upper", 0.05);
		config.addDefault("childskew-lower", -0.1);
		config.addDefault("gender-ratio", 0.125);
		config.addDefault("enable-inspector", true);
		config.addDefault("enable-inspector-attributes", true);
		config.addDefault("enable-equicide-protection", true);
		config.addDefault("enable-secret-horses", true);
		
		
		config.options().copyDefaults(true);
		saveConfig();
		
		// Clamp the skew values between -1 and 1, and ensure that upper is >= lower.
		double upper = config.getDouble("childskew-upper");
		double lower = config.getDouble("childskew-lower");
		
		if(lower > upper) {
			if(lower <= 0)
			   upper = lower;
			else
			  lower = upper;
		}
		
		if(upper > 1.0)
			config.set("childskew-upper", Math.min( 1.0, upper));
		
		if(lower < -1.0)
			config.set("childskew-lower", Math.max( -1.0, lower));
		
		logger.log(Level.INFO, "Loaded stat skew: [" + lower + " - " + upper + "]");
		
		double genderRatio = Math.max( 0.0, Math.min( 1.0, config.getDouble("gender-ratio")));
		config.set("gender-ratio", genderRatio);
		
		logger.log(Level.INFO, "Loaded gender ratio with value of [" + genderRatio + "]");
		logger.log(Level.INFO, "Gelding tool is [" + config.getString("gelding-tool") + "]");
		logger.log(Level.INFO, "Inspection tool is [" + config.getString("inspection-tool") + "]");
		
		String msg = "HorseEnhancer configuration loaded successfully.";
		logger.log(Level.INFO, msg);
		
		return msg;
	}
	
	
	public StorableHashMap<UUID, HorseData> getHorses(){
		return horses;
	}
	
	public void purgeInvalidHorses() {
		purgeInvalidHorses(false);
	}
	
	public void purgeInvalidHorses(Boolean doLogOutput)
	{
		int invalidHorses = 0;
		Iterator<UUID> horseKeys = horses.keySet().iterator();
		while(horseKeys.hasNext()) {
			UUID horseId = horseKeys.next();
			if(this.getServer().getEntity(horseId) == null || this.getServer().getEntity(horseId).isDead()) {
				horseKeys.remove();
				invalidHorses++;
			}
		}
		
		if(invalidHorses > 0 && Boolean.TRUE.equals(doLogOutput)) {
			String msg = "Unloading [" + invalidHorses +"] invalid horses.";
			getLogger().log(Level.INFO, msg);
		}
		
	    try { horses.saveToFile(); } 
	    catch (IOException e)
	    {
	    	getLogger().log(Level.WARNING, "Error: Failed to save horse data!", e);
	    }
	}
}
