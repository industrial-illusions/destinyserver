/* BattleField.java
 * Created on December 19, 2006, 4:05 PM
 * This file is a part of Shoddy Battle.
 * Copyright (C) 2006 Colin Fitzpatrick
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, visit the Free Software Foundation, Inc.
 * online at http://gnu.org. */

package org.destiny.server.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.mechanics.BattleMechanics;
import org.destiny.server.battle.mechanics.ModData;
import org.destiny.server.battle.mechanics.MoveQueueException;
import org.destiny.server.battle.mechanics.PokemonType;
import org.destiny.server.battle.mechanics.ValidationException;
import org.destiny.server.battle.mechanics.clauses.Clause;
import org.destiny.server.battle.mechanics.moves.MoveList;
import org.destiny.server.battle.mechanics.moves.MoveListEntry;
import org.destiny.server.battle.mechanics.moves.PokemonMove;
import org.destiny.server.battle.mechanics.moves.MoveList.SpeedSwapEffect;
import org.destiny.server.battle.mechanics.statuses.StatusEffect;
import org.destiny.server.battle.mechanics.statuses.field.FieldEffect;
import org.destiny.server.battle.mechanics.statuses.field.RainEffect;

/**
 * @author Colin
 */
public abstract class BattleField
{

	/**
	 * A wrapper for a pokemon and a turn. Can be compared on the basis of
	 * move priority, or, failing that, speed.
	 */
	@SuppressWarnings("unchecked")
	protected static class PokemonWrapper implements Comparable
	{
		private int m_idx;
		private Pokemon m_poke;
		private BattleTurn m_turn;

		/**
		 * Initialise a PokemonWrapper with a Pokemon and a BattleTurn.
		 */
		private PokemonWrapper(Pokemon p, BattleTurn turn, int idx)
		{
			m_poke = p;
			m_turn = turn;
			m_idx = idx;
		}

		/**
		 * Compare based on speed.
		 */
		public static int compareSpeed(Pokemon p1, Pokemon p2)
		{
			if(p1 == null)
				return 1;
			if(p2 == null)
				return -1;
			int s1, s2;
			if(p1.getAbility().getName().equalsIgnoreCase("Swift swim") && p1.getField().getEffectByType(RainEffect.class) != null)
			{
				s1 = p1.getStat(Pokemon.S_SPEED) * 2;
			}
			else
			{
				s1 = p1.getStat(Pokemon.S_SPEED);
			}
			if(p2.getAbility().getName().equalsIgnoreCase("Swift swim") && p2.getField().getEffectByType(RainEffect.class) != null)
			{
				s2 = p2.getStat(Pokemon.S_SPEED) * 2;
			}
			else
			{
				s2 = p2.getStat(Pokemon.S_SPEED);
			}
			int comp = 0;
			if(s1 > s2)
				comp = -1;
			else if(s2 > s1)
				comp = 1;

			// Note: shoddy.
			if(comp != 0)
			{
				if(p1.getField().getEffectByType(SpeedSwapEffect.class) != null)
					return -comp;
				return comp;
			}

			// Since the speeds are equal, pick a random pokemon.
			return p1.getField().getRandom().nextBoolean() ? -1 : 1;
		}

		/**
		 * Sort pokemon and moves in descending order. Reorders the elements of
		 * the arrays passed in and also returns an array of the indices of the
		 * pokemon as rearranged.
		 */
		public static int[] sortMoves(Pokemon[] active, BattleTurn[] move)
		{
			final PokemonWrapper[] wrap = new PokemonWrapper[active.length];
			for(int i = 0; i < wrap.length; ++i)
				wrap[i] = new PokemonWrapper(active[i], move[i], i);
			Collections.sort(Arrays.asList(wrap));
			final int[] order = new int[wrap.length];
			for(int i = 0; i < wrap.length; ++i)
			{
				PokemonWrapper item = wrap[i];
				active[i] = item.m_poke;
				move[i] = item.m_turn;
				order[i] = item.m_idx;
			}
			return order;
		}

