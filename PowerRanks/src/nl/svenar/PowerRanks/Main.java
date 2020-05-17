package nl.svenar.PowerRanks;

import org.bukkit.command.ConsoleCommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.permissions.PermissionAttachment;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandExecutor;
import nl.svenar.PowerRanks.Commands.Cmd;
import nl.svenar.PowerRanks.Events.OnBuild;
import nl.svenar.PowerRanks.Events.OnChat;
import nl.svenar.PowerRanks.Events.OnJoin;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import nl.svenar.PowerRanks.api.Rank;
import nl.svenar.PowerRanks.metrics.Metrics;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	public PluginDescriptionFile pdf;
	public String colorChar;
	public ChatColor black;
	public ChatColor aqua;
	public ChatColor red;
	public ChatColor dark_red;
	public ChatColor blue;
	public ChatColor dark_blue;
	public ChatColor reset;
	public String plp;
	public Logger log;
	public String configFileLoc;
	public String fileLoc;
	File configFile;
	File ranksFile;
	File playersFile;
	FileConfiguration config;
	FileConfiguration ranks;
	FileConfiguration players;
	protected UpdateChecker updatechecker;
	public String updatemsg;
	public Map<String, PermissionAttachment> playerPermissionAttachment = new HashMap<String, PermissionAttachment>();
	public Map<Player, String> playerTablistNameBackup = new HashMap<Player, String>();

	public Main() {
		this.pdf = this.getDescription();
		this.colorChar = "&";
		this.black = ChatColor.BLACK;
		this.aqua = ChatColor.AQUA;
		this.red = ChatColor.RED;
		this.dark_red = ChatColor.DARK_RED;
		this.blue = ChatColor.BLUE;
		this.dark_blue = ChatColor.DARK_BLUE;
		this.reset = ChatColor.RESET;
		this.plp = this.black + "[" + this.aqua + this.pdf.getName() + this.black + "]" + this.reset + " ";
		this.configFileLoc = this.getDataFolder() + File.separator;
		this.fileLoc = this.getDataFolder() + File.separator + "Ranks" + File.separator;
		this.updatemsg = "";
	}

	public void onEnable() {
		this.log = this.getLogger();
		Rank.main = this;
		Bukkit.getServer().getPluginManager().registerEvents((Listener) this, (Plugin) this);
		Bukkit.getServer().getPluginManager().registerEvents((Listener) new OnJoin(this), (Plugin) this);
		Bukkit.getServer().getPluginManager().registerEvents((Listener) new OnChat(this), (Plugin) this);
		Bukkit.getServer().getPluginManager().registerEvents((Listener) new OnBuild(this), (Plugin) this);
		Bukkit.getServer().getPluginCommand("powerranks").setExecutor((CommandExecutor) new Cmd(this));
		Bukkit.getServer().getPluginCommand("pr").setExecutor((CommandExecutor) new Cmd(this));
		this.createDir(this.fileLoc);
		this.log.info("Enabled " + this.pdf.getName() + " v" + this.pdf.getVersion().replaceAll("[a-zA-Z]", ""));
//		this.log.info("By: " + this.pdf.getAuthors().get(0));
		this.configFile = new File(this.getDataFolder(), "config.yml");
		this.ranksFile = new File(this.fileLoc, "Ranks.yml");
		this.playersFile = new File(this.fileLoc, "Players.yml");
		this.config = (FileConfiguration) new YamlConfiguration();
		this.ranks = (FileConfiguration) new YamlConfiguration();
		this.players = (FileConfiguration) new YamlConfiguration();
		try {
			this.copyFiles();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.loadAllFiles();
		this.verifyConfig();

		for (Player player : this.getServer().getOnlinePlayers()) {
			String playerName = player.getName();
			if (playerPermissionAttachment.get(playerName) == null)
				playerPermissionAttachment.put(playerName, player.addAttachment(this));
		}

		this.setupPermissions();
		final File rankFile = new File(String.valueOf(this.fileLoc) + "Ranks" + ".yml");
		final File playerFile = new File(String.valueOf(this.fileLoc) + "Players" + ".yml");
		final YamlConfiguration rankYaml = new YamlConfiguration();
		final YamlConfiguration playerYaml = new YamlConfiguration();
		try {
			rankYaml.load(rankFile);
			playerYaml.load(playerFile);

			for (final Player player : this.getServer().getOnlinePlayers()) {
				if (playerYaml.getString("players." + player.getUniqueId() + ".rank") == null) {
					playerYaml.set("players." + player.getUniqueId() + ".rank", rankYaml.get("Default"));
				}
			}
			playerYaml.save(playerFile);
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		int pluginId = 7565;
		@SuppressWarnings("unused")
		Metrics metrics = new Metrics(this, pluginId);
	}

	public void onDisable() {
		Bukkit.getServer().getScheduler().cancelTasks(this);

		for (Entry<String, PermissionAttachment> pa : playerPermissionAttachment.entrySet()) {
			pa.getValue().remove();
		}
		playerPermissionAttachment.clear();
		
		for (Entry<Player, String> pa : playerTablistNameBackup.entrySet()) {
			pa.getKey().setPlayerListName(pa.getValue());
		}
		playerTablistNameBackup.clear();

		if (this.log != null && this.pdf != null) {
			this.log.info("Disabled " + this.pdf.getName() + " v" + this.pdf.getVersion().replaceAll("[a-zA-Z]", ""));
		}
	}

	public void createDir(final String path) {
		final File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public void verifyConfig() {
		final File rankFile = new File(String.valueOf(this.fileLoc) + "Ranks" + ".yml");
		final File playerFile = new File(String.valueOf(this.fileLoc) + "Players" + ".yml");
		final File configFile = new File(this.getDataFolder() + File.separator + "config" + ".yml");
		final YamlConfiguration rankYaml = new YamlConfiguration();
		final YamlConfiguration playerYaml = new YamlConfiguration();
		final YamlConfiguration configYaml = new YamlConfiguration();
		try {
			rankYaml.load(rankFile);
			playerYaml.load(playerFile);
			configYaml.load(configFile);

			if (rankYaml.getString("version") == null) {
				rankYaml.set("version", this.pdf.getVersion().replaceAll("[a-zA-Z ]", ""));
			} else {
				if (!rankYaml.getString("version").equalsIgnoreCase(this.pdf.getVersion().replaceAll("[a-zA-Z ]", ""))) {
					printVersionError("Ranks.yml");
				}
			}

			if (playerYaml.getString("version") == null) {
				playerYaml.set("version", this.pdf.getVersion().replaceAll("[a-zA-Z ]", ""));
			} else {
				if (!playerYaml.getString("version").equalsIgnoreCase(this.pdf.getVersion().replaceAll("[a-zA-Z ]", ""))) {
					printVersionError("Players.yml");
				}
			}

			if (configYaml.getString("version") == null) {
				configYaml.set("version", this.pdf.getVersion().replaceAll("[a-zA-Z ]", ""));
			} else {
				if (!configYaml.getString("version").equalsIgnoreCase(this.pdf.getVersion().replaceAll("[a-zA-Z ]", ""))) {
					printVersionError("config.yml");
				}
			}

			rankYaml.save(rankFile);
			playerYaml.save(playerFile);
			configYaml.save(configFile);
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	public void printVersionError(String fileName) {
		this.log.warning("===------------------------------===");
		this.log.warning("              WARNING!");
		this.log.warning("Version mismatch detected in:");
		this.log.warning(fileName);
		this.log.warning(this.pdf.getName() + " may not work with this config.");
		this.log.warning("Manual verification is required.");
		this.log.warning("Visit " + this.pdf.getWebsite() + " for more info.");
		this.log.warning("===------------------------------===");
	}

	private void copyFiles() throws Exception {
		if (!this.configFile.exists()) {
			this.configFile.getParentFile().mkdirs();
			this.copy(this.getResource("config.yml"), this.configFile);
		}
		if (!this.ranksFile.exists()) {
			this.ranksFile.getParentFile().mkdirs();
			this.copy(this.getResource("Ranks.yml"), this.ranksFile);
		}
		if (!this.playersFile.exists()) {
			this.playersFile.getParentFile().mkdirs();
			this.copy(this.getResource("Players.yml"), this.playersFile);
		}
	}

	private void copy(final InputStream in, final File file) {
		try {
			final OutputStream out = new FileOutputStream(file);
			final byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveAllFiles() {
		try {
			this.config.save(this.configFile);
			this.ranks.save(this.ranksFile);
			this.players.save(this.playersFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadAllFiles() {
		try {
			this.config.load(this.configFile);
			this.ranks.load(this.ranksFile);
			this.players.load(this.playersFile);
		} catch (Exception e) {
			System.out.println("-----------------------------");
			this.log.warning("Failed to load the config files (If this is the first time PowerRanks starts you could ignore this message)");
			this.log.warning("Try reloading the server. If this message continues to display report this to the plugin page on bukkit.");
			System.out.println("-----------------------------");
		}
	}

	private void setupPermissions() {
		for (final Player player : Bukkit.getServer().getOnlinePlayers()) {
			this.setupPermissions(player);
			this.updateTablistName(player);
		}
	}

	public void setupPermissions(Player player) {
		final PermissionAttachment attachment = playerPermissionAttachment.get(player.getName());

		final File rankFile = new File(String.valueOf(this.fileLoc) + "Ranks" + ".yml");
		final File playerFile = new File(String.valueOf(this.fileLoc) + "Players" + ".yml");
		final YamlConfiguration rankYaml = new YamlConfiguration();
		final YamlConfiguration playerYaml = new YamlConfiguration();
		try {
			rankYaml.load(rankFile);
			playerYaml.load(playerFile);
			final String rank = playerYaml.getString("players." + player.getUniqueId() + ".rank");
			final List<String> GroupPermissions = (List<String>) rankYaml.getStringList("Groups." + rank + ".permissions");
			final List<String> Inheritances = (List<String>) rankYaml.getStringList("Groups." + rank + ".inheritance");

			if (GroupPermissions != null) {
				for (int i = 0; i < GroupPermissions.size(); i++) {

					boolean enabled = !GroupPermissions.get(i).startsWith("-");
					if (enabled) {
						attachment.setPermission((String) GroupPermissions.get(i), true);
					} else {
						attachment.setPermission((String) GroupPermissions.get(i).replaceFirst("-", ""), false);
					}
				}
			}

			if (Inheritances != null) {
				for (int i = 0; i < Inheritances.size(); i++) {
					List<String> Permissions = (List<String>) rankYaml.getStringList("Groups." + Inheritances.get(i) + ".permissions");
					if (Permissions != null) {
						for (int j = 0; j < Permissions.size(); j++) {

							boolean enabled = !Permissions.get(j).startsWith("-");
							if (enabled) {
								attachment.setPermission((String) Permissions.get(j), true);
							} else {
								attachment.setPermission((String) Permissions.get(j).replaceFirst("-", ""), false);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removePermissions(Player player) {
		final PermissionAttachment attachment = playerPermissionAttachment.get(player.getName());

		final File rankFile = new File(String.valueOf(this.fileLoc) + "Ranks" + ".yml");
		final File playerFile = new File(String.valueOf(this.fileLoc) + "Players" + ".yml");
		final YamlConfiguration rankYaml = new YamlConfiguration();
		final YamlConfiguration playerYaml = new YamlConfiguration();
		try {
			rankYaml.load(rankFile);
			playerYaml.load(playerFile);
			final String rank = playerYaml.getString("players." + player.getUniqueId() + ".rank");
			final List<String> GroupPermissions = (List<String>) rankYaml.getStringList("Groups." + rank + ".permissions");
			final List<String> Inheritances = (List<String>) rankYaml.getStringList("Groups." + rank + ".inheritance");

			if (GroupPermissions != null) {
				for (int i = 0; i < GroupPermissions.size(); ++i) {
					attachment.unsetPermission((String) GroupPermissions.get(i));
				}
			}

			if (Inheritances != null) {
				for (int i = 0; i < Inheritances.size(); ++i) {
					final List<String> Permissions = (List<String>) rankYaml.getStringList("Groups." + Inheritances.get(i) + ".permissions");
					if (Permissions != null) {
						for (int j = 0; j < Permissions.size(); ++j) {
							attachment.unsetPermission((String) Permissions.get(j));
							// attachment.unsetPermission((String) GroupPermissions.get(j));
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void updateTablistName(Player player) {
		playerTablistNameBackup.put(player, player.getPlayerListName());
		
		File configFile = new File(this.getDataFolder() + File.separator + "config" + ".yml");
		File rankFile = new File(String.valueOf(this.fileLoc) + "Ranks" + ".yml");
		File playerFile = new File(String.valueOf(this.fileLoc) + "Players" + ".yml");
		YamlConfiguration configYaml = new YamlConfiguration();
		YamlConfiguration rankYaml = new YamlConfiguration();
		YamlConfiguration playerYaml = new YamlConfiguration();
		try {
			configYaml.load(configFile);
			if (!configYaml.getBoolean("tablist_modification.enabled")) return;
			
			rankYaml.load(rankFile);
			playerYaml.load(playerFile);
			
			String format = configYaml.getString("tablist_modification.format");
			String rank = playerYaml.getString("players." + player.getUniqueId() + ".rank");
			String prefix = rankYaml.getString("Groups." + rank + ".chat.prefix");
			String suffix = rankYaml.getString("Groups." + rank + ".chat.suffix");

			format = Util.replaceAll(format, "[name]", player.getPlayerListName());
			format = Util.replaceAll(format, "[prefix]", prefix);
			format = Util.replaceAll(format, "[suffix]", suffix);
			format = Util.replaceAll(format, "&", "§");
			player.setPlayerListName(format);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public String chatColor(final char altColorChar, final String textToTranslate) {
		final char[] charArray = textToTranslate.toCharArray();
		for (int i = 0; i < charArray.length - 1; ++i) {
			if (charArray[i] == altColorChar && "0123456789AaBbCcDdEeFfKkNnRrLlMmOo".indexOf(charArray[i + 1]) > -1) {
				charArray[i] = '§';
				charArray[i + 1] = Character.toLowerCase(charArray[i + 1]);
			}
		}
		return new String(charArray);
	}

	public void errorMessage(final Player player, final String args) {
		player.sendMessage(ChatColor.RED + "--------" + ChatColor.DARK_BLUE + this.pdf.getName() + ChatColor.RED + "--------");
		player.sendMessage("Argument " + args + " not found");
		player.sendMessage(ChatColor.GREEN + "/pr help");
		player.sendMessage(ChatColor.RED + "--------------------------");
	}

	public void messageNoArgs(final Player player) {
		player.sendMessage(ChatColor.DARK_AQUA + "--------" + ChatColor.DARK_BLUE + this.pdf.getName() + ChatColor.DARK_AQUA + "--------");
		player.sendMessage(ChatColor.GREEN + "/pr help" + ChatColor.DARK_GREEN + " - For the command list.");
		player.sendMessage(new StringBuilder().append(ChatColor.GREEN).toString());
		player.sendMessage(ChatColor.DARK_GREEN + "Authors: " + ChatColor.GREEN + this.pdf.getAuthors());
		player.sendMessage(ChatColor.DARK_GREEN + "Version: " + ChatColor.GREEN + this.pdf.getVersion());
		player.sendMessage(ChatColor.DARK_GREEN + "Bukkit DEV: " + ChatColor.GREEN + this.pdf.getWebsite());
		player.sendMessage(ChatColor.DARK_AQUA + "--------------------------");
	}

	public void messageNoArgs(final ConsoleCommandSender console) {
		console.sendMessage(ChatColor.DARK_AQUA + "--------" + ChatColor.DARK_BLUE + this.pdf.getName() + ChatColor.DARK_AQUA + "--------");
		console.sendMessage(ChatColor.GREEN + "/pr help" + ChatColor.DARK_GREEN + " - For the command list.");
		console.sendMessage(new StringBuilder().append(ChatColor.GREEN).toString());
		console.sendMessage(ChatColor.DARK_GREEN + "Authors: " + ChatColor.GREEN + this.pdf.getAuthors());
		console.sendMessage(ChatColor.DARK_GREEN + "Version: " + ChatColor.GREEN + this.pdf.getVersion());
		console.sendMessage(ChatColor.DARK_GREEN + "Bukkit DEV: " + ChatColor.GREEN + this.pdf.getWebsite());
		console.sendMessage(ChatColor.DARK_AQUA + "--------------------------");
	}

	public void helpMenu(final Player player) {
		player.sendMessage(ChatColor.DARK_AQUA + "--------" + ChatColor.DARK_BLUE + this.pdf.getName() + ChatColor.DARK_AQUA + "--------");
		player.sendMessage(ChatColor.AQUA + "[Optional] <Required> " + ChatColor.DARK_AQUA + "|" + ChatColor.AQUA + " Rank names are casesensetive");
		player.sendMessage(ChatColor.GREEN + "/pr help" + ChatColor.DARK_GREEN + " - Shows this menu");
		player.sendMessage(ChatColor.GREEN + "/pr createrank <rankName>" + ChatColor.DARK_GREEN + " - Create a new rank");
		player.sendMessage(ChatColor.GREEN + "/pr deleterank <rankName>" + ChatColor.DARK_GREEN + " - Delete a rank");
		player.sendMessage(ChatColor.GREEN + "/pr set <playerName> <rankName>" + ChatColor.DARK_GREEN + " - Set someone's rank");
		player.sendMessage(ChatColor.GREEN + "/pr setown <rankName>" + ChatColor.DARK_GREEN + " - Set your own rank");
		player.sendMessage(ChatColor.GREEN + "/pr promote <playerName>" + ChatColor.DARK_GREEN + " - Promote a player to the next rank");
		player.sendMessage(ChatColor.GREEN + "/pr demote <playerName>" + ChatColor.DARK_GREEN + " - Demote a player to the previous rank");
		player.sendMessage(ChatColor.GREEN + "/pr check <playerName>" + ChatColor.DARK_GREEN + " - Check someone's rank");
		player.sendMessage(ChatColor.GREEN + "/pr reload" + ChatColor.DARK_GREEN + " - Reload PowerRanks");
		player.sendMessage(ChatColor.GREEN + "/pr addperm <rank> <permission>" + ChatColor.DARK_GREEN + " - Add a permission to a rank");
		player.sendMessage(ChatColor.GREEN + "/pr delperm <rank> <permission>" + ChatColor.DARK_GREEN + " - Remove a permission from a rank");
		player.sendMessage(ChatColor.GREEN + "/pr setprefix <rank> <prefix>" + ChatColor.DARK_GREEN + " - Change the prefix of a rank");
		player.sendMessage(ChatColor.GREEN + "/pr setsuffix <rank> <suffix>" + ChatColor.DARK_GREEN + " - Change the suffix of a rank");
		player.sendMessage(ChatColor.GREEN + "/pr setchatcolor <rank> <color>" + ChatColor.DARK_GREEN + " - Change the chat color of a rank");
		player.sendMessage(ChatColor.GREEN + "/pr setnamecolor <rank> <color>" + ChatColor.DARK_GREEN + " - Change the name color of a rank");
		player.sendMessage(ChatColor.GREEN + "/pr addinheritance <rank> <inheritance>" + ChatColor.DARK_GREEN + " - Add a inheritance to a rank");
		player.sendMessage(ChatColor.GREEN + "/pr delinheritance <rank> <inheritance>" + ChatColor.DARK_GREEN + " - Remove a inheritance from a rank");
		player.sendMessage(ChatColor.GREEN + "/pr enablebuild <rank>" + ChatColor.DARK_GREEN + " - Enable building on a rank");
		player.sendMessage(ChatColor.GREEN + "/pr disablebuild <rank>" + ChatColor.DARK_GREEN + " - Disable building on a rank");
		player.sendMessage(ChatColor.DARK_AQUA + "--------------------------");
	}

	public void helpMenu(final ConsoleCommandSender console) {
		console.sendMessage(ChatColor.DARK_AQUA + "--------" + ChatColor.DARK_BLUE + this.pdf.getName() + ChatColor.DARK_AQUA + "--------");
		console.sendMessage(ChatColor.AQUA + "[Optional] <Required> " + ChatColor.DARK_AQUA + "|" + ChatColor.AQUA + " Rank names are case-sensetive");
		console.sendMessage(ChatColor.GREEN + "/pr help" + ChatColor.DARK_GREEN + " - Shows this menu");
		console.sendMessage(ChatColor.GREEN + "/pr createrank <rankName>" + ChatColor.DARK_GREEN + " - Create a new rank");
		console.sendMessage(ChatColor.GREEN + "/pr deleterank <rankName>" + ChatColor.DARK_GREEN + " - Delete a rank");
		console.sendMessage(ChatColor.GREEN + "/pr set <playerName> <rankName>" + ChatColor.DARK_GREEN + " - Set someone's rank");
		console.sendMessage(ChatColor.GREEN + "/pr promote <playerName>" + ChatColor.DARK_GREEN + " - Promote a player to the next rank");
		console.sendMessage(ChatColor.GREEN + "/pr demote <playerName>" + ChatColor.DARK_GREEN + " - Demote a player to the previous rank");
		console.sendMessage(ChatColor.GREEN + "/pr check <playerName>" + ChatColor.DARK_GREEN + " - Check someone's rank");
		console.sendMessage(ChatColor.GREEN + "/pr reload" + ChatColor.DARK_GREEN + " - Reload PowerRanks");
		console.sendMessage(ChatColor.GREEN + "/pr addperm <rank> <permission>" + ChatColor.DARK_GREEN + " - Add a permission to a rank");
		console.sendMessage(ChatColor.GREEN + "/pr delperm <rank> <permission>" + ChatColor.DARK_GREEN + " - Remove a permission from a rank");
		console.sendMessage(ChatColor.GREEN + "/pr setprefix <rank> <prefix>" + ChatColor.DARK_GREEN + " - Change the prefix of a rank");
		console.sendMessage(ChatColor.GREEN + "/pr setsuffix <rank> <suffix>" + ChatColor.DARK_GREEN + " - Change the suffix of a rank");
		console.sendMessage(ChatColor.GREEN + "/pr setchatcolor <rank> <color>" + ChatColor.DARK_GREEN + " - Change the chat color of a rank");
		console.sendMessage(ChatColor.GREEN + "/pr setnamecolor <rank> <color>" + ChatColor.DARK_GREEN + " - Change the name color of a rank");
		console.sendMessage(ChatColor.GREEN + "/pr addinheritance <rank> <inheritance>" + ChatColor.DARK_GREEN + " - Add a inheritance to a rank");
		console.sendMessage(ChatColor.GREEN + "/pr delinheritance <rank> <inheritance>" + ChatColor.DARK_GREEN + " - Remove a inheritance from a rank");
		console.sendMessage(ChatColor.GREEN + "/pr enablebuild <rank>" + ChatColor.DARK_GREEN + " - Enable building on a rank");
		console.sendMessage(ChatColor.GREEN + "/pr disablebuild <rank>" + ChatColor.DARK_GREEN + " - Disable building on a rank");
		console.sendMessage(ChatColor.DARK_AQUA + "--------------------------");
	}

	public void errorMessageCheck(final Player player) {
		player.sendMessage(ChatColor.RED + "--------" + ChatColor.DARK_BLUE + this.pdf.getName() + ChatColor.RED + "--------");
		player.sendMessage(ChatColor.AQUA + "[Optional] <Requires>");
		player.sendMessage(ChatColor.GREEN + "/pr check [playerName]");
		player.sendMessage(ChatColor.RED + "--------------------------");
	}

	public void errorArgsSet(final Player player) {
		player.sendMessage(ChatColor.RED + "--------" + ChatColor.DARK_BLUE + this.pdf.getName() + ChatColor.RED + "--------");
		player.sendMessage(ChatColor.AQUA + "[Optional] <Requires>");
		player.sendMessage(ChatColor.GREEN + "/pr set <playerName> <Rank (Case sensitive)>");
		player.sendMessage(ChatColor.RED + "--------------------------");
	}

	public void noPermission(Player player) {
		player.sendMessage(plp + ChatColor.DARK_RED + "You don't have permission to perform this command!");
	}
}
