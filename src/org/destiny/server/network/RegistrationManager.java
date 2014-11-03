package org.destiny.server.network;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.mechanics.PokemonNature;
import org.destiny.server.battle.mechanics.moves.MoveListEntry;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.protocol.ServerMessage;

/**
 * Handles registrations
 * 
 * @author shadowkanji
 */
public class RegistrationManager implements Runnable
{
	private MySqlManager m_database;
	private boolean m_isRunning = false;
	// private Queue<Session> m_queue;
	private Thread m_thread;
	private static String[] forbiddenWords = new String[] { "fuck", "sex", "troll", "penis", "vagina", "ass" };

	/**
	 * Constructor
	 */
	public RegistrationManager()
	{
		m_database = MySqlManager.getInstance();
		// m_queue = new LinkedList<Session>();
	}

	/**
	 * Queues a registration
	 * 
	 * @param session
	 * @param packet
	 */
	/* public void queueRegistration(IoSession session, String packet) { if(m_thread == null || !m_thread.isAlive()) start(); if(!m_queue.contains(session)) { session.setAttribute("reg", packet); m_queue.offer(session); } session.suspendRead(); session.suspendWrite(); } */

	/**
	 * Registers a new player
	 * 
	 * @param session
	 */
	public void register(Session session, int region, String packet /* IoSession session */) throws Exception
	{
		if(session.getChannel() == null)
			return;
		String[] info = packet.split(",");
		/* Check the username */
		for(String s : forbiddenWords)
		{
			if(info[0].toLowerCase().contains(s))
			{
				ServerMessage message = new ServerMessage(ClientPacket.REGISTER_ISSUES);
				message.addInt(7);
				message.addString(s);
				session.Send(message);
				return;
			}
		}
		if(info[0].equalsIgnoreCase("NULL") || info[0].equalsIgnoreCase("!NPC!"))
		{

			ServerMessage message = new ServerMessage(ClientPacket.REGISTER_ISSUES);
			message.addInt(4);
			session.Send(message);
			return;
		}
		if(info[0].startsWith(" ") || info[0].endsWith(" ") || info[0].contains("!") || info[0].contains("&") || info[0].contains("%") || info[0].contains("(") || info[0].contains(")")
				|| info[0].contains("[") || info[0].contains("]") || info[0].contains("~") || info[0].contains("#") || info[0].contains("|") || info[0].contains("?") || info[0].contains("/")
				|| info[0].contains("\""))
		{
			ServerMessage message = new ServerMessage(ClientPacket.REGISTER_ISSUES);
			message.addInt(4);
			session.Send(message);
			return;
		}
		int s = Integer.parseInt(info[4]);
		/* Check if the username is already taken. */
		ResultSet data = m_database.query("SELECT * FROM pn_members WHERE username='" + MySqlManager.parseSQL(info[0]) + "'");
		if(data != null)
		{
			data.first();
			try
			{
				if(data.getString("username") != null && data.getString("username").equalsIgnoreCase(MySqlManager.parseSQL(info[0])))
				{
					ServerMessage message = new ServerMessage(ClientPacket.REGISTER_ISSUES);
					message.addInt(2);
					session.Send(message);
					return;
				}
			}
			catch(Exception e)
			{
			}
		}
		/* Check if an account is already registered with the specified email address. */
		data = m_database.query("SELECT * FROM pn_members WHERE email='" + MySqlManager.parseSQL(info[2]) + "'");
		if(data != null)
		{
			data.first();
			try
			{
				if(data.getString("email") != null && data.getString("email").equalsIgnoreCase(MySqlManager.parseSQL(info[2])))
				{
					ServerMessage message = new ServerMessage(ClientPacket.REGISTER_ISSUES);
					message.addInt(5);
					session.Send(message);
					return;
				}
				if(info[2].length() > 52)
				{
					ServerMessage message = new ServerMessage(ClientPacket.REGISTER_ISSUES);
					message.addInt(6);
					session.Send(message);
					return;
				}
			}
			catch(Exception e)
			{
			}
		}
		/* Check if user is not trying to register their starter as a non-starter Pokemon. */
		if(!(s == 1 || s == 4 || s == 7 || s == 152 || s == 155 || s == 158 || s == 252 || s == 255 || s == 258 || s == 387 || s == 390 || s == 393))
		{
			ServerMessage message = new ServerMessage(ClientPacket.REGISTER_ISSUES);
			message.addInt(4);
			session.Send(message);
			return;
		}
		/* Generate badge string. */
		String badges = "";
		for(int i = 0; i < 50; i++)
			badges = badges + "0";
		/* Generate starting position. */
		int mapX, mapY, x, y;
		switch(region)
		{
			case 0: // Kanto
				mapX = 3;
				mapY = 1;
				x = 512;
				y = 440;
				break;
			case 1: // Johto
				mapX = 0;
				mapY = 0;
				x = 256;
				y = 440;
				break;
			default: // Default to Kanto
				mapX = 3;
				mapY = 1;
				x = 512;
				y = 440;
				break;
		}
		/* Add the player to the Database. */
		m_database.query("INSERT INTO pn_members (username, password, dob, email, lastLoginTime, lastLoginServer, " + "sprite, money, skHerb, skCraft, skFish, skTrain, skCoord, skBreed, "
				+ "x, y, mapX, mapY, badges, healX, healY, healMapX, healMapY, isSurfing, adminLevel, muted) VALUE " + "('"
				+ MySqlManager.parseSQL(info[0])
				+ "', '"
				+ MySqlManager.parseSQL(info[1])
				+ "', '"
				+ MySqlManager.parseSQL(info[3])
				+ "', '"
				+ MySqlManager.parseSQL(info[2])
				+ "', "
				+ "'0', 'null', '"
				+ MySqlManager.parseSQL(info[5])
				+ "', '1000', '0', "
				+ "'0', '0', '0', '0', '0', '"
				+ x
				+ "', '"
				+ y
				+ "', "
				+ "'"
				+ mapX
				+ "', '"
				+ mapY
				+ "', '"
				+ badges
				+ "', '" + x + "', '" + y + "', '" + mapX + "', '" + mapY + "', 'false', '0', 'false')");
		/* Retrieve Player ID which is used to create the bag 'on the fly'. */
		data = m_database.query("SELECT * FROM pn_members WHERE username='" + MySqlManager.parseSQL(info[0]) + "'");
		data.first();
		int playerId = data.getInt("id");
		/* Create the player's party. */
		Pokemon p = createStarter(s);
		p.setOriginalTrainer(info[0]);
		p.setDateCaught(new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss").format(new Date()));
		saveNewPokemon(p, m_database);
		m_database.query("INSERT INTO pn_party (member, pokemon0, pokemon1, pokemon2, pokemon3, pokemon4, pokemon5) VALUES ('" + +playerId + "','" + p.getDatabaseID() + "','-1','-1','-1','-1','-1')");
		data = m_database.query("SELECT * FROM pn_party WHERE member='" + playerId + "'");
		data.first();
		/* Attach pokemon to the player (his party). */
		m_database.query("UPDATE pn_members SET party='" + data.getInt("id") + "' WHERE id='" + playerId + "'");
		/* Give the player 5 Pokéballs to start his journey. */
		m_database.query("INSERT INTO pn_bag (member,item,quantity) VALUES ('" + playerId + "', '35', '5')");
		/* Create a new pokedex record. */
		m_database
				.query("INSERT INTO `pn_pokedex` VALUES(NULL, "
						+ MySqlManager.parseSQL("" + playerId)
						+ ", '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0')");
		String playeridsql = MySqlManager.parseSQL("" + playerId);
		data = m_database.query("SELECT pokedexId FROM pn_pokedex WHERE memberid = " + playeridsql);
		data.first();
		int pokedexId = data.getInt("pokedexId");
		/* Bind the pokedex ID to the member and add the starter to the Pokedex. */
		m_database.query("UPDATE pn_members SET pokedexId = " + MySqlManager.parseSQL("" + pokedexId) + " WHERE id = '" + playeridsql + "'");
		m_database.query("UPDATE pn_pokedex SET " + "`" + MySqlManager.parseSQL("" + (p.getPokedexNumber())) + "`" + " = '2' WHERE pokedexId = '" + MySqlManager.parseSQL("" + pokedexId) + "'");
		ServerMessage message = new ServerMessage(ClientPacket.REGISTER_SUCCESS);
		session.Send(message);
	}

	/**
	 * Called by m_thread.start()
	 */
	public void run()
	{
		if(m_isRunning)
		{
			/* TODO: Review possibility of implementing Queue again. */
		}
	}

	/**
	 * Start the registration manager
	 */
	public void start()
	{
		if(m_thread == null || !m_thread.isAlive())
		{
			m_thread = new Thread(this);
			m_isRunning = true;
			m_thread.start();
		}
	}

	/**
	 * Stop the registration manager
	 */
	public void stop()
	{
		m_isRunning = false;
	}

	/**
	 * Creates a starter Pokemon
	 * 
	 * @param speciesIndex The Pokédex number for the starter.
	 * @return The starter Pokémon that was requested.
	 * @throws Exception
	 */
	private Pokemon createStarter(int speciesIndex) throws Exception
	{
		/* Get the Pokemon species. Use getPokemonByName as once the species array gets to gen 3 it loses the pokedex numbering. */
		PokemonSpecies species = null;
		switch(speciesIndex)
		{
			case 1:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Bulbasaur");
				break;
			case 4:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Charmander");
				break;
			case 7:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Squirtle");
				break;
			case 152:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Chikorita");
				break;
			case 155:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Cyndaquil");
				break;
			case 158:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Totodile");
				break;
			case 252:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Treecko");
				break;
			case 255:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Torchic");
				break;
			case 258:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Mudkip");
				break;
			case 387:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Turtwig");
				break;
			case 390:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Chimchar");
				break;
			case 393:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Piplup");
				break;
			default:
				species = PokemonSpecies.getDefaultData().getPokemonByName("Mudkip");
		}

		ArrayList<MoveListEntry> possibleMoves = new ArrayList<MoveListEntry>();
		MoveListEntry[] moves = new MoveListEntry[4];
		Random random = DataService.getBattleMechanics().getRandom();
		for(int i = 0; i < species.getStarterMoves().length; i++)
			possibleMoves.add(DataService.getMovesList().getMove(species.getStarterMoves()[i]));
		for(int i = 1; i <= 5; i++)
			if(species.getLevelMoves().containsKey(i))
				possibleMoves.add(DataService.getMovesList().getMove(species.getLevelMoves().get(i)));
		/* possibleMoves sometimes has null moves stored in it, get rid of them. */
		for(int i = 0; i < possibleMoves.size(); i++)
			if(possibleMoves.get(i) == null)
				possibleMoves.remove(i);
		/* Now the store the final set of moves for the Pokemon. */
		if(possibleMoves.size() <= moves.length)
			for(int i = 0; i < possibleMoves.size(); i++)
				moves[i] = possibleMoves.get(i);
		else
		{
			MoveListEntry m = null;
			for(int i = 0; i < moves.length; i++)
				if(possibleMoves.size() == 0)
					moves[i] = null;
				else
				{
					while(m == null)
						m = possibleMoves.get(random.nextInt(possibleMoves.size()));
					moves[i] = m;
					possibleMoves.remove(m);
					m = null;
				}
		}
		/* Get all possible abilities, select a random one and make sure no null slots are selected. */
		String[] abilities = species.getAbilities();
		String ab = abilities[random.nextInt(abilities.length)];
		while(ab == null || ab.equals(""))
			ab = abilities[random.nextInt(abilities.length)];
		/* Create the Pokemon object */
		Pokemon starter = new Pokemon(DataService.getBattleMechanics(), species, PokemonNature.N_QUIRKY, ab, null,
				random.nextInt(100) > 87 ? PokemonSpecies.GENDER_FEMALE : PokemonSpecies.GENDER_MALE, 5, new int[] { random.nextInt(32), // IVs
						random.nextInt(32), random.nextInt(32), random.nextInt(32), random.nextInt(32), random.nextInt(32) }, new int[] { 0, 0, 0, 0, 0, 0 }, // EVs
				moves, new int[] { 0, 0, 0, 0 });
		// Attach the growth rate, experience, current experience, happiness and their speciesname to the starter.
		starter.setExpType(species.getGrowthRate());
		starter.setPokemonBaseExp(species.getBaseEXP());
		starter.setExp(DataService.getBattleMechanics().getExpForLevel(starter, 5));
		starter.setHappiness(species.getHappiness());
		starter.setName(starter.getSpeciesName());
		starter.setPokemonNumber(species.getPokemonNumber());
		return starter;
	}

	/**
	 * Saves a pokemon to the database that didn't exist in it before
	 * 
	 * @param p
	 */
	private boolean saveNewPokemon(Pokemon p, MySqlManager db)
	{
		try
		{
			/* Insert the Pokemon into the database. */
			db.query("INSERT INTO pn_pokemon" + "(name, speciesName, exp, baseExp, expType, isFainted, level, happiness, "
					+ "gender, nature, abilityName, itemName, isShiny, currentTrainerName, originalTrainerName, date, contestStats)" + "VALUES (" + "'"
					+ MySqlManager.parseSQL(p.getName())
					+ "', "
					+ "'"
					+ MySqlManager.parseSQL(p.getSpeciesName())
					+ "', "
					+ "'"
					+ String.valueOf(p.getExp())
					+ "', "
					+ "'"
					+ p.getPokemonBaseExp()
					+ "', "
					+ "'"
					+ MySqlManager.parseSQL(p.getExpType().name())
					+ "', "
					+ "'"
					+ String.valueOf(p.isFainted())
					+ "', "
					+ "'"
					+ p.getLevel()
					+ "', "
					+ "'"
					+ p.getHappiness()
					+ "', "
					+ "'"
					+ p.getGender()
					+ "', "
					+ "'"
					+ MySqlManager.parseSQL(p.getNature().getName())
					+ "', "
					+ "'"
					+ MySqlManager.parseSQL(p.getAbilityName())
					+ "', "
					+ "'"
					+ MySqlManager.parseSQL(p.getItemName())
					+ "', "
					+ "'"
					+ String.valueOf(p.isShiny())
					+ "', "
					+ "'"
					+ MySqlManager.parseSQL(p.getOriginalTrainer())
					+ "', "
					+ "'"
					+ MySqlManager.parseSQL(p.getOriginalTrainer()) + "', " + "'" + MySqlManager.parseSQL(p.getDateCaught()) + "', " + "'" + p.getContestStatsAsString() + "')");
			/* Get the pokemon's database id and attach it to the pokemon. This needs to be done so it can be attached to the player in the database later. */
			ResultSet result = db.query("SELECT * FROM pn_pokemon WHERE originalTrainerName='" + MySqlManager.parseSQL(p.getOriginalTrainer()) + "' AND date='"
					+ MySqlManager.parseSQL(p.getDateCaught()) + "'");
			result.first();
			p.setDatabaseID(result.getInt("id"));
			db.query("UPDATE pn_pokemon SET move0='" + MySqlManager.parseSQL(p.getMove(0).getName()) + "', move1='" + (p.getMove(1) == null ? "null" : MySqlManager.parseSQL(p.getMove(1).getName()))
					+ "', move2='" + (p.getMove(2) == null ? "null" : MySqlManager.parseSQL(p.getMove(2).getName())) + "', move3='"
					+ (p.getMove(3) == null ? "null" : MySqlManager.parseSQL(p.getMove(3).getName())) + "', hp='" + p.getHealth() + "', atk='" + p.getStat(1) + "', def='" + p.getStat(2)
					+ "', speed='" + p.getStat(3) + "', spATK='" + p.getStat(4) + "', spDEF='" + p.getStat(5) + "', evHP='" + p.getEv(0) + "', evATK='" + p.getEv(1) + "', evDEF='" + p.getEv(2)
					+ "', evSPD='" + p.getEv(3) + "', evSPATK='" + p.getEv(4) + "', evSPDEF='" + p.getEv(5) + "' WHERE id='" + p.getDatabaseID() + "'");
			db.query("UPDATE pn_pokemon SET ivHP='" + p.getIv(0) + "', ivATK='" + p.getIv(1) + "', ivDEF='" + p.getIv(2) + "', ivSPD='" + p.getIv(3) + "', ivSPATK='" + p.getIv(4) + "', ivSPDEF='"
					+ p.getIv(5) + "', pp0='" + p.getPp(0) + "', pp1='" + p.getPp(1) + "', pp2='" + p.getPp(2) + "', pp3='" + p.getPp(3) + "', maxpp0='" + p.getMaxPp(0) + "', maxpp1='"
					+ p.getMaxPp(1) + "', maxpp2='" + p.getMaxPp(2) + "', maxpp3='" + p.getMaxPp(3) + "', ppUp0='" + p.getPpUpCount(0) + "', ppUp1='" + p.getPpUpCount(1) + "', ppUp2='"
					+ p.getPpUpCount(2) + "', ppUp3='" + p.getPpUpCount(3) + "' WHERE id='" + p.getDatabaseID() + "'");
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