		/**
		 * Compare this object to another PokemonWrapper.
		 */
		public int compareTo(Object obj)
		{
			PokemonWrapper comp = (PokemonWrapper) obj;
			if(comp == null || comp.m_turn == null)
				return -1;
			if(m_turn == null)
				return 1;
			if(m_turn.isMoveTurn() && comp.m_turn.isMoveTurn())
			{
				int p1 = 0, p2 = 0;
				PokemonMove m1 = m_turn.getMove(m_poke);
				if(m1 != null)
					p1 = m1.getPriority();
				PokemonMove m2 = comp.m_turn.getMove(comp.m_poke);
				if(m2 != null)
					p2 = m2.getPriority();
				if(p1 > p2)
					return -1;
				if(p2 > p1)
					return 1;
				return compareSpeed(m_poke, comp.m_poke);
			}
			return !m_turn.isMoveTurn() ? -1 : 1;
		}
	}

	// Cache of Struggle.
	private static final MoveListEntry m_struggle = MoveList.getDefaultData().getMove("Struggle");
	protected int[] m_active = { 0, 0 };
	/* The dispatch thread */
	protected Thread m_dispatch = null;
	protected ArrayList<FieldEffect> m_effects = new ArrayList<FieldEffect>();
	/* Stores if we are waiting for a switch */
	protected boolean m_isWaiting = false;
	/* The number of people who can actually participate. This could be four
	 * later, but for now it will always be two. */
	protected int m_participants = 2;
	/* The Pokemon in this battlefield */
	protected Pokemon[][] m_pokemon;
	/* Needed for request and wait for switch
	 * Tells battle threadlets if the player forced to switch, has switched */
	protected boolean[] m_replace;
	private BattleMechanics m_mechanics;

	private boolean m_narration = true;

	/* Store lists of spectators and effects */
	private ArrayList<Player> m_spectators = new ArrayList<Player>();

	/** Creates a new instance of BattleField */
	public BattleField(BattleMechanics mech, Pokemon[][] pokemon)
	{
		m_mechanics = mech;
		m_pokemon = pokemon;
		/* Set m_hasSwitched to 4 to allow for 2 v 2 battles in future */
		m_replace = new boolean[4];
		for(int i = 0; i < m_replace.length; i++)
			m_replace[i] = false;
		attachField();
		setPokemon(pokemon);
	}

	/**
	 * Allows children to construct without pokemon.
	 * 
	 * @param mech
	 */
	protected BattleField(BattleMechanics mech)
	{
		m_mechanics = mech;
	}

	public static MoveListEntry getStruggle()
	{
		return m_struggle;
	}

	/**
	 * Adds a spectator to the battle
	 * 
	 * @param p
	 */
	public void addSpectator(Player p)
	{
		m_spectators.add(p);
	}

	/**
	 * Apply a new FieldEffect to this BattleField.
	 * Note that the actual effect passed in is not used -- it is cloned via
	 * eff.getFieldCopy(), not eff.clone(), the latter of which should return
	 * the same object.
	 */
	public boolean applyEffect(FieldEffect eff)
	{
		if(eff != null)
		{
			Iterator<FieldEffect> i = m_effects.iterator();
			while(i.hasNext())
			{
				FieldEffect j = i.next();
				if(j.isRemovable())
					continue;
				if(eff.equals(j))
					return false;

				if(eff.isExclusiveWith(j))
				{
					// FieldEffects overwrite each other rather than failing if
					// another in their class is present.
					removeEffect(j);
					// We know that no other statuses can possibly be in this
					// category, so it is safe to skip the rest of this loop.
					break;
				}
			}

			FieldEffect applied = eff.getFieldCopy();
			if(!applied.applyToField(this))
				return false;

			m_effects.add(applied);

			// Apply to each pokemon in the field.
			Pokemon[] active = getActivePokemon();
			if(active != null)
				for(int j = 0; j < active.length; ++j)
					active[j].addStatus(null, applied);
			return true;
		}
		return false;
	}

	/**
	 * Applies weather to the battlefield.
	 * Must be implemented by children classes.
	 */
	public abstract void applyWeather();

	public void attachField(int i)
	{
		Pokemon[] team = m_pokemon[i];
		for(int j = 0; j < team.length; ++j)
			if(team[j] != null)
				team[j].attachToField(this, i, j);
	}

