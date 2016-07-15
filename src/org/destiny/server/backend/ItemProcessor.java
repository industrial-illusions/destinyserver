package org.destiny.server.backend;

import org.destiny.server.GameServer;
import org.destiny.server.Logger;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.backend.item.Item;
import org.destiny.server.battle.BattleTurn;
import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonEvolution;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.PokemonEvolution.EvolutionTypes;
import org.destiny.server.battle.impl.WildBattleField;
import org.destiny.server.battle.mechanics.MoveQueueException;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.constants.ItemID;
import org.destiny.server.protocol.ServerMessage;

/**
 * Processes an item using a thread
 * 
 * @author shadowkanji
 */
public class ItemProcessor implements Runnable
{
	public enum PokeBall
	{
		CHERISHBALL, DIVEBALL, DUSKBALL, FASTBALL, FRIENDBALL, GREATBALL, HEALBALL, HEAVYBALL, LEVELBALL, LOVEBALL, LUREBALL, LUXURYBALL, MASTERBALL, MOONBALL, NESTBALL, NETBALL, PARKBALL, POKEBALL, PREMIERBALL, QUICKBALL, REPEATBALL, SAFARIBALL, TIMERBALL, ULTRABALL,
	};
	
	private final Player m_player;
	private final String[] m_details;

	/**
	 * Constructor
	 * 
	 * @param player Reference to the player.
	 * @param details The data regarding the item.
	 */
	public ItemProcessor(Player player, String[] details)
	{
		m_player = player;
		m_details = details;
	}

