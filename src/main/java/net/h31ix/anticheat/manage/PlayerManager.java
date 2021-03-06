/*
 * AntiCheat for Bukkit.
 * Copyright (C) 2012 AntiCheat Team | http://h31ix.net
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.h31ix.anticheat.manage;

import java.util.HashMap;
import java.util.Map;
import net.h31ix.anticheat.util.Configuration;
import net.h31ix.anticheat.util.Language;
import net.h31ix.anticheat.util.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * <p>
 * The manager that AntiCheat will use to monitor player's hack levels and to
 * execute events that the admin has set
 */

public class PlayerManager
{
    private static Map<String, Integer> level = new HashMap<String, Integer>();
    private static Configuration config = null;
    private static Language lang;
    private static final int MED_THRESHOLD = 20;
    private static final int HIGH_THRESHOLD = 50;
    private static final int LEVEL_MAX = 60;
    private static final int LEVEL_BOOST = 5;

    public PlayerManager(AnticheatManager instance)
    {
        config = instance.getConfiguration();
        lang = config.getLang();
    }

    private static void reactMedium(Player player, final String name)
    {
        execute("Medium", player, name);
        Utilities.alert(formatArray(lang.getMediumAlert(), name, ChatColor.YELLOW));
    }

    private static void reactHigh(Player player, final String name)
    {
        execute("High", player, name);
        Utilities.alert(formatArray(lang.getHighAlert(), name, ChatColor.RED));
    }

    public boolean increaseLevel(final Player player, CheckType type)
    {
        if (config.silentMode() && type.getUses(player) % 4 != 0)
        {
            return false;
        }
        else
        {
            // Why, Why, Whyyyyy!
            if (player == null)
                return false;
            
            final String name = player.getName();
            
            if (level.get(name) == null || level.get(name) == 0)
            {
                level.put(name, 1);
            }
            else
            {
                final int playerLevel = level.get(name);
                level.put(name, playerLevel + 1);
                if (playerLevel <= MED_THRESHOLD && playerLevel + 1 > MED_THRESHOLD && playerLevel + 1 <= HIGH_THRESHOLD)
                {
                    reactMedium(player,name);
                }
                else if (playerLevel <= HIGH_THRESHOLD && playerLevel + 1 > HIGH_THRESHOLD)
                {
                    reactHigh(player,name);
                    level.put(player.getName(), MED_THRESHOLD + LEVEL_BOOST);
                }
                else if (playerLevel > LEVEL_MAX)
                {
                    level.put(name, MED_THRESHOLD + LEVEL_BOOST);
                }
            }
        }
        return true;
    }

    public void decreaseLevel(Player player)
    {
        final String name = player.getName();
        if (level.get(name) != null && level.get(name) != 0)
        {
            int playerLevel = level.get(name) - 1;
            level.put(name, playerLevel);
        }
    }

    public int getLevel(Player player)
    {
        final String name = player.getName();
        if (level.get(name) != null)
        {
            return level.get(name);
        }
        else
        {
            return 0;
        }
    }

    public void setLevel(Player player, int x)
    {
        if (!(x > LEVEL_MAX && x < 0))
        {
            level.put(player.getName(), x);
        }
    }

    public boolean hasLevel(Player player)
    {
        return level.containsKey(player.getName());
    }

    public void reset(Player player)
    {
        level.put(player.getName(), 0);
        for (CheckType type : CheckType.values())
        {
            type.clearUse(player);
        }
    }

    public Map<String, Integer> getLevels()
    {
        return level;
    }

    public void removePlayer(Player pl)
    {
        level.remove(pl.getName());
    }

    private static void execute(String level, Player player, String name)
    {
        String result = config.getResult(level);
        if (result.startsWith("COMMAND["))
        {
            String command = result.replaceAll("COMMAND\\[", "").replaceAll("]", "").replaceAll("&player", player.getName()).replaceAll("&world", player.getWorld().getName());
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        else if (result.equalsIgnoreCase("KICK"))
        {
            player.kickPlayer(formatString(lang.getKickReason(), name, ChatColor.RED));
            player.getServer().broadcastMessage(formatString(lang.getKickBroadcast(), name, ChatColor.RED));
        }
        else if (result.equalsIgnoreCase("WARN"))
        {
            String[] message = formatArray(lang.getWarning(), name, ChatColor.RED);
            for (String string : message)
            {
                player.sendMessage(string);
            }
        }
        else if (result.equalsIgnoreCase("BAN"))
        {
            player.setBanned(true);
            player.kickPlayer(formatString(lang.getBanReason(), name, ChatColor.RED));
            player.getServer().broadcastMessage(formatString(lang.getBanBroadcast(), name, ChatColor.RED));
        }
    }

    private static String[] formatArray(String[] array, final String name, ChatColor color)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = formatString(array[i], name, color);
        }
        return array;
    }

    private static String formatString(String string, final String name, ChatColor color)
    {
        return color + string.replaceAll("&player", ChatColor.WHITE + name + color);
    }
}