	/**
	 * Check if one party has won the battle and inform victory if so.
	 */
	public void checkBattleEnd(int i)
	{
		if(getAliveCount(i) != 0)
			return;
		int opponent = i == 0 ? 1 : 0;
		if(getAliveCount(opponent) == 0)
			// It's a draw!
			opponent = -1;
		informVictory(opponent);
	}

	public abstract void clearQueue();

	/**
	 * Dispose of this object by breaking all links to other objects, making it
	 * easy to the garbage collector to find and free them.
	 */
	public void dispose()
	{
		detachField();
		m_spectators = null;
		m_effects = null;
		m_pokemon = null;
		m_active = null;
		m_mechanics = null;
	}

	public abstract void executeItemTurn(int i);

	/**
	 * Execute a turn.
	 */
	public void executeTurn(BattleTurn[] move)
	{
		Pokemon[] active = getActivePokemon();
		int[] order = PokemonWrapper.sortMoves(active, move);
		for(int i = 0; i < active.length; ++i)
		{
			BattleTurn turn = move[i];
			if(turn == null)
				continue;
			if(turn.isMoveTurn())
			{
				PokemonMove pokemonMove = turn.getMove(active[i]);
				if(pokemonMove != null)
				{
					try
					{
						pokemonMove.beginTurn(move, i, active[i]);
					}
					catch(Exception e)
					{
						System.err.println("A pokemon tried to use a move and something went wrong, check this error:");
						System.err.println(e);
					}
				}
			}
		}
		for(int i = 0; i < active.length; ++i)
		{
			int other = order[i] == 0 ? 1 : 0;
			BattleTurn turn = move[i];
			if(turn != null)
				executeTurn(turn, order[i], other);
		}

		/* TODO: On Victory the active Pokemon returned are null. We have 2 choices:
		 * Throw NPE in getActivePokemon() and catch it, or do a null check here and return. */
		if(getActivePokemon() == null)
			return;
		/* Refresh the active array in case a trainer switched. */
		active = getActivePokemon();
		tickStatuses(active);
		boolean request = true;
		for(int i = 0; i < active.length; ++i)
		{
			/* Synchronise statuses. */
			active[i].synchroniseStatuses();
			if(!active[i].isFainted())
				continue;
			requestPokemonReplacement(i);
			request = false;
		}
		/* Synchronise FieldEffects. */
		synchroniseFieldEffects();
		if(request)
			requestMoves();
	}

	/**
	 * Forces move executions
	 */
	public abstract void forceExecuteTurn();

	/**
	 * Get the active pokemon.
	 */
	public Pokemon[] getActivePokemon()
	{
		if(m_pokemon == null || m_pokemon[0] == null || m_pokemon[1] == null)
			return null;
		return new Pokemon[] { m_pokemon[0][m_active[0]], m_pokemon[1][m_active[1]] };
	}

	/**
	 * Return the number of party members in a given party who are alive.
	 */
	public int getAliveCount(int idx)
	{
		if(idx < 0 || idx >= m_participants)
			throw new IllegalArgumentException("0 <= idx < participants");
		int alive = 0;
		Pokemon[] pokemon = m_pokemon[idx];
		for(int i = 0; i < pokemon.length; ++i)
			if(pokemon[i] != null && !pokemon[i].isFainted())
				alive++;
		return alive;
	}

	/**
	 * Returns the first instance of an effect of a certain class that is
	 * applied to the BattleField.
	 */
	@SuppressWarnings("rawtypes")
	// TODO:Supressed this warning because we are passing different kind of effects and can't parameterize the Class type
	public FieldEffect getEffectByType(Class type)
	{
		ArrayList<FieldEffect> list = getEffectsByType(type);
		if(list.size() == 0)
			return null;
		return list.get(0);
	}

	/**
	 * Get the effectiveness of a move against a given pokemon on this field.
	 */
	public double getEffectiveness(PokemonType move, PokemonType pokemon, boolean enemy)
	{
		return Pokemon.getEffectiveness(m_effects, move, pokemon, enemy);
	}