	/**
	 * Executes the item usage
	 */
	public void run()
	{
		String[] data = new String[m_details.length - 1];
		for(int i = 1; i < m_details.length; i++)
		{
			data[i - 1] = m_details[i];
		}
		int itemNumber = Integer.parseInt(m_details[0]);
		try {
			if(useItem(m_player, itemNumber, data) && !GameServer.getServiceManager().getItemDatabase().getItem(itemNumber).getName().contains("Rod"))
			{
				m_player.getBag().removeItem(itemNumber, 1);
				ServerMessage message = new ServerMessage(m_player.getSession());
				message.init(ClientPacket.REMOVE_ITEM_BAG.getValue());
				message.addInt(itemNumber);
				message.addInt(1);
				message.sendResponse();
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Uses an item in the player's bag.
	 * 
	 * @param player Reference to the player.
	 * @param itemId The id of the item to be used.
	 * @param data Extra data received from client
	 * @return True if the item has been used correctly, otherwise false.
	 * @throws ClassNotFoundException 
	 */
	public boolean useItem(Player player, int itemId, String[] data) throws ClassNotFoundException
	{
		if(player.getBag().containsItem(itemId) < 0)
			return false;
		int pokePartyPos;
		try
		{
			pokePartyPos = Integer.parseInt(data[0]);
		}
		catch(Exception e)
		{
			pokePartyPos = 0;
		}
		Pokemon poke = player.getParty()[pokePartyPos];
		Item item = GameServer.getServiceManager().getItemDatabase().getItem(itemId);
		String itemName = item.getName().toUpperCase();
		String itemCategory = item.getCategory().toUpperCase();
		boolean returnValue = false;
		switch(itemId)
		{
			case ItemID.REPEL:
				player.setRepel(100);
				return true;
			case ItemID.SUPER_REPEL:
				player.setRepel(200);
				return true;
			case ItemID.MAX_REPEL:
				player.setRepel(250);
				return true;
			case ItemID.ESCAPE_ROPE:
				if(player.isBattling())
					return false;
				player.setX(player.getHealX());
				player.setY(player.getHealY());
				player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(player.getHealMapX(), player.getHealMapY()), null);
				return true;
		}
		
		/* TODO: Start using scripts! */
		String script = item.getScript().toUpperCase();
		if(script == null || script == "NULL"){
			Logger.logError("Unknown Item Script: Found NULL script item, "+ itemId, "Perhaps try adding something?");
			return false;
		} else {
			String[] split = script.split(",");
			String cmd = split[0];
			if(cmd == "healPokemon"){
				Integer val = Integer.parseInt(split[1]);
				returnValue = script_healPokemon(player, poke, itemId, pokePartyPos, "You used " + itemName + " on " + poke.getName() + "/nThe " + itemName + " restored " + val + " HP", val);
				return returnValue;
			} else if(cmd == "healPokemonAmt"){
				Integer val = Integer.parseInt(split[1]);
				returnValue = script_healPokemonAmt(player, poke, itemId, pokePartyPos, "You used " + itemName + " on " + poke.getName() + "/nThe " + itemName + " restored " + val + " HP", val);
				return returnValue;
			} else if(cmd == "healPokemonEffect"){
				//Integer val = Integer.parseInt(split[1]);
				//String effect = split[2];
				//returnValue = script_healPokemonPPEffect(player, poke, itemId, pokePartyPos, "You used " + itemName + " on " + poke.getName() + "/nThe " + itemName + " restored " + val + " HP", val, effect);
				//return returnValue;
			} else if(cmd == "feedBerry"){
				Integer val = Integer.parseInt(split[1]);
				returnValue = script_feedBerry(player, poke, itemId, pokePartyPos, poke.getName() + " ate the " + itemName + "/nThe " + itemName + " restored " + val + " HP", val);
				return returnValue;
			} else if(cmd == "removeEffect"){
				Class<?> val = Class.forName(split[1]);
				returnValue = script_removeEffect(itemId, val, itemName + " has restored " + poke.getName() + " status to normal", poke, player);
				return returnValue;
			} else if(cmd == "removeAllEffect"){
				returnValue = script_removeAllEffect(itemId, itemName + " restored " + poke.getName() + " status to normal", poke, player);
				return returnValue;
			} else if(cmd == "catchRate"){
				//PokeBall val = split[1];
				//returnValue = script_usePokeBall(player, val);
			} else if(cmd == "useRod"){
				Integer val = Integer.parseInt(split[1]);
				returnValue = script_useRod(player, val);
				return returnValue;
			} else if(cmd == "evolve"){
				String val = split[1];
				/* TODO: */
				if(player.isBattling() || poke == null){ return false; }
				/* Get the pokemon's evolution data */
				PokemonSpecies pokeData = PokemonSpecies.getDefaultData().getPokemonByName(poke.getSpeciesName());
				for(int j = 0; j < pokeData.getEvolutions().length; j++) {
					PokemonEvolution evolution = pokeData.getEvolutions()[j];
					if(evolution.getType() == EvolutionTypes.Item) {
						if(evolution.getAttribute().equalsIgnoreCase(val)){
							returnValue = script_evolve(evolution, poke, player);
							return returnValue;
						}
					}
				}
			} else {
				Logger.logError("Unknown Item Script: " + cmd, "Perhaps try adding " + cmd + " to ItemProcessor!");
			}
			
			
		}

		
		/* Determine what do to with the item */
		if(item.getCategory().equals("MOVESLOT"))
		{
			/* TMs & HMs */
			if(player.isBattling() || poke == null)
				return false;
			String moveName = itemName.substring(5);
			if(DataService.getMoveSetData().getMoveSet(poke.getSpeciesNumber()).canLearn(moveName))
			{
				poke.getMovesLearning().add(moveName);
				ServerMessage message = new ServerMessage(ClientPacket.MOVE_LEARN_LVL);
				message.addInt(pokePartyPos);
				message.addString(moveName);
				m_player.getSession().Send(message);
				return true;
			}
		}
		return false;
	}

	
	/**************************************************
	 * NEW ITEM SCRIPTS
	 * 
	 * TODO: Add more!
	 * 
	 **************************************************/
	
	
	/**
	 * Processes the potion's effects and sends them to the client.
	 * 
	 * @param player Reference to the player.
	 * @param poke The pokemon.
	 * @param itemId The id of the used item.
	 * @param pokeId The pokemons position in the players party.
	 * @param message The message to be sent to the client.
	 * @param hpChange The amoubt of hp that the item will heal.
	 * @return True, unless an exception is thrown.
	 */
	private boolean script_healPokemon(Player player, Pokemon poke, int itemId, int pokeId, String message, int healAmount)
	{
		if(poke == null){
			return false;
		}
		if(poke.getHealth() <= 0){
			ServerMessage cantUse = new ServerMessage(ClientPacket.CANT_USE_ITEM);
			player.getSession().Send(cantUse);
			return false;
		}
		poke.changeHealth(healAmount);
		if(player.isBattling()){
			player.getBattleField().executeItemTurn(itemId);
		} else {
			ServerMessage hpChange = new ServerMessage(ClientPacket.POKE_HP_CHANGE);
			hpChange.addInt(pokeId);
			hpChange.addInt(poke.getHealth());
			player.getSession().Send(hpChange);
			ServerMessage itemUse = new ServerMessage(ClientPacket.USE_ITEM);
			itemUse.addString(message);
			player.getSession().Send(itemUse);
		}
		return true;
	}
	
	
	/**
	 * Processes the potion's effects and sends them to the client.
	 * 
	 * @param player Reference to the player.
	 * @param poke The pokemon.
	 * @param itemId The id of the used item.
	 * @param pokeId The pokemons position in the players party.
	 * @param message The message to be sent to the client.
	 * @param hpChange The amoubt of hp that the item will heal.
	 * @return True, unless an exception is thrown.
	 */
	private boolean script_healPokemonAmt(Player player, Pokemon poke, int itemId, int pokeId, String message, int val)
	{
		if(poke == null){
			return false;
		}
		if(poke.getHealth() <= 0){
			ServerMessage cantUse = new ServerMessage(ClientPacket.CANT_USE_ITEM);
			player.getSession().Send(cantUse);
			return false;
		}
		Integer healAmount = poke.getRawStat(0) / val;
		poke.changeHealth(healAmount);
		if(player.isBattling()){
			player.getBattleField().executeItemTurn(itemId);
		} else {
			ServerMessage hpChange = new ServerMessage(ClientPacket.POKE_HP_CHANGE);
			hpChange.addInt(pokeId);
			hpChange.addInt(poke.getHealth());
			player.getSession().Send(hpChange);
			ServerMessage itemUse = new ServerMessage(ClientPacket.USE_ITEM);
			itemUse.addString(message);
			player.getSession().Send(itemUse);
		}
		return true;
	}
	
	
	/**
	 * Processes the effects of berries and sends them to the client.
	 * 
	 * @param player Reference to the player.
	 * @param poke The pokemon.
	 * @param itemId The id of the used item.
	 * @param pokeId The pokemons position in the players party.
	 * @param message The message to be sent to the client.
	 * @param hpChange The amoubt of hp that the item will heal.
	 * @return True, unless an exception is thrown.
	 */
	private boolean script_feedBerry(Player player, Pokemon poke, int itemId, int pokeId, String message, int healAmount)
	{
		if(poke == null){
			return false;
		}
		if(poke.getHealth() <= 0){
			ServerMessage cantUse = new ServerMessage(ClientPacket.CANT_USE_ITEM);
			player.getSession().Send(cantUse);
			return false;
		}
		poke.changeHealth(healAmount);
		if(player.isBattling()){
			player.getBattleField().executeItemTurn(itemId);
		} else {
			ServerMessage hpChange = new ServerMessage(ClientPacket.POKE_HP_CHANGE);
			hpChange.addInt(pokeId);
			hpChange.addInt(poke.getHealth());
			player.getSession().Send(hpChange);
			ServerMessage itemUse = new ServerMessage(ClientPacket.USE_ITEM);
			itemUse.addString(message);
			player.getSession().Send(itemUse);
		}
		return true;
	}


	/**
	 * Processes the type of fishing rod and fishes if the player can use it.
	 * 
	 * @param player Reference to the player.
	 * @param rodLvl The required lvl to use the fishing rod.
	 * @return True is the player can use the fishing rod, otherwise false.
	 */
	private boolean script_useRod(Player player, int rodLvl)
	{
		if(!player.isBattling() && !player.isFishing())
		{
			if(player.getFishingLevel() >= rodLvl)
				player.fish(rodLvl);
			else
			{
				ServerMessage message = new ServerMessage(m_player.getSession());
				message.init(ClientPacket.CANT_USE_ROD.getValue());
				message.addInt(rodLvl);
				message.sendResponse();
				return false;
			}
		}
		return true;
	}


	/**
	 * Throws a pokeball and checks if the Pokemon is caught.
	 * 
	 * @param player Owner of the Pokeball.
	 * @param pokeBall The Pokeball that was thrown.
	 * @return True if the player is in a wild battle, otherwise false.
	 * @throws MoveQueueException If the battle turn doesn't get queued correctly.
	 */
	private boolean script_usePokeBall(Player player, PokeBall pokeBall)
	{
		if(player.getBattleField() instanceof WildBattleField)
		{
			WildBattleField wildbf = (WildBattleField) player.getBattleField();
			try
			{
				if(!wildbf.throwPokeball(pokeBall))
					wildbf.queueMove(0, BattleTurn.getMoveTurn(-1));
			}
			catch(MoveQueueException mqe)
			{
				mqe.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}


	/**
	 * Starts the evolution progress for the Pokemon.
	 * 
	 * @param evolution The evolution type.
	 * @param poke The Pokemon to evolve.
	 * @param player The owner of the Pokemon
	 * @return True in all cases.
	 */
	private boolean script_evolve(PokemonEvolution evolution, Pokemon poke, Player player)
	{
		poke.setEvolution(evolution);
		poke.evolutionResponse(true, player);
		return true;
	}
	

	/**
	 * Processes effect removal through item usage and sends the information to the client.
	 * 
	 * @param itemId The id of the used item.
	 * @param effect The status effect to remove.
	 * @param message The message to send to the client.
	 * @param player The owner of the pokemon.
	 * @param poke The pokemon to receive the medicine.
	 * @return True, unless an exception is thrown.
	 */
	private boolean script_removeEffect(int itemId, Class<?> effect, String message, Pokemon poke, Player player)
	{
		
		poke.removeStatus(effect);
		processItemUse(player, itemId, message);
		return true;
	}
	

	/**
	 * Processes removing all effects through item usage and sends the information to the client.
	 * 
	 * @param itemId The id of the used item.
	 * @param message The message to send to the client.
	 * @param player The owner of the pokemon.
	 * @param poke The pokemon to receive the medicine.
	 * @return True, unless an exception is thrown.
	 */
	private boolean script_removeAllEffect(int itemId, String message, Pokemon poke, Player player)
	{
		poke.removeStatusEffects(true);
		processItemUse(player, itemId, message);
		return true;
	}
	



	/**
	 * Checks if the item is used in or outside of battle and handles it.
	 * 
	 * @param player The player using the item.
	 * @param itemId The id of the used item.
	 * @param message The message to send to the client.
	 */
	private boolean processItemUse(Player player, int itemId, String message)
	{
		if(player.isBattling())
			player.getBattleField().executeItemTurn(itemId);
		else
		{
			ServerMessage itemUse = new ServerMessage(ClientPacket.USE_ITEM);
			itemUse.addString(message);
			player.getSession().Send(itemUse);
		}
		return true;
	}
}
