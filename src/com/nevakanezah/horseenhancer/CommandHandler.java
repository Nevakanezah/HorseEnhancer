package com.nevakanezah.horseenhancer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;

import com.nevakanezah.horseenhancer.util.StorableHashMap;

import net.md_5.bungee.api.ChatColor;

public class CommandHandler implements CommandExecutor {

	private HorseEnhancerPlugin plugin;
	private StorableHashMap<UUID, HorseData> horseList;
	
	private final String onlyForPlayersMessage = "This command is only available to players.";
	
	public CommandHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
		horseList = plugin.getHorses();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if(sender instanceof Player && !((Player) sender).isOp())
		{
			((Player) sender).sendMessage(ChatColor.RED + "Error: Requires op permission!");
			return true;
		}
		
		if(args.length == 0)
			return showUsage(sender);

		boolean result = false;
		
		switch(args[0].toLowerCase()) {
			case "reload":
				result = pluginReload(sender);
				break;
			case "list":
				result = showList(sender, horseList);
				break;
			case "help":
				result = showHelp(sender);
				break;
			case "genderratio":
				result = genderRatio(sender, args);
				break;
			case "statskew":
				result = statSkew(sender, args);
				break;
			case "inspect":
				result = inspectHorse(sender, args);
				break;
			case "tp":
				result = horseTeleport(sender, args);
				break;
			case "tphere":
				result = horseTeleport(sender, args);
				break;
			case "summon":
				result = horseSummon(sender, args);
				break;
			default:
				result = showUsage(sender);
		}
		