	/**
	 * Returns a list of the effects of a certain class that are applied to
	 * this BattleField.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	// TODO:Supressed this warning because we are passing different kind of effects and can't parameterize the Class type
	public ArrayList<FieldEffect> getEffectsByType(Class type)
	{
		ArrayList<FieldEffect> ret = new ArrayList<FieldEffect>();
		Iterator<FieldEffect> i = m_effects.iterator();
		while(i.hasNext())
		{
			FieldEffect effect = i.next();
			if(effect == null || !effect.isActive())
				continue;
			if(type.isAssignableFrom(effect.getClass()))
				ret.add(effect);
		}
		return ret;
	}

	/**
	 * Get the mechanics used on this battle field.
	 */
	public BattleMechanics getMechanics()
	{
		return m_mechanics;
	}

	/**
	 * Get the opponent of the Pokemon passed in.
	 */
	public Pokemon getOpponent(Pokemon p)
	{
		int idx = getPokemonTrainer(p);
		int opponent = idx == 0 ? 1 : 0;
		return m_pokemon[opponent][m_active[opponent]];
	}

	/**
	 * Get a party.
	 */
	public Pokemon[] getParty(int idx) throws IllegalArgumentException
	{
		if(idx < 0 || idx >= m_participants)
			throw new IllegalArgumentException("0 <= idx < participants");
		return m_pokemon[idx];
	}

	/**
	 * Gets the party index of a pokemon
	 * 
	 * @param p
	 * @return
	 */
	public int getPokemonPartyIndex(int trainer, Pokemon p)
	{
		/* Check player 1 */
		for(int i = 0; i < m_pokemon[trainer].length; i++)
			if(m_pokemon[trainer][i] != null && m_pokemon[trainer][i].compareTo(p) == 0)
				return i;
		/* This Pokemon came out of nowhere it seems */
		return -1;
	}

	/**
	 * Get the index of a trainer from one of his pokemon.
	 * 
	 * @param p a Pokemon who with 100% certainty belongs to one of the clients
	 */
	public int getPokemonTrainer(Pokemon p)
	{
		Pokemon[] team = m_pokemon[0];
		for(int i = 0; i < team.length; ++i)
			if(team[i] == p)
				return 0;
		return 1;
	}

	/**
	 * Returns the queued battle turns
	 * 
	 * @return
	 */
	public abstract BattleTurn[] getQueuedTurns();

	/**
	 * Return the instance of Random used on this BattleField.
	 */
	public Random getRandom()
	{
		return m_mechanics.getRandom();
	}

	/**
	 * Get the name of a trainer by number.
	 */
	public abstract String getTrainerName(int idx);

	/**
	 * Inform that a pokemon fainted.
	 */
	public abstract void informPokemonFainted(int trainer, int idx);

	/**
	 * Inform that a pokemon's health was changed.
	 */
	public abstract void informPokemonHealthChanged(Pokemon poke, int change);

	/**
	 * Inform that a status was applied to a pokemon.
	 */
	public abstract void informStatusApplied(Pokemon poke, StatusEffect eff);

	/**
	 * Inform that a status effect was removed from a pokemon.
	 */
	public abstract void informStatusRemoved(Pokemon poke, StatusEffect eff);

	/**
	 * Inform that a pokemon was switched in.
	 */
	public abstract void informSwitchInPokemon(int trainer, Pokemon poke);

	/**
	 * Inform that a pokemon used a move.
	 * 
	 * @param poke the pokemon who used the move
	 * @param name the name of the move that was used
	 */
	public abstract void informUseMove(Pokemon poke, String name);

	/**
	 * Inform that a player has won.
	 */
	public abstract void informVictory(int winner);

	/**
	 * Return whether narration is enabled.
	 */
	public boolean isNarrationEnabled()
	{
		return m_narration;
	}

	/**
	 * Queue a move.
	 */
	public abstract void queueMove(int trainer, BattleTurn move) throws MoveQueueException;

	/**
	 * Refresh all active pokemon. The exact meaning of this is for the
	 * implementation to decide.
	 */
	public abstract void refreshActivePokemon();

	/**
	 * Remove a FieldEffect from this field.
	 */
	public void removeEffect(FieldEffect eff)
	{
		Pokemon[] active = getActivePokemon();
		for(int i = 0; i < active.length; ++i)
			eff.unapply(active[i]);
		eff.unapplyToField(this);
		eff.disable();
	}

