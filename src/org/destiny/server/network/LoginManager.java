package org.destiny.server.network;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Bag;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.backend.entity.Pokedex;
import org.destiny.server.backend.entity.PokemonBox;
import org.destiny.server.backend.entity.Player.Language;
import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.Pokemon.ExpTypes;
import org.destiny.server.battle.mechanics.PokemonNature;
import org.destiny.server.battle.mechanics.moves.MoveListEntry;
import org.destiny.server.battle.mechanics.statuses.items.HoldItem;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.feature.TimeService;
import org.destiny.server.protocol.ServerMessage;

/**
 * Handles logging players in
 * 
 * @author shadowkanji
 */
public class LoginManager implements Runnable
{
	private MySqlManager m_database;
	private boolean m_isRunning = false;
	private Queue<Object[]> m_loginQueue;
	private Queue<Object[]> m_passChangeQueue;
	private Thread m_thread;

	/**
	 * Default constructor. Requires a logout manager to be passed in so the server can check if player's data is not being saved as they are logging in.
	 * 
	 * @param manager
	 */
	public LoginManager(LogoutManager manager)
	{
		m_database = MySqlManager.getInstance();
		m_loginQueue = new LinkedList<Object[]>();
		m_passChangeQueue = new LinkedList<Object[]>();
		m_thread = null;
	}

	/**
	 * Places a player in the queue to update their password
	 * 
	 * @param session
	 * @param username
	 * @param newPassword
	 * @param oldPassword
	 */
	public void queuePasswordChange(Session session, String username, String newPassword, String oldPassword)
	{
		if(m_thread == null || !m_thread.isAlive())
			start();
		m_passChangeQueue.offer(new Object[] { session, username, newPassword, oldPassword });
	}

	/**
	 * Places a player in the login queue
	 * 
	 * @param session
	 * @param username
	 * @param password
	 * @param forceLogin True if player wants to force login
	 */
	public void queuePlayer(Session session, String username, String password)
	{
		if(m_thread == null || !m_thread.isAlive())
			start();
		m_loginQueue.offer(new Object[] { session, username, password });
	}

