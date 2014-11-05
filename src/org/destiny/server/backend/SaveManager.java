package org.destiny.server.backend;

import java.sql.ResultSet;

import org.destiny.server.backend.entity.Bag;
import org.destiny.server.backend.entity.BagItem;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.mechanics.statuses.abilities.IntrinsicAbility;
import org.destiny.server.network.MySqlManager;

public class SaveManager
{
	private MySqlManager m_database;
	private int fail = 0;

	/* TODO: Check some queries and possibly rewrite. */
	public SaveManager()
	{
		m_database = MySqlManager.getInstance();
	}

	/**
	 * Saves a bag to the database.
	 * 
	 * @param bag
	 * @return
	 */
	public void saveBag(Bag bag)
	{
		for(BagItem item : bag.getItems())
		{
			if(item != null && item.getQuantity() > 0)
				m_database.query("INSERT INTO `pn_bag` (`member`, `item`, `quantity`) VALUES (" + bag.getMemberId() + ", " + item.getItemNumber() + ", " + item.getQuantity()
						+ ") ON DUPLICATE KEY UPDATE `quantity` = " + item.getQuantity() + ";");
		}
	}

	public void updateBagItem(int memberId, int itemNumber, int quantity)
	{
		m_database.query("UPDATE `pn_bag` SET `quantity` = " + quantity + " WHERE `member` = " + memberId + " AND `item` = " + itemNumber + ";");
	}

	public void removeBagItem(int memberId, int itemNumber)
	{
		m_database.query("DELETE FROM `pn_bag` WHERE `member` = " + memberId + " AND `item` = " + itemNumber + ";");
	}

