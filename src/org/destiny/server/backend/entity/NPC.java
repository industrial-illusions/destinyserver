package org.destiny.server.backend.entity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.impl.NpcBattleField;
import org.destiny.server.battle.impl.NpcSleepTimer;
import org.destiny.server.battle.mechanics.PokemonNature;
import org.destiny.server.battle.mechanics.statuses.abilities.IntrinsicAbility;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.constants.ItemID;
import org.destiny.server.protocol.ServerMessage;

/**
 * Represents a Non Playable Character
 * 
 * @author shadowkanji
 */
public class NPC extends Character
{
	private int m_badge = -1;
	private boolean m_isBox = false;
	private boolean m_isHeal = false;
	private int m_isShop = 0;
	private long m_lastBattle = 0;
	private int m_minPartySize = 1;
	private Direction originalDirection;
	/* Trainers can have an more than 6 possible Pokemon.
	 * When a battle is started with this NPC, it'll check the min party size.
	 * If you have a party bigger than min party,
	 * it'll generate a party of random size between minParty and your party size + 1.
	 * (Unless your party size is 6) */
	private HashMap<String, Integer> m_possiblePokemon;
	private Shop m_shop = null;
	private final ArrayList<Integer> m_speech = new ArrayList<Integer>();

	/**
	 * Constructor
	 */
	public NPC()
	{
	}

	/**
	 * Adds speech to this npc
	 * 
	 * @param id
	 */
	public void addSpeech(int id)
	{
		m_speech.add(id);
	}

	/* set the original direction of an NPC */
	public void setOriginalDirection(Direction d)
	{
		originalDirection = d;
	}

	/**
	 * Returns true if this NPC can battle
	 * 
	 * @return
	 */
	public boolean canBattle()
	{
		return m_lastBattle == 0;
	}

	/**
	 * Returns true if this npc can see the player
	 * 
	 * @param p
	 * @return
	 */
	public boolean canSee(Player p)
	{
		if(!p.isBattling() && !isGymLeader() && canBattle())
		{
			Random r = new Random();
			switch(getFacing())
			{
				case Up:
					if(p.getY() >= getY() - 32 * (r.nextInt(4) + 1))
						return true;
					break;
				case Down:
					if(p.getY() <= getY() + 32 * (r.nextInt(4) + 1))
						return true;
					break;
				case Left:
					if(p.getX() >= getX() - 32 * (r.nextInt(4) + 1))
						return true;
					break;
				case Right:
					if(p.getX() <= getX() + 32 * (r.nextInt(4) + 1))
						return true;
					break;
			}
		}
		return false;
	}

	/**
	 * Challenges a player (NOTE: Should only be called from NpcBattleLauncher)
	 * 
	 * @param p
	 */
	public void challengePlayer(Player p)
	{
		String speech = getSpeech();
		if(!speech.equalsIgnoreCase(""))
		{
			ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
			message.addInt(2);
			message.addString(speech);
			p.getSession().Send(message);
		}
	}

	/**
	 * Returns the number of the badge this npc gives. -1 if no badge.
	 * 
	 * @return
	 */
	public int getBadge()
	{
		return m_badge;
	}

	/**
	 * Returns the time this NPC last battled
	 * NOTE: Is valued 0 if the NPC is able to battle
	 * 
	 * @return
	 */
	public long getLastBattleTime()
	{
		return m_lastBattle;
	}

