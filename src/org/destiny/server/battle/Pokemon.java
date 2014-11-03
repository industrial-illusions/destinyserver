/* Pokemon.java Created on December 13, 2006, 5:38 PM This file is a part of
 * Shoddy Battle. Copyright (C) 2006 Colin Fitzpatrick This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, visit the Free Software Foundation, Inc. online at
 * http://gnu.org. */

package org.destiny.server.battle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.mechanics.BattleMechanics;
import org.destiny.server.battle.mechanics.ModData;
import org.destiny.server.battle.mechanics.MoveQueueException;
import org.destiny.server.battle.mechanics.PokemonNature;
import org.destiny.server.battle.mechanics.PokemonType;
import org.destiny.server.battle.mechanics.StatException;
import org.destiny.server.battle.mechanics.StatMultiplier;
import org.destiny.server.battle.mechanics.ValidationException;
import org.destiny.server.battle.mechanics.clauses.Clause.PendanticDamageClause;
import org.destiny.server.battle.mechanics.moves.MoveList;
import org.destiny.server.battle.mechanics.moves.MoveListEntry;
import org.destiny.server.battle.mechanics.moves.PokemonMove;
import org.destiny.server.battle.mechanics.moves.MoveList.AttractEffect;
import org.destiny.server.battle.mechanics.statuses.AwesomeEffect;
import org.destiny.server.battle.mechanics.statuses.BurnEffect;
import org.destiny.server.battle.mechanics.statuses.ChargeEffect;
import org.destiny.server.battle.mechanics.statuses.ConfuseEffect;
import org.destiny.server.battle.mechanics.statuses.FlinchEffect;
import org.destiny.server.battle.mechanics.statuses.FreezeEffect;
import org.destiny.server.battle.mechanics.statuses.MultipleStatChangeEffect;
import org.destiny.server.battle.mechanics.statuses.ParalysisEffect;
import org.destiny.server.battle.mechanics.statuses.PercentEffect;
import org.destiny.server.battle.mechanics.statuses.PoisonEffect;
import org.destiny.server.battle.mechanics.statuses.StatChangeEffect;
import org.destiny.server.battle.mechanics.statuses.StatusEffect;
import org.destiny.server.battle.mechanics.statuses.StatusListener;
import org.destiny.server.battle.mechanics.statuses.ToxicEffect;
import org.destiny.server.battle.mechanics.statuses.abilities.IntrinsicAbility;
import org.destiny.server.battle.mechanics.statuses.items.HoldItem;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.protocol.ServerMessage;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;

/**
 * This class represents a pokemon in a battle. Its stats are automatically
 * modified as it attacks and is attacked, so no method for directly modifying
 * its stats are provided. Using this class requires a BattleMechanics object to
 * initialise its stats.
 * 
 * @author Colin
 */
public class Pokemon extends PokemonSpecies
{

	public enum ExpTypes
	{
		ERRATIC, FAST, FLUCTUATING, MEDIUM, PARABOLIC, SLOW
	}

	public static final int S_ACCURACY = 6;
	public static final int S_ATTACK = 1;
	/* Content stats */
	public static final int S_BEAUTY = 0;
	public static final int S_COOL = 3;
	public static final int S_CUTE = 1;
	public static final int S_DEFENCE = 2;
	public static final int S_EVASION = 7;
	/* Constants representing each statistic. */
	public static final int S_HP = 0;
	public static final int S_SMART = 2;
	public static final int S_SPATTACK = 4;
	public static final int S_SPDEFENCE = 5;
	public static final int S_SPEED = 3;
	public static final int S_TOUGH = 4;
	transient private IntrinsicAbility m_ability;
	// of
	// PP
	// Ups
	// applied
	// to
	// each
	// move.
	@Element
	private String m_abilityName;                          // Intrinsic
	transient private StatMultiplier m_accuracy;
	@Element
	private int m_baseExp;
	private int m_caughtWith;
	transient private final int[] m_contestStat = new int[5];
	private int m_databaseID = -1;
	// Transient statistics.
	@Element
	transient private String m_dateCaught;
	@ElementArray
	private int m_ev[];
	transient private StatMultiplier m_evasion;
	/* Stores the evolution this Pokemon is waiting to evolve to */
	private PokemonEvolution m_evolution = null;
	@Element
	private double m_exp;

	/* Stores the EXP growth rate of the Pokemon */
	@Element
	transient private ExpTypes m_expType;
	@Element
	transient private boolean m_fainted;
	transient private BattleField m_field;
	transient private boolean m_firstTurn = false;
	@Element
	private int m_gender = GENDER_MALE;
	@Element
	private int m_happiness;
	transient private int m_hp;
	@Element
	transient private int m_id;
	@Element(required = false)
	transient private HoldItem m_item;

	/* a variable to store a temp pokemon in for transform */
	@Element(required = false)
	transient private Pokemon temp;
	// whether a pokemon is transformed or not
	private Boolean transformed = false;

	// ability.
	@Element
	private String m_itemName;                             // Item
	// Hidden statistics.
	@ElementArray
	private int m_iv[];

	transient private MoveListEntry m_lastMove;
	// Intrinsic statistics.
	@Element
	private int m_level = -1;

	@ElementArray
	transient private int[] m_maxPp;
	// Battle mechanics.
	private BattleMechanics m_mech;
	private MoveListEntry[] m_move;

	private ArrayList<String> m_movesLearning;
	@ElementArray
	transient private StatMultiplier[] m_multiplier;
	@Element
	private PokemonNature m_nature;
	@Element
	private String m_nickname;
	transient private IntrinsicAbility m_originalAbility;
	@Element
	private long m_originalNo;
	@Element
	private String m_originalTrainer;
	transient private int m_party;
	@ElementArray
	transient private int[] m_pp;
	@ElementArray
	private int[] m_ppUp;                                 // Number
	// initially
	// held
	// by
	// the
	// pokemon.
	@Element
	private boolean m_shiny = false;
	@ElementArray
	transient private int[] m_stat;
	transient private ArrayList<StatusEffect> m_statuses;

	/**
	 * The health of a substitute, or zero if no substitute is out.
	 */
	transient private int m_substitute;

	/**
	 * Create and validate a new pokemon.
	 */
	public Pokemon(BattleMechanics mech, PokemonSpecies species, PokemonNature nature, String ability, String item, int gender, int level, int[] ivs, int[] evs, MoveListEntry[] moves, int[] ppUps)
			throws StatException
	{
		this(mech, species, nature, ability, item, gender, level, ivs, evs, moves, ppUps, true);
	}

	/** Creates a new instance of Pokemon */
	public Pokemon(BattleMechanics mech, PokemonSpecies species, PokemonNature nature, String ability, String item, int gender, int level, int[] ivs, int[] evs, MoveListEntry[] moves, int[] ppUps,
			boolean validate) throws StatException
	{
		super(species);
		m_mech = mech;
		m_iv = ivs;
		m_ev = evs;
		m_nature = nature;
		m_gender = gender;
		m_level = level;
		m_move = moves;
		m_abilityName = ability;
		if(m_ability == null)
			m_ability = IntrinsicAbility.getInstance(ability);
		m_itemName = item;
		m_ppUp = ppUps;
		m_name = species.getName();
		initialise();
	}

	public Pokemon(Pokemon p)
	{
		super(p);
		m_mech = p.getMech();
		Random gen = new Random();
		m_iv = new int[] { gen.nextInt(32), // IVs
				gen.nextInt(32), gen.nextInt(32), gen.nextInt(32), gen.nextInt(32), gen.nextInt(32) };
		m_ev = new int[] { 0, 0, 0, 0, 0, 0 };
		m_nature = p.getNature();
		m_gender = p.getGender();
		m_level = p.getLevel();
		m_move = p.getMoves();
		m_abilityName = p.getAbilityName();
		m_itemName = p.getItemName();
		m_ppUp = new int[] { 0, 0, 0, 0 };
		m_nickname = getSpeciesName();
		initialise();
	}

	public static int generateGender(int possibleGenders)
	{
		switch(possibleGenders)
		{
			case 0:
				return 0;
			case 1:
				return 1;
			case 2:
				return 2;
			case 3:
				if(DataService.getBattleMechanics().getRandom().nextBoolean())
					return 1;
				else
					return 2;
			default:
				return -1;
		}
	}