	/**
	 * Saves a pokemon to the database that didn't exist in it before
	 * 
	 * @param p
	 */
	public int saveNewPokemon(Pokemon poke, String currentTrainer)
	{
		try
		{
			/* Due to issues with Pokemon not receiving abilities, we're going to ensure they have one */
			if(poke.getAbility() == null || poke.getAbility().getName().equalsIgnoreCase(""))
			{
				String[] abilities = PokemonSpecies.getDefaultData().getPossibleAbilities(poke.getSpeciesName());
				/* First select an ability randomly */
				String ab = "";
				if(abilities.length == 1)
					ab = abilities[0];
				else
					ab = abilities[DataService.getBattleMechanics().getRandom().nextInt(abilities.length)];
				poke.setAbility(IntrinsicAbility.getInstance(ab), true);
			}
			/* Insert the Pokemon into the database */
			m_database.query("INSERT INTO `pn_pokemon` VALUES (NULL, '" + MySqlManager.parseSQL(poke.getName()) + "', '" + MySqlManager.parseSQL(poke.getSpeciesName()) + "', '"
					+ String.valueOf(poke.getExp()) + "', " + poke.getPokemonBaseExp() + ", '" + MySqlManager.parseSQL(poke.getExpType().name()) + "', '" + String.valueOf(poke.isFainted()) + "', "
					+ poke.getLevel() + ", " + poke.getHappiness() + ", " + poke.getGender() + ", '" + MySqlManager.parseSQL(poke.getNature().getName()) + "', '"
					+ MySqlManager.parseSQL(poke.getAbilityName()) + "', '" + MySqlManager.parseSQL(poke.getItemName()) + "', " + poke.getCaughtWithBall() + ", '" + String.valueOf(poke.isShiny()) + "', '"
					+ MySqlManager.parseSQL(poke.getOriginalTrainer()) + "', '" + MySqlManager.parseSQL(currentTrainer) + "', '" + poke.getContestStatsAsString() + "', '"
					+ MySqlManager.parseSQL(poke.getMove(0).getName()) + "', '" + (poke.getMove(1) == null ? "null" : MySqlManager.parseSQL(poke.getMove(1).getName())) + "', '"
					+ (poke.getMove(2) == null ? "null" : MySqlManager.parseSQL(poke.getMove(2).getName())) + "', '"
					+ (poke.getMove(3) == null ? "null" : MySqlManager.parseSQL(poke.getMove(3).getName())) + "', " + poke.getHealth() + ", " + poke.getStat(1) + ", " + poke.getStat(2) + ", "
					+ poke.getStat(3) + ", " + poke.getStat(4) + ", " + poke.getStat(5) + ", " + poke.getEv(0) + ", " + poke.getEv(1) + ", " + poke.getEv(2) + ", " + poke.getEv(3) + ", "
					+ poke.getEv(4) + ", " + poke.getEv(5) + ", " + poke.getIv(0) + ", " + poke.getIv(1) + ", " + poke.getIv(2) + ", " + poke.getIv(3) + ", " + poke.getIv(4) + ", " + poke.getIv(5)
					+ ", " + poke.getPp(0) + ", " + poke.getPp(1) + ", " + poke.getPp(2) + ", " + poke.getPp(3) + ", " + poke.getMaxPp(0) + ", " + poke.getMaxPp(1) + ", " + poke.getMaxPp(2) + ", "
					+ poke.getMaxPp(3) + ", " + poke.getPpUpCount(0) + ", " + poke.getPpUpCount(1) + ", " + poke.getPpUpCount(2) + ", " + poke.getPpUpCount(3) + ", '"
					+ MySqlManager.parseSQL(poke.getDateCaught()) + "');");
			/* Get the pokemon's database id and attach it to the pokemon. This needs to be done so it can be attached to the player in the database later. */
			ResultSet result = m_database.query("SELECT `id` FROM `pn_pokemon` WHERE `originalTrainerName`='" + MySqlManager.parseSQL(poke.getOriginalTrainer()) + "' AND `date`='"
					+ MySqlManager.parseSQL(poke.getDateCaught()) + "';");
			result.first();
			int pokeId = result.getInt("id");
			poke.setDatabaseID(pokeId);
			return pokeId;
		}
		catch(Exception e)
		{
			System.err.println("INSERT INTO `pn_pokemon` VALUES (NULL, '" + MySqlManager.parseSQL(poke.getName()) + "', '" + MySqlManager.parseSQL(poke.getSpeciesName()) + "', '"
					+ String.valueOf(poke.getExp()) + "', " + poke.getPokemonBaseExp() + ", '" + MySqlManager.parseSQL(poke.getExpType().name()) + "', '" + String.valueOf(poke.isFainted()) + "', "
					+ poke.getLevel() + ", " + poke.getHappiness() + ", " + poke.getGender() + ", '" + MySqlManager.parseSQL(poke.getNature().getName()) + "', '"
					+ MySqlManager.parseSQL(poke.getAbilityName()) + "', '" + MySqlManager.parseSQL(poke.getItemName()) + "', " + poke.getCaughtWithBall() + ", '" + String.valueOf(poke.isShiny()) + "', '"
					+ MySqlManager.parseSQL(poke.getOriginalTrainer()) + "', '" + MySqlManager.parseSQL(currentTrainer) + "', '" + poke.getContestStatsAsString() + "', '"
					+ MySqlManager.parseSQL(poke.getMove(0).getName()) + "', '" + (poke.getMove(1) == null ? "null" : MySqlManager.parseSQL(poke.getMove(1).getName())) + "', '"
					+ (poke.getMove(2) == null ? "null" : MySqlManager.parseSQL(poke.getMove(2).getName())) + "', '"
					+ (poke.getMove(3) == null ? "null" : MySqlManager.parseSQL(poke.getMove(3).getName())) + "', " + poke.getHealth() + ", " + poke.getStat(1) + ", " + poke.getStat(2) + ", "
					+ poke.getStat(3) + ", " + poke.getStat(4) + ", " + poke.getStat(5) + ", " + poke.getEv(0) + ", " + poke.getEv(1) + ", " + poke.getEv(2) + ", " + poke.getEv(3) + ", "
					+ poke.getEv(4) + ", " + poke.getEv(5) + ", " + poke.getIv(0) + ", " + poke.getIv(1) + ", " + poke.getIv(2) + ", " + poke.getIv(3) + ", " + poke.getIv(4) + ", " + poke.getIv(5)
					+ ", " + poke.getPp(0) + ", " + poke.getPp(1) + ", " + poke.getPp(2) + ", " + poke.getPp(3) + ", " + poke.getMaxPp(0) + ", " + poke.getMaxPp(1) + ", " + poke.getMaxPp(2) + ", "
					+ poke.getMaxPp(3) + ", " + poke.getPpUpCount(0) + ", " + poke.getPpUpCount(1) + ", " + poke.getPpUpCount(2) + ", " + poke.getPpUpCount(3) + ", '"
					+ MySqlManager.parseSQL(poke.getDateCaught()) + "');");
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Saves a player object to the database (Updates an existing player)
	 * 
	 * @param player
	 * @return
	 */
	public int savePlayer(Player player)
	{
		try(ResultSet data = m_database.query("SELECT `lastLoginTime` FROM `pn_members` WHERE `id` = '" + player.getId() + "'"))
		{
			fail = 0;
			/* First, check if they have logged in somewhere else. This is useful for when a server loses it's internet connection. */
			if(data.first() && data.getLong("lastLoginTime") == player.getLastLoginTime())
			{
				/* Check they are not trading */
				if(player.isTrading())
					/* If the trade is still executing, don't save them yet */
					if(!player.getTrade().endTrade())
						fail++;
				// return false;
				/* Update the player row */
				String badges = "";
				for(int i = 0; i < player.getBadges().length; i++)
					if(player.hasBadge(i))
						badges += "1";
					else
						badges += "0";
				m_database.query("UPDATE `pn_members` SET " + "muted='" + player.isMuted() + "', " + "sprite='" + player.getRawSprite() + "', " + "money='" + player.getMoney() + "', " + "skHerb='"
						+ player.getHerbalismExp() + "', " + "skCraft='" + player.getCraftingExp() + "', " + "skFish='" + player.getFishingExp() + "', " + "skTrain='" + player.getTrainingExp()
						+ "', " + "skCoord='" + player.getCoordinatingExp() + "', " + "skBreed='" + player.getBreedingExp() + "', " + "x='" + player.getX() + "', " + "y='" + player.getY() + "', "
						+ "mapX='" + player.getMapX() + "', " + "mapY='" + player.getMapY() + "', " + "healX='" + player.getHealX() + "', " + "healY='" + player.getHealY() + "', " + "healMapX='"
						+ player.getHealMapX() + "', " + "healMapY='" + player.getHealMapY() + "', " + "isSurfing='" + String.valueOf(player.isSurfing()) + "', " + "badges='" + badges + "' "
						+ "WHERE id='" + player.getId() + "';");
				/* Second, update the party */
				// Save all the Pokemon
				for(int i = 0; i < 6; i++)
					if(player.getParty() != null && player.getParty()[i] != null)
						if(player.getParty()[i].getDatabaseID() < 1)
						{
							// This is a new Pokemon, add it to the database
							if(saveNewPokemon(player.getParty()[i], player.getName()) < 1)
							{
								System.out.println("failed to save pokemon: " + player.getParty()[i].getName() + " of " + player.getName());
								fail++;
								// return false;
							}
						}
						else // Old Pokemon, just update
						if(!savePokemon(player.getParty()[i], player.getName()))
						{
							fail++;
							// return false;
						}
				// Save all the Pokemon id's in the player's party
				if(player.getParty() != null)
					m_database.query("UPDATE `pn_party` SET " + "`pokemon0` = '" + (player.getParty()[0] != null ? player.getParty()[0].getDatabaseID() : -1) + "', " + "`pokemon1` = '"
							+ (player.getParty()[1] != null ? player.getParty()[1].getDatabaseID() : -1) + "', " + "`pokemon2` = '"
							+ (player.getParty()[2] != null ? player.getParty()[2].getDatabaseID() : -1) + "', " + "`pokemon3` = '"
							+ (player.getParty()[3] != null ? player.getParty()[3].getDatabaseID() : -1) + "', " + "`pokemon4` = '"
							+ (player.getParty()[4] != null ? player.getParty()[4].getDatabaseID() : -1) + "', " + "`pokemon5` = '"
							+ (player.getParty()[5] != null ? player.getParty()[5].getDatabaseID() : -1) + "' " + "WHERE `member` = '" + player.getId() + "';");
				else
					return fail;
				/* Save the player's bag */
				if(player.getBag() != null)
					saveBag(player.getBag());
				/* Finally, update all the boxes */
				if(player.getBoxes() != null)
					for(int i = 0; i < 9; i++)
						if(player.getBoxes()[i] != null)
							/* Save all pokemon in box */
							for(int j = 0; j < player.getBoxes()[i].getPokemon().length; j++)
								if(player.getBoxes()[i].getPokemon()[j] != null)
									if(player.getBoxes()[i].getPokemon()[j].getDatabaseID() < 1)
									{
										/* This is a new Pokemon, create it in the database */
										if(saveNewPokemon(player.getBoxes()[i].getPokemon(j), player.getName()) < 1)
										{
											System.out.println("failed to save pokemon: " + player.getBoxes()[i].getPokemon(j).getName() + " of " + player.getName());
											fail++;
											// return false;
										}
									}
									else /* Update an existing pokemon */
									if(!savePokemon(player.getBoxes()[i].getPokemon()[j], player.getName()))
									{
										fail++;
										// return false;
									}
				// Dispose of the player object
				if(player.getMap() != null)
					player.getMap().removeChar(player);
				return fail;
			}
			else
				return fail;
		}
		catch(Exception e)
		{
			System.err.println("UPDATE `pn_members` SET " + "muted='" + player.isMuted() + "', " + "sprite='" + player.getRawSprite() + "', " + "money='" + player.getMoney() + "', " + "skHerb='"
					+ player.getHerbalismExp() + "', " + "skCraft='" + player.getCraftingExp() + "', " + "skFish='" + player.getFishingExp() + "', " + "skTrain='" + player.getTrainingExp() + "', "
					+ "skCoord='" + player.getCoordinatingExp() + "', " + "skBreed='" + player.getBreedingExp() + "', " + "x='" + player.getX() + "', " + "y='" + player.getY() + "', " + "mapX='"
					+ player.getMapX() + "', " + "mapY='" + player.getMapY() + "', " + "healX='" + player.getHealX() + "', " + "healY='" + player.getHealY() + "', " + "healMapX='"
					+ player.getHealMapX() + "', " + "healMapY='" + player.getHealMapY() + "', " + "isSurfing='" + String.valueOf(player.isSurfing()) + "', " + "badges='" + "' " + "WHERE id='"
					+ player.getId() + "'");
			System.err.println(player.getName() + " has " + fail + " fails.");
			e.printStackTrace();
			return fail;
		}
	}

	/**
	 * Updates a pokemon in the database
	 * 
	 * @param poke
	 */
	public boolean savePokemon(Pokemon poke, String currentTrainer)
	{
		/* Due to issues with Pokemon not receiving abilities, we're going to ensure they have one */
		String ab = "";
		if(poke.getAbility() == null || poke.getAbility().getName().equalsIgnoreCase(""))
		{
			String[] abilities = PokemonSpecies.getDefaultData().getPossibleAbilities(poke.getSpeciesName());
			/* First select an ability randomly */
			if(abilities.length == 1)
				ab = abilities[0];
			else
				ab = abilities[DataService.getBattleMechanics().getRandom().nextInt(abilities.length)];
			poke.setAbility(IntrinsicAbility.getInstance(ab), true);
		}
		else
		{
			ab = poke.getAbility().getName();
		}
		/* Update the pokemon in the database */
		try
		{
			m_database.query("UPDATE `pn_pokemon` SET name = '" + MySqlManager.parseSQL(poke.getName()) + "', speciesName = '" + MySqlManager.parseSQL(poke.getSpeciesName()) + "', exp = "
					+ String.valueOf(poke.getExp()) + ", baseExp = " + poke.getPokemonBaseExp() + ", expType = '" + MySqlManager.parseSQL(poke.getExpType().name()) + "', isFainted = '"
					+ String.valueOf(poke.isFainted()) + "', level = " + poke.getLevel() + ", happiness = " + poke.getHappiness() + ", abilityName = '" + ab + "', itemName = '"
					+ MySqlManager.parseSQL(poke.getItemName()) + "', currentTrainerName = '" + currentTrainer + "', contestStats = '" + poke.getContestStatsAsString() + "', move0 = '"
					+ (poke.getMove(0) == null ? "null" : MySqlManager.parseSQL(poke.getMove(0).getName())) + "', move1 = '"
					+ (poke.getMove(1) == null ? "null" : MySqlManager.parseSQL(poke.getMove(1).getName())) + "', move2 = '"
					+ (poke.getMove(2) == null ? "null" : MySqlManager.parseSQL(poke.getMove(2).getName())) + "', move3 = '"
					+ (poke.getMove(3) == null ? "null" : MySqlManager.parseSQL(poke.getMove(3).getName())) + "', hp = " + poke.getHealth() + ", atk = " + poke.getStat(1) + ", def = "
					+ poke.getStat(2) + ", speed = " + poke.getStat(3) + ", spATK = " + poke.getStat(4) + ", spDEF = " + poke.getStat(5) + ", evHP = " + poke.getEv(0) + ", evATK = " + poke.getEv(1)
					+ ", evDEF = " + poke.getEv(2) + ", evSPD = " + poke.getEv(3) + ", evSPATK = " + poke.getEv(4) + ", evSPDEF = " + poke.getEv(5) + ", ivHP = " + poke.getIv(0) + ", ivATK = "
					+ poke.getIv(1) + ", ivDEF = " + poke.getIv(2) + ", ivSPD = " + poke.getIv(3) + ", ivSPATK = " + poke.getIv(4) + ", ivSPDEF = " + poke.getIv(5) + ", pp0 = " + poke.getPp(0)
					+ ", pp1 = " + poke.getPp(1) + ", pp2 = " + poke.getPp(2) + ", pp3 = " + poke.getPp(3) + ", maxpp0 = " + poke.getMaxPp(0) + ", maxpp1 = " + poke.getMaxPp(1) + ", maxpp2 = "
					+ poke.getMaxPp(2) + ", maxpp3 = " + poke.getMaxPp(3) + ", ppUp0 = " + poke.getPpUpCount(0) + ", ppUp1 = " + poke.getPpUpCount(1) + ", ppUp2 = " + poke.getPpUpCount(2)
					+ ", ppUp3 = " + poke.getPpUpCount(3) + " WHERE id = " + poke.getDatabaseID() + ";");
		}
		catch(NullPointerException e)
		{
			e.printStackTrace();
			System.err.println("Database is `" + m_database + "`");
			System.err.println("Pokemon object is " + poke);
			System.err.println("Database ID is " + poke.getDatabaseID());
			System.err.println("Pokemon name is " + poke.getName());
			System.err.println("Pokemon moves are " + poke.getMove(0).getName() + "|" + poke.getMove(1).getName() + "|" + poke.getMove(2).getName() + "|" + poke.getMove(3).getName());
			System.err.println("', hp='" + poke.getHealth() + "', atk='" + poke.getStat(1) + "', def='" + poke.getStat(2) + "', speed='" + poke.getStat(3) + "', spATK='" + poke.getStat(4)
					+ "', spDEF='" + poke.getStat(5) + "', evHP='" + poke.getEv(0) + "', evATK='" + poke.getEv(1) + "', evDEF='" + poke.getEv(2) + "', evSPD='" + poke.getEv(3) + "', evSPATK='"
					+ poke.getEv(4) + "', evSPDEF='" + poke.getEv(5));
		}
		return true;
	}
}
