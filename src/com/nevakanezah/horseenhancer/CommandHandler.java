package com.nevakanezah.horseenhancer;

import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
		
		if(args.length == 0 )
		{
			// If issued by player, send info message
			if(sender instanceof Player)
			{
				((Player) sender).sendMessage(ChatColor.DARK_PURPLE + "HorseEnhancer version 1.0 by Nevakanezah");
				((Player) sender).sendMessage(ChatColor.DARK_PURPLE + "For Minecraft 1.12.2");
				((Player) sender).sendMessage(ChatColor.DARK_PURPLE + "Aliases: /horseenhancer, /he");
			}
			
			return true;
		}
		
		if(args.length >= 1 && args[0].contentEquals("reload"))
		{
			ArrayList<String> msg = plugin.loadConfig();
			
			// If issued by player, send success message
			if(sender instanceof Player)
			{
				for(String m : msg) {
					((Player) sender).sendMessage(ChatColor.GREEN + m);
				}
			}
			
			return true;
		}
		
		return false;
	}

}