	/**
	 * Removes a spectator from the battle
	 * 
	 * @param p
	 */
	public void removeSpectator(Player p)
	{
		m_spectators.remove(p);
	}

	/**
	 * Replace a fainted pokemon.
	 */
	public void replaceFaintedPokemon(int party, int pokemon, boolean search)
	{
		if(pokemon < 0 || pokemon > 5)
			return;
		switchInPokemon(party, pokemon);
		if(!search)
			return;
		for(int i = 0; i < 2; ++i)
			if(m_pokemon[i][m_active[i]].isFainted())
			{
				requestPokemonReplacement(i);
				return;
			}
		requestMoves();
	}

	/**
	 * Wait for a player to switch pokemon.
	 */
	public abstract void requestAndWaitForSwitch(int party);

	/**
	 * Set whether to narrate the battle.
	 */
	public void setNarrationEnabled(boolean enabled)
	{
		m_narration = enabled;
	}

	/**
	 * Narrate the battle.
	 */
	public abstract void showMessage(String message);

	/**
	 * Switch in a pokemon and apply FieldEffects to it.
	 */
	public void switchInPokemon(int trainer, int idx)
	{
		m_pokemon[trainer][m_active[trainer]].switchOut();
		m_active[trainer] = idx;
		Pokemon poke = m_pokemon[trainer][idx];
		informSwitchInPokemon(trainer, poke);

		applyEffects(poke);
		poke.switchIn();
	}

	/**
	 * Synchronise FieldEffects.
	 */
	public void synchroniseFieldEffects()
	{
		if(m_effects == null)
			return;
		Iterator<FieldEffect> i = m_effects.iterator();
		while(i.hasNext())
		{
			StatusEffect eff = i.next();
			if(eff.isRemovable())
				i.remove();
		}
	}

	/**
	 * Return whether a client's team is valid.
	 */
	public void validateTeam(Pokemon[] team, int idx) throws ValidationException
	{
		final int length = team.length;
		if(length < 1 || length > 6)
			throw new ValidationException("The team is an invalid size.");
		ModData data = ModData.getDefaultData();
		for(int i = 0; i < length; ++i)
			team[i].validate(data);

		// Check any clauses.
		Collections.sort(m_effects, new Comparator<Object>()
		{
			public int compare(Object o1, Object o2)
			{
				StatusEffect e1 = (StatusEffect) o1;
				StatusEffect e2 = (StatusEffect) o2;
				return e1.getTier() - e2.getTier();
			}
		});
		Iterator<FieldEffect> i = m_effects.iterator();
		while(i.hasNext())
		{
			StatusEffect eff = i.next();
			if(eff instanceof Clause)
			{
				Clause clause = (Clause) eff;
				if(!clause.isTeamValid(this, team, idx))
					throw new ValidationException("The team violates " + clause.getClauseName() + ".");
			}
		}
	}

	/**
	 * Attach this field to all of its pokemon.
	 */
	protected void attachField()
	{
		for(int i = 0; i < m_pokemon.length; ++i)
			attachField(i);
	}

	/**
	 * Requests a move from a specific player
	 * 
	 * @param trainer
	 */
	protected abstract void requestMove(int trainer);

	/**
	 * Request moves for the next turn from both players
	 */
	protected abstract void requestMoves();

	/**
	 * Obtain a replacement pokemon for the team identified by the parameter.
	 */
	protected abstract void requestPokemonReplacement(int i);

	protected void setPokemon(Pokemon[][] pokemon)
	{
		Pokemon[] active = getActivePokemon();
		sortBySpeed(active);
		for(int i = 0; i < active.length; ++i)
		{
			Pokemon p = active[i];
			if(p != null)
			{
				applyEffects(p);
				p.switchIn();
			}
		}
	}

	/**
	 * Apply field effects to a pokemon.
	 */
	private void applyEffects(Pokemon p)
	{
		Iterator<FieldEffect> i = m_effects.iterator();
		while(i.hasNext())
		{
			FieldEffect eff = i.next();
			if(!eff.isRemovable())
				p.addStatus(null, eff);
		}
	}

	/**
	 * Detaches the battlefield from all pokemon
	 */
	private void detachField()
	{
		for(int i = 0; i < m_pokemon.length; ++i)
			detachField(i);
	}