	/**
	 * Get the effectiveness of this pokemon attacking a particular type.
	 */
	public static double getEffectiveness(List<?> statuses, PokemonType move, PokemonType pokemon, boolean enemy)
	{
		double expected = move.getMultiplier(pokemon);
		synchronized(statuses)
		{
			Iterator<?> i = statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect eff = (StatusEffect) i.next();
				if(eff.isActive() && eff.isEffectivenessTransformer(enemy))
				{
					double actual = eff.getEffectiveness(move, pokemon, enemy);
					if(actual != expected)
						return actual;
				}
			}
		}
		return expected;
	}

	/**
	 * Get a random Pokemon object.
	 */
	public static Pokemon getRandomPokemon(PokemonSpeciesData d, BattleMechanics mech, int lvl)
	{
		Random random = mech.getRandom();
		int[] ivs = new int[6];
		for(int i = 0; i < ivs.length; ++i)
			ivs[i] = random.nextInt(32);
		int[] evs = new int[6];
		int evTotal = 0;
		final int inc = 16;
		while(evTotal + inc <= 510)
		{
			evs[random.nextInt(evs.length)] += inc;
			evTotal += inc;
		}
		PokemonNature nature = PokemonNature.getNature(random.nextInt(25));
		PokemonSpeciesData speciesData = d;
		int chosenPoke = random.nextInt(478);	// speciesData.getSpeciesCount()); //cut off the top so 4th gen legends are not included
		switch(chosenPoke)
		{
			case 132:
				chosenPoke = random.nextInt(131);
				break;
			case 137:
				chosenPoke = random.nextInt(131);
				break;
			case 144:
				chosenPoke = random.nextInt(131);
				break;
			case 145:
				chosenPoke = random.nextInt(131);
				break;
			case 146:
				chosenPoke = random.nextInt(131);
				break;
			case 150:
				chosenPoke = random.nextInt(131);
				break;
			case 151:
				chosenPoke = random.nextInt(131);
				break;
			// johto
			case 233:
				chosenPoke = random.nextInt(81) + 151;
				break;
			case 235:
				chosenPoke = random.nextInt(81) + 151;
				break;
			case 243:
				chosenPoke = random.nextInt(81) + 151;
				break;
			case 244:
				chosenPoke = random.nextInt(81) + 151;
				break;
			case 245:
				chosenPoke = random.nextInt(81) + 151;
				break;
			case 249:
				chosenPoke = random.nextInt(81) + 151;
				break;
			case 250:
				chosenPoke = random.nextInt(81) + 151;
				break;
			case 251:
				chosenPoke = random.nextInt(81) + 151;
				break;
			// hoenn
			case 377:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 378:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 379:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 380:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 381:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 382:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 383:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 384:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 385:
				chosenPoke = random.nextInt(124) + 252;
				break;
			case 386:
				chosenPoke = random.nextInt(124) + 252;
				break;
			// sinnoh
			case 474:
				chosenPoke = random.nextInt(87) + 252;
				break;
		}
		PokemonSpecies species = speciesData.getSpecies(chosenPoke);
		// String[] moveset = species.getStarterMoves();
		// if(moveset == null || moveset.length == 0)
		// return null;
		int moveCount = 4;
		String[] moves = species.getLevelMoves().values().toArray(new String[species.getLevelMoves().size()]);
		// int j =0;
		// for(int i = moves.length; i < moves.length + species.getTMMoves().length; i++)
		// {
		// moves[i] = species.getTMMoves()[j];
		// j++;
		// }

		MoveListEntry[] entries = new MoveListEntry[moveCount >= 4 ? 4 : moveCount];
		Set<String> moveSet = new HashSet<String>();
		int[] ppUp = new int[entries.length];
		for(int i = 0; i < entries.length; ++i)
		{
			String move;
			do
				move = moves[random.nextInt(moves.length)];
			while(moveSet.contains(move));
			moveSet.add(move);
			entries[i] = MoveList.getDefaultData().getMove(move);// d.getMoveData().getMove(move);
			ppUp[i] = random.nextInt(4);
		}

		String ability = null;
		String[] itemes = species.getPossibleAbilities(speciesData);
		if(itemes != null)
			ability = itemes[random.nextInt(itemes.length)];

		int genders = species.getPossibleGenders();
		int gender = GENDER_NONE;
		if(genders != GENDER_NONE)
		{
			int[] choices = { GENDER_MALE, GENDER_FEMALE };
			while(true)
			{
				gender = choices[random.nextBoolean() ? 0 : 1];
				if((genders & gender) != 0)
					break;
			}
		}
		Pokemon p = new Pokemon(mech, species, nature, ability, "", gender, lvl, ivs, evs, entries, ppUp);
		// Give it a 5% chance of being shiny.
		if(random.nextDouble() < (1 / 8192))

			p.setShiny(true);
		return p;
	}

	/**
	 * Returns a random Pokemon based on a species name and a level
	 * 
	 * @param speciesName
	 * @param level
	 * @return
	 */
	public static Pokemon getRandomPokemon(String species, int level)
	{
		Pokemon p;
		Random random = DataService.getBattleMechanics().getRandom();
		/* First obtain species data */
		PokemonSpecies ps = PokemonSpecies.getDefaultData().getPokemonByName(species);
		MoveListEntry[] moves = new MoveListEntry[4];
		/* Generate a list of possible moves this Pokemon could have at this level */
		ArrayList<MoveListEntry> possibleMoves = new ArrayList<MoveListEntry>();
		MoveList moveList = MoveList.getDefaultData();
		/* Get all starter moves */
		for(int i = 0; i < ps.getStarterMoves().length; i++)
			possibleMoves.add(moveList.getMove(ps.getStarterMoves()[i]));
		/* Get moves learned by levelling up */
		for(int i = 1; i <= level; i++)
			if(ps.getLevelMoves().containsKey(i))
			{
				MoveListEntry m = moveList.getMove(ps.getLevelMoves().get(i));
				boolean exists = false;
				/* Check if this move is already in the list of possible moves */
				for(int j = 0; j < possibleMoves.size(); j++)
					if(possibleMoves.get(j) != null && possibleMoves.get(j).getName() != null && m != null && m.getName() != null && possibleMoves.get(j).getName().equalsIgnoreCase(m.getName()))
					{
						exists = true;
						break;
					}
				/* If the move is not already in the list, add it to the list */
				if(!exists)
					possibleMoves.add(m);
			}
		/* possibleMoves sometimes has null moves stored in it, get rid of them */
		for(int i = 0; i < possibleMoves.size(); i++)
			if(possibleMoves.get(i) == null)
				possibleMoves.remove(i);
		possibleMoves.trimToSize();
		/* Now the store the final set of moves for the Pokemon */
		if(possibleMoves.size() <= 4)
			for(int i = 0; i < possibleMoves.size(); i++)
				moves[i] = possibleMoves.get(i);
		else
		{
			MoveListEntry m = null;
			for(int i = 0; i < 4; i++)
				if(possibleMoves.size() == 0)
					moves[i] = null;
				else
				{
					m = possibleMoves.get(random.nextInt(possibleMoves.size()));
					moves[i] = m;
					possibleMoves.remove(m);
					possibleMoves.trimToSize();
					m = null;
				}
		}
		/* Get all possible abilities */
		String[] abilities = PokemonSpecies.getDefaultData().getPokemonByName(species).getAbilities();
		/* First select an ability randomly */
		String ab = abilities[random.nextInt(abilities.length)];
		/* Now lets create the pokemon itself */
		p = new Pokemon(DataService.getBattleMechanics(), ps, PokemonNature.getNature(random.nextInt(PokemonNature.getNatureNames().length)), ab, null,
				Pokemon.generateGender(ps.getPossibleGenders()), level, new int[] { random.nextInt(32), // IVs
						random.nextInt(32), random.nextInt(32), random.nextInt(32), random.nextInt(32), random.nextInt(32) }, new int[] { 0, 0, 0, 0, 0, 0 }, // EVs
				moves, new int[] { 0, 0, 0, 0 });
		p.setPokemonBaseExp(ps.getBaseEXP());
		p.setExpType(ps.getGrowthRate());
		p.setExp(DataService.getBattleMechanics().getExpForLevel(p, level));
		p.setHappiness(ps.getHappiness());
		p.setRareness(ps.getRareness());
		p.setPokemonNumber(ps.getPokemonNumber());
		if(random.nextDouble() < (1 / 8192))
			p.setShiny(true);
		return p;
	}

	/* used for setting a pokemon for transform */
	public void setTempPoke(Pokemon t)
	{
		temp = t;
	}

	/* Used for transform */
	public Pokemon getTempPoke()
	{
		return temp;
	}

	/**
	 * Returns a Pokemon based on a species name, a level, evs and moves
	 * 
	 * @param speciesName
	 * @param level
	 * @return
	 */
	public static Pokemon getGymLeaderPokemon(String species, int level, PokemonNature n, int hp, int att, int def, int spd, int spdef, int spatt, String move0, String move1, String move2,
			String move3)
	{
		Pokemon p;
		Random random = DataService.getBattleMechanics().getRandom();
		/* First obtain species data */
		PokemonSpecies ps = PokemonSpecies.getDefaultData().getPokemonByName(species);
		MoveListEntry[] moves = new MoveListEntry[4];
		moves[0] = MoveList.getDefaultData().getMove(move0);
		moves[1] = MoveList.getDefaultData().getMove(move1);
		moves[2] = MoveList.getDefaultData().getMove(move2);
		moves[3] = MoveList.getDefaultData().getMove(move3);
		/* Get all possible abilities */
		String[] abilities = PokemonSpecies.getDefaultData().getPokemonByName(species).getAbilities();
		/* First select an ability randomly */
		String ab = abilities[random.nextInt(abilities.length)];
		/* Now lets create the pokemon itself */
		p = new Pokemon(DataService.getBattleMechanics(), ps, n, ab, null, Pokemon.generateGender(ps.getPossibleGenders()), level, new int[] { 32, 32, 32, 32, 32, 32 }, new int[] { hp, att, def, spd,
				spatt, spdef }, // EVs
				moves, new int[] { 0, 0, 0, 0 });
		p.setPokemonBaseExp(ps.getBaseEXP());
		p.setExpType(ps.getGrowthRate());
		p.setExp(DataService.getBattleMechanics().getExpForLevel(p, level));
		p.setHappiness(ps.getHappiness());
		p.setRareness(ps.getRareness());
		return p;
	}

	/**
	 * Get the name of a stat.
	 */
	public static String getStatName(int stat)
	{
		switch(stat)
		{
			case S_HP:
				return "HP";
			case S_ATTACK:
				return "attack";
			case S_DEFENCE:
				return "defence";
			case S_SPEED:
				return "speed";
			case S_SPATTACK:
				return "special attack";
			case S_SPDEFENCE:
				return "special defence";
			case S_ACCURACY:
				return "accuracy";
			case S_EVASION:
				return "evasion";
		}
		return "";
	}

	/**
	 * Get the shortened name of a stat.
	 */
	public static String getStatShortName(int stat)
	{
		switch(stat)
		{
			case S_HP:
				return "HP";
			case S_ATTACK:
				return "Atk";
			case S_DEFENCE:
				return "Def";
			case S_SPEED:
				return "Spd";
			case S_SPATTACK:
				return "SAtk";
			case S_SPDEFENCE:
				return "SDef";
			case S_ACCURACY:
				return "Acc";
			case S_EVASION:
				return "Evas";
		}
		return "";
	}

	/**
	 * Load a team from a file and return the ModData used by the team.
	 */
	public static ModData loadTeam(File f, Pokemon[] team)
	{
		ModData modData = null;

		try
		{
			FileInputStream file = new FileInputStream(f);
			ObjectInputStream obj = new ObjectInputStream(file);
			// First thing in file is a UUID identifying the server.
			String uuid = (String) obj.readObject();
			modData = ModData.getModData(uuid);
			if(modData == null)
				modData = ModData.getDefaultData();
			Pokemon[] pokemon = null;
			synchronized(PokemonSpecies.class)
			{
				PokemonSpeciesData data = PokemonSpecies.getDefaultData();
				PokemonSpecies.setDefaultData(modData.getSpeciesData());
				try
				{
					pokemon = (Pokemon[]) obj.readObject();
				}
				finally
				{
					PokemonSpecies.setDefaultData(data);
				}
			}
			if(pokemon != null)
				System.arraycopy(pokemon, 0, team, 0, team.length);
			obj.close();
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
			return null;
		}
		catch(ClassNotFoundException e)
		{
			System.out.println(e.getMessage());
			return null;
		}
		return modData;
	}

	/**
	 * Add a status effect to this pokemon.
	 */
	public StatusEffect addStatus(Pokemon source, StatusEffect eff)
	{
		if(m_fainted)
			return null;

		// Make sure there isn't another copy of this effect applied already.
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				Object o = i.next();
				if(o != null)
				{
					StatusEffect j = (StatusEffect) o;
					if(!j.isRemovable() && (eff.equals(j) || eff.isExclusiveWith(j)))
						return null;
				}
			}
		}

		StatusEffect applied = (StatusEffect) eff.clone();
		applied.activate();
		applied.setInducer(source);
		if(m_field != null && !allowsStatus(applied, source))
			return null;

		if(applied.apply(this))
		{
			m_statuses.add(applied);
			if(m_field != null)
				m_field.informStatusApplied(this, applied);
			informStatusListeners(source, applied, true);

		}
		return applied;
	}

	/**
	 * Add a status effect to this pokemon caused by item.
	 */
	public StatusEffect addStatus(StatusEffect eff)
	{
		if(m_fainted)
			return null;

		// Make sure there isn't another copy of this effect applied already.
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				Object o = i.next();
				if(o != null)
				{
					StatusEffect j = (StatusEffect) o;
					if(!j.isRemovable() && (eff.equals(j) || eff.isExclusiveWith(j)))
						return null;
				}
			}
		}

		StatusEffect applied = (StatusEffect) eff.clone();
		applied.activate();

		if(applied.apply(this))
		{
			m_statuses.add(applied);
			if(m_field != null)
				m_field.informStatusApplied(this, applied);

		}
		return applied;
	}

	/**
	 * Check whether the effects present on this pokemon permit the application of
	 * the given status effect to this pokemon.
	 */
	public boolean allowsStatus(StatusEffect eff, Pokemon source)
	{
		Iterator<StatusEffect> i = m_statuses.iterator();
		while(i.hasNext())
		{
			StatusEffect clause = i.next();
			if(clause == null || !clause.isActive())
				continue;
			if(!clause.allowsStatus(eff, source, this))
				return false;
		}
		return true;
	}

	/**
	 * Attach this pokemon to a battle field.
	 */
	public void attachToField(BattleField field, int party, int position)
	{
		m_field = field;
		m_mech = m_field.getMechanics();
		m_party = party;
		m_id = position;

		if(m_abilityName != null && m_abilityName.length() != 0)
		{
			m_originalAbility = IntrinsicAbility.getInstance(m_abilityName);
			if(m_originalAbility != null)
				m_ability = (IntrinsicAbility) addStatus(this, m_originalAbility);
		}

		// if(m_itemName != null && m_itemName.length() != 0)
		// {
		// IntrinsicAbility item = IntrinsicAbility.getInstance(m_itemName);
		// if(item != null && item instanceof HoldItem)
		// addStatus(this, item);
		// }
	}

	/**
	 * Begin ticking effects.
	 */
	public void beginStatusTicks()
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				effect.beginTick();
			}
		}
	}

	/**
	 * Recalculates this Pokemon's stats. If reset is true, the Pokemon is also
	 * healed fully
	 * 
	 * @param reset
	 */
	public void calculateStats(boolean reset)
	{
		m_stat = new int[6];
		m_multiplier = new StatMultiplier[m_stat.length];
		if(reset)
			removeStatusEffects(true);
		for(int i = 0; i < m_stat.length; ++i)
		{
			m_stat[i] = m_mech.calculateStat(this, i);
			m_multiplier[i] = new StatMultiplier(false);
		}
		if(reset)
			m_hp = m_stat[S_HP];
		if(reset)
			for(int i = 0; i < m_move.length; ++i)
				if(m_move[i] != null)
				{
					m_move[i] = (MoveListEntry) m_move[i].clone();
					PokemonMove move = m_move[i].getMove();
					if(move != null)
						m_maxPp[i] = m_pp[i] = move.getPp() * (5 + m_ppUp[i]) / 5;
				}
	}

	/**
	 * Calculate stats from a given set of IVs and EVs. The data given are assumed
	 * to be valid; no checking is done for illegal values in this function.
	 */
	public void calculateStats(int base[], int[] ivs, int[] evs)
	{
		m_iv = ivs;
		m_ev = evs;
		m_base = base;
		for(int i = 0; i < m_stat.length; ++i)
			m_stat[i] = m_mech.calculateStat(this, i);
	}

	/**
	 * Return whether this pokemon can switch.
	 */
	public boolean canSwitch()
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				if(effect.isActive() && !effect.canSwitch(this))
					return false;
			}
		}
		return true;
	}

	/**
	 * Change the health of this pokemon, doing damage to a substitute if one is
	 * present.
	 */
	public void changeHealth(int hp)
	{
		changeHealth(hp, false);
	}

	/**
	 * Change the health of this pokemon, optionally hitting through a substitute.
	 */
	public void changeHealth(int hp, boolean throughSubstitute)
	{
		if(m_fainted)
			return;
		if(!hasSubstitute() || throughSubstitute || hp > 0)
		{
			if(throughSubstitute && hp < 0 && hasAbility("Magic Guard"))
				return;
			int max = m_stat[S_HP];
			int display = hp;
			int result = m_hp + hp;
			if(hasEffect(PendanticDamageClause.class))
				if(result > max)
					display = max - m_hp;
				else if(result < 0)
					display = -m_hp;
			if(m_field != null)
			{
				m_field.informPokemonHealthChanged(this, display);
				if(result <= 0 && !throughSubstitute)
				{
					boolean live = false;
					if(hasEffect(MoveList.EndureEffect.class))
					{
						m_field.showMessage(getName() + " endured the attack!");
						live = true;
					}
					else if(m_hp == max && hasItem("Focus Sash"))
					{
						m_field.showMessage(getName() + " hung on using its Focus Sash!");
						live = true;
						setItem(null);
					}
					else if(hasItem("Focus Band"))
						if(m_field.getRandom().nextDouble() <= 0.1)
						{
							m_field.showMessage(getName() + " hung on using its Focus Band!");
							live = true;
						}
					if(live)
						hp = -m_hp + 1;
				}
			}
			m_hp += hp;
			if(m_hp <= 0)
				faint();
			else if(m_hp > max)
				m_hp = max;
		}
		else
		{
			m_substitute += hp;
			String name = getName();
			m_field.showMessage("The substitute took damage for " + name + "!");
			if(m_substitute <= 0)
			{
				m_field.showMessage(name + "'s substitute faded!");
				m_substitute = 0;
				removeStatus(MoveList.SubstituteEffect.class);
			}
		}
	}

	/**
	 * Returns 0 if they are the same Pokemon
	 * 
	 * @param p
	 * @return
	 */
	public int compareTo(Pokemon p)
	{
		if(getDateCaught() != null && p.getDateCaught() != null && p.getDateCaught().equalsIgnoreCase(getDateCaught()))
			return 0;
		if(getDatabaseID() != -1 && p.getDatabaseID() != -1 && p.getDatabaseID() == getDatabaseID())
			return 0;
		if(p.getSpeciesName().equalsIgnoreCase(getSpeciesName()) && p.getStat(0) == this.getStat(0) && p.getStat(1) == this.getStat(1))
			return 0;
		return -1;
	}

	/**
	 * Create a substitute to take hits for this pokemon.
	 */
	public boolean createSubstitute()
	{
		if(hasSubstitute())
			return false;
		int quarter = m_stat[S_HP] / 4;
		if(quarter >= m_hp)
			return false;
		changeHealth(-quarter);
		m_substitute = quarter;
		return true;
	}

	/**
	 * Detaches a battlefield from the Pokemon
	 */
	public void detachField()
	{
		m_field = null;
		m_mech = DataService.getBattleMechanics();
	}

	/**
	 * Dispose of this object.
	 */
	public void dispose()
	{
		m_multiplier = null;
		m_accuracy = null;
		m_evasion = null;
		m_statuses = null;
		m_field = null;
		m_nature = null;
		m_move = null;
		m_abilityName = null;
		m_itemName = null;
		m_mech = null;
	}

	/**
	 * Handles the response from the client, whether they allowed evolution or not
	 * 
	 * @param allow
	 *        - If the evolution is allowed
	 * @param p
	 *        - The player that owns the Pokemon
	 */
	public void evolutionResponse(boolean allow, Player p)
	{
		if(m_evolution != null)
		{
			/* Get the index of the Pokemon in the player's party */
			int index = p.getPokemonIndex(this);
			if(allow)
				/* The player is allowing evolution, evolve the Pokemon */
				evolve(PokemonSpecies.getDefaultData().getPokemonByName(m_evolution.getEvolveTo()));
			/* Retrieve the Pokemon data */
			PokemonSpecies pokeData = PokemonSpecies.getDefaultData().getPokemonByName(getSpeciesName());
			setHappiness(m_happiness + 2);
			calculateStats(false);
			/* Now learn any moves that need learning */
			int level = DataService.getBattleMechanics().calculateLevel(this);
			int oldLevel = getLevel();
			String move = "";
			/* Generate a list of moves this Pokemon wants to learn */
			m_movesLearning.clear();
			for(int i = oldLevel + 1; i <= level; i++)
				if(pokeData.getLevelMoves().get(i) != null)
				{
					move = pokeData.getLevelMoves().get(i);
					if(move != null && !move.equalsIgnoreCase("") && !hasMove(move))
						m_movesLearning.add(move);
				}
			setLevel(level);
			/* Update the client with new Pokemon information */
			p.updateClientParty(index);
			/* Inform the client this Pokemon wants to learn new moves */
			for(int i = 0; i < m_movesLearning.size(); i++)
			{
				ServerMessage message = new ServerMessage(ClientPacket.MOVE_LEARN_LVL);
				message.addInt(index);
				message.addString(m_movesLearning.get(i));
				p.getSession().Send(message);
			}
			p.updateClientPokemonStats(index);
			/* Enter this pokemon in the pokedex. */
			p.setPokemonCaught(pokeData.getPokedexNumber());
		}
	}

	/**
	 * This method is called when the pokemon is just about to execute its turn.
	 * 
	 * @param turn
	 *        the turn that is about to be executed
	 */
	public void executeTurn(BattleTurn turn)
	{
		Iterator<StatusEffect> i = m_statuses.iterator();
		while(i.hasNext())
		{
			StatusEffect j = i.next();
			if(j == null || !j.isActive())
				continue;
			j.executeTurn(this, turn);
		}
	}

	/**
	 * Cause this pokemon to faint.
	 */
	public void faint()
	{
		m_hp = 0;
		m_fainted = true;
		if(m_field != null)
		{
			m_field.informPokemonFainted(m_party, getId());
			m_field.checkBattleEnd(m_party);
		}
	}

	/**
	 * Return this pokemon's ability.
	 */
	public IntrinsicAbility getAbility()
	{
		if(m_ability == null && m_abilityName != null && m_abilityName.length() != 0)
			return IntrinsicAbility.getInstance(m_abilityName);
		return m_ability;
	}

	/**
	 * Return the name of this pokemon's ability.
	 */
	public String getAbilityName()
	{
		if(m_ability == null || m_ability.isRemovable())
			return "";
		return m_ability.getName();
	}

	public StatMultiplier getAccuracy()
	{
		return m_accuracy;
	}

	/**
	 * Returns the base EXP of the Pokemon
	 * 
	 * @return
	 */
	public int getPokemonBaseExp()
	{
		return m_baseExp;
	}

	public int getCaughtWithBall()
	{
		return m_caughtWith;
	}

	/**
	 * Returns the contest of the pokemon. NOTE: Use S_BEAUTY, S_CUTE, etc.
	 * 
	 * @param i
	 */
	public int getContestStat(int i)
	{
		return m_contestStat[i];
	}

	/* Sets a pokemon move directly used only for sketch
	 * @author sadhi
	 * @param i index of move
	 * @param m moveListEntry */
	public void setMove(int i, MoveListEntry m)
	{
		m_move[i] = m;
	}

	/**
	 * Returns this pokemon's contest stats in string format NOTE: Only used for
	 * saving MySQL
	 * 
	 * @return
	 */
	public String getContestStatsAsString()
	{
		return m_contestStat[0] + "," + m_contestStat[1] + "," + m_contestStat[2] + "," + m_contestStat[3] + "," + m_contestStat[4];
	}

	/**
	 * Get the (additive) critical hit ability of this Pokemon.
	 */
	public int getCriticalHitFactor()
	{
		return hasAbility("Super Luck") ? 2 : 1;
	}

	/**
	 * Returns the database id
	 * 
	 * @return
	 */
	public int getDatabaseID()
	{
		return m_databaseID;
	}

	/**
	 * Get the date this pokemon was caught
	 */
	public String getDateCaught()
	{
		return m_dateCaught;
	}

	/**
	 * Return the effect of a particular class applied to this pokemon, or null if
	 * there is no such effect.
	 */
	public StatusEffect getEffect(Class<?> type)
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect eff = i.next();
				if(eff == null || !eff.isActive())
					continue;
				if(type.isAssignableFrom(eff.getClass()))
					return eff;
			}
		}
		return null;
	}

	/**
	 * Return the effect applied to this pokemon of a particular lock or null if
	 * there is no such effect applied.
	 */
	public StatusEffect getEffect(int lock)
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect eff = i.next();
				if(eff == null || !eff.isActive())
					continue;
				if(eff.getLock() == lock)
					return eff;
			}
		}
		return null;
	}

	public double getEffectiveness(PokemonType move, PokemonType pokemon, boolean enemy)
	{
		return getEffectiveness(m_statuses, move, pokemon, enemy);
	}

	public int getEv(int i) throws StatException
	{
		if(i < 0 || i > 5)
			throw new StatException();
		return m_ev[i];
	}

	public StatMultiplier getEvasion()
	{
		return m_evasion;
	}

	/**
	 * Get EV total.
	 */
	public int getEvTotal()
	{
		int total = 0;
		for(int i = 0; i < 6; i++)
			total += getEv(i);
		return total;
	}

	/**
	 * Returns the EXP of the Pokemon
	 * 
	 * @return
	 */
	public double getExp()
	{
		return m_exp;
	}

	/**
	 * Returns the amount of EXP required for this pokemon reach a level
	 * based on a pokemon's EXP type
	 * 
	 * @param poke
	 * @param level
	 * @return
	 */
	public double getExpForLevel(int level)
	{
		double exp = 0;
		switch(getExpType())
		{
			case MEDIUM:
				exp = (int) java.lang.Math.pow(level, 3);
				break;
			case ERRATIC:
				double p = 0;
				switch(level % 3)
				{
					case 0:
						p = 0;
						break;
					case 1:
						p = 0.008;
						break;
					case 2:
						p = 0.014;
						break;
				}
				if(level <= 50)
					exp = java.lang.Math.pow(level, 3) * ((100 - (double) level) / 50);
				else if(level <= 68)
					exp = java.lang.Math.pow(level, 3) * ((150 - (double) level) / 50);
				else if(level <= 98)
					exp = java.lang.Math.pow(level, 3) * (1.274 - 1 / 50 * ((double) level / 3) - p);
				else
					exp = java.lang.Math.pow(level, 3) * ((160 - (double) level) / 50);
				break;
			case FAST:
				exp = 4 * java.lang.Math.pow(level, 3) / 5;
				break;
			case FLUCTUATING:
				if(level <= 15)
					exp = java.lang.Math.pow(level, 3) * ((24 + ((double) level + 1) / 3) / 50);
				else if((double) level <= 35)
					exp = java.lang.Math.pow(level, 3) * ((14 + (double) level) / 50);
				else
					exp = java.lang.Math.pow(level, 3) * ((32 + (double) level / 2) / 50);
				break;
			case PARABOLIC:
				exp = 6 * (java.lang.Math.pow(level, 3) / 5) - 15 * java.lang.Math.pow(level, 2) + 100 * (double) level - 140;
				break;
			case SLOW:
				exp = 5 * java.lang.Math.pow(level, 3) / 4;
				;
				break;
		}
		return exp;
	}

	/**
	 * Returns the EXP growth rate of the Pokemon
	 * 
	 * @return
	 */
	public ExpTypes getExpType()
	{
		return m_expType;
	}

	/**
	 * Get the field to which this pokemon is attached.
	 */
	public BattleField getField()
	{
		return m_field;
	}

	/**
	 * Return this Pokemon's gender.
	 */
	public int getGender()
	{
		return m_gender;
	}

	/**
	 * Returns the happiness of the Pokemon
	 * 
	 * @return
	 */
	@Override
	public int getHappiness()
	{
		return m_happiness;
	}

	/**
	 * Get the health of this pokemon.
	 */
	public int getHealth()
	{
		return m_hp;
	}

	/**
	 * Get this pokemon's position on the field to which it is attached.
	 */
	public int getId()
	{
		return m_id;
	}

	/**
	 * Get this pokemon's item.
	 */
	public HoldItem getItem()
	{
		// if ((m_item != null) && m_item.isRemovable()) { return null; }
		if(m_item == null)
			return null;
		return m_item;
	}

	/**
	 * Return the name of this pokemon's holditem.
	 */
	public String getItemName()
	{
		// if ((m_item == null) || m_item.isRemovable()) { return ""; }
		if(m_item == null)
			return "";
		return m_item.getName();
	}

	public int getIv(int i) throws StatException
	{
		if(i < 0 || i > 5)
			throw new StatException();
		return m_iv[i];
	}

	/**
	 * Get the last move used by this pokemon, or null if the pokemon has not used
	 * a move since it has been out.
	 */
	public MoveListEntry getLastMove()
	{
		return m_lastMove;
	}

	public int getLevel()
	{
		return m_level;
	}

	/**
	 * Get a move's max pp.
	 */
	public int getMaxPp(int i)
	{
		if(i < 0 || i >= m_move.length || m_move[i] == null)
			return -1;
		return m_maxPp[i];
	}

	public BattleMechanics getMech()
	{
		return m_mech;
	}

	/* is this pokemon transformed? */
	public Boolean isTransformed()
	{
		return transformed;
	}

	/* sets whether this pokemon is transformed */
	public void setTransformed(Boolean t)
	{
		transformed = t;
	}

	/**
	 * Get one of this pokemon's moves.
	 */
	public MoveListEntry getMove(int i)
	{
		if(i == -1)
			return BattleField.getStruggle();
		if(i < -1 || i >= m_move.length || m_move[i] == null)
			return null;
		return m_move[i];
	}

	/**
	 * Get the name of this pokemon's moves.
	 */
	public String getMoveName(int i)
	{
		if(!(i < m_move.length) || m_move[i] == null)
			return null;
		return m_move[i].getName();
	}

	public MoveListEntry[] getMoves()
	{
		return m_move;
	}

	/**
	 * Returns an arraylist of moves waiting to be learned
	 * 
	 * @return
	 */
	public ArrayList<String> getMovesLearning()
	{
		return m_movesLearning;
	}

	/**
	 * Get a stat multiplier, including the ones for accuracy and evasion.
	 */
	public StatMultiplier getMultiplier(int i) throws StatException
	{
		if(m_multiplier == null)
			m_multiplier = new StatMultiplier[m_stat.length];
		if(i < m_multiplier.length)
		{
			if(m_multiplier[i] == null)
				m_multiplier[i] = new StatMultiplier(false);
			if(i < 0)
				throw new StatException();
			if(i < 6)
				return m_multiplier[i];
			if(i == S_ACCURACY)
				return m_accuracy;
			if(i == S_EVASION)
				return m_evasion;
		}
		throw new StatException();
	}

	/**
	 * Get the display name of this pokemon (i.e. its nickname).
	 */
	@Override
	public String getName()
	{
		return super.getName();
	}

	public PokemonNature getNature()
	{
		return m_nature;
	}

	/**
	 * Get a list of statuses that are not special, weather, abilities, or items.
	 * 
	 * @param lock
	 *        status lock to allow
	 */
	public List<StatusEffect> getNormalStatuses(int lock)
	{
		List<StatusEffect> ret = new ArrayList<StatusEffect>();
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				if(!effect.isActive())
					continue;
				// Note: HoldItem is a subclass of IntrinsicAbility.
				if(!(effect instanceof IntrinsicAbility))
				{
					int effLock = effect.getLock();
					if(effLock == 0 || effLock == lock)
						ret.add(effect);
				}
			}
		}
		return ret;
	}

	/**
	 * Get the Pokemon that this Pokemon is fighting in a battle.
	 */
	public Pokemon getOpponent()
	{
		if(m_field == null)
			return null;
		Pokemon[] active = m_field.getActivePokemon();
		return active[m_party == 0 ? 1 : 0];
	}

	/**
	 * Return the original ability of this pokemon.
	 */
	public IntrinsicAbility getOriginalAbility()
	{
		return m_originalAbility;
	}

	/**
	 * Returns the original trainer's name
	 * 
	 * @return
	 */
	public String getOriginalTrainer()
	{
		return m_originalTrainer;
	}

	/**
	 * Get this pokemon's party. This will be in the range [0, <b>parties</b> - 1]
	 * where <b>parties</b> is the number of parties on the battle field (probably
	 * two).
	 */
	public int getParty()
	{
		return m_party;
	}

	/**
	 * Get a move's pp.
	 */
	public int getPp(int i)
	{
		if(i < 0 || i >= m_move.length || m_move[i] == null)
			return -1;
		return m_pp[i];
	}

	/**
	 * Get the number of PP Ups that have been applied to the given move slot.
	 */
	public int getPpUpCount(int i)
	{
		if(i < 0 || i >= m_ppUp.length)
			return -1;
		return m_ppUp[i];
	}

	public int getRawStat(int i)
	{
		if(i < 0 || i > 5)
			throw new StatException();
		return m_stat[i];
	}

	/**
	 * Get the name of this species.
	 */
	public String getSpeciesName()
	{
		return super.getName();
	}

	public int getStat(int i)
	{
		if(m_multiplier == null)
			m_multiplier = new StatMultiplier[m_stat.length];
		if(m_multiplier[i] == null)
			m_multiplier[i] = new StatMultiplier(false);
		if(i < 0 || i > 5)
			throw new StatException();
		// Consider stat modifications.
		return getStat(i, m_multiplier[i].getMultiplier());
	}

	public int getStat(int i, double multiplier)
	{
		if(i < 0 || i > 5)
			throw new StatException();
		if(m_stat == null)
			calculateStats(false);
		return (int) (m_stat[i] * multiplier);
	}

	/**
	 * Get all status effects of a certain tier.
	 */
	public List<StatusEffect> getStatusesByTier(int tier)
	{
		List<StatusEffect> ret = new ArrayList<StatusEffect>();
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				if(effect.isActive() && effect.getTier() == tier)
					ret.add(effect);
			}
		}
		return ret;
	}

	/**
	 * Get the health of the substitute.
	 */
	public int getSubstitute()
	{
		return m_substitute;
	}

	/**
	 * Get this pokemon's teammates, including this pokemon.
	 */
	public Pokemon[] getTeammates()
	{
		if(m_field == null)
			return null;
		return m_field.getParty(m_party);
	}

	/**
	 * Get the name of this pokemon's trainer.
	 */
	public String getTrainerName()
	{
		if(m_field == null)
			return null;
		return m_field.getTrainerName(m_party);
	}

	/**
	 * Return the effect that vetoes the use of a particular one of this pokemon's
	 * moves.
	 */
	public StatusEffect getVetoingEffect(int idx) throws MoveQueueException
	{
		if(idx < 0 || idx >= m_move.length)
			throw new MoveQueueException("No such move.");
		MoveListEntry entry = m_move[idx];
		if(entry == null)
			throw new MoveQueueException("No such move.");
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect j = i.next();
				if(j == null || !j.isActive())
					continue;
				if(j.vetoesMove(this, entry))
					return j;
			}
		}
		return null;
	}

	/**
	 * Return whether this pokemon has a particular ability.
	 */
	public boolean hasAbility(String name)
	{
		if(m_ability == null)
			return false;
		return m_ability.isActive() && m_ability.getName().equals(name);
	}

	/**
	 * Return whether this Pokemon has a particular class of effect.
	 */
	public boolean hasEffect(Class<?> type)
	{
		return getEffect(type) != null;
	}

	/**
	 * Return whether this Pokemon has a particular class of effect.
	 */
	public boolean hasEffect(int lock)
	{
		return getEffect(lock) != null;
	}

	/**
	 * Return whether this pokemon has a particular effect.
	 */
	public boolean hasEffect(StatusEffect eff)
	{
		if(eff == null)
			return false;
		Iterator<StatusEffect> i = m_statuses.iterator();
		while(i.hasNext())
		{
			StatusEffect j = i.next();
			if(j == null || !j.isActive())
				continue;
			if(eff.equals(j))
				return true;
		}
		return false;
	}

	/**
	 * Return whether this pokemon has a particular item.
	 */
	public boolean hasItem(String name)
	{
		if(m_item == null)
			return false;
		return m_item.isActive() && m_item.getName().equals(name);
	}

	/**
	 * Returns true if a pokemon knows the move
	 * 
	 * @param move
	 * @return
	 */
	public boolean hasMove(String move)
	{
		for(int i = 0; i < m_move.length; i++)
			if(m_move[i] != null && m_move[i].getName() != null && m_move[i].getName().equalsIgnoreCase(move))
				return true;
		return false;
	}

	/**
	 * Return whether this pokemon has a substitute.
	 */
	public boolean hasSubstitute()
	{
		return m_substitute != 0;
	}

	/**
	 * Returns true if this Pokemon is weak against Pokemon b
	 * 
	 * @param b
	 * @return
	 */
	public boolean hasTypeWeakness(Pokemon b)
	{
		switch(getTypes()[0].getType())
		{
			case 0:
				/* NORMAL - Weak against Fighting */
				if(b.getTypes()[0].getType() == 6)
					return true;
				break;
			case 1:
				/* FIRE - Weak against Ground, Rock, Water */
				if(b.getTypes()[0].getType() == 8 || b.getTypes()[0].getType() == 12 || b.getTypes()[0].getType() == 2)
					return true;
				break;
			case 2:
				/* WATER - Weak against Electric, Grass */
				if(b.getTypes()[0].getType() == 3 || b.getTypes()[0].getType() == 4)
					return true;
				break;
			case 3:
				/* ELECTRIC - Weak against Ground */
				if(b.getTypes()[0].getType() == 8)
					return true;
				break;
			case 4:
				/* GRASS - Weak against Bug, Fire, Flying, Ice, Poison */
				if(b.getTypes()[0].getType() == 11 || b.getTypes()[0].getType() == 1 || b.getTypes()[0].getType() == 9 || b.getTypes()[0].getType() == 5 || b.getTypes()[0].getType() == 7)
					return true;
				break;
			case 5:
				/* ICE - Weak against Fighting, Fire, Rock, Steel */
				if(b.getTypes()[0].getType() == 6 || b.getTypes()[0].getType() == 1 || b.getTypes()[0].getType() == 12 || b.getTypes()[0].getType() == 16)
					return true;
				break;
			case 6:
				/* FIGHTING - Weak against Flying, Psychic */
				if(b.getTypes()[0].getType() == 9 || b.getTypes()[0].getType() == 10)
					return true;
				break;
			case 7:
				/* POISON - Weak against Ground, Psychic */
				if(b.getTypes()[0].getType() == 8 || b.getTypes()[0].getType() == 10)
					return true;
				break;
			case 8:
				/* GROUND - Weak against Ice, Grass, Water */
				if(b.getTypes()[0].getType() == 5 || b.getTypes()[0].getType() == 4 || b.getTypes()[0].getType() == 2)
					return true;
				break;
			case 9:
				/* FLYING - Weak against Electric, Ice, Rock */
				if(b.getTypes()[0].getType() == 3 || b.getTypes()[0].getType() == 5 || b.getTypes()[0].getType() == 12)
					return true;
				break;
			case 10:
				/* PSYCHIC - Weak against Bug, Dark, Ghost */
				if(b.getTypes()[0].getType() == 11 || b.getTypes()[0].getType() == 15 || b.getTypes()[0].getType() == 13)
					return true;
				break;
			case 11:
				/* BUG - Weak against Flying, Fire, Rock */
				if(b.getTypes()[0].getType() == 9 || b.getTypes()[0].getType() == 1 || b.getTypes()[0].getType() == 12)
					return true;
				break;
			case 12:
				/* ROCK - Weak against Fighting, Grass, Ground, Steel, Water */
				if(b.getTypes()[0].getType() == 6 || b.getTypes()[0].getType() == 4 || b.getTypes()[0].getType() == 8 || b.getTypes()[0].getType() == 16 || b.getTypes()[0].getType() == 2)
					return true;
				break;
			case 13:
				/* GHOST - Weak against Dark, Ghost */
				if(b.getTypes()[0].getType() == 13 || b.getTypes()[0].getType() == 15)
					return true;
				break;
			case 14:
				/* DRAGON - Weak against Dragon, Ice */
				if(b.getTypes()[0].getType() == 14 || b.getTypes()[0].getType() == 5)
					return true;
				break;
			case 15:
				/* DARK - Weak against Bug, Fighting */
				if(b.getTypes()[0].getType() == 6 || b.getTypes()[0].getType() == 11)
					return true;
				break;
			case 16:
				/* STEEL - Weak against Fire, Fighting, Ground */
				if(b.getTypes()[0].getType() == 1 || b.getTypes()[0].getType() == 6 || b.getTypes()[0].getType() == 8)
					return true;
				break;
		}
		return false;
	}

	/**
	 * Return whether this pokemon is active (able to choose moves and switch).
	 */
	public boolean isActive()
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect eff = i.next();
				if(eff.isActive() && eff.deactivates(this))
					return false;
			}
		}
		return true;
	}

	public boolean isCaughtWith(int ball)
	{
		if(m_caughtWith == ball)
			return true;
		return false;
	}

	/**
	 * Return whether this Pokemon is immune to critical hits.
	 */
	public boolean isCriticalImmune()
	{
		return hasAbility("Battle Armor") || hasAbility("Shell Armor");
	}

	/**
	 * Determine whether this pokemon has fainted.
	 */
	public boolean isFainted()
	{
		return m_fainted;
	}

	/**
	 * Return whether it is the pokemon's first turn out.
	 */
	public boolean isFirstTurn()
	{
		return m_firstTurn;
	}

	/**
	 * Is this pokemon immobilised?
	 * 
	 * @param exception
	 *        status not to check for
	 */
	public boolean isImmobilised(Class<?> exception)
	{
		synchronized(m_statuses)
		{
			Collections.sort(m_statuses, new Comparator<Object>()
			{
				public int compare(Object o1, Object o2)
				{
					StatusEffect e1 = (StatusEffect) o1;
					StatusEffect e2 = (StatusEffect) o2;
					return e1.getTier() - e2.getTier();
				}
			});
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect eff = i.next();
				if(eff.isActive() && eff.immobilises(this))
					if(exception == null || !exception.isAssignableFrom(eff.getClass()))
					{
						m_lastMove = null;
						m_firstTurn = false;
						return true;
					}
			}
		}
		return false;
	}

	/**
	 * Return whether this pokemon is shiny.
	 */
	public boolean isShiny()
	{
		return m_shiny;
	}

	/**
	 * Return whether a pokemon is a particular type.
	 */
	public boolean isType(PokemonType type)
	{
		for(int i = 0; i < m_type.length; ++i)
			if(m_type[i].equals(type))
				return true;
		return false;
	}

	/**
	 * Returns true if this Pokemon is waiting to evolve
	 * 
	 * @return
	 */
	public boolean isWaitToEvolve()
	{
		return m_evolution != null;
	}

	/**
	 * Learn a new move.
	 * 
	 * @param idx
	 * @param moveName
	 */
	public void learnMove(int idx, String moveName)
	{
		if(idx >= 0 && idx <= 3)
			if(MoveList.getDefaultData().containsMove(moveName))
			{
				m_move[idx] = MoveList.getDefaultData().getMove(moveName);
				m_maxPp[idx] = m_move[idx].getMove().getPp();
				setPp(idx, m_move[idx].getMove().getPp());
			}
	}

	/**
	 * Return whether this pokemon must struggle if it wants to use a move.
	 */
	public boolean mustStruggle()
	{
		for(int i = 0; i < m_move.length; ++i)
		{
			try
			{
				if(getVetoingEffect(i) != null)
					continue;
			}
			catch(MoveQueueException e)
			{
				continue;
			}
			if(getPp(i) > 0)
				return false;
		}
		return true;
	}

	/**
	 * Reinitialises the Pokemon
	 * 
	 * @param b
	 */
	public void reinitialise()
	{
		boolean hasNeg = false;
		for(int i = 0; i < 6; i++)
			if(getEv(i) < 0)
				hasNeg = true;
		if(hasNeg || getEvTotal() > 510)
			for(int i = 0; i < 6; i++)
				setEv(i, 0);
		m_accuracy = new StatMultiplier(true);
		m_evasion = new StatMultiplier(true);
		m_statuses = new ArrayList<StatusEffect>();
		m_movesLearning = new ArrayList<String>();
		/* m_pp = new int[4]; m_maxPp = new int[4]; m_ppUp = new int[4]; */
		/* Check validity of moves
		 * this is turned off by sadhi because this prevents anything from standard move to be learned
		 * and he didn't want to write something else
		 * and with this gone you could give out moves the pokemon doesn't learn as prizes */
		/* if(m_move[0] != null && !PokemonSpecies.getDefaultData().
		 * canLearn(PokemonSpecies.getDefaultData().
		 * getPokemonByName(this.getSpeciesName()), getMoveName(0))) {
		 * m_move[0] = null;
		 * }
		 * if(m_move[1] != null && !PokemonSpecies.getDefaultData().
		 * canLearn(PokemonSpecies.getDefaultData().
		 * getPokemonByName(this.getSpeciesName()), getMoveName(1))) {
		 * m_move[1] = null;
		 * }
		 * if(m_move[2] != null && !PokemonSpecies.getDefaultData().
		 * canLearn(PokemonSpecies.getDefaultData().
		 * getPokemonByName(this.getSpeciesName()), getMoveName(2))) {
		 * m_move[2] = null;
		 * }
		 * if(m_move[3] != null && !PokemonSpecies.getDefaultData().
		 * canLearn(PokemonSpecies.getDefaultData().
		 * getPokemonByName(this.getSpeciesName()), getMoveName(3))) {
		 * m_move[3] = null;
		 * } */
	}

	/**
	 * Remove statuses by class type.
	 */
	public void removeStatus(Class<?> type)
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				if(!effect.isRemovable() && type.isAssignableFrom(effect.getClass()))
					unapplyEffect(effect);
			}
		}
	}

	/**
	 * Remove a class of statuses from this pokemon.
	 */
	public void removeStatus(int lock)
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				if(effect.getLock() == lock && !effect.isRemovable())
					unapplyEffect(effect);
			}
		}
	}

	/**
	 * Remove a status effect from this pokemon.
	 */
	public void removeStatus(StatusEffect eff)
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				if(effect == eff)
				{
					unapplyEffect(eff);
					return;
				}
			}
		}
	}

	/**
	 * Removes temporary or all status effects
	 * 
	 * @param all
	 */
	public void removeStatusEffects(boolean all)
	{
		if(all)
		{
			removeStatus(AwesomeEffect.class);
			removeStatus(BurnEffect.class);
			removeStatus(ChargeEffect.class);
			removeStatus(ConfuseEffect.class);
			removeStatus(FlinchEffect.class);
			removeStatus(FreezeEffect.class);
			removeStatus(MultipleStatChangeEffect.class);
			removeStatus(ParalysisEffect.class);
			removeStatus(PercentEffect.class);
			removeStatus(PoisonEffect.class);
			removeStatus(ToxicEffect.class);
			removeStatus(StatusEffect.class);
			removeStatus(StatChangeEffect.class);
			removeStatus(AttractEffect.class);
			removeStatus(MoveList.LeechSeedEffect.class);
		}
		else
		{
			removeStatus(AwesomeEffect.class);
			removeStatus(ChargeEffect.class);
			removeStatus(ConfuseEffect.class);
			removeStatus(FlinchEffect.class);
			removeStatus(MultipleStatChangeEffect.class);
			removeStatus(PercentEffect.class);
			removeStatus(ToxicEffect.class);
			removeStatus(ConfuseEffect.class);
			removeStatus(StatusEffect.class);
			removeStatus(StatChangeEffect.class);
			removeStatus(AttractEffect.class);
			removeStatus(MoveList.LeechSeedEffect.class);
		}
	}

	/**
	 * Set this pokemon's ability. If ignoreTransferability is true then the
	 * isTransferrable() method of the ability is ignored. Otherwise it is
	 * respected.
	 */
	public void setAbility(IntrinsicAbility abl, boolean ignoreTransferability)
	{
		removeStatus(m_ability);
		if(abl != null)
		{
			m_abilityName = abl.getName();
			if(ignoreTransferability || abl.isEffectTransferrable())
				m_ability = (IntrinsicAbility) addStatus(this, abl);
			else
				m_ability = null;
		}
		else
			m_abilityName = null;
	}

	/**
	 * Sets the base EXP of the Pokemon
	 * 
	 * @param baseEXP
	 */
	public void setPokemonBaseExp(int baseEXP)
	{
		m_baseExp = baseEXP;
	}

	public void setCaughtWith(int ball)
	{
		m_caughtWith = ball;
	}

	/**
	 * Sets the contest stat of the pokemon. NOTE: Use S_BEAUTY, S_CUTE, etc. for
	 * i
	 * 
	 * @param i
	 * @param amount
	 */
	public void setContestStat(int i, int amount)
	{
		m_contestStat[i] = amount <= 255 ? amount : 255;
	}

	/**
	 * Sets the database id
	 * 
	 * @param id
	 */
	public void setDatabaseID(int id)
	{
		m_databaseID = id;
	}

	/**
	 * Set the date this pokemon was caught
	 * 
	 * @param date
	 */
	public void setDateCaught(String date)
	{
		m_dateCaught = date;
	}

	/**
	 * Sets the EV of the pokemon
	 * 
	 * @param i
	 * @param j
	 */
	public void setEv(int i, int j)
	{

		if(j < 256)
			m_ev[i] = j;
		else
			m_ev[i] = 255;
	}

	/**
	 * Sets if this Pokemon is waiting to evolve and the evolution it is waiting
	 * to go to
	 * 
	 * @param e
	 */
	public void setEvolution(PokemonEvolution e)
	{
		m_evolution = e;
	}

	/**
	 * Sets the exp of the Pokemon
	 * 
	 * @param exp
	 */
	public void setExp(double exp)
	{
		if(exp > 100000000)
		{
			m_exp = 100000000;
			return;
		}
		m_exp = exp;
	}

	/**
	 * Sets the EXP growth rate of the Pokemon
	 * 
	 * @param growthRate
	 */
	public void setExpType(ExpTypes growthRate)
	{
		m_expType = growthRate;
	}

	/**
	 * Sets the happiness of the Pokemon
	 * 
	 * @param happiness
	 */
	@Override
	public void setHappiness(int happiness)
	{
		int original = m_happiness;
		int diff = happiness - original;
		if(m_caughtWith == 48)
			diff = diff * 2;
		m_happiness = happiness + diff <= 255 ? happiness + diff : 255;
	}

	/**
	 * Sets the Pokemon's HP
	 * 
	 * @param h
	 */
	public void setHealth(int h)
	{
		m_hp = h;
	}

	/**
	 * Sets if this pokemon is fainted
	 * 
	 * @param b
	 */
	public void setIsFainted(boolean b)
	{
		m_fainted = b;
	}

	/**
	 * Set this pokemon's item.
	 */
	public void setItem(HoldItem item)
	{
		removeStatus(m_item);
		if(item != null)
		{
			m_item = item;// (HoldItem)
			addStatus(this, item);
		}
		m_itemName = getItemName();
	}

	public void setLevel(int level)
	{
		m_level = level;
	}

	/**
	 * Sets the max pp of a move
	 * 
	 * @param index
	 * @param value
	 */
	public void setMaxPP(int i, int value)
	{
		if(i < 0 || i >= m_maxPp.length)
			return;
		m_maxPp[i] = value;
	}

	/**
	 * Set this pokemon's name.
	 */
	@Override
	public void setName(String name)
	{
		m_nickname = name;
	}

	public void setOriginalNo(int m_no)
	{
		m_originalNo = m_no;
	}

	/**
	 * Sets the name of the original trainer
	 * 
	 * @param name
	 */
	public void setOriginalTrainer(String name)
	{
		m_originalTrainer = name;
	}

	/**
	 * Set a move's pp.
	 */
	public void setPp(int i, int value)
	{
		if(i < 0 || i >= m_pp.length)
			return;
		m_pp[i] = value;
	}

	/**
	 * Sets a pp up
	 * 
	 * @param i
	 * @param value
	 */
	public void setPpUp(int i, int value)
	{
		if(i < 0 || i >= m_ppUp.length)
			return;
		m_ppUp[i] = value;
	}

	public void setRawStat(int i, int newStat)
	{
		if(i < 0 || i > 5)
			throw new StatException();
		m_stat[i] = newStat;
	}

	/**
	 * Set whether this pokemon is shiny.
	 */
	public void setShiny(boolean shiny)
	{
		m_shiny = shiny;
	}

	/**
	 * Set the health of the substitute.
	 */
	public void setSubstitute(int hp)
	{
		m_substitute = hp;
	}

	/**
	 * Switch in this pokemon.
	 */
	public void switchIn()
	{
		// No iterator - it will freak out if switchIn() adds new statuses.
		m_lastMove = null;
		m_firstTurn = true;
		// Inform PokemonMoves that their potential user is switching in.
		for(int i = 0; i < m_move.length; ++i)
		{
			MoveListEntry entry = m_move[i];
			if(entry != null)
			{
				PokemonMove move = entry.getMove();
				if(move != null)
					move.switchIn(this);
			}
		}
		// Inform status effects.
		int size = m_statuses.size();
		for(int i = 0; i < size; ++i)
			m_statuses.get(i).switchIn(this);
	}

	/**
	 * return a list of all status effects that effect this pokemon
	 */
	public ArrayList<StatusEffect> getStatusEffects()
	{
		return m_statuses;
	}

	/**
	 * Switch out this pokemon.
	 */
	public void switchOut()
	{
		ArrayList<StatusEffect> list = new ArrayList<StatusEffect>(m_statuses);
		Iterator<StatusEffect> i = list.iterator();
		while(i.hasNext())
		{
			StatusEffect effect = i.next();
			if(effect.isActive() && effect.switchOut(this))
			{
				unapplyEffect(effect, false);
				i.remove();
			}
		}
		m_statuses = list;
		setAbility(m_originalAbility, true);
		synchroniseStatuses();
	}

	/**
	 * Remove status effects that have ended.
	 */
	public void synchroniseStatuses()
	{
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect effect = i.next();
				if(effect.isRemovable())
					i.remove();
			}
		}
	}

	/**
	 * Use one of this pokemon's intrinsic moves.
	 */
	public int useMove(int i, Pokemon target)
	{
		if(i == -1)
		{
			MoveListEntry move = BattleField.getStruggle();
			int ret = useMove(move, target);
			m_lastMove = move;
			m_firstTurn = false;
			return ret;
		}
		if(i >= m_move.length || m_move[i] == null)
			return 0;
		if(m_pp[i] == 0)
			return 0;

		MoveListEntry entry = m_move[i];
		PokemonMove move = m_move[i].getMove();

		final int cost = target.hasAbility("Pressure") && move.isAttack() ? 2 : 1;
		m_pp[i] -= cost;
		if(m_pp[i] < 0)
			m_pp[i] = 0;

		int ret = useMove(entry, target);
		m_lastMove = entry;
		m_firstTurn = false;
		return ret;
	}

	/**
	 * Use a a move from the move list (so its name is known and displayed).
	 */
	public int useMove(MoveListEntry move, Pokemon target)
	{
		PokemonMove pmove = move.getMove();
		move = getTransformedMove(move, false);
		if(move != null)
		{
			if(target != this)
				if((move = target.getTransformedMove(move, true)) == null)
					return 0;
			m_field.informUseMove(this, move.getName());
			int hp = target.getHealth();
			pmove = move.getMove();
			useMove(pmove, target);
			int damage = hp - target.getHealth();
			if(damage > 0)
				target.informDamaged(this, move, damage);
			return damage;
		}
		return 0;
	}

	/**
	 * Check for accuracy and then use an arbitrary move.
	 */
	public int useMove(PokemonMove move, Pokemon target)
	{
		if(!move.attemptHit(m_mech, this, target))
			return 0;
		return move.use(m_mech, this, target);
	}

	/**
	 * Validate this pokemon.
	 */
	public void validate(ModData data) throws ValidationException
	{
		m_mech.validateHiddenStats(this);
		PokemonSpeciesData speciesData = data.getSpeciesData();
		Set<String> set = new HashSet<String>();
		int moveCount = 0;
		for(int i = 0; i < m_move.length; ++i)
		{
			MoveListEntry move = m_move[i];
			if(move != null)
			{
				++moveCount;
				String name = move.getName();
				if(set.contains(name))
					throw new ValidationException("This pokemon learns two of the same move.");
				set.add(name);
				if(!canLearn(speciesData, name))
					throw new ValidationException("This pokemon cannot learn " + name + ".");
				if(m_ppUp[i] > 3 || m_ppUp[i] < 0)
					throw new ValidationException("Each move must have between zero and " + "three PP ups applied to it.");
			}
		}
		if(moveCount == 0)
			// Pokemon must have at least one move.
			throw new ValidationException("This pokemon learns no moves.");
		else if(moveCount > 4)
			throw new ValidationException("This pokemon learns move than four moves.");

		int genders = getPossibleGenders();
		if((genders & m_gender) == 0 && (genders != 0 || m_gender != 0))
			throw new ValidationException("This pokemon has an invalid gender.");

		if(!canUseAbility(speciesData, m_abilityName))
		{
			String[] possibilities = getPossibleAbilities(speciesData);
			if(possibilities != null)
				m_abilityName = possibilities[0];
		}

		if(m_itemName != null && !data.getHoldItemData().canUseItem(getSpeciesName(), m_itemName))
			throw new ValidationException("This pokemon's item is invalid.");
	}

	/**
	 * Transform a move based on the status effects applied to the pokemon.
	 * 
	 * @param enemy
	 *        whether this Pokemon is an enemy
	 */
	protected MoveListEntry getTransformedMove(MoveListEntry move, boolean enemy)
	{
		// For now, do this in no particular order.
		synchronized(m_statuses)
		{
			Iterator<StatusEffect> i = m_statuses.iterator();
			while(i.hasNext())
			{
				StatusEffect eff = i.next();
				if(eff.isActive() && eff.isMoveTransformer(enemy))
				{
					move = eff.getMove(this, (MoveListEntry) move.clone(), enemy);
					if(move == null)
						return null;
				}
			}
		}
		return move;
	}

	/**
	 * Evolves this pokemon into the new species
	 * 
	 * @param species
	 */
	private void evolve(PokemonSpecies species)
	{
		m_species = species.getSpeciesNumber();
		m_name = species.getName();
		m_base = species.getBaseStats();
		m_genders = species.m_genders;
		m_type = species.getTypes();
		try
		{
			String[] abilities = PokemonSpecies.getDefaultData().getPossibleAbilities(getSpeciesName());
			m_ability = IntrinsicAbility.getInstance(abilities[DataService.getBattleMechanics().getRandom().nextInt(abilities.length)]);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		calculateStats(m_base, m_iv, m_ev);
		m_evolution = null;
	}

	/**
	 * Inform that this pokemon was damaged.
	 */
	private void informDamaged(Pokemon source, MoveListEntry entry, int damage)
	{
		int size = m_statuses.size();
		for(int i = 0; i < size; ++i)
		{
			StatusEffect eff = m_statuses.get(i);
			if(eff.isActive() && eff.isListener())
				eff.informDamaged(source, this, entry, damage);
		}
	}

	/**
	 * Inform listeners that a status effect was applied this pokemon.
	 */
	private void informStatusListeners(Pokemon source, StatusEffect eff, boolean applied)
	{
		synchronized(m_statuses)
		{
			int size = m_statuses.size();
			for(int i = 0; i < size; ++i)
			{
				StatusEffect j = m_statuses.get(i);
				if(j.isActive() && j instanceof StatusListener)
				{
					StatusListener k = (StatusListener) j;
					if(applied)
						k.informStatusApplied(source, this, eff);
					else
						k.informStatusRemoved(this, eff);
				}
			}
		}
	}

	/**
	 * Calculate this pokemon's stats.
	 */
	private void initialise() throws StatException
	{
		// Recreate transient members.
		m_movesLearning = new ArrayList<String>();
		m_accuracy = new StatMultiplier(true);
		m_evasion = new StatMultiplier(true);
		m_statuses = new ArrayList<StatusEffect>();
		m_pp = new int[4];
		m_maxPp = new int[m_pp.length];
		m_fainted = false;
		m_field = null;
		m_substitute = 0;
		m_ability = IntrinsicAbility.getInstance(m_abilityName);

		calculateStats(true);

		for(int i = 0; i < m_move.length; ++i)
			if(m_move[i] != null)
			{
				m_move[i] = (MoveListEntry) m_move[i].clone();
				PokemonMove move = m_move[i].getMove();
				if(move != null)
					m_maxPp[i] = m_pp[i] = move.getPp() * (5 + m_ppUp[i]) / 5;
			}
	}

	/**
	 * Unserialises a Pokemon.
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		if(m_ppUp == null)
		{
			m_ppUp = new int[m_move.length];
			Arrays.fill(m_ppUp, 3);
		}
		if(m_nickname == null)
			m_nickname = getSpeciesName();
		else
		{
			m_nickname = m_nickname.trim();
			if(m_nickname.length() == 0)
				m_nickname = getSpeciesName();
		}
		try
		{
			initialise();
		}
		catch(StatException e)
		{
			throw new IOException();
		}
	}

	/**
	 * Invoke unapply on a status effect and disable it as well.
	 */
	private void unapplyEffect(StatusEffect eff)
	{
		unapplyEffect(eff, true);
	}

	/**
	 * Invoke unapply on a status effect, optionally disabling it.
	 */
	private void unapplyEffect(StatusEffect eff, boolean disable)
	{
		if(eff.isActive())
			eff.unapply(this);
		if(disable)
			eff.disable();
		if(m_field != null)
			m_field.informStatusRemoved(this, eff);
		informStatusListeners(null, eff, false);
	}

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();
	}
}
