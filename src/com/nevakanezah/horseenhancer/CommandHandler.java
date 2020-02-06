package com.nevakanezah.horseenhancer;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nevakanezah.horseenhancer.util.StorableHashMap;

import net.md_5.bungee.api.ChatColor;

public class CommandHandler implements CommandExecutor {

	private HorseEnhancerPlugin plugin;
	
	public CommandHandler(HorseEnhancerPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		// Require OP permission to run reload
		if(sender instanceof Player && !((Player) sender).isOp())
		{
			((Player) sender).sendMessage(ChatColor.RED + "Error: Requires op permission!");
			return true;
		}
		
		if(args.length == 0 || args == null)
			return showUsage((Player)sender);

		if(sender instanceof Player) {
			boolean result = false;
			
			switch(args[0]) {
				case "reload":
					result = pluginReload((Player)sender);
					break;
				case "list":
					StorableHashMap<UUID, HorseData> horseList;
					horseList = plugin.getHorses();
					result = showList((Player)sender, horseList);
					break;
				default:
					result = showUsage((Player)sender);
			}
			
			return result;
		}
		
		return false;
	}
	
	private boolean showUsage(Player sender) {
		sender.sendMessage(ChatColor.DARK_PURPLE + "HorseEnhancer version 0.0.2a by "
				+ ChatColor.BLUE + "Nev"
				+ ChatColor.DARK_GREEN + "a"
				+ ChatColor.GOLD + "ka"
				+ ChatColor.DARK_RED + "nez"
				+ ChatColor.LIGHT_PURPLE + "ah");
		sender.sendMessage(ChatColor.DARK_PURPLE + "For Minecraft " + ChatColor.GREEN + "1.12.2");
		sender.sendMessage(ChatColor.DARK_PURPLE + "Aliases:" + ChatColor.GREEN +" /horseenhancer, /he");
		sender.sendMessage(ChatColor.DARK_PURPLE + "Use " + ChatColor.GREEN +" /horseenhancer help" + ChatColor.DARK_PURPLE + " for a list of commands.");
		return true;
	}
	
	private boolean pluginReload(Player sender) {
		ArrayList<String> msg = plugin.loadConfig();
		for(String m : msg) {
			sender.sendMessage(ChatColor.GREEN + m);
		}
		return true;
	}
	
	private boolean showList(Player sender, StorableHashMap<UUID, HorseData> horseList) {
		if(horseList.isEmpty()) {
			sender.sendMessage(ChatColor.DARK_PURPLE + "There are no registered horses.");
			return true;
		} else {
			sender.sendMessage(ChatColor.DARK_PURPLE + "There are currently [" + horseList.size() + "] registered horses");
			sender.sendMessage(ChatColor.DARK_PURPLE + "---");
			horseList.forEach((k,v) -> sender.sendMessage(ChatColor.DARK_PURPLE + "[" + k + "]\n"));
			return true;
		}

	}

}