	/**
	 * Called by Thread.start()
	 */
	public void run()
	{
		Object[] obj;
		Session session;
		String username;
		String password;
		String newPassword;
		char lang;
		while(m_isRunning)
		{
			synchronized(m_loginQueue)
			{
				try
				{
					if(m_loginQueue.peek() != null && ActiveConnections.getActiveConnections() < GameServer.getMaxPlayers())
					{
						obj = m_loginQueue.poll();
						session = (Session) obj[0];
						lang = ((String) obj[1]).charAt(0);
						username = ((String) obj[1]).substring(1);
						password = (String) obj[2];
						attemptLogin(session, lang, username, password);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException ie)
			{
			}
			synchronized(m_passChangeQueue)
			{
				try
				{
					if(m_passChangeQueue.peek() != null)
					{
						obj = m_passChangeQueue.poll();
						session = (Session) obj[0];
						username = (String) obj[1];
						newPassword = (String) obj[2];
						password = (String) obj[3];
						changePass(username, newPassword, password, session);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException ie)
			{
			}
		}
		m_thread = null;
	}

	/**
	 * Starts the login manager
	 */
	public void start()
	{
		if(m_thread == null || !m_thread.isAlive())
		{
			m_thread = new Thread(this, "LoginManager-Thread");
			m_isRunning = true;
			m_thread.start();
		}
	}

	/**
	 * Stops the login manager
	 */
	public void stop()
	{
		m_isRunning = false;
	}

	/**
	 * Attempts to login a player. Upon success, it sends a packet to the player to inform them they are logged in.
	 * 
	 * @param session
	 * @param l
	 * @param username
	 * @param password
	 */
	private void attemptLogin(Session session, char language, String username, String password)
	{
		System.out.println("INFO: " + username + " attempting to log in.");
		/* Now, check that they are not banned. */
		try(ResultSet rs = m_database.query("SELECT * FROM `pn_bans` WHERE `ip` = '" + session.getIpAddress() + "' OR `playername` = '" + username + "';"))
		{
			/* Make sure they are not banned. */
			if(rs == null)
			{
				ServerMessage message = new ServerMessage(ClientPacket.LOGIN_FAILED);
				session.Send(message);
				return;
			}
			if(rs.first() && rs.getString("username").equalsIgnoreCase(username))
			{
				ServerMessage message = new ServerMessage(ClientPacket.PLAYER_BANNED);
				session.Send(message);
				return;
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		/* Find the member's information. */
		try(ResultSet rs = m_database.query("SELECT * FROM `pn_members` WHERE `username` = '" + username + "';"))
		{
			if(!rs.first())
			{
				/* Member doesn't exist, say user or pass wrong. We don't want someone to guess usernames. */
				ServerMessage message = new ServerMessage(ClientPacket.USER_OR_PASS_WRONG);
				session.Send(message);
				return;
			}
			/* Check if the password is correct. */
			if(rs.getString("password").compareTo(password) == 0)
			{
				/* Remove the player from the map to prevent duplicates. */
				// GameServer.getServiceManager().getMovementService().removePlayer(username);
				long time = System.currentTimeMillis();
				/* Check if they are logged in elsewhere. */
				if(rs.getString("lastLoginServer").equals(GameServer.getServerName()))
				{
					/* They are already logged in on this server. Attach the session to the existing player if they exist, if not, log them in */
					if(ActiveConnections.getPlayer(username) != null)
					{
						Player player = ActiveConnections.getPlayer(username);
						player.setSession(session);
						player.getSession().setPlayer(player);
						player.setLastLoginTime(time);
						player.setLanguage(Language.values()[Integer.parseInt(String.valueOf(language))]);
						m_database.query("UPDATE `pn_members` SET `lastLoginServer` = '" + MySqlManager.parseSQL(GameServer.getServerName()) + "', `lastLoginTime` = '" + time + "', `lastLoginIP` = '"
								+ MySqlManager.parseSQL(session.getIpAddress()) + "', `lastLanguageUsed` = " + language + " WHERE `username` = '" + MySqlManager.parseSQL(username) + "';");
						initialiseClient(player, session);
					}
					else
					{
						ServerMessage message = new ServerMessage(ClientPacket.LOGGED_ELSEWHERE);
						session.Send(message);
						return;
					}
				}
				else if(rs.getString("lastLoginServer").equals("null"))
					/* They are not logged in elsewhere, log them in. */
					login(username, language, session, rs);
			}
			else
			{
				/* Password is wrong, let them know. */
				ServerMessage message = new ServerMessage(ClientPacket.USER_OR_PASS_WRONG);
				session.Send(message);
				return;
			}
			/* Something went wrong, make sure the player is registered as logged out. */
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			m_database.query("UPDATE `pn_members` SET `lastLoginServer` = 'null' WHERE `username` = '" + MySqlManager.parseSQL(username) + "';");
		}
	}

	/**
	 * Changes the password of a player
	 * 
	 * @param username
	 * @param newPassword
	 * @param oldPassword
	 * @param session
	 */
	private void changePass(String username, String newPassword, String oldPassword, Session session)
	{
		ResultSet result = m_database.query("SELECT `password` FROM `pn_members` WHERE `username` = '" + MySqlManager.parseSQL(username) + "';");
		try
		{
			if(result.first())
				/* if we got a result, compare their old password to the one we have stored for them. */
				if(result.getString("password").compareTo(oldPassword) == 0)
				{
					/* Old password matches the one on file, therefore they got their old password correct, so it can be changed to their new one. */
					m_database.query("UPDATE `pn_members` SET `password` = '" + MySqlManager.parseSQL(newPassword) + "' WHERE `username` = '" + MySqlManager.parseSQL(username) + "';");
					// tell them their password was changed successfully
					ServerMessage message = new ServerMessage(ClientPacket.PASS_CHANGE_RESULT);
					message.addInt(1);
					session.Send(message);
					return;
				}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		/* Tell them we failed to change their password. */
		ServerMessage message = new ServerMessage(ClientPacket.PASS_CHANGE_RESULT);
		message.addInt(0);
		session.Send(message);
	}

	private int generateNewPokedex(int pokedexid, ResultSet result, ResultSet pokemons, Player p)
	{
		try
		{
			int memberID = result.getInt("id");
			m_database
					.query("INSERT INTO `pn_pokedex` VALUES(NULL, "
							+ MySqlManager.parseSQL("" + memberID)
							+ ", '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0')");
			ResultSet id = m_database.query("SELECT `pokedexId` FROM `pn_pokedex` WHERE `memberId` = " + MySqlManager.parseSQL("" + memberID));
			id.first();
			pokedexid = id.getInt("pokedexId");
			m_database.query("UPDATE `pn_members` SET `pokedexId` = " + MySqlManager.parseSQL("" + pokedexid) + " WHERE `id` = " + MySqlManager.parseSQL("" + memberID));
			/* We need to check all the previously and currently owned pokemn and change the values on the Pokedex to caught. */
			pokemons = m_database.query("SELECT * FROM `pn_pokemon` WHERE `originalTrainerName`='" + p.getName() + "'");
			pokemons.beforeFirst();
			while(pokemons.next())
			{
				String pokemonSpecie = pokemons.getString("speciesName");
				int pokemonNumber = PokemonSpecies.getDefaultData().getPokemonByName(pokemonSpecie).getPokedexNumber();
				if(isThirdStageStarter(pokemonNumber))
					for(int i = 0; i < 3; i++)
					{
						int tempNumber = pokemonNumber - i;
						m_database.query("UPDATE `pn_pokedex` SET " + "`" + MySqlManager.parseSQL("" + tempNumber) + "`" + " = '2' WHERE `pokedexId` = '" + MySqlManager.parseSQL("" + pokedexid) + "';");
					}
				else if(isSecondStageStarter(pokemonNumber))
					for(int i = 0; i < 2; i++)
					{
						int tempNumber = pokemonNumber - i;
						m_database.query("UPDATE `pn_pokedex` SET " + "`" + MySqlManager.parseSQL("" + tempNumber) + "`" + " = '2' WHERE `pokedexId` = '" + MySqlManager.parseSQL("" + pokedexid) + "';");
					}
				else
					// Regular pokemon or 1st stage starter
					m_database.query("UPDATE `pn_pokedex` SET " + "`" + MySqlManager.parseSQL("" + pokemonNumber) + "`" + " = '2' WHERE `pokedexId` = '" + MySqlManager.parseSQL("" + pokedexid) + "';");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		return pokedexid;
	}

	/**
	 * Returns a bag object
	 * 
	 * @param data
	 * @return
	 */
	private Bag getBagObject(ResultSet data, int memberid)
	{
		Bag b = null;
		try
		{
			b = new Bag(memberid);
			while(data.next())
				b.addItem(data.getInt("item"), data.getInt("quantity"));
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		return b;
	}

	/**
	 * Returns a playerchar object from a resultset of player data
	 * 
	 * @param data
	 * @return
	 */
	private Player getPlayerObject(ResultSet result)
	{
		try
		{
			Player player = new Player(result.getString("username"));
			Pokemon[] party = new Pokemon[6];
			PokemonBox[] boxes = new PokemonBox[9];
			player.setName(result.getString("username"));
			player.setVisible(true);
			/* Set co-ordinates. */
			player.setX(result.getInt("x"));
			player.setY(result.getInt("y"));
			player.setMapX(result.getInt("mapX"));
			player.setMapY(result.getInt("mapY"));
			player.setId(result.getInt("id"));
			player.setAdminLevel(result.getInt("adminLevel"));
			player.setMuted(result.getBoolean("muted"));
			player.setLastHeal(result.getInt("healX"), result.getInt("healY"), result.getInt("healMapX"), result.getInt("healMapY"));
			player.setSurfing(Boolean.parseBoolean(result.getString("isSurfing")));
			/* Set money and skills. */
			player.setSprite(result.getInt("sprite"));
			player.setMoney(result.getInt("money"));
			player.setHerbalismExp(result.getInt("skHerb"));
			player.setCraftingExp(result.getInt("skCraft"));
			player.setFishingExp(result.getInt("skFish"));
			player.setTrainingExp(result.getInt("skTrain"));
			player.setCoordinatingExp(result.getInt("skCoord"));
			player.setBreedingExp(result.getInt("skBreed"));
			/* Retrieve refences to all Pokemon. */
			int partyId = result.getInt("party");
			ResultSet partyData = m_database.query("SELECT * FROM `pn_party` WHERE `id` = '" + partyId + "'");
			partyData.first(); /* Got a NPE here. */
			ResultSet pokemons = m_database.query("SELECT * FROM `pn_pokemon` WHERE `currentTrainerName` = '" + player.getName() + "'");
			int boxNumber = 0;
			int boxPosition = 0;
			/* Loop through all Pokemon belonging to this player and add them to their party/box */
			while(pokemons.next())
			{
				boolean isParty = false;
				int partyIndex = -1;
				/* Checks if Pokemon is in party */
				for(int i = 0; i < party.length; i++)
					if(partyData.getInt("pokemon" + i) == pokemons.getInt("id"))
					{
						isParty = true;
						partyIndex = i;
						break;
					}
				/* If the pokemon is in party, add it to party */
				if(isParty)
					party[partyIndex] = getPokemonObject(pokemons);
				else /* Else, add it to box if space is available */
				if(boxNumber < boxes.length)
				{
					/* If there's space in this box, add it to the box */
					if(boxPosition < 30)
					{
						if(boxes[boxNumber] == null)
							boxes[boxNumber] = new PokemonBox();
						boxes[boxNumber].setPokemon(boxPosition, getPokemonObject(pokemons));
					}
					else
					{
						/* Else open up a new box and add it to box */
						boxPosition = 0;
						boxNumber++;
						if(boxNumber < boxes.length)
						{
							if(boxes[boxNumber] == null)
								boxes[boxNumber] = new PokemonBox();
							boxes[boxNumber].setPokemon(boxPosition, getPokemonObject(pokemons));
						}
					}
					boxPosition++;
				}
			}
			player.setParty(party);
			player.setBoxes(boxes);
			/* Attach bag. */
			player.setBag(getBagObject(m_database.query("SELECT * FROM `pn_bag` WHERE `member`='" + result.getInt("id") + "'"), player.getId()));
			/* Attach badges. */
			player.generateBadges(result.getString("badges"));
			// Retrieve the players pokedexID, if it doesnt have one.
			int pokedexid = result.getInt("pokedexId");
			// If this returns 0, that means the player is a 'pre-1.4BETA player' and we need to assign one
			if(pokedexid == 0)
				pokedexid = generateNewPokedex(pokedexid, result, pokemons, player);
			ResultSet pokedexData = m_database.query("SELECT * FROM `pn_pokedex` WHERE `pokedexId` = '" + pokedexid + "'");
			pokedexData.first();
			int[] pokedex = new int[494];
			for(int i = 1; i < pokedex.length; i++)
				pokedex[i] = pokedexData.getInt(Integer.toString(i));
			Pokedex px = new Pokedex(pokedexid, pokedex);
			player.setPokedex(px);
			return player;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a Pokemon object based on a set of data
	 * 
	 * @param data
	 * @return
	 */
	private Pokemon getPokemonObject(ResultSet data)
	{
		Pokemon pokemon = null;
		if(data != null)
			try
			{
				/* First generate the Pokemons moves. */
				MoveListEntry[] moves = new MoveListEntry[4];
				moves[0] = data.getString("move0") != null && !data.getString("move0").equalsIgnoreCase("null") ? DataService.getMovesList().getMove(data.getString("move0")) : null;
				moves[1] = data.getString("move1") != null && !data.getString("move1").equalsIgnoreCase("null") ? DataService.getMovesList().getMove(data.getString("move1")) : null;
				moves[2] = data.getString("move2") != null && !data.getString("move2").equalsIgnoreCase("null") ? DataService.getMovesList().getMove(data.getString("move2")) : null;
				moves[3] = data.getString("move3") != null && !data.getString("move3").equalsIgnoreCase("null") ? DataService.getMovesList().getMove(data.getString("move3")) : null;
				/* If the abilty is empty give it an ability. */
				String abi = data.getString("abilityName");
				if(abi.equals(""))
					abi = PokemonSpecies.getDefaultData().getPokemonByName(data.getString("speciesName")).getAbilities()[0];
				/* Create the new Pokemon. */
				pokemon = new Pokemon(DataService.getBattleMechanics(), PokemonSpecies.getDefaultData().getPokemonByName(data.getString("speciesName")), PokemonNature.getNatureByName(data
						.getString("nature")), abi, data.getString("itemName"), data.getInt("gender"), data.getInt("level"), new int[] { data.getInt("ivHP"), data.getInt("ivATK"),
						data.getInt("ivDEF"), data.getInt("ivSPD"), data.getInt("ivSPATK"), data.getInt("ivSPDEF") }, new int[] { data.getInt("evHP"), data.getInt("evATK"), data.getInt("evDEF"),
						data.getInt("evSPD"), data.getInt("evSPATK"), data.getInt("evSPDEF") }, moves, new int[] { data.getInt("ppUp0"), data.getInt("ppUp1"), data.getInt("ppUp2"),
						data.getInt("ppUp3") });
				pokemon.reinitialise();
				/* Set exp, nickname, isShiny and exp gain type. */
				pokemon.setPokemonBaseExp(data.getInt("baseExp"));
				pokemon.setExp(Double.parseDouble(data.getString("exp")));
				pokemon.setName(data.getString("name"));
				pokemon.setHappiness(data.getInt("happiness"));
				pokemon.setShiny(Boolean.parseBoolean(data.getString("isShiny")));
				pokemon.setExpType(ExpTypes.valueOf(data.getString("expType")));
				pokemon.setOriginalTrainer(data.getString("originalTrainerName"));
				pokemon.setDatabaseID(data.getInt("id"));
				pokemon.setDateCaught(data.getString("date"));
				pokemon.setIsFainted(Boolean.parseBoolean(data.getString("isFainted")));
				pokemon.setItem(new HoldItem(data.getString("itemName")));
				pokemon.setCaughtWith(data.getInt("caughtWith"));
				/* Contest stats (beauty, cute, etc.). */
				String[] cstats = data.getString("contestStats").split(",");
				pokemon.setContestStat(0, Integer.parseInt(cstats[0]));
				pokemon.setContestStat(1, Integer.parseInt(cstats[1]));
				pokemon.setContestStat(2, Integer.parseInt(cstats[2]));
				pokemon.setContestStat(3, Integer.parseInt(cstats[3]));
				pokemon.setContestStat(4, Integer.parseInt(cstats[4]));
				/* Sets the stats. */
				pokemon.calculateStats(true);
				pokemon.setHealth(data.getInt("hp"));
				pokemon.setRawStat(1, data.getInt("atk"));
				pokemon.setRawStat(2, data.getInt("def"));
				pokemon.setRawStat(3, data.getInt("speed"));
				pokemon.setRawStat(4, data.getInt("spATK"));
				pokemon.setRawStat(5, data.getInt("spDEF"));
				/* Sets the pp information. */
				pokemon.setPp(0, data.getInt("pp0"));
				pokemon.setPp(1, data.getInt("pp1"));
				pokemon.setPp(2, data.getInt("pp2"));
				pokemon.setPp(3, data.getInt("pp3"));
				pokemon.setPpUp(0, data.getInt("ppUp0"));
				pokemon.setPpUp(0, data.getInt("ppUp1"));
				pokemon.setPpUp(0, data.getInt("ppUp2"));
				pokemon.setPpUp(0, data.getInt("ppUp3"));
			}
			catch(NumberFormatException nfe)
			{
				nfe.printStackTrace();
			}
			catch(SQLException sqle)
			{
				sqle.printStackTrace();
			}
		return pokemon;
	}

	/**
	 * Sends initial information to the client
	 * 
	 * @param p
	 * @param session
	 */
	private void initialiseClient(Player player, Session session)
	{
		ServerMessage message = new ServerMessage(ClientPacket.LOGIN_SUCCESS);
		message.addInt(player.getId());
		message.addString(TimeService.getTime());
		session.Send(message);
		/* Add the player to the map, add it to a movement service, send the Pokemon information, send the bag, send the pokedex, send the money, send the friends, send the badges and finally initialise the Player's skills. */
		player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(player.getMapX(), player.getMapY()), null);
		GameServer.getServiceManager().getMovementService().getMovementManager().addPlayer(player);
		player.updateClientParty();
		player.updateClientBag();
		player.updateClientPokedex();
		player.updateClientMoney();
		player.updateClientFriends();
		player.updateClientBadges();
		player.initializeClientSkills();
	}

	private boolean isSecondStageStarter(int pokemonIndex)
	{
		Integer[] secondPokemon = { 2, 5, 8, 153, 156, 159, 253, 256, 259, 388, 391, 394, 496, 499, 502 };
		for(int idx = 0; idx < secondPokemon.length; idx++)
			if(secondPokemon[idx] == pokemonIndex)
				return true;
		return false;
	}

	private boolean isThirdStageStarter(int pokemonIndex)
	{
		Integer[] thirdPokemon = { 3, 6, 9, 154, 157, 160, 254, 257, 260, 389, 392, 395, 497, 500, 503 };
		for(int idx = 0; idx < thirdPokemon.length; idx++)
			if(thirdPokemon[idx] == pokemonIndex)
				return true;
		return false;
	}

	/**
	 * Logs in a player
	 * 
	 * @param username
	 * @param language
	 * @param session
	 * @param result
	 */
	private void login(String username, char language, Session session, ResultSet result)
	{
		/* They are not logged in elsewhere, set the current login to the current server. */
		long time = System.currentTimeMillis();
		/* Attempt to log the player in */
		Player player = getPlayerObject(result);
		player.setLastLoginTime(time);
		session.setPlayer(player);
		session.setLoggedIn(true);
		player.setLanguage(Language.values()[Integer.parseInt(String.valueOf(language))]);
		/* Update the database with login information. */
		m_database.query("UPDATE `pn_members` SET `lastLoginServer` = '" + MySqlManager.parseSQL(GameServer.getServerName()) + "', `lastLoginTime` = '" + time + "', `lastLoginIP` = '"
				+ MySqlManager.parseSQL(session.getIpAddress()) + "', `lastLanguageUsed` = " + language + " WHERE `username` = '" + MySqlManager.parseSQL(username) + "';");
		/* Send success packet to player, set their map and add them to a movement service. */
		initialiseClient(player, session);
		/* Add them to the list of players */
		GameServer.getInstance().updatePlayerCount();
		System.out.println("INFO: " + username + " logged in.");
	}
}
