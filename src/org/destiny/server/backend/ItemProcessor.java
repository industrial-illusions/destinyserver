package org.destiny.server.backend;

import java.util.Random;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.backend.item.Item;
import org.destiny.server.backend.item.Item.ItemAttribute;
import org.destiny.server.battle.BattleTurn;
import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonEvolution;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.PokemonEvolution.EvolutionTypes;
import org.destiny.server.battle.impl.WildBattleField;
import org.destiny.server.battle.mechanics.MoveQueueException;
import org.destiny.server.battle.mechanics.statuses.BurnEffect;
import org.destiny.server.battle.mechanics.statuses.ConfuseEffect;
import org.destiny.server.battle.mechanics.statuses.FreezeEffect;
import org.destiny.server.battle.mechanics.statuses.ParalysisEffect;
import org.destiny.server.battle.mechanics.statuses.PoisonEffect;
import org.destiny.server.battle.mechanics.statuses.SleepEffect;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.constants.HealItem;
import org.destiny.server.constants.ItemID;
import org.destiny.server.constants.Rod;
import org.destiny.server.protocol.ServerMessage;

/**
 * Processes an item using a thread
 * 
 * @author shadowkanji
 */
public class ItemProcessor implements Runnable
{
	/* The enum that handles Pokeball types */
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
		if(useItem(m_player, itemNumber, data) && !GameServer.getServiceManager().getItemDatabase().getItem(itemNumber).getName().contains("Rod"))
		{
			m_player.getBag().removeItem(itemNumber, 1);
			ServerMessage message = new ServerMessage(m_player.getSession());
			message.init(ClientPacket.REMOVE_ITEM_BAG.getValue());
			message.addInt(itemNumber);
			message.addInt(1);
			message.sendResponse();
		}
	}

	/**
	 * Uses an item in the player's bag.
	 * 
	 * @param player Reference to the player.
	 * @param itemId The id of the item to be used.
	 * @param data Extra data received from client
	 * @return True if the item has been used correctly, otherwise false.
	 */
	public boolean useItem(Player player, int itemId, String[] data)
	{
		/* TODO: Test all item uses in-game to verify changes and possible improvements (Code might include fixes). */
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
			case ItemID.OLD_ROD:
				returnValue = processRod(player, Rod.OLD_ROD_LVL);
				return returnValue;
			case ItemID.GOOD_ROD:
				returnValue = processRod(player, Rod.GOOD_ROD_LVL);
				return returnValue;
			case ItemID.GREAT_ROD:
				returnValue = processRod(player, Rod.GREAT_ROD_LVL);
				return returnValue;
			case ItemID.ULTRA_ROD:
				returnValue = processRod(player, Rod.ULTRA_ROD_LVL);
				return returnValue;
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
		/* Determine what do to with the item */
		if(item.getAttributes().contains(ItemAttribute.MOVESLOT))
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
		else if(item.getAttributes().contains(ItemAttribute.POKEMON))
		{
			/* Status healers, hold items, etc. */
			switch(itemCategory)
			{
				case "POTIONS":
					if(poke == null)
						return false;
					if(poke.getHealth() <= 0)
					{
						ServerMessage cantUse = new ServerMessage(ClientPacket.CANT_USE_ITEM);
						player.getSession().Send(cantUse);
						return false;
					}
					switch(itemId)
					{
						case 1:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, "You used Potion on " + poke.getName() + "/nThe Potion restored 20 HP", HealItem.POTION_HP);
							break;
						case 2:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, "You used Super Potion on " + poke.getName() + "/nThe Super Potion restored 50 HP",
									HealItem.SUPER_POTION_HP);
							break;
						case 3:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, "You used Hyper Potion on " + poke.getName() + "/nThe Hyper Potion restored 200 HP",
									HealItem.HYPER_POTION_HP);
							break;
						case 4:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, "You used Max Potion on " + poke.getName() + "/nThe Max Potion restored All HP", poke.getRawStat(0));
							break;
						default:
							returnValue = false;
							break;
					}
					return returnValue;
				case "EVOLUTION":
					/* Evolution items can't be used in battle, Pokemon shouldn't be null */
					if(player.isBattling() || poke == null)
						return false;
					/* Get the pokemon's evolution data */
					PokemonSpecies pokeData = PokemonSpecies.getDefaultData().getPokemonByName(poke.getSpeciesName());
					for(int j = 0; j < pokeData.getEvolutions().length; j++)
					{
						PokemonEvolution evolution = pokeData.getEvolutions()[j];
						/* Check if this pokemon evolves by item */
						if(evolution.getType() == EvolutionTypes.Item)
						{
							/* TODO: Add Oval Stone? */
							/* Check if the item is an evolution stone If so, evolve the Pokemon */
							if(itemId == ItemID.FIRE_STONE && evolution.getAttribute().equalsIgnoreCase("FIRESTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.WATER_STONE && evolution.getAttribute().equalsIgnoreCase("WATERSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.THUNDER_STONE && evolution.getAttribute().equalsIgnoreCase("THUNDERSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.LEAF_STONE && evolution.getAttribute().equalsIgnoreCase("LEAFSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.MOON_STONE && evolution.getAttribute().equalsIgnoreCase("MOONSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.SUN_STONE && evolution.getAttribute().equalsIgnoreCase("SUNSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.SHINY_STONE && evolution.getAttribute().equalsIgnoreCase("SHINYSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.DUSK_STONE && evolution.getAttribute().equalsIgnoreCase("DUSKSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							else if(itemId == ItemID.DAWN_STONE && evolution.getAttribute().equalsIgnoreCase("DAWNSTONE"))
								returnValue = evolveWithItem(evolution, poke, player);
							return returnValue;
						}
					}
					break;
				case "MEDICINE":
					if(poke == null)
						return false;
					/* Check if this pokemon is alive to use all items but revive, TODO: Implement revive before this piece of code! */
					if(poke.getHealth() <= 0)
					{
						ServerMessage cantUse = new ServerMessage(ClientPacket.CANT_USE_ITEM);
						player.getSession().Send(cantUse);
						return false;
					}
					switch(itemId)
					{
						case ItemID.ANTIDOTE:
							returnValue = processEffectRemoval(itemId, PoisonEffect.class,
									"You used Antidote on " + poke.getName() + "/nThe Antidote restored " + poke.getName() + " status to normal", poke, player);
							break;
						case ItemID.PARALYZ_HEAL:
							returnValue = processEffectRemoval(itemId, ParalysisEffect.class, "You used Parlyz Heal on " + poke.getName() + "/nThe Parlyz Heal restored " + poke.getName()
									+ " status to normal", poke, player);
							break;
						case ItemID.AWAKENING:
							returnValue = processEffectRemoval(itemId, SleepEffect.class, "You used Awakening on " + poke.getName() + "/nThe Awakening restored " + poke.getName()
									+ " status to normal", poke, player);
							break;
						case ItemID.BURN_HEAL:
							returnValue = processEffectRemoval(itemId, BurnEffect.class,
									"You used Burn Heal on " + poke.getName() + "/nThe Burn Heal restored " + poke.getName() + " status to normal", poke, player);
							break;
						case ItemID.ICE_HEAL:
							returnValue = processEffectRemoval(itemId, FreezeEffect.class,
									"You used Ice Heal on " + poke.getName() + "/nThe Ice Heal restored " + poke.getName() + " status to normal", poke, player);
							break;
						case ItemID.FULL_HEAL:
							returnValue = processClearEffects(itemId, "You used Full Heal on " + poke.getName() + "/nThe Full Heal restored " + poke.getName() + " status to normal", poke, player);
							break;
						case ItemID.LAVA_COOKIE:
							returnValue = processClearEffects(itemId, "You used Lava Cookies on " + poke.getName() + "/nThe Lava Cookies restored " + poke.getName() + " status to normal", poke,
									player);
							break;
						case ItemID.OLD_GATEAU:
							returnValue = processClearEffects(itemId, "You used Old Gateau on " + poke.getName() + "/nThe Old Gateau restored " + poke.getName() + " status to normal", poke, player);
							break;
					}
					return returnValue;
				case "FOOD":
					Random rand = new Random();
					if(poke == null)
						return false;
					switch(itemId)
					{
						case ItemID.CHERI_BERRY:
							returnValue = processEffectRemoval(itemId, ParalysisEffect.class,
									poke.getName() + " ate the Cheri Berry/nThe Cheri Berry restored " + poke.getName() + " status to normal", poke, player);
							break;
						case ItemID.CHESTO_BERRY:
							returnValue = processEffectRemoval(itemId, SleepEffect.class, poke.getName() + " ate the Chesto Berry/nThe Chesto Berry restored " + poke.getName() + " status to normal",
									poke, player);
							break;
						case ItemID.PECHA_BERRY:
							returnValue = processEffectRemoval(itemId, PoisonEffect.class, poke.getName() + " ate the Pecha Berry/nThe Pecha Berry restored " + poke.getName() + " status to normal",
									poke, player);
							break;
						case ItemID.RAWST_BERRY:
							returnValue = processEffectRemoval(itemId, BurnEffect.class, poke.getName() + " ate the Rawst Berry/nThe Rawst Berry restored " + poke.getName() + " status to normal",
									poke, player);
							break;
						case ItemID.ASPEAR_BERRY:
							returnValue = processEffectRemoval(itemId, FreezeEffect.class, poke.getName() + " ate the Aspear Berry/nThe Aspear Berry restored " + poke.getName() + " status to normal",
									poke, player);
							break;
						case ItemID.LEPPA_BERRY:
							/* Move selection not completed, temp message TODO: Add support for this */
							int ppSlot = Integer.parseInt(data[1]);
							if(poke.getPp(ppSlot) + 10 <= poke.getMaxPp(ppSlot))
								poke.setPp(ppSlot, poke.getPp(ppSlot) + 10);
							else
								poke.setPp(ppSlot, poke.getMaxPp(ppSlot));
							returnValue = processItemUse(player, itemId, "Leppa Berry had no effect");
							break;
						case ItemID.ORAN_BERRY:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Oran Berry/nThe Oran Berry restored 10 HP", HealItem.ORAN_BERRY_HP);
							break;
						case ItemID.PERSIM_BERRY:
							returnValue = processEffectRemoval(itemId, ConfuseEffect.class,
									poke.getName() + " ate the Persim Berry/nThe Persim Berry restored " + poke.getName() + " status to normal", poke, player);
							break;
						case ItemID.LUM_BERRY:
							returnValue = processClearEffects(itemId, poke.getName() + " ate the Lum Berry/nThe Lum Berry restored " + poke.getName() + " status to normal", poke, player);
							break;
						case ItemID.SITRUS_BERRY:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Sitrus Berry/nThe Sitrus Berry restored 30 HP", HealItem.SITRUS_BERRY_HP);
							break;
						case ItemID.FIGY_BERRY:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Figy Berry/nThe Figy Berry restored" + poke.getRawStat(0) / 8 + " HP to "
									+ poke.getName() + "!", poke.getRawStat(0) / 8);
							break;
						case ItemID.WIKI_BERRY:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Wiki Berry/nThe Wiki Berry restored" + poke.getRawStat(0) / 8 + " HP to "
									+ poke.getName() + "!", poke.getRawStat(0) / 8);
							break;
						case ItemID.MAGO_BERRY:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Mago Berry/nThe Mago Berry restored" + poke.getRawStat(0) / 8 + " HP to "
									+ poke.getName() + "!", poke.getRawStat(0) / 8);
							break;
						case ItemID.AGUAV_BERRY:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Aguav Berry/nThe Aguav Berry restored" + poke.getRawStat(0) / 8 + " HP to "
									+ poke.getName() + "!", poke.getRawStat(0) / 8);
							break;
						case ItemID.IAPAPA_BERRY:
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Iapapa Berry/nThe Iapapa Berry restored" + poke.getRawStat(0) / 8 + " HP to "
									+ poke.getName() + "!", poke.getRawStat(0) / 8);
							break;
						case ItemID.VOLTORB_LOLLIPOP:
							String message = poke.getName() + " ate the Voltorb Lollipop/nThe Lollipop restored 50 HP to " + poke.getName() + "!";
							if(rand.nextInt(10) < 3)
							{
								poke.addStatus(new ParalysisEffect());
								message += "/n" + poke.getName() + " was Paralyzed from the Lollipop!";
							}
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, message, HealItem.VOLTORB_LOLLIPOP_HP);
							break;
						case ItemID.SWEET_CHILLS:
							message = poke.getName() + " ate the Sweet Chill/nThe Sweet Chill restored " + poke.getName() + "'s moves!";
							for(ppSlot = 0; ppSlot < 4; ppSlot++)
							{
								if(poke.getPp(ppSlot) + 5 <= poke.getMaxPp(ppSlot))
									poke.setPp(ppSlot, poke.getPp(ppSlot) + 5);
								else
									poke.setPp(ppSlot, poke.getMaxPp(ppSlot));
							}
							if(rand.nextInt(10) < 3)
							{
								poke.addStatus(new FreezeEffect());
								message += "/n" + poke.getName() + " was frozen solid from the cold candy!";
							}
							returnValue = processItemUse(player, itemId, message);
							break;
						case ItemID.CINNAMON_CANDY:
							message = poke.getName() + " ate the Cinnamon Candy./nThe Cinnamon Candy restored " + poke.getName() + "'s status to normal!";
							poke.removeStatusEffects(true);
							if(rand.nextInt(10) < 3)
							{
								poke.addStatus(new BurnEffect());
								message += "/n" + poke.getName() + " was burned from the candy!";
							}
							returnValue = processHealItem(player, poke, itemId, pokePartyPos, message, 0);
							break;
						case ItemID.CANDY_CORN:
							message = poke.getName() + " ate the Candy Corn./n" + poke.getName() + " is happier!";
							int happiness = poke.getHappiness() + 15;
							if(happiness <= 300)
								poke.setHappiness(happiness);
							else
								poke.setHappiness(300);
							if(rand.nextInt(10) < 3)
							{
								poke.addStatus(new PoisonEffect());
								message += "/n" + poke.getName() + " got Poisoned from the rotten candy!";
							}
							returnValue = processItemUse(player, itemId, message);
							break;
						case ItemID.POKE_CHOC:
							message = poke.getName() + " ate the Poke'Choc Bar!/n" + poke.getName() + " is happier!";
							happiness = poke.getHappiness() + 10;
							if(happiness <= 300)
								poke.setHappiness(happiness);
							else
								poke.setHappiness(300);
							if(rand.nextInt(10) <= 3)
							{
								poke.changeHealth(30);
								message += "/n" + poke.getName() + " recovered 30HP.";
							}
							returnValue = processItemUse(player, itemId, message);
							break;
						case ItemID.GUMMILAX:
							message = poke.getName() + " ate the Gummilax./n" + poke.getName() + " is happier!";
							happiness = poke.getHappiness() + rand.nextInt(30);
							if(happiness <= 255)
								poke.setHappiness(happiness);
							else
								poke.setHappiness(255);
							if(rand.nextInt(10) < 3)
							{
								poke.addStatus(new ParalysisEffect());
								message += "/nThe gummi was too sweet for " + poke.getName() + "./n" + poke.getName() + " fell asleep!";
							}
							returnValue = processItemUse(player, itemId, message);
							break;
						case ItemID.GENGUM:
							returnValue = processGengum(player, poke, itemId, pokePartyPos, poke.getName() + " ate the Gengum./n" + poke.getName());
							break;
					}
					return returnValue;
			}
		}
		else if(item.getAttributes().contains(ItemAttribute.BATTLE))
		{
			switch(itemId)
			{
				case ItemID.POKE_BALL:
					returnValue = processPokeBalls(player, PokeBall.POKEBALL);
					break;
				case ItemID.GREAT_BALL:
					returnValue = processPokeBalls(player, PokeBall.GREATBALL);
					break;
				case ItemID.ULTRA_BALL:
					returnValue = processPokeBalls(player, PokeBall.ULTRABALL);
					break;
				case ItemID.MASTER_BALL:
					returnValue = processPokeBalls(player, PokeBall.MASTERBALL);
					break;
				case ItemID.LEVEL_BALL:
					returnValue = processPokeBalls(player, PokeBall.LEVELBALL);
					break;
				case ItemID.LURE_BALL:
					returnValue = processPokeBalls(player, PokeBall.LUREBALL);
					break;
				case ItemID.MOON_BALL:
					returnValue = processPokeBalls(player, PokeBall.MOONBALL);
					break;
				case ItemID.FRIEND_BALL:
					returnValue = processPokeBalls(player, PokeBall.FRIENDBALL);
					break;
				case ItemID.LOVE_BALL:
					returnValue = processPokeBalls(player, PokeBall.LOVEBALL);
					break;
				case ItemID.HEAVY_BALL:
					returnValue = processPokeBalls(player, PokeBall.HEAVYBALL);
					break;
				case ItemID.FAST_BALL:
					returnValue = processPokeBalls(player, PokeBall.FASTBALL);
					break;
				case ItemID.PARK_BALL:
					returnValue = processPokeBalls(player, PokeBall.PARKBALL);
					break;
				case ItemID.PREMIER_BALL:
					returnValue = processPokeBalls(player, PokeBall.PREMIERBALL);
					break;
				case ItemID.REPEAT_BALL:
					returnValue = processPokeBalls(player, PokeBall.REPEATBALL);
					break;
				case ItemID.TIMER_BALL:
					returnValue = processPokeBalls(player, PokeBall.TIMERBALL);
					break;
				case ItemID.NEST_BALL:
					returnValue = processPokeBalls(player, PokeBall.NESTBALL);
					break;
				case ItemID.NET_BALL:
					returnValue = processPokeBalls(player, PokeBall.NETBALL);
					break;
				case ItemID.DIVE_BALL:
					returnValue = processPokeBalls(player, PokeBall.DIVEBALL);
					break;
				case ItemID.LUXURY_BALL:
					returnValue = processPokeBalls(player, PokeBall.LUXURYBALL);
					break;
				case ItemID.HEAL_BALL:
					returnValue = processPokeBalls(player, PokeBall.HEALBALL);
					break;
				case ItemID.QUICK_BALL:
					returnValue = processPokeBalls(player, PokeBall.QUICKBALL);
					break;
				case ItemID.DUSK_BALL:
					returnValue = processPokeBalls(player, PokeBall.DUSKBALL);
					break;
				case ItemID.CHERISH_BALL:
					returnValue = processPokeBalls(player, PokeBall.CHERISHBALL);
					break;
			}
			return returnValue;
		}
		return false;
	}

	/**
	 * Processes the type of fishing rod and fishes if the player can use it.
	 * 
	 * @param player Reference to the player.
	 * @param rodLvl The required lvl to use the fishing rod.
	 * @return True is the player can use the fishing rod, otherwise false.
	 */
	private boolean processRod(Player player, int rodLvl)
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
	private boolean processHealItem(Player player, Pokemon poke, int itemId, int pokeId, String message, int healAmount)
	{
		poke.changeHealth(healAmount);
		if(player.isBattling())
			player.getBattleField().executeItemTurn(itemId);
		else
		{
			ServerMessage hpChange = new ServerMessage(ClientPacket.POKE_HP_CHANGE);
			hpChange.addInt(pokeId);
			hpChange.addInt(poke.getHealth());
			m_player.getSession().Send(hpChange);
			ServerMessage itemUse = new ServerMessage(ClientPacket.USE_ITEM);
			itemUse.addString(message);
			m_player.getSession().Send(itemUse);
		}
		return true;
	}

	/**
	 * Starts the evolution progress for the Pokemon.
	 * 
	 * @param evolution The evolution type.
	 * @param poke The Pokemon to evolve.
	 * @param player The owner of the Pokemon
	 * @return True in all cases.
	 */
	private boolean evolveWithItem(PokemonEvolution evolution, Pokemon poke, Player player)
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
	private boolean processEffectRemoval(int itemId, Class<?> effect, String message, Pokemon poke, Player player)
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
	private boolean processClearEffects(int itemId, String message, Pokemon poke, Player player)
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

	/**
	 * Throws a pokeball and checks if the Pokemon is caught.
	 * 
	 * @param player Owner of the Pokeball.
	 * @param pokeBall The Pokeball that was thrown.
	 * @return True if the player is in a wild battle, otherwise false.
	 * @throws MoveQueueException If the battle turn doesn't get queued correctly.
	 */
	private boolean processPokeBalls(Player player, PokeBall pokeBall)
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

	private boolean processGengum(Player player, Pokemon poke, int itemId, int pokeId, String message)
	{
		Random rand = new Random();
		int randHealth = rand.nextInt(100) - 20;
		if(randHealth > 0)
			message += " healed " + randHealth + "HP";
		else
			message += " lost " + -randHealth + "HP";
		if(poke.getHealth() + randHealth < 0)
			poke.setHealth(1);
		else
			poke.changeHealth(randHealth);
		try
		{
			if(player.isBattling())
				player.getBattleField().queueMove(0, BattleTurn.getMoveTurn(-1));
			else
			{
				ServerMessage hpChange = new ServerMessage(ClientPacket.POKE_HP_CHANGE);
				hpChange.addInt(pokeId);
				hpChange.addInt(poke.getHealth());
				m_player.getSession().Send(hpChange);
				ServerMessage itemUse = new ServerMessage(ClientPacket.USE_ITEM);
				itemUse.addString(message);
				m_player.getSession().Send(itemUse);
			}
		}
		catch(MoveQueueException mqe)
		{
			mqe.printStackTrace();
			return false;
		}
		return true;
	}
}
