/**
 * BetonQuest - advanced quests for Bukkit
 * Copyright (C) 2015  Jakub "Co0sh" Sapalski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pl.betoncraft.betonquest.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import pl.betoncraft.betonquest.api.QuestEvent;
import pl.betoncraft.betonquest.utils.Debug;
import pl.betoncraft.betonquest.utils.PlayerConverter;

/**
 * Fires the custom event for Skript to listen to
 * 
 * @author Coosh
 */
public class BQEventSkript extends QuestEvent {
    
    public BQEventSkript(String playerID, String instructions) {
        super(playerID, instructions);
        Player player = PlayerConverter.getPlayer(playerID);
        if (player == null) {
            Debug.error("Player " + playerID  + " is offline, terminating event!");
            return;
        }
        String [] parts = instructions.split(" ");
        if (parts.length < 2) {
            Debug.error("Error in Skript event: " + instructions);
            return;
        }
        String id = parts[1];
        CustomEventForSkript event = new CustomEventForSkript(player, id);
        Bukkit.getServer().getPluginManager().callEvent(event);
    }
    
    /**
     * Custom event, which runs for Skript to listen.
     * 
     * @author Coosh
     */
    public static class CustomEventForSkript extends PlayerEvent {

        private static final HandlerList handlers = new HandlerList();
        /**
         * ID of the event, as defined by the BetonQuest's event
         */
        private final String id;

        
        /**
         * @param the Player
         */
        public CustomEventForSkript(Player who, String id) {
            super(who);
            this.id = id;
        }
     
        /**
         * @return ID of the event, as defined by the BetonQuest's event
         */
        public String getID() {
            return id;
        }
     
        public HandlerList getHandlers() {
            return handlers;
        }
     
        public static HandlerList getHandlerList() {
            return handlers;
        }

    }
}
