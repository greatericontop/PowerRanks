package nl.svenar.powerranks.bukkit.commands.usertags;

import java.util.ArrayList;

import com.google.common.collect.ImmutableMap;

import nl.svenar.powerranks.bukkit.PowerRanks;
import nl.svenar.powerranks.bukkit.commands.PowerCommand;
import nl.svenar.powerranks.bukkit.data.Users;
import nl.svenar.powerranks.common.utils.PRUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class cmd_removeusertag extends PowerCommand {

	private Users users;

	public cmd_removeusertag(PowerRanks plugin, String command_name, COMMAND_EXECUTOR ce) {
		super(plugin, command_name, ce);
		this.users = new Users(plugin);
		this.setCommandPermission("powerranks.cmd." + command_name.toLowerCase());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String commandName,
			String[] args) {
		if (!PowerRanks.plugin_hook_deluxetags) {
			if (args.length == 1) {
				final String tag = args[0];
				final boolean result = this.users.removeUserTag(tag);
				if (result) {
					sender.sendMessage(PRUtil.powerFormatter(
							PowerRanks.getLanguageManager().getFormattedMessage(
									"commands." + commandName.toLowerCase() + ".success"),
							ImmutableMap.<String, String>builder()
									.put("player", sender.getName())
									.put("usertag", tag)
									.build(),
							'[', ']'));
				} else {
					sender.sendMessage(PRUtil.powerFormatter(
							PowerRanks.getLanguageManager().getFormattedMessage(
									"commands." + commandName.toLowerCase() + ".failed"),
							ImmutableMap.<String, String>builder()
									.put("player", sender.getName())
									.put("usertag", tag)
									.build(),
							'[', ']'));
				}
			} else {
				sender.sendMessage(
						PowerRanks.getLanguageManager().getFormattedUsageMessage(commandLabel, commandName,
								"commands." + commandName.toLowerCase() + ".arguments", sender instanceof Player));
			}
		} else {
				sender.sendMessage(
						PowerRanks.getLanguageManager().getFormattedMessage(
								"commands." + commandName.toLowerCase() + ".disabled"));
		}

		return false;
	}

	public ArrayList<String> tabCompleteEvent(CommandSender sender, String[] args) {
		ArrayList<String> tabcomplete = new ArrayList<String>();

		if (args.length == 1) {
			for (String tag : this.users.getUserTags()) {
				if (tag.toLowerCase().contains(args[0].toLowerCase()))
					tabcomplete.add(tag);
			}
		}

		return tabcomplete;
	}
}