	/**
	 * Detaches battlefield from a specific party
	 * 
	 * @param i
	 */
	private void detachField(int i)
	{
		Pokemon[] team = m_pokemon[i];
		for(int j = 0; j < team.length; ++j)
			if(team[j] != null)
				team[j].detachField();
	}

	/**
	 * Execute a turn.
	 */
	private void executeTurn(BattleTurn turn, int source, int target)
	{
		/* TODO: Check this code, it is known to cause Battle Crashes. (NPE)
		 * m_pokemon and m_active are null (Battle is over.) */
		if(m_pokemon == null || m_active == null)
			return;
		Pokemon psource = m_pokemon[source][m_active[source]];
		if(psource.isFainted())
			return;
		if(turn.isSwitchTurn())
		{
			switchInPokemon(source, turn.getId());
			return;
		}
		psource.executeTurn(turn);
		int move = turn.getId();
		if(turn.isItemTurn())
			return;
		MoveListEntry entry = psource.getMove(move);
		if(entry == null)
			return;
		PokemonMove theMove = entry.getMove();
		if(psource.isImmobilised(theMove.getStatusException()))
			return;
		Pokemon ptarget = m_pokemon[target][m_active[target]];
		if(theMove.isAttack() && ptarget.isFainted())
		{
			informUseMove(psource, entry.getName());
			showMessage("But there was no target!");
			return;
		}
		psource.useMove(move, ptarget);
		if(psource.getItem() != null)
		{
			ArrayList<StatusEffect> tmp = psource.getStatusEffects();// getNormalStatuses(0);
			for(StatusEffect statusEff : tmp)
			{
				if(statusEff.getName() != null)
				{
					switch(statusEff.getName())
					{
						case "Sleep":
							if(psource.getItemName().equalsIgnoreCase("Chesto Berry"))
							{
								psource.removeStatus(statusEff);
								this.showMessage(psource.m_name + "ate the " + psource.getItemName() + " it was holding");
								this.informStatusRemoved(psource, statusEff);
								psource.setItem(null);
							}
							break;
						case "Poison":
							if(psource.getItemName().equalsIgnoreCase("Pecha Berry"))
							{
								psource.removeStatus(statusEff);
								this.showMessage(psource.getName() + "ate the " + psource.getItemName() + " it was holding");
								this.informStatusRemoved(psource, statusEff);
								psource.setItem(null);
							}
							break;
						case "Freeze":
							if(psource.getItemName().equalsIgnoreCase("Aspear Berry"))
							{
								psource.removeStatus(statusEff);
								this.showMessage(psource.getName() + "ate the " + psource.getItemName() + " it was holding");
								informStatusRemoved(psource, statusEff);
								psource.setItem(null);
							}
							break;
						case "Confusion":
							if(psource.getItemName().equalsIgnoreCase("Persim Berry"))
							{
								psource.removeStatus(statusEff);
								this.showMessage(psource.getName() + "ate the " + psource.getItemName() + " it was holding");
								this.informStatusRemoved(psource, statusEff);
								psource.setItem(null);
							}
							break;
						case "Burn":
							if(psource.getItemName().equalsIgnoreCase("Rawst Berry"))
							{
								psource.removeStatus(statusEff);
								this.showMessage(psource.getName() + "ate the " + psource.getItemName() + " it was holding");
								this.informStatusRemoved(psource, statusEff);
								psource.setItem(null);
							}
							break;
						case "Paralysis":
							if(psource.getItemName().equalsIgnoreCase("Cheri Berry"))
							{
								psource.removeStatus(statusEff);
								this.showMessage(psource.getName() + "ate the " + psource.getItemName() + " it was holding");
								this.informStatusRemoved(psource, statusEff);
								psource.setItem(null);
							}
							break;
						default:
							break;
					}
				}
				if(psource.getHealth() < (psource.getRawStat(Pokemon.S_HP) * 0.5))
				{
					if(psource.getItemName().equalsIgnoreCase("Oran Berry"))
					{
						this.showMessage(psource.getName() + " ate the Oran Berry it was holding/nThe Oran Berry restored 10HP");
						psource.changeHealth(10);
						psource.setItem(null);
						break;
					}
					else if(psource.getItemName().equalsIgnoreCase("Sitrus Berry"))
					{
						this.showMessage(psource.getName() + " ate the Sitrus Berry it was holding/nThe Sitrus Berry restored 30HP");
						psource.changeHealth(30);
						psource.setItem(null);
						break;
					}
				}
				else
				{
					switch(psource.getItemName())
					{
						case "Leftovers":
							if(psource.getHealth() < psource.getRawStat(Pokemon.S_HP))
							{
								if(psource.getRawStat(Pokemon.S_HP) - psource.getHealth() > psource.getStat(Pokemon.S_HP) / 16)
									psource.changeHealth(psource.getStat(Pokemon.S_HP) / 16);
								else
									psource.changeHealth(psource.getRawStat(Pokemon.S_HP) - psource.getHealth());
								System.out.println(psource.getStat(Pokemon.S_HP) / 16);
							}
							this.showMessage(psource.getName() + "'s leftovers heals a bit of HP");
							break;
						case "Black Sludge":
							if(psource.m_type1.equalsIgnoreCase("Poison") || psource.m_type2.equalsIgnoreCase("Poison"))
							{
								if(psource.getRawStat(Pokemon.S_HP) - psource.getHealth() > psource.getStat(Pokemon.S_HP) / 16)
									psource.changeHealth(psource.getStat(Pokemon.S_HP) / 16);
								else
									psource.changeHealth(psource.getRawStat(Pokemon.S_HP) - psource.getHealth());
								this.showMessage(psource.getName() + "'s black sludge heals a bit of HP");
							}
							else
							{
								if(psource.getRawStat(Pokemon.S_HP) - psource.getHealth() > psource.getStat(Pokemon.S_HP) / 16)	// if hp is bigger than 1/16 of total just remove
									psource.changeHealth(-psource.getStat(Pokemon.S_HP) / 16);
								else if(psource.getRawStat(Pokemon.S_HP) - psource.getHealth() == 1)	// if hp is 1 do nothing
								{
								}
								else
									// less than 1/16 of hp, remove what is left but leave 1 hp
									psource.changeHealth(-psource.getRawStat(Pokemon.S_HP) - psource.getHealth() + 1);
								this.showMessage(psource.getName() + " is hurt by black sludge");
							}
							break;
						case "Sticky Barb":
							if(psource.getRawStat(Pokemon.S_HP) - psource.getHealth() > psource.getStat(Pokemon.S_HP) / 8)	// if hp is bigger than 1/8 of total just remove
								psource.changeHealth(-psource.getStat(Pokemon.S_HP) / 8);
							else if(psource.getRawStat(Pokemon.S_HP) - psource.getHealth() == 1)	// if hp is 1 do nothing
							{
							}
							else
								// less than 1/16 of hp, remove what is left but leave 1 hp
								psource.changeHealth(-psource.getRawStat(Pokemon.S_HP) - psource.getHealth() + 1);
							this.showMessage(psource.getName() + " is hurt by Sticky Barb");
							break;
						default:
							break;
					}
					break;
				}
			}
		}
	}

	/**
	 * Determine the order in which pokemon attack, etc.
	 */
	@SuppressWarnings("unchecked")
	private void sortBySpeed(Pokemon[] active)
	{
		/* Sort pokemon by speed. */
		ArrayList<Pokemon> list = new ArrayList<Pokemon>(Arrays.asList(active));
		Collections.sort(list, new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				return PokemonWrapper.compareSpeed((Pokemon) o1, (Pokemon) o2);
			}
		});
	}

	/**
	 * Tick status effects at the end of a turn.
	 */
	private void tickStatuses(Pokemon[] active)
	{
		sortBySpeed(active);

		for(int i = 0; i < active.length; ++i)
			active[i].beginStatusTicks();

		// For each tier.
		final int tiers = StatusEffect.getTierCount();
		for(int i = 0; i < tiers; ++i)
			// For each pokemon.
			for(int j = 0; j < active.length; ++j)
			{
				Pokemon poke = active[j];
				if(poke.isFainted())
					continue;

				List v = poke.getStatusesByTier(i);
				Iterator k = v.iterator();
				while(k.hasNext())
					((StatusEffect) k.next()).tick(poke);
			}
	}
}