		return result;
	}
	
	private boolean showUsage(CommandSender sender) {
		if(sender instanceof Player) {
			sender.sendMessage(ChatColor.DARK_PURPLE + "HorseEnhancer version 0.0.2a by "
					+ ChatColor.BLUE + "Nev"
					+ ChatColor.DARK_GREEN + "a"
					+ ChatColor.GOLD + "ka"
					+ ChatColor.DARK_RED + "nez"
					+ ChatColor.LIGHT_PURPLE + "ah");
			sender.sendMessage(ChatColor.DARK_PURPLE + "For Minecraft " + ChatColor.GREEN + "1.12.2");
			sender.sendMessage(ChatColor.DARK_PURPLE + "Aliases:" + ChatColor.GREEN +" /horseenhancer, /he");
			sender.sendMessage(ChatColor.DARK_PURPLE + "Use " + ChatColor.GREEN +"/horseenhancer help" + ChatColor.DARK_PURPLE + " for a list of commands.");
		} 
		if(sender instanceof ConsoleCommandSender) {
			sender.sendMessage("HorseEnhancer version 0.0.2a by Nevakanezah for MC 1.12.2.");
			sender.sendMessage("Aliases: horseenhancer, he");
			sender.sendMessage("Use: 'horseenhancer help' to see a list of commands.");
		}
		return true;
	}
	
	private boolean pluginReload(CommandSender sender) {
		ArrayList<String> msg = plugin.loadConfig();
		for(String m : msg) {
			if(sender instanceof Player)
				sender.sendMessage(ChatColor.GREEN + m);
			if(sender instanceof ConsoleCommandSender)
				sender.sendMessage(m);
		}
		return true;
	}
	
	private boolean showList(CommandSender sender, StorableHashMap<UUID, HorseData> horseList) {
		if(sender instanceof Player) {
			if(horseList.isEmpty()) {
				sender.sendMessage(ChatColor.DARK_PURPLE + "There are no registered horses.");
			} else {
				sender.sendMessage(ChatColor.DARK_PURPLE + "There are currently [" + horseList.size() + "] registered horses");
				sender.sendMessage(ChatColor.DARK_PURPLE + "---");
				horseList.forEach((k,v) -> sender.sendMessage(listMessage(k,v)));
			}
			return true;
		}
		else if(sender instanceof ConsoleCommandSender) {
			sender.sendMessage("There are currently [" + horseList.size() + "] registered horses");
			sender.sendMessage("---");
			horseList.forEach((k,v) -> sender.sendMessage("[" + k + "]\n"));
			return true;
		}
		return false;
	}
	
	private String listMessage(UUID id, HorseData horseData) {
		String msg = ChatColor.BLUE + "#" + horseData.getHorseID();
		AbstractHorse horse = ((AbstractHorse)Bukkit.getEntity(id));
		
		if(horse.getCustomName() != null)
			msg += " " + ChatColor.GREEN + horse.getCustomName();
		if(horse.getOwner() != null)
			msg += " " + ChatColor.DARK_PURPLE + horse.getOwner().getName();
		else
			msg += " " + ChatColor.DARK_PURPLE + "Untamed";
		
		return msg;
	}
	
	private boolean showHelp(CommandSender sender) {
		if(sender instanceof ConsoleCommandSender) {
			sender.sendMessage("HorseEnhancer commands:");
			sender.sendMessage("/he help\tShow this dialog");
			sender.sendMessage("/he reload\tReload plugin configuration");
			sender.sendMessage("/he list\tList all registered horse IDs");
			sender.sendMessage("/he genderRatio [0.0 - 1.0]");
			sender.sendMessage("\tChange the percentage of horses born male.");
			sender.sendMessage("/he statSkew [min] [max]");
			sender.sendMessage("\tChange the degree by which foal stats can be better"
					+ "\n\t\t\tor worse than their parents.");
			sender.sendMessage("\tValues must be between -1.0 and 1.0.");
			sender.sendMessage("/he genderRatio [0.0 - 1.0]");
			sender.sendMessage("\tChange the percentage of horses born male.");
		}
		if(sender instanceof Player) {
			sender.sendMessage(ChatColor.DARK_PURPLE + "HorseEnhancer commands:");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he help" + ChatColor.YELLOW + " Show this dialog");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he reload" + ChatColor.YELLOW + " Reload plugin configuration");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he list" + ChatColor.YELLOW + " List all registered horse IDs");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he genderRatio [0.0 - 1.0]" + ChatColor.YELLOW + " Change the percentage of horses born male.");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he statSkew [-1.0] [1.0]" + ChatColor.YELLOW + " Range by which foal stats can differ from their parents.");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he inspect [horseID|horseCustomName]" + ChatColor.YELLOW + " Show inspection details for the specified horse.");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he tp [horseID|horseName]" + ChatColor.YELLOW + " Teleport yourself to the specified horse.");
			sender.sendMessage(ChatColor.DARK_PURPLE + "  /he tphere [horseID|horseName]" + ChatColor.YELLOW + " Teleport the specified horse to your location.");
			sender.sendMessage(ChatColor.DARK_PURPLE + "---");
		}
		return true;
	}
	
	private boolean genderRatio(CommandSender sender, String[] args) {
		boolean valid = true;
		double value = plugin.getConfig().getDouble("gender-ratio");
		
		try{
			value = Double.parseDouble(args[1]);
		} catch(NumberFormatException e) {
			valid = false;
		}
		
		if(!valid || args.length < 2 || value < 0.0 || value > 1.0) {
			if(sender instanceof Player)
				sender.sendMessage(ChatColor.DARK_PURPLE + "Usage: " + ChatColor.DARK_PURPLE + "/he genderratio [0.0 - 1.0]");
			if(sender instanceof ConsoleCommandSender)
				sender.sendMessage("Usage: /he genderRatio [0.0 - 1.0]");
		}
		else {
			plugin.getConfig().set("gender-ratio", value);
			plugin.saveConfig();
			
			if(sender instanceof Player)
				sender.sendMessage(ChatColor.DARK_PURPLE + "Gender ratio is now [" + ChatColor.GREEN + value + ChatColor.DARK_PURPLE + "]");
			if(sender instanceof ConsoleCommandSender)
				sender.sendMessage("Gender ratio is now [" + value + "]");
		}
		
		return valid;
	}

	private boolean statSkew(CommandSender sender, String[] args) {
		boolean valid = true;
		double low = plugin.getConfig().getDouble("childskew-lower");
		double high = plugin.getConfig().getDouble("childskew-upper");
		
		try{
			low = Double.parseDouble(args[1]);
			high = Double.parseDouble(args[2]);
		} catch(NumberFormatException e) {
			valid = false;
		}
		
		if(high < low)
		{
			Double tmp = low;
			low = high;
			high = tmp;
		}
	
		low = low < -1.0 ? -1.0 : low;
		high = high > 1.0 ? 1.0 : high;
		
		if(!valid || args.length < 3) {
			if(sender instanceof Player)
				sender.sendMessage(ChatColor.DARK_PURPLE + "Usage: " + ChatColor.DARK_PURPLE + "/he statSkew [min] [max]");
			if(sender instanceof ConsoleCommandSender)
				sender.sendMessage("Usage: /he statSkew [min] [max]");
		}
		else {
			plugin.getConfig().set("childskew-lower", low);
			plugin.getConfig().set("childskew-upper", high);
			plugin.saveConfig();
			
			if(sender instanceof Player)
				sender.sendMessage(ChatColor.DARK_PURPLE + "Skew range is now [" 
						+ ChatColor.GREEN + low + ChatColor.DARK_PURPLE 
						+ " - " + ChatColor.GREEN + high + ChatColor.DARK_PURPLE + "]");
			if(sender instanceof ConsoleCommandSender)
				sender.sendMessage("Skew range is now [" + low + " - " + high + "]");
		}
		
		return valid;
	}
	
	private boolean inspectHorse(CommandSender sender, String[] args) {
		if(sender instanceof ConsoleCommandSender)
			sender.sendMessage(onlyForPlayersMessage);
		
		if(args.length < 2)
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage: /horseenhancer inspect [horseID|horseCustomName]");
		
		String searchParam = args[1].startsWith("#") ? args[1].replace("#", "") : args[1];
		
		horseList.forEach((k,v) -> reportMatchingHorses(k, v, (Player)sender, searchParam));
		return true;
	}
	
	private void reportMatchingHorses(UUID id, HorseData horseData, Player player, String searchParam) {
		AbstractHorse horse = (AbstractHorse)Bukkit.getEntity(id);
		
		if(!horseData.getHorseID().equalsIgnoreCase(searchParam) && (horse.getCustomName() == null || !horse.getCustomName().equalsIgnoreCase(searchParam)))
			return;
		
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
	
	private boolean horseTeleport(CommandSender sender, String[] args) {
		boolean result = true;
		
		if(sender instanceof ConsoleCommandSender) {
			sender.sendMessage(onlyForPlayersMessage);
			return false;
		}
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage: /horseenhancer tp [horseID|horseCustomName]");
			return false;
		}
		
		String searchParam = args[1];
		
		if(searchParam.startsWith("#"))
			searchParam = searchParam.replace("#", "");
		
		ArrayList<UUID> matches = new ArrayList<>();
		for(HorseData horseData : horseList.values()) {
			AbstractHorse horse = (AbstractHorse)Bukkit.getEntity(horseData.getUniqueID());
			String name = horse.getCustomName();
			
			if(horseData.getHorseID().equalsIgnoreCase(searchParam) || (name != null && name.equalsIgnoreCase(searchParam)))
				matches.add(horseData.getUniqueID());
		}
		
		if(matches.isEmpty())
			sender.sendMessage(ChatColor.RED + "No horses found matching: " + args[1]);
		
		if(matches.size() > 1) {
			sender.sendMessage(ChatColor.DARK_PURPLE + "Found multiple horses matching " + ChatColor.GREEN + args[1] + ChatColor.DARK_PURPLE + ":");
			matches.forEach(k -> sender.sendMessage(ChatColor.BLUE + "#" + horseList.get(k).getHorseID() + " " 
					+ ChatColor.GREEN + Bukkit.getEntity(k).getCustomName()));
		}
		
		if(matches.size() == 1) {
			if(args[0].equals("tp"))
				result = ((Player)sender).teleport(Bukkit.getEntity(matches.get(0)).getLocation());
			if(args[0].equals("tphere"))
				result = Bukkit.getEntity(matches.get(0)).teleport((Player)sender);
		}
		
		return result;
	}
	
	private boolean horseSummon(CommandSender sender, String[] args) {
		throw new NotImplementedException();
	}
}