	/**
	 * Returns a dynamically generated Pokemon party based on how well trained a player is
	 * 
	 * @param p
	 * @return
	 */
	public Pokemon[] getParty(Player p)
	{
		Pokemon[] party = new Pokemon[6];
		Pokemon poke;
		int level;
		String name;
		Random r = DataService.getBattleMechanics().getRandom();
		/* TODO: Extract to a function and rewrite to a switchcase. */
		if(isGymLeader())
		{
			try
			{
				int lvl = 0;
				// kanto leaders
				if(m_name.equalsIgnoreCase("brock"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 37;
						poke = Pokemon.getGymLeaderPokemon("Onix", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Stealth Rock", "Earthquake", "Stone Edge", "Taunt");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Golem", lvl, PokemonNature.N_ADAMANT, 0, 255, 255, 0, 0, 0, "Stone Edge", "Earthquake", "Explosion", "Sucker Punch");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Kabutops", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Rapid Spin", "Stone Edge", "Aqua Jet", "Waterfall");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Omastar", lvl, PokemonNature.N_BOLD, 0, 255, 255, 0, 0, 0, "Stealth Rock", "Spikes", "Surf", "Ice Beam");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Aerodactyl", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Stealth Rock", "Earthquake", "Rock Slide", "Taunt");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Tyranitar", lvl, PokemonNature.N_NAIVE, 0, 0, 0, 255, 0, 255, "Flamethrower", "Ice Beam", "Crunch", "Superpower");
						party[5] = poke;
					}
					else
					{
						lvl = 7;
						poke = Pokemon.getGymLeaderPokemon("Onix", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Stealth Rock", "Earthquake", "Stone Edge", "Taunt");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Golem", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Stone Edge", "Earthquake", "Explosion", "Sucker Punch");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Kabutops", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Rapid Spin", "Stone Edge", "Aqua Jet", "Waterfall");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Omastar", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Stealth Rock", "Spikes", "Surf", "Ice Beam");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Aerodactyl", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Stealth Rock", "Earthquake", "Rock Slide", "Taunt");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Tyranitar", lvl, PokemonNature.N_NAIVE, 0, 0, 0, 0, 0, 0, "Flamethrower", "Ice Beam", "Crunch", "Superpower");
						party[5] = poke;
					}

				}
				else if(m_name.equalsIgnoreCase("misty"))
				{
					if(p.getBadgeCount() > 7)
					{
						lvl = 41;
						poke = Pokemon.getGymLeaderPokemon("Seaking", lvl, PokemonNature.N_LONELY, 0, 255, 0, 255, 0, 0, "Aqua Tail", "Megahorn", "Return", "Ice Beam");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Quagsire", lvl, PokemonNature.N_IMPISH, 255, 0, 0, 255, 0, 0, "Recover", "Earthquake", "Waterfall", "Toxic");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Golduck", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 0, 255, "Surf", "Ice Beam", "Cross Chop", "Psychic");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Lapras", lvl, PokemonNature.N_MODEST, 255, 0, 0, 255, 0, 0, "Surf", "Ice Beam", "Thunderbolt", "Heal Bell");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Blastoise", lvl, PokemonNature.N_BOLD, 255, 0, 0, 255, 0, 0, "Surf", "Ice Beam", "Rapid Spin", "Earthquake");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Starmie", lvl, PokemonNature.N_TIMID, 255, 0, 0, 255, 0, 0, "Flamethrower", "Ice Beam", "Thunderbolt", "Psychic");
						party[5] = poke;
					}
					else
					{
						lvl = 10;
						poke = Pokemon.getGymLeaderPokemon("Seaking", lvl, PokemonNature.N_LONELY, 0, 0, 0, 0, 0, 0, "Aqua Tail", "Megahorn", "Return", "Ice Beam");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Quagsire", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Recover", "Earthquake", "Waterfall", "Toxic");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Golduck", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Surf", "Ice Beam", "Cross Chop", "Psychic");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Lapras", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Surf", "Ice Beam", "Thunderbolt", "Heal Bell");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Blastoise", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Surf", "Ice Beam", "Rapid Spin", "Earthquake");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Starmie", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Flamethrower", "Ice Beam", "Thunderbolt", "Psychic");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("Lt. Surge"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 45;
						poke = Pokemon.getGymLeaderPokemon("Raichu", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 0, 255, "Nasty Plot", "Volt Tackle", "Encore", "Grass Knot");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Electabuzz", lvl, PokemonNature.N_HASTY, 0, 0, 0, 255, 0, 255, "Thunderbolt", "Cross Chop", "Psychic", "Surf");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Ampharos", lvl, PokemonNature.N_MODEST, 255, 0, 0, 255, 0, 0, "Rain Dance", "Thunder", "Reflect", "Signal Beam");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Electrode", lvl, PokemonNature.N_MODEST, 255, 0, 0, 255, 0, 0, "Rain Dance", "Taunt", "Thunder", "Explosion");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Jolteon", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 0, 255, "Thunderbolt", "Shadow Ball", "Hypnosis", "Protect");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Electrode", lvl, PokemonNature.N_NAIVE, 0, 0, 0, 255, 0, 255, "Ancientpower", "Taunt", "Thunderbolt", "Explosion");
						party[5] = poke;
					}
					else
					{
						lvl = 13;
						poke = Pokemon.getGymLeaderPokemon("Raichu", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Nasty Plot", "Volt Tackle", "Encore", "Grass Knot");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Electabuzz", lvl, PokemonNature.N_HASTY, 0, 0, 0, 0, 0, 0, "Thunderbolt", "Cross Chop", "Psychic", "Surf");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Ampharos", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Rain Dance", "Thunder", "Reflect", "Signal Beam");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Electrode", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Rain Dance", "Taunt", "Thunder", "Explosion");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Jolteon", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Thunderbolt", "Shadow Ball", "Crunch", "Protect");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Electrode", lvl, PokemonNature.N_NAIVE, 0, 0, 0, 0, 0, 0, "Ancientpower", "Taunt", "Thunderbolt", "Explosion");
						party[5] = poke;

					}
				}
				else if(m_name.equalsIgnoreCase("erika"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 49;
						poke = Pokemon.getGymLeaderPokemon("Vileplume", lvl, PokemonNature.N_TIMID, 140, 0, 0, 140, 0, 255, "Sludge Bomb", "Energy Ball", "Leech Seed", "Aromatherapy");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Meganium", lvl, PokemonNature.N_BOLD, 255, 0, 140, 140, 0, 0, "Seed Bomb", "Aromatherapy", "Leech Seed", "Synthesis");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Sunflora", lvl, PokemonNature.N_MODEST, 255, 0, 0, 0, 0, 255, "SolarBeam", "Ice Beam", "Earth Power", "Sunny Day");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Victreebel", lvl, PokemonNature.N_MODEST, 255, 0, 0, 0, 0, 255, "SolarBeam", "Weather Ball", "Sleep Powder", "Rock Slide");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Tangela", lvl, PokemonNature.N_TIMID, 255, 0, 0, 0, 0, 255, "Sunny Day", "Endeavor", "Sleep Powder", "Leaf Storm");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Bellossom", lvl, PokemonNature.N_MODEST, 255, 0, 0, 255, 0, 0, "Sunny Day", "Sleep Powder", "Moonlight", "SolarBeam");
						party[5] = poke;

					}
					else
					{
						lvl = 17;
						poke = Pokemon.getGymLeaderPokemon("Vileplume", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Sludge Bomb", "Energy Ball", "Leech Seed", "Aromatherapy");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Meganium", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Seed Bomb", "Aromatherapy", "Leech Seed", "Synthesis");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Sunflora", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "SolarBeam", "Ice Beam", "Earth Power", "Sunny Day");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Victreebel", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "SolarBeam", "Weather Ball", "Sleep Powder", "Rock Slide");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Tangela", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Sunny Day", "Endeavor", "Sleep Powder", "Leaf Storm");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Bellossom", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Sunny Day", "Sleep Powder", "Moonlight", "SolarBeam");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("koga"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 53;
						poke = Pokemon.getGymLeaderPokemon("Muk", lvl, PokemonNature.N_CAREFUL, 255, 0, 255, 0, 0, 0, "Curse", "Poison Jab", "Brick Break", "Shadow Sneak");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Weezing", lvl, PokemonNature.N_BOLD, 0, 255, 255, 0, 0, 0, "Thunderbolt", "Flamethrower", "Will-O-Wisp", "Pain Split");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Ariados", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 255, 0, 255, "Toxic Spikes", "Sucker Punch", "Protect", "Signal Beam");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Forretress", lvl, PokemonNature.N_RELAXED, 0, 255, 255, 0, 0, 0, "Toxic Spikes", "Rapid Spin", "Gyro Ball", "Explosion");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Venomoth", lvl, PokemonNature.N_TIMID, 255, 0, 0, 0, 0, 255, "Psychic", "Shadow Ball", "Sleep Powder", "Bug Buzz");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Crobat", lvl, PokemonNature.N_JOLLY, 255, 255, 0, 0, 0, 0, "Taunt", "Roost", "Super Fang", "Brave Bird");
						party[5] = poke;
					}
					else
					{
						lvl = 21;
						poke = Pokemon.getGymLeaderPokemon("Muk", lvl, PokemonNature.N_CAREFUL, 0, 0, 0, 0, 0, 0, "Curse", "Poison Jab", "Brick Break", "Shadow Sneak");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Weezing", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Thunderbolt", "Flamethrower", "Will-O-Wisp", "Pain Split");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Ariados", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Toxic Spikes", "Sucker Punch", "Protect", "Signal Beam");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Forretress", lvl, PokemonNature.N_RELAXED, 0, 0, 0, 0, 0, 0, "Toxic Spikes", "Rapid Spin", "Gyro Ball", "Explosion");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Venomoth", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Psychic", "Shadow Ball", "Sleep Powder", "Bug Buzz");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Crobat", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Taunt", "Roost", "Super Fang", "Brave Bird");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("sabrina"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 57;
						poke = Pokemon.getGymLeaderPokemon("Slowking", lvl, PokemonNature.N_CALM, 255, 0, 144, 112, 0, 0, "Surf", "Ice Beam", "Slack Off", "Toxic");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Exeggutor", lvl, PokemonNature.N_MODEST, 40, 0, 0, 216, 0, 255, "Sleep Powder", "Leaf Storm", "Psychic", "Synthesis");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Alakazam", lvl, PokemonNature.N_TIMID, 4, 0, 0, 255, 0, 255, "Taunt", "Psychic", "Counter", "Signal Beam");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Espeon", lvl, PokemonNature.N_TIMID, 40, 0, 0, 216, 0, 255, "Calm Mind", "Psychic", "Earth Power", "Shadow Ball");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hypno", lvl, PokemonNature.N_CALM, 255, 0, 200, 0, 56, 0, "Wish", "Seismic Toss", "Psychic", "Protect");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Wobbuffet", lvl, PokemonNature.N_BOLD, 28, 0, 228, 0, 255, 0, "Encore", "Counter", "Mirror Coat", "Safeguard");
						party[5] = poke;
					}
					else
					{
						lvl = 25;
						poke = Pokemon.getGymLeaderPokemon("Slowking", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Surf", "Ice Beam", "Slack Off", "Toxic");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Exeggutor", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Sleep Powder", "Leaf Storm", "Psychic", "Synthesis");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Alakazam", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Taunt", "Psychic", "Counter", "Signal Beam");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Espeon", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Calm Mind", "Psychic", "Earth Power", "Shadow Ball");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hypno", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Wish", "Seismic Toss", "Psychic", "Protect");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Jynx", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Ice Beam", "Nasty Plot", "Lovely Kiss", "Psychic");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("blaine"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 61;
						poke = Pokemon.getGymLeaderPokemon("Arcanine", lvl, PokemonNature.N_ADAMANT, 160, 160, 0, 255, 0, 0, "Flare Blitz", "ExtremeSpeed", "Morning Sun", "Toxic");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Charizard", lvl, PokemonNature.N_TIMID, 0, 0, 16, 255, 0, 255, "Sunny Day", "Flamethrower", "Solarbeam", "Air Slash");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Houndoom", lvl, PokemonNature.N_HASTY, 0, 255, 255, 0, 0, 32, "Pursuit", "Sucker Punch", "Thunder Fang", "Fire Blast");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Flareon", lvl, PokemonNature.N_CALM, 255, 0, 80, 0, 180, 0, "Wish", "Protect", "Flamethrower", "Toxic");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Rapidash", lvl, PokemonNature.N_CALM, 255, 16, 255, 0, 0, 0, "Flare Blitz", "Hypnosis", "Megahorn", "Will-O-Wisp");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Magmortar", lvl, PokemonNature.N_NAIVE, 0, 16, 0, 255, 0, 255, "Flamethrower", "Thunderbolt", "Cross Chop", "Ice Punch");
						party[5] = poke;
					}
					else
					{
						lvl = 30;
						poke = Pokemon.getGymLeaderPokemon("Arcanine", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Flare Blitz", "ExtremeSpeed", "Morning Sun", "Toxic");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Charizard", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Sunny Day", "Flamethrower", "Solarbeam", "Air Slash");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Houndoom", lvl, PokemonNature.N_HASTY, 0, 0, 0, 0, 0, 0, "Pursuit", "Sucker Punch", "Thunder Fang", "Fire Blast");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Flareon", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Wish", "Protect", "Flamethrower", "Toxic");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Rapidash", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Flare Blitz", "Hypnosis", "Megahorn", "Will-O-Wisp");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Magmortar", lvl, PokemonNature.N_NAIVE, 0, 0, 0, 0, 0, 0, "Flamethrower", "Thunderbolt", "Cross Chop", "Ice Punch");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("Giovani"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 65;
						poke = Pokemon.getGymLeaderPokemon("Dugtrio", lvl, PokemonNature.N_ADAMANT, 80, 255, 0, 255, 0, 0, "Earthquake", "Night Slash", "Stone Edge", "Pursuit");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Nidoking", lvl, PokemonNature.N_BOLD, 0, 80, 0, 255, 0, 255, "Earthquake", "Ice Beam", "Thunderbolt", "Shadow Ball");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Nidoqueen", lvl, PokemonNature.N_BOLD, 255, 0, 255, 0, 32, 0, "Earth Power", "Stealth Rock", "Ice Beam", "Toxic Spikes");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Rhydon", lvl, PokemonNature.N_ADAMANT, 160, 255, 32, 84, 180, 0, "Earthquake", "Rock Blast", "Megahorn", "Stealth Rock");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Kangaskhan", lvl, PokemonNature.N_CALM, 255, 16, 255, 0, 0, 0, "Flare Blitz", "Hypnosis", "Megahorn", "Will-O-Wisp");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Rhyperior", lvl, PokemonNature.N_JOLLY, 0, 255, 4, 255, 0, 0, "Earthquake", "Stone Edge", "Megahorn", "Rock Polish");
						party[5] = poke;
					}
					else
					{
						lvl = 35;
						poke = Pokemon.getGymLeaderPokemon("Dugtrio", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Earthquake", "Night Slash", "Stone Edge", "Pursuit");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Nidoking", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Earthquake", "Ice Beam", "Thunderbolt", "Shadow Ball");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Nidoqueen", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Earth Power", "Stealth Rock", "Ice Beam", "Toxic Spikes");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Rhyhorn", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Earthquake", "Rock Blast", "Fire Fang", "Aqua Tail");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Kangaskhan", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Flare Blitz", "Hypnosis", "Megahorn", "Will-O-Wisp");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Rhyhorn", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Earthquake", "Stone Edge", "Ice Fang", "Rock Polish");
						party[5] = poke;
					}
				}
				// johto
				else if(m_name.equalsIgnoreCase("falkner"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 37;
						poke = Pokemon.getGymLeaderPokemon("Noctowl", lvl, PokemonNature.N_CALM, 255, 0, 0, 0, 255, 0, "Air Slash", "Roost", "Night Shade", "Toxic");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Pidgeot", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Brave Bird", "Return", "Quick Attack", "Pursuit");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Fearow", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Drill Peck", "Return", "Quick Attack", "Steel Wing");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Farfetch'd", lvl, PokemonNature.N_ADAMANT, 255, 0, 0, 255, 0, 0, "Slash", "Night Slash", "Air Cutter", "Steel Wing");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Honchkrow", lvl, PokemonNature.N_NAUGHTY, 0, 255, 0, 255, 0, 0, "Brave Bird", "Superpower", "Sucker Punch", "Heat Wave");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Dodrio", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Brave Bird", "Return", "Taunt", "Toxic");
						party[5] = poke;
					}
					else
					{
						lvl = 7;
						poke = Pokemon.getGymLeaderPokemon("Noctowl", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Air Slash", "Roost", "Night Shade", "Toxic");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Pidgeotto", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Brave Bird", "Return", "Quick Attack", "Pursuit");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Fearow", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Drill Peck", "Return", "Quick Attack", "Steel Wing");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Farfetch'd", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Slash", "Night Slash", "Air Cutter", "Steel Wing");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Murkrow", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Brave Bird", "Hypnosis", "Sucker Punch", "Heat Wave");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Dodrio", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Brave Bird", "Return", "Taunt", "Toxic");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("bugsy"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 41;
						poke = Pokemon.getGymLeaderPokemon("Scizor", lvl, PokemonNature.N_ADAMANT, 0, 255, 0, 255, 0, 0, "Swords Dance", "Bullet Punch", "Brick Break", "Quick Attack");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Heracross", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Close Combat", "Megahorn", "Stone Edge", "Toxic");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Butterfree", lvl, PokemonNature.N_TIMID, 0, 0, 255, 0, 0, 255, "Sleep Powder", "Stun Spore", "Bug Buzz", "Confusion");
						if(poke.getAbility() == null)
							poke.setAbility(IntrinsicAbility.getInstance("Compoundeyes"), true);
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Beedrill", lvl, PokemonNature.N_ADAMANT, 0, 255, 0, 255, 0, 0, "Swords Dance", "X-Scissor", "Poison Jab", "Brick Break");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Yanmega", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 0, 255, "Bug Buzz", "Air Slash", "Protect", "Fire Fang");
						if(poke.getAbility() == null)
							poke.setAbility(IntrinsicAbility.getInstance("Compoundeyes"), true);
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Parasect", lvl, PokemonNature.N_ADAMANT, 255, 255, 0, 0, 0, 0, "Spore", "Seed Bomb", "Brick Break", "Synthesis");
						party[5] = poke;
					}
					else
					{
						lvl = 10;
						poke = Pokemon.getGymLeaderPokemon("Scyther", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Swords Dance", "Aerial Ace", "Brick Break", "Quick Attack");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Heracross", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Close Combat", "Megahorn", "Stone Edge", "Toxic");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Butterfree", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Sleep Powder", "Stun Spore", "Bug Buzz", "Confusion");
						poke.setAbility(IntrinsicAbility.getInstance("Compoundeyes"), true);
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Beedrill", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Swords Dance", "X-Scissor", "Poison Jab", "Brick Break");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Yanma", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Bug Buzz", "Air Slash", "Protect", "Hypnosis");
						if(poke.getAbility() == null)
							poke.setAbility(IntrinsicAbility.getInstance("Compoundeyes"), true);
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Parasect", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Spore", "Seed Bomb", "Brick Break", "Synthesis");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("whitney"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 45;
						poke = Pokemon.getGymLeaderPokemon("Miltank", lvl, PokemonNature.N_JOLLY, 140, 140, 0, 255, 0, 0, "Heal Bell", "Milk Drink", "Curse", "Body Slam");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Blissey", lvl, PokemonNature.N_BOLD, 255, 0, 255, 0, 0, 0, "Thunder Wave", "Ice Beam", "Seismic Toss", "Softboiled");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Snorlax", lvl, PokemonNature.N_CAREFUL, 184, 0, 184, 0, 184, 0, "Curse", "Body Slam", "Earthquake", "Rest");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Ursaring", lvl, PokemonNature.N_JOLLY, 0, 255, 0, 255, 0, 0, "Facade", "Close Combat", "Crunch", "Swords Dance");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Granbull", lvl, PokemonNature.N_IMPISH, 180, 0, 180, 180, 0, 0, "Body Slam", "Crunch", "Heal Bell", "Thunder Wave");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Clefable", lvl, PokemonNature.N_CALM, 255, 0, 0, 0, 255, 0, "Ice Beam", "Softboiled", "Seismic Toss", "Thunder Wave");
						party[5] = poke;
					}
					else
					{
						lvl = 13;
						poke = Pokemon.getGymLeaderPokemon("Miltank", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Heal Bell", "Milk Drink", "Curse", "Body Slam");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Blissey", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Thunder Wave", "Ice Beam", "Seismic Toss", "Softboiled");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Snorlax", lvl, PokemonNature.N_CAREFUL, 0, 0, 0, 0, 0, 0, "Curse", "Body Slam", "Earthquake", "Rest");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Ursaring", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Facade", "Close Combat", "Crunch", "Swords Dance");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Granbull", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Body Slam", "Crunch", "Heal Bell", "Thunder Wave");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Clefable", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Ice Beam", "Softboiled", "Seismic Toss", "Thunder Wave");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("morty"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 49;
						poke = Pokemon.getGymLeaderPokemon("Gastly", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 255, 0, 255, "Shadow Ball", "Sludge Bomb", "Thunderbolt", "Hypnosis");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Haunter", lvl, PokemonNature.N_BOLD, 255, 0, 255, 0, 0, 0, "Shadow Ball", "Crunch", "Curse", "Thunderbolt");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Gengar", lvl, PokemonNature.N_CAREFUL, 184, 0, 184, 0, 184, 0, "Substitue", "Shadow Ball", "Focus Blast", "Flamethrower");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Misdreavus", lvl, PokemonNature.N_TIMID, 0, 255, 0, 255, 0, 0, "Taunt", "Nasty Plot", "Shadow Ball", "Thunderbolt");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Duskull", lvl, PokemonNature.N_RELAXED, 255, 0, 170, 0, 170, 0, "Will-O-Wisp", "Shadow Sneak", "Pain Split", "Ice Beam");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Misdreavus", lvl, PokemonNature.N_TIMID, 255, 0, 0, 0, 255, 0, "Taunt", "Will-O-Wisp", "Shadow Ball", "Pain Split");
						party[5] = poke;
					}
					else
					{
						lvl = 17;
						poke = Pokemon.getGymLeaderPokemon("Gastly", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Shadow Ball", "Sludge Bomb", "Thunderbolt", "Hypnosis");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Haunter", lvl, PokemonNature.N_BOLD, 0, 0, 0, 0, 0, 0, "Shadow Ball", "Crunch", "Curse", "Thunderbolt");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Gengar", lvl, PokemonNature.N_CAREFUL, 0, 0, 0, 0, 0, 0, "Substitue", "Shadow Ball", "Focus Blast", "Flamethrower");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Misdreavus", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Taunt", "Nasty Plot", "Shadow Ball", "Thunderbolt");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Duskull", lvl, PokemonNature.N_RELAXED, 0, 0, 0, 0, 0, 0, "Will-O-Wisp", "Shadow Sneak", "Pain Split", "Ice Beam");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Misdreavus", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Taunt", "Will-O-Wisp", "Shadow Ball", "Pain Split");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("chuck"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 53;
						poke = Pokemon.getGymLeaderPokemon("Poliwrath", lvl, PokemonNature.N_ADAMANT, 255, 255, 0, 0, 0, 0, "Substitute", "Focus Punch", "Waterfall", "Bulk Up");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Machamp", lvl, PokemonNature.N_ADAMANT, 255, 0, 255, 0, 0, 0, "DynamicPunch", "Payback", "Bullet Punch", "Stone Edge");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Primeape", lvl, PokemonNature.N_ADAMANT, 0, 255, 0, 255, 0, 0, "Earthquake", "Close Combat", "Stone Edge", "Ice Punch");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hitmonlee", lvl, PokemonNature.N_ADAMANT, 0, 255, 0, 255, 0, 0, "Close Combat", "Stone Edge", "Blaze Kick", "Sucker Punch");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hitmonchan", lvl, PokemonNature.N_ADAMANT, 180, 255, 0, 160, 0, 0, "Agility", "Close Combat", "Ice Punch", "Thunder Punch");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hitmontop", lvl, PokemonNature.N_IMPISH, 255, 0, 255, 0, 0, 0, "Rapid Spin", "Foresight", "Close Combat", "Sucker Punch");
						party[5] = poke;
					}
					else
					{
						lvl = 21;
						poke = Pokemon.getGymLeaderPokemon("Poliwrath", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Substitute", "Focus Punch", "Waterfall", "Bulk Up");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Machamp", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "DynamicPunch", "Payback", "Bullet Punch", "Stone Edge");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Primeape", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Earthquake", "Close Combat", "Stone Edge", "Ice Punch");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hitmonlee", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Close Combat", "Stone Edge", "Blaze Kick", "Sucker Punch");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hitmonchan", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Agility", "Close Combat", "Ice Punch", "Thunder Punch");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Hitmontop", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Rapid Spin", "Foresight", "Close Combat", "Sucker Punch");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("jasmine"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 57;
						poke = Pokemon.getGymLeaderPokemon("Steelix", lvl, PokemonNature.N_SASSY, 255, 16, 0, 0, 255, 0, "Stealth Rock", "Earthquake", "Gyro Ball", "Iron Tail");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Magneton", lvl, PokemonNature.N_MODEST, 108, 0, 0, 160, 0, 255, "Rain Dance", "Thunderbolt", "Magnet Rise", "Flamethrower");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Scizor", lvl, PokemonNature.N_ADAMANT, 32, 255, 0, 255, 0, 0, "Swords Dance", "Bullet Punch", "Superpower", "Quick Attack");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Forretress", lvl, PokemonNature.N_CAREFUL, 255, 0, 32, 0, 255, 0, "Spikes", "Toxic Spikes", "Payback", "Rapid Spin");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Skarmory", lvl, PokemonNature.N_CAREFUL, 255, 0, 32, 0, 255, 0, "Spikes", "Roost", "Brave Bird", "Steel Wing");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Mawile", lvl, PokemonNature.N_IMPISH, 255, 32, 255, 0, 0, 0, "Swords Dance", "Baton Pass", "Taunt", "Iron Head");
						party[5] = poke;
					}
					else
					{
						lvl = 25;
						poke = Pokemon.getGymLeaderPokemon("Steelix", lvl, PokemonNature.N_SASSY, 0, 0, 0, 0, 0, 0, "Stealth Rock", "Earthquake", "Gyro Ball", "Iron Tail");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Magneton", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Rain Dance", "Thunderbolt", "Magnet Rise", "Flamethrower");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Scizor", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Swords Dance", "Bullet Punch", "Superpower", "Quick Attack");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Forretress", lvl, PokemonNature.N_CAREFUL, 0, 0, 0, 0, 0, 0, "Spikes", "Toxic Spikes", "Payback", "Rapid Spin");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Skarmory", lvl, PokemonNature.N_CAREFUL, 0, 0, 0, 0, 0, 0, "Spikes", "Roost", "Brave Bird", "Steel Wing");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Mawile", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Swords Dance", "Baton Pass", "Taunt", "Iron Head");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("pryce"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 61;
						poke = Pokemon.getGymLeaderPokemon("Dewgong", lvl, PokemonNature.N_TIMID, 255, 0, 0, 120, 0, 180, "Rain Dance", "Surf", "Ice Beam", "Rest");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Jynx", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 32, 255, "Lovely Kiss", "Ice beam", "Grass Knot", "Taunt");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Cloyster", lvl, PokemonNature.N_ADAMANT, 160, 255, 0, 0, 120, 0, "Ice Shard", "Rock Blast", "Rapid Spin", "Spikes");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Delibird", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 32, 255, "Ice Beam", "Signal Beam", "Water Pulse", "Grass Knot");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Piloswine", lvl, PokemonNature.N_JOLLY, 32, 255, 0, 255, 0, 0, "Ice Shard", "Earthquake", "Substitute", "Stone Edge");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Lapras", lvl, PokemonNature.N_MODEST, 255, 0, 0, 0, 80, 200, "Surf", "Ice Beam", "Thunderbolt", "Heal Bell");
						party[5] = poke;
					}
					else
					{
						lvl = 30;
						poke = Pokemon.getGymLeaderPokemon("Dewgong", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Rain Dance", "Surf", "Ice Beam", "Rest");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Jynx", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Lovely Kiss", "Ice beam", "Grass Knot", "Taunt");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Cloyster", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Ice Shard", "Rock Blast", "Rapid Spin", "Spikes");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Delibird", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Ice Beam", "Signal Beam", "Water Pulse", "Grass Knot");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Piloswine", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Ice Shard", "Earthquake", "Substitute", "Stone Edge");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Lapras", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Surf", "Ice Beam", "Thunderbolt", "Heal Bell");
						party[5] = poke;
					}
				}
				else if(m_name.equalsIgnoreCase("clair"))
				{

					if(p.getBadgeCount() > 7)
					{
						lvl = 65;
						poke = Pokemon.getGymLeaderPokemon("Dragonite", lvl, PokemonNature.N_MILD, 0, 80, 0, 200, 0, 255, "Draco Meteor", "Flamethrower", "Earthquake", "Extremespeed");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Dragonair", lvl, PokemonNature.N_ADAMANT, 80, 255, 0, 200, 0, 0, "Dragon Dance", "Extremespeed", "outrage", "Aqua Tail");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Charizard", lvl, PokemonNature.N_TIMID, 0, 0, 32, 255, 0, 255, "Flamethrower", "Air Slash", "Solarbeam", "Roost");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Gyarados", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 255, 32, 255, "Dragon Dance", "Waterfall", "Stone Edge", "Rain Dance");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Kingdra", lvl, PokemonNature.N_ADAMANT, 160, 160, 0, 160, 80, 0, "Rest", "Dragon Dance", "Outrage", "Waterfall");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Altaria", lvl, PokemonNature.N_CAREFUL, 255, 0, 80, 0, 220, 0, "Dragon Claw", "Roost", "Heal Bell", "Perish Song");
						party[5] = poke;
					}
					else
					{
						lvl = 35;
						poke = Pokemon.getGymLeaderPokemon("Dragonite", lvl, PokemonNature.N_MILD, 0, 0, 0, 0, 0, 0, "Draco Meteor", "Flamethrower", "Earthquake", "Extremespeed");
						party[3] = poke;
						poke = Pokemon.getGymLeaderPokemon("Dragonair", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Dragon Dance", "Extremespeed", "outrage", "Aqua Tail");
						party[2] = poke;
						poke = Pokemon.getGymLeaderPokemon("Charizard", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Flamethrower", "Air Slash", "Solarbeam", "Roost");
						party[1] = poke;
						poke = Pokemon.getGymLeaderPokemon("Gyarados", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Dragon Dance", "Waterfall", "Stone Edge", "Rain Dance");
						party[0] = poke;
						poke = Pokemon.getGymLeaderPokemon("Kingdra", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Rest", "Dragon Dance", "Outrage", "Waterfall");
						party[4] = poke;
						poke = Pokemon.getGymLeaderPokemon("Altaria", lvl, PokemonNature.N_CAREFUL, 0, 0, 0, 0, 0, 0, "Dragon Claw", "Roost", "Heal Bell", "Perish Song");
						party[5] = poke;
					}
				}
				// Hoenn
				else if(m_name.equalsIgnoreCase("roxanne"))
				{
					lvl = 70;
					poke = Pokemon.getGymLeaderPokemon("Probopass", lvl, PokemonNature.N_CALM, 252, 0, 255, 0, 255, 0, "Stealth Rock", "Thunderbolt", "Power Gem", "Toxic");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Golem", lvl, PokemonNature.N_ADAMANT, 255, 255, 0, 255, 0, 0, "Rock Blast", "Earthquake", "Explosion", "Sucker Punch");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Onix", lvl, PokemonNature.N_JOLLY, 255, 255, 0, 255, 0, 0, "Stealth Rock", "Earthquake", "Stone Edge", "Taunt");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Kabutops", lvl, PokemonNature.N_ADAMANT, 255, 255, 0, 255, 0, 0, "Swords Dance", "Waterfall", "Stone Edge", "Aqua Jet");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Corsola", lvl, PokemonNature.N_BOLD, 255, 0, 255, 0, 255, 0, "Recover", "Toxic", "Surf", "Reflect");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Tyranitar", lvl, PokemonNature.N_HASTY, 0, 255, 0, 255, 0, 255, "Crunch", "Pursuit", "Superpower", "Flamethrower");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("brawly"))
				{
					lvl = 74;
					poke = Pokemon.getGymLeaderPokemon("Medicham", lvl, PokemonNature.N_JOLLY, 255, 255, 0, 255, 0, 0, "Hi Jump Kick", "Psycho Cut", "Thunderpunch", "Ice Punch");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Hariyama", lvl, PokemonNature.N_ADAMANT, 0, 255, 255, 255, 0, 0, "Close Combat", "Payback", "Bullet Punch", "Ice Punch");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Hitmontop", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 0, 0, 0, "Fake Out", "Close Combat", "Stone Edge", "Mach Punch");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Machamp", lvl, PokemonNature.N_ADAMANT, 255, 255, 0, 255, 0, 0, "Rest", "Sleep Talk", "Dynamicpunch", "Ice Punch");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Riolu", lvl, PokemonNature.N_ADAMANT, 0, 255, 140, 255, 140, 0, "Agility", "Hi Jump Kick", "Crunch", "Ice Punch");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Breloom", lvl, PokemonNature.N_IMPISH, 255, 0, 255, 255, 0, 0, "Substitute", "Leech Seed", "Spore", "Focus Punch");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("wattson"))
				{
					lvl = 78;
					poke = Pokemon.getGymLeaderPokemon("Electrode", lvl, PokemonNature.N_JOLLY, 255, 255, 0, 255, 0, 0, "Rain Dance", "Taunt", "Thunder", "Explosion");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Manectric", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 255, 255, "Thunderbolt", "Flamethrower", "Charge Beam", "Leaf Storm");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Magneton", lvl, PokemonNature.N_MODEST, 255, 0, 0, 255, 0, 255, "Metal Sound", "Thunderbolt", "Ice Beam", "Substitute");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Amapharos", lvl, PokemonNature.N_MODEST, 255, 0, 0, 0, 255, 255, "Rain Dance", "Safeguard", "Thunder", "Waterpulse");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Electabuzz", lvl, PokemonNature.N_HASTY, 0, 255, 0, 255, 0, 255, "Thunderbolt", "Ice Punch", "Cross Chop", "Psychic");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Jolteon", lvl, PokemonNature.N_TIMID, 0, 0, 0, 255, 255, 255, "Shadow Ball", "Thunderbolt", "Bite", "Charge Beam");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("flannery"))
				{
					lvl = 82;
					poke = Pokemon.getGymLeaderPokemon("Torkoal", lvl, PokemonNature.N_BOLD, 255, 0, 255, 0, 255, 0, "Stealth Rock", "Rapid Spin", "Lava Plume", "Toxic");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Camerupt", lvl, PokemonNature.N_RASH, 0, 255, 0, 255, 0, 255, "Rock Polish", "Fire Blast", "Earth Power", "Stone Edge");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Rapidash", lvl, PokemonNature.N_JOLLY, 0, 255, 255, 255, 0, 0, "Flare Blitz", "Hypnosis", "Megahorn", "Morning Sun");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Magcargo", lvl, PokemonNature.N_BOLD, 255, 0, 255, 0, 255, 0, "Lava Plume", "Rock Slide", "Recover", "Will-O-Wisp");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Flareon", lvl, PokemonNature.N_CALM, 255, 0, 255, 0, 255, 0, "Wish", "Protect", "Flamethrower", "Toxic");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Magmar", lvl, PokemonNature.N_NAIVE, 0, 255, 0, 255, 0, 255, "Fire Blast", "Thunderbolt", "Cross Chop", "Earthquake");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("norman"))
				{
					lvl = 87;
					poke = Pokemon.getGymLeaderPokemon("Spinda", lvl, PokemonNature.N_JOLLY, 255, 255, 255, 255, 0, 0, "Endure", "Flail", "Sucker Punch", "Faint Attack");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Linoone", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 255, 0, 0, "Belly Drum", "ExtremeSpeed", "Seed Bomb", "Shadow Claw");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Chansey", lvl, PokemonNature.N_BOLD, 255, 255, 255, 0, 255, 0, "Wish", "Protect", "Seismic Toss", "Toxic");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Delcatty", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 255, 0, 0, "Thunder Wave", "Sing", "Heal Bell", "Double-Edge");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Vigoroth", lvl, PokemonNature.N_JOLLY, 255, 255, 255, 255, 0, 0, "Encore", "Substitute", "Return", "Sucker Punch");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Slaking", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 255, 0, 0, "Return", "Earthquake", "Shadow Claw", "Fire Punch");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("winona"))
				{
					lvl = 92;
					poke = Pokemon.getGymLeaderPokemon("Tropius", lvl, PokemonNature.N_BOLD, 255, 255, 255, 255, 0, 0, "Leaf Blade", "Swords Dance", "Aerial Ace", "Earthquake");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Pelipper", lvl, PokemonNature.N_MODEST, 255, 0, 255, 255, 0, 255, "Hydro Pump", "Air Slash", "Bullet Seed", "Roost");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Skarmory", lvl, PokemonNature.N_IMPISH, 255, 255, 255, 0, 255, 0, "Spikes", "Steel Wing", "Roost", "Brave Bird");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Altaria", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 255, 0, 0, "Rest", "Sleep Talk", "Dynamicpunch", "Ice Punch");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Togekiss", lvl, PokemonNature.N_MODEST, 255, 0, 255, 255, 0, 255, "Nasty Plot", "Air Slash", "Aura Sphere", "Heal Bell");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Flygon", lvl, PokemonNature.N_JOLLY, 0, 255, 255, 255, 255, 0, "Earthquake", "Outrage", "Stone Edge", "Fire Blast");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("tate_&_liza"))
				{
					lvl = 97;
					poke = Pokemon.getGymLeaderPokemon("Lunatone", lvl, PokemonNature.N_MODEST, 255, 0, 0, 255, 255, 255, "Rock Polish", "Psychic", "Earth Power", "Shadow Ball");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Solrock", lvl, PokemonNature.N_BRAVE, 255, 255, 255, 255, 0, 0, "Trick Room", "Explosion", "Stealth Rock", "Zen Headbutt");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Claydol", lvl, PokemonNature.N_BOLD, 255, 0, 255, 255, 0, 255, "Rapid Spin", "Stealth Rock", "Earth Power", "Ice Beam");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Xatu", lvl, PokemonNature.N_TIMID, 0, 0, 255, 255, 255, 255, "Calm Mind", "Psychic", "Heat Wave", "Earthquake");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Chimecho", lvl, PokemonNature.N_BOLD, 255, 0, 255, 140, 255, 140, "Psychic", "Heal Bell", "Wish", "Thunder Wave");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Wobbuffet", lvl, PokemonNature.N_CALM, 255, 0, 255, 140, 255, 140, "Encore", "Counter", "Mirror Coat", "Tickle");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("wallace"))
				{
					lvl = 103;
					poke = Pokemon.getGymLeaderPokemon("Walrein", lvl, PokemonNature.N_BOLD, 255, 255, 255, 255, 0, 0, "Protect", "Substitute", "Super Fang", "Blizzard");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Whiscash", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 255, 0, 0, "Dragon Dance", "Aqua Tail", "Earthquake", "Bounce");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Crawdaunt", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 255, 0, 0, "Dragon Dance", "Waterfall", "Crunch", "x-Scissor");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Lapras", lvl, PokemonNature.N_MODEST, 255, 0, 0, 255, 255, 255, "Surf", "Ice Beam", "Thunderbolt", "Heal Bell");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Gyarados", lvl, PokemonNature.N_ADAMANT, 255, 255, 255, 255, 0, 0, "Dragon Dance", "Waterfall", "Stone Edge", "Earthquake");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Glalie", lvl, PokemonNature.N_IMPISH, 255, 0, 255, 255, 255, 0, "Spikes", "Taunt", "Ice Shard", "Surf");
					party[5] = poke;
				}
				// sinnoh
				else if(m_name.equalsIgnoreCase("roark"))
				{
					lvl = 110;
					poke = Pokemon.getGymLeaderPokemon("Golem", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Stone Edge", "Earthquake", "Double-Edge", "Sucker Punch");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Onix", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Stone Edge", "Earthquake", "Stealth Rock", "Taunt");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Rampardos", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Stone Edge", "Earthquake", "Zen Headbutt", "Fire Punch");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Rhyperior", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Earthquake", "Rock Blast", "Megahorn", "Stealth Rock");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Mamoswine", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Earthquake", "Stone Edge", "Ice Shard", "Superpower");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Gliscor", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Taunt", "Earthquake", "Roost", "Toxic");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("gardenia"))
				{
					lvl = 115;
					poke = Pokemon.getGymLeaderPokemon("Cherrim", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Sunny Day", "GrassWhistle", "Energy Ball", "Aqua Tail");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Torterra", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Rock Polish", "Earthquake", "Wood Hammer", "Stone Edge");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Roserade", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Toxic Spikes", "Sleep Powder", "Leaf Storm", "Blizzard");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Leafeon", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Swords Dance", "Leaf Blade", "Double-Edge", "Quick Attack");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Tangrowth", lvl, PokemonNature.N_RELAXED, 0, 0, 0, 0, 0, 0, "Power Whip", "Synthesis", "Sleep Powder", "Earthquake");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Abomasnow", lvl, PokemonNature.N_HASTY, 0, 0, 0, 0, 0, 0, "Blizzard", "Wood Hammer", "Flamethrower", "Earthquake");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("maylene"))
				{
					lvl = 120;
					poke = Pokemon.getGymLeaderPokemon("Medicham", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Hi Jump Kick", "Psycho Cut", "Thunderpunch", "Ice Punch");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Meditite", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Close Combat", "Payback", "Bullet Punch", "Ice Punch");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Hitmonchan", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Fake Out", "Close Combat", "Stone Edge", "Mach Punch");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Machamp", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Rest", "Sleep Talk", "Dynamicpunch", "Ice Punch");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Lucario", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Aura Sphere", "Shadow Ball", "Dragon Pulse", "Vacuum Wave");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Hitmonlee", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Agility", "Hi Jump Kick", "Ice Punch", "Focus Punch");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("crasher wake"))
				{
					lvl = 125;
					poke = Pokemon.getGymLeaderPokemon("Gyarados", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Dragon Dance", "Waterfall", "Stone Edge", "Bounce");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Quagsire", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Recover", "Earthquake", "Waterfall", "Toxic");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Floatzel", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Waterfall", "Ice Punch", "Return", "Rain Dance");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Manaphy", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Rest", "Calm Mind", "Surf", "Ice Beam");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Gastrodon", lvl, PokemonNature.N_CALM, 0, 0, 0, 0, 0, 0, "Surf", "Recover", "Toxic", "Ice Beam");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Vaporeon", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Surf", "Ice Beam", "Thunderbolt", "Signal Beam");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("fatina"))
				{
					lvl = 130;
					poke = Pokemon.getGymLeaderPokemon("Drifblim", lvl, PokemonNature.N_MILD, 0, 0, 0, 0, 0, 0, "Mach Punch", "Shadow Ball", "Explosion", "Hypnosis");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Gengar", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Substitute", "Shadow Ball", "Crunch", "Fire Punch");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Mismagius", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Taunt", "Nasty Plot", "Shadow Ball", "Thunderbolt");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Rotom", lvl, PokemonNature.N_MODEST, 0, 0, 0, 0, 0, 0, "Thunderbolt", "Shadow Ball", "Earthquake", "Ice Punch");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Dusknoir", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Will-O-Wisp", "Pain Split", "Crunch", "Ice Punch");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Froslass", lvl, PokemonNature.N_TIMID, 0, 0, 0, 0, 0, 0, "Substitute", "Thunder Wave", "Blizzard", "Water Pulse");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("byron"))
				{
					lvl = 135;
					poke = Pokemon.getGymLeaderPokemon("Bronzong", lvl, PokemonNature.N_SASSY, 0, 0, 0, 0, 0, 0, "Stealth Rock", "Gyro Ball", "Thunderpunch", "Ice Punch");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Steelix", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Earthquake", "Iron Head", "Stealth Rock", "Stone Edge");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Bastiodon", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "AncientPower", "Iron Head", "Blizzard", "Thunderbolt");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Metagross", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Hammer Arm", "Meteor Mash", "Zen Headbutt", "Earthquake");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Lucario", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Aura Sphere", "Dragon Pulse", "Extremespeed", "Ice Punch");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Scizor", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Razor Wind", "X-Scissor", "Night Slash", "Steel Wing");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("candice"))
				{
					lvl = 140;
					poke = Pokemon.getGymLeaderPokemon("Medicham", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Hi Jump Kick", "Fire Punch", "Thunderpunch", "Ice Punch");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Glaceon", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Blizzard", "Crunch", "Iron Tail", "Shadow Ball");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Snover", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Wood Hammer", "Blizzard", "Water Pulse", "Toxic");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Abomasnow", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Wood Hammer", "Blizzard", "Brick Break", "Earthquake");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Lapras", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Reflect", "Thunderbolt", "Hydro Pump", "Ice Beam");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Weavile", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Dark Pulse", "Ice Beam", "Iron Tail", "Focus Punch");
					party[5] = poke;
				}
				else if(m_name.equalsIgnoreCase("volkner"))
				{
					lvl = 145;
					poke = Pokemon.getGymLeaderPokemon("Raichu", lvl, PokemonNature.N_JOLLY, 0, 0, 0, 0, 0, 0, "Brick Break", "Grass Knot", "Strength", "Volt Tackle");
					party[0] = poke;
					poke = Pokemon.getGymLeaderPokemon("Ambipom", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Astonish", "Shadow Ball", "Thunderbolt", "Brick Break");
					party[1] = poke;
					poke = Pokemon.getGymLeaderPokemon("Octillery", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Rock Blast", "Surf", "Sludge Bomb", "Ice Beam");
					party[2] = poke;
					poke = Pokemon.getGymLeaderPokemon("Luxray", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Scary Face", "Thunder Fang", "Crunch", "Earthquake");
					party[3] = poke;
					poke = Pokemon.getGymLeaderPokemon("Magnezone", lvl, PokemonNature.N_ADAMANT, 0, 0, 0, 0, 0, 0, "Supersonic", "Magnet Bomb", "Zap Cannon", "Reflect");
					party[4] = poke;
					poke = Pokemon.getGymLeaderPokemon("Electivire", lvl, PokemonNature.N_IMPISH, 0, 0, 0, 0, 0, 0, "Rain Dance", "Light Screen", "Thunderpunch", "Fire Punch");
					party[5] = poke;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				if(p.getBadgeCount() > 7)
				{
					/* If a player has 8 badges, level 80s all round */
					for(int i = 0; i < 6; i++)
					{
						name = (String) m_possiblePokemon.keySet().toArray()[r.nextInt(m_possiblePokemon.keySet().size())];
						level = 80;
						poke = Pokemon.getRandomPokemon(name, level);
						party[i] = poke;
					}
				}
				else
				{
					/* If a player hasn't got 8 badges, give them normal levels */
					for(int i = 0; i < m_minPartySize; i++)
					{
						name = (String) m_possiblePokemon.keySet().toArray()[r.nextInt(m_possiblePokemon.keySet().size())];
						level = m_possiblePokemon.get(name);
						poke = Pokemon.getRandomPokemon(name, level);
						party[i] = poke;
					}
				}
			}
		}
		// TODO: Random trainer's pokemon.
		else if(m_name.equalsIgnoreCase("Random_Battlefrontier_Trainer"))
		{
			int lvl;
			if(p.getHighestLevel() <= 50)
				lvl = 50;
			else
				lvl = p.getHighestLevel();
			// r.nextInt(472);
			poke = Pokemon.getRandomPokemon(PokemonSpecies.getDefaultData(), DataService.getBattleMechanics(), lvl);
			party[0] = poke;
			poke = Pokemon.getRandomPokemon(PokemonSpecies.getDefaultData(), DataService.getBattleMechanics(), lvl);
			party[1] = poke;
			poke = Pokemon.getRandomPokemon(PokemonSpecies.getDefaultData(), DataService.getBattleMechanics(), lvl);
			party[2] = poke;
		}
		else
		{
			int playerPartySize = p.getPartyCount();
			if(m_minPartySize < playerPartySize && m_possiblePokemon.size() >= playerPartySize + 1)
			{
				/* The player has more Pokemon, generate a random party */
				/* First, get a random party size that is greater than m_minPartySize
				 * and less than or equal to the amount of pokemon in the player's party + 1 */
				int pSize = r.nextInt(playerPartySize + 1 > 6 ? 6 : playerPartySize + 1);
				while(pSize < m_minPartySize)
					pSize = r.nextInt(playerPartySize + 1 > 6 ? 6 : playerPartySize + 1);
				/* Now generate the random Pokemon */
				for(int i = 0; i <= pSize; i++)
				{
					// Select a random Pokemon
					name = (String) m_possiblePokemon.keySet().toArray()[r.nextInt(m_possiblePokemon.keySet().size())];
					level = m_possiblePokemon.get(name);
					/* Level scaling */
					/* while(level < p.getHighestLevel() - 3) {
					 * level = r.nextInt(p.getHighestLevel() + 5);
					 * } */
					poke = Pokemon.getRandomPokemon(name, level);
					party[i] = poke;
				}
			}
			else
				/* Generate a party of size m_minPartySize */
				for(int i = 0; i < m_minPartySize; i++)
				{
					// Select a random Pokemon from this list of possible Pokemons
					name = (String) m_possiblePokemon.keySet().toArray()[r.nextInt(m_possiblePokemon.keySet().size())];
					level = m_possiblePokemon.get(name);
					// Ensure levels are the similiar
					/* while(level < p.getHighestLevel() - 3) {
					 * level = r.nextInt(p.getHighestLevel() + 5);
					 * } */
					poke = Pokemon.getRandomPokemon(name, level);
					party[i] = poke;
				}
		}
		return party;
	}

	/**
	 * Returns true if this npc allows box access
	 * 
	 * @return
	 */
	public boolean isBox()
	{
		return m_isBox;
	}

	/**
	 * Returns true if the npc is a gym leader
	 * 
	 * @return
	 */
	public boolean isGymLeader()
	{
		return m_badge > -1;
	}

	/**
	 * Return true if this npc heals your pokemon
	 * 
	 * @return
	 */
	public boolean isHealer()
	{
		return m_isHeal;
	}

	/**
	 * Returns true if this npc is a shop keeper
	 * 
	 * @return
	 */
	public boolean isShopKeeper()
	{
		if(m_isShop > 0)
			return true;
		return false;
	}

	/**
	 * Returns true if an NPC is a trainer
	 * 
	 * @return
	 */
	public boolean isTrainer()
	{
		return m_possiblePokemon != null && m_minPartySize > 0 && m_possiblePokemon.size() > 0;
	}

	public void resetLastBattleTime()
	{
		m_lastBattle = 0;
	}

	/**
	 * Sets the badge this npc gives, if any
	 * 
	 * @param i
	 */
	public void setBadge(int i)
	{
		m_badge = i;
	}

	/**
	 * Sets if this npc allows box access
	 * 
	 * @param b
	 */
	public void setBox(boolean b)
	{
		m_isBox = b;
	}

	/**
	 * Sets if this npc is a healer or not
	 * 
	 * @param b
	 */
	public void setHealer(boolean b)
	{
		m_isHeal = b;
	}

	/**
	 * Sets the minimum sized party this npc should have
	 * 
	 * @param size
	 */
	public void setPartySize(int size)
	{
		m_minPartySize = size > 6 ? 6 : size;
	}

	/**
	 * Sets the possible Pokemon this trainer can have
	 * 
	 * @param pokes
	 */
	public void setPossiblePokemon(HashMap<String, Integer> pokes)
	{
		m_possiblePokemon = pokes;
	}

	/**
	 * Sets if this npc is a shop keeper
	 * 
	 * @param b
	 */
	public void setShopKeeper(int b)
	{
		m_isShop = b;
		if(b > 0)
			try
			{
				m_shop = new Shop(b);
				m_shop.start();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	/**
	 * Talks to a player
	 * 
	 * @param player
	 */
	public void talkToPlayer(Player player)
	{
		if(isTrainer())
		{
			if(m_name.equalsIgnoreCase("Random_Battlefrontier_Trainer"))
				if(player.canBattle)
					m_lastBattle = 0;
			if(canBattle())
			{
				String speech = getSpeech();
				if(!speech.equalsIgnoreCase(""))
				{
					ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
					message.addInt(2);
					message.addString(speech);
					player.getSession().Send(message);
				}
				updateLastBattleTime();
				player.setBattling(true);
				player.canBattle = false;
				player.setBattleField(new NpcBattleField(DataService.getBattleMechanics(), player, this));
				return;
			}
			else
			{
				player.setTalking(false);
				return;
			}
		}
		else
		{
			/* If this NPC wasn't a trainer, handle other possibilities */
			String speech = getSpeech();
			if(m_name.equals("Kurt"))
			{
				speech = getKurtSpeech();
			}
			if(!speech.equalsIgnoreCase(""))
			{
				if(!player.isShopping())
				{
					/* Dont send if player is shopping! */
					if(player.m_facing == Direction.Down)
						setFacing(Direction.Up, player.m_map);
					else if(player.m_facing == Direction.Up)
						setFacing(Direction.Down, player.m_map);
					else if(player.m_facing == Direction.Left)
						setFacing(Direction.Right, player.m_map);
					else if(player.m_facing == Direction.Right)
						setFacing(Direction.Left, player.m_map);
					final Timer timer = new Timer();
					timer.schedule(new TimerTask()
					{
						@Override
						public void run()
						{
							setFacing(originalDirection);
							timer.cancel();
						}
					}, 20 * 1000);
					ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
					message.addInt(2);
					message.addString(speech);
					player.getSession().Send(message);
				}
			}
			/* If this NPC is a sprite selection npc */
			if(m_name.equalsIgnoreCase("Spriter"))
			{
				player.setSpriting(true);
				ServerMessage message = new ServerMessage(ClientPacket.SPRITE_SELECT);
				player.getSession().Send(message);
				return;
			}
			// @author sadhi
			/* If this NPC is a sailor, show warp options. */
			if(m_name.equalsIgnoreCase("Seasond_Sailor"))
			{
				player.setIsTaveling(true);
				ServerMessage message = new ServerMessage(ClientPacket.TRAVEL_BOAT);
				player.getSession().Send(message);
				return;
			}
			// @author sadhi
			/* If this NPC is a train conductor. */
			if(m_name.equalsIgnoreCase("Ticket_Vendor"))
			{
				player.setIsTaveling(true);
				ServerMessage message = new ServerMessage(ClientPacket.TRAVEL_EVENT);
				player.getSession().Send(message);
				return;
			}
			// @author sadhi
			// TODO: finish the random trainer.
			/* Random trainer different from normal a trainer. */
			if(m_name.equalsIgnoreCase("Random_Battlefrontier_Trainer"))
			{
				player.setBattling(true);
				player.setBattleField(new NpcBattleField(DataService.getBattleMechanics(), player, this));
				return;
			}
			// @author sadhi
			/* This npc teleportss a player a battle room in a specific battle frontier. */
			if(m_name.contains("Battlefrontier"))
			{
				player.setIsTaveling(true);
				ServerMessage message = new ServerMessage(ClientPacket.BATTLEFRONTIER_EVENT);
				player.getSession().Send(message);
				return;
			}
			// @author sadhi
			/**
			 * This NPC is the moverelearner.
			 * For the price of what is behind the name: "MoveRelearner_[xx-ItemName]"
			 * In which xx stands for the ammount and ItemName for the item he wants in return
			 */
			if(m_name.contains("MoveRelearner"))
			{
				player.setShopping(true);
				ServerMessage message = new ServerMessage(ClientPacket.MOVERETUTOR);
				message.addString(m_name);
				player.getSession().Send(message);
				return;
			}
			// @author sadhi
			/**
			 * This NPC is the moveretutor.
			 * He will teach the move that is behind his name: "MoveTutor_[Move]"
			 * [move] can also be a number in this case he will teach more than one move.
			 * check the code to see which number corresponds to which move.
			 */
			if(m_name.contains("MoveTutor"))
			{
				player.setShopping(true);
				ServerMessage message = new ServerMessage(ClientPacket.MOVERETUTOR);
				String tmp = parseMoveTutor(m_name.split("_")[1]);
				message.addString(tmp);
				player.getSession().Send(message);
				return;
			}
			/**
			 * This is Kurt.
			 * He will make a different pokeball each day for you if you bring him the correct apricorn.
			 */
			if(m_name.contains("Kurt"))
			{
				player.setShopping(true);
				int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
				switch(day)
				{
					case Calendar.MONDAY:
						switchDailyApricorn("White Apricorn", "Fast Ball", player, ItemID.WHITE_APRICORN);
						break;
					case Calendar.TUESDAY:
						switchDailyApricorn("Red Apricorn", "Level Ball", player, ItemID.RED_APRICORN);
						break;
					case Calendar.WEDNESDAY:
						switchDailyApricorn("Blue Apricorn", "Lure Ball", player, ItemID.BLUE_APRICORN);
						break;
					case Calendar.THURSDAY:
						switchDailyApricorn("Black Apricorn", "Heavy Ball", player, ItemID.BLACK_APRICORN);
						break;
					case Calendar.FRIDAY:
						switchDailyApricorn("Pink Apricorn", "Love Ball", player, ItemID.PINK_APRICORN);
						break;
					case Calendar.SATURDAY:
						switchDailyApricorn("Green Apricorn", "Friend Ball", player, ItemID.GREEN_APRICORN);
						break;
					case Calendar.SUNDAY:
						switchDailyApricorn("Yellow Apricorn", "Moon Ball", player, ItemID.YELLOW_APRICORN);
						break;
				}
				return;
			}
			/* Box access */
			if(m_isBox)
			{
				/* Send the data for the player's first box, they may change this later. */
				player.setTalking(false);
				player.setBoxing(true);
				player.sendBoxInfo(0);
			}
			/* Healer */
			if(m_isHeal)
			{
				player.healPokemon();
				if(m_name.equalsIgnoreCase("Battlefrontier Nurse Joy"))
					player.canBattle = true;
				player.setLastHeal(player.getX(), player.getY(), player.getMapX(), player.getMapY());
			}
			/* Shop access */
			if(m_isShop > 0)
			{
				if(!player.isShopping())
				{
					ServerMessage message = new ServerMessage(ClientPacket.SHOP_LIST);
					message.addString(m_shop.getStockData());
					player.getSession().Send(message);
					player.setShopping(true);
					player.setShop(m_shop);
				}
			}
		}
	}

	private String getKurtSpeech()
	{
		int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		switch(day)
		{
			case Calendar.MONDAY:
				return "0,2,3,10";
			case Calendar.TUESDAY:
				return "0,2,4,10";
			case Calendar.WEDNESDAY:
				return "0,2,5,10";
			case Calendar.THURSDAY:
				return "0,2,6,10";
			case Calendar.FRIDAY:
				return "0,2,7,10";
			case Calendar.SATURDAY:
				return "0,2,8,10";
			case Calendar.SUNDAY:
				return "0,2,9,10";
		}
		return "0";
	}

	/**
	 * @param apricorn, the apricorn being sold today
	 * @param pokeball, the pokeball being made today
	 * @param player, the player that is talking with kurt
	 * @param id, the ID of the apricorn
	 */
	public void switchDailyApricorn(String apricorn, String pokeball, Player player, int id)
	{
		int pos = -1;
		int quantity = 0;
		pos = player.getBag().containsItem(id);
		if(pos == -1)
		{
			player.setShopping(false);
			/* Return You don't have that item, fool! */
			ServerMessage msg = new ServerMessage(ClientPacket.DONT_HAVE_ITEM);
			msg.addString(apricorn);
			player.getSession().Send(msg);
		}
		else
		{
			quantity = player.getBag().getItems().get(pos).getQuantity();
			if(player.getMoney() >= 1000)
			{
				ServerMessage message = new ServerMessage(ClientPacket.KURT);
				message.addString(pokeball);
				message.addString(apricorn);
				if(player.getMoney() >= quantity * 1000)
				{
					message.addInt(quantity);
				}
				else
				{
					int m = (int) Math.ceil(player.getMoney() / 1000);
					message.addInt(m);
				}
				player.getSession().Send(message);
			}
			else
			{
				/* Return You have no money, fool! */
				ServerMessage message = new ServerMessage(ClientPacket.NOT_ENOUGH_MONEY);
				player.getSession().Send(message);
				player.setShopping(false);
			}
		}
	}

	/**
	 * Updates the time this NPC last battled.
	 */
	public void updateLastBattleTime()
	{
		/* Only set this if they are not gym leaders */
		if(!isGymLeader())
		{
			m_lastBattle = System.currentTimeMillis();
			NpcSleepTimer.addNPC(this);
		}
	}

	/**
	 * Returns a string of this npcs speech id's.
	 */
	private String getSpeech()
	{
		String result = "";
		for(int i = 0; i < m_speech.size(); i++)
			result += m_speech.get(i) + ",";
		return result;
	}

	public String parseMoveTutor(String s)
	{
		String moves = "";
		int shop = 1; // default is one
		try
		{
			shop = Integer.parseInt(s);
		}
		catch(Exception e)
		{
			System.err.println("movetutor has to end with an integer");
		}
		switch(shop)
		{
			case 0:
				moves += "Draco Meteor - $25k, ";
				moves += "Blast Burn - $25k, ";
				moves += "Frenzy Plant - $25k, ";
				moves += "Hydro Cannon - $25k";
				break;
			case 1:
				moves += "Blast Burn - $25k, ";
				moves += "Frenzy Plant - $25k, ";
				moves += "Hydro Cannon - $25k";
				break;
			case 2:
				moves += "Counter - 48BP, ";
				moves += "Defense Curl - 16BP, ";
				moves += "Dream Eater - 24BP, ";
				moves += "Icy Wind - 24BP, ";
				moves += "Mud-Slap - 24BP, ";
				moves += "Psych Up - 48BP, ";
				moves += "Rock Slide - 48BP, ";
				moves += "Snore - 24BP, ";
				moves += "Softboiled - 16BP, ";
				moves += "Swift - 24BP, ";
				moves += "Swords Dance - 48BP, ";
				moves += "Thunder Wave - 48BP";
				break;
			case 3:
				moves += "Fire Punch - 48BP, ";
				moves += "Ice Punch - 48BP, ";
				moves += "ThunderPunch - 48BP, ";
				moves += "Mega Kick - 48BP, ";
				moves += "Mega Punch - 24BP, ";
				moves += "Seismic Toss - 24BP, ";
				moves += "Endure - 48BP, ";
				moves += "Body Slam - 48BP";
				break;
			case 4:
				moves += "Body Slam - $20k, ";
				moves += "Double-Edge - $20k, ";
				moves += "Dream Eater - $10k, ";
				moves += "Icy Wind - $20k, ";
				moves += "Mimic - $10k, ";
				moves += "Nightmare - $10k, ";
				moves += "Seismic Toss - $20k, ";
				moves += "Selfdestruct - $20k, ";
				moves += "Sky Attack - $20k, ";
				moves += "Substitute - $25k, ";
				moves += "Swagger - $20k, ";
				moves += "Thunder Wave - $10k";
				break;
			default:
				break;
		}

		return moves;
	}
}
