package org.destiny.server.battle.impl;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.BattleField;
import org.destiny.server.battle.BattleTurn;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.mechanics.BattleMechanics;
import org.destiny.server.battle.mechanics.MoveQueueException;
import org.destiny.server.battle.mechanics.statuses.BurnEffect;
import org.destiny.server.battle.mechanics.statuses.FreezeEffect;
import org.destiny.server.battle.mechanics.statuses.ParalysisEffect;
import org.destiny.server.battle.mechanics.statuses.PoisonEffect;
import org.destiny.server.battle.mechanics.statuses.SleepEffect;
import org.destiny.server.battle.mechanics.statuses.StatusEffect;
import org.destiny.server.battle.mechanics.statuses.field.FieldEffect;
import org.destiny.server.battle.mechanics.statuses.field.HailEffect;
import org.destiny.server.battle.mechanics.statuses.field.RainEffect;
import org.destiny.server.battle.mechanics.statuses.field.SandstormEffect;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.feature.TimeService;
import org.destiny.server.protocol.ServerMessage;

/**
 * A battlefield for PvP battles
 * 
 * @author shadowkanji
 */
public class PvPBattleField extends BattleField
{
	private boolean m_finished = false;
	private Player[] m_players;
	private BattleTurn[] m_turn = new BattleTurn[2];

	/**
	 * Constructor
	 * 
	 * @param mech
	 * @param p1
	 * @param p2
	 */
	public PvPBattleField(BattleMechanics mech, Player p1, Player p2)
	{
		super(mech, new Pokemon[][] { p1.getParty(), p2.getParty() });
		/* Store the players */
		m_players = new Player[2];
		m_players[0] = p1;
		m_players[1] = p2;
		/* Set the player to battling */
		p1.setBattling(true);
		p2.setBattling(true);
		/* Set the battlefield for the players */
		p2.setBattleField(this);
		p1.setBattleField(this);
		/* Set battle ids */
		p1.setBattleId(0);
		p2.setBattleId(1);
		/* Send battle initialisation packets */
		ServerMessage startBattleFirst = new ServerMessage(p1.getSession());
		startBattleFirst.init(ClientPacket.BATTLE_STARTED.getValue());
		startBattleFirst.addBool(false);
		startBattleFirst.addInt(p2.getPartyCount());
		startBattleFirst.sendResponse();
		ServerMessage startBattleSecond = new ServerMessage(p2.getSession());
		startBattleSecond.init(ClientPacket.BATTLE_STARTED.getValue());
		startBattleSecond.addBool(false);
		startBattleSecond.addInt(p1.getPartyCount());
		startBattleSecond.sendResponse();
		/* Check if p1 player has seen this enemy pokemon before, if not, update pokedex */
		if(!p1.isPokemonSeen(getActivePokemon()[1].getPokedexNumber()))
			p1.setPokemonSeen(getActivePokemon()[1].getPokedexNumber());
		/* Check if p2 player has seen this enemy pokemon before, if not, update pokedex */
		if(!p2.isPokemonSeen(getActivePokemon()[0].getSpeciesNumber() + 1))
			p2.setPokemonSeen(getActivePokemon()[0].getSpeciesNumber() + 1);
		/* Send the enemy's name to both players */
		ServerMessage enemyNameFirst = new ServerMessage(p1.getSession());
		enemyNameFirst.init(ClientPacket.ENEMY_TRAINER_NAME.getValue());
		enemyNameFirst.addString(p2.getName());
		enemyNameFirst.sendResponse();
		ServerMessage enemyNameSecond = new ServerMessage(p2.getSession());
		enemyNameSecond.init(ClientPacket.ENEMY_TRAINER_NAME.getValue());
		enemyNameSecond.addString(p1.getName());
		enemyNameSecond.sendResponse();
		/* Send pokemon data to both players */
		sendPokemonData(p1, p2);
		sendPokemonData(p2, p1);
		/* Apply weather and request moves */
		applyWeather();
		requestMoves();
	}

	@Override
	public void applyWeather()
	{
		if(m_players[0].getMap().isWeatherForced())
			switch(m_players[0].getMap().getWeather())
			{
				case NORMAL:
					return;
				case RAIN:
					applyEffect(new RainEffect());
					return;
				case HAIL:
					applyEffect(new HailEffect());
					return;
				case SANDSTORM:
					applyEffect(new SandstormEffect());
					return;
				default:
					return;
			}
		else
		{
			FieldEffect f = TimeService.getWeatherEffect();
			if(f != null)
				applyEffect(f);
		}
	}

	@Override
	public void clearQueue()
	{
		m_turn[0] = null;
		m_turn[1] = null;
	}

	/**
	 * Handles a disconnect from a player Rewards other player with some money
	 * 
	 * @param trainer
	 */
	public void disconnect(int trainer)
	{
		if(m_players != null)
			if(trainer == 0)
			{
				ServerMessage victory = new ServerMessage(m_players[1].getSession());
				victory.init(ClientPacket.BATTLE_RESULT.getValue());
				victory.addInt(0);
				victory.sendResponse();
				m_players[1].setBattling(false);
			}
			else
			{
				ServerMessage victory = new ServerMessage(m_players[0].getSession());
				victory.init(ClientPacket.BATTLE_RESULT.getValue());
				victory.addInt(0);
				victory.sendResponse();
				m_players[0].setBattling(false);
			}
	}

	@Override
	public void executeItemTurn(int i)
	{
		if(m_turn[0] == null)
			m_turn[0] = BattleTurn.getMoveTurn(-1);
		if(m_turn[1] == null)
			m_turn[1] = BattleTurn.getMoveTurn(-1);
		executeTurn(m_turn);
	}

	@Override
	public void forceExecuteTurn()
	{
		if(m_turn[0] == null)
			m_turn[0] = BattleTurn.getMoveTurn(-1);
		if(m_turn[1] == null)
			m_turn[1] = BattleTurn.getMoveTurn(-1);
		executeTurn(m_turn);
	}

	@Override
	public BattleTurn[] getQueuedTurns()
	{
		return m_turn;
	}

	@Override
	public String getTrainerName(int idx)
	{
		return m_players[idx].getName();
	}

	@Override
	public void informPokemonFainted(int trainer, int idx)
	{
		if(m_players != null)
		{
			ServerMessage informFaintFirst = new ServerMessage(m_players[0].getSession());
			informFaintFirst.init(ClientPacket.POKEMON_FAINTED.getValue());
			informFaintFirst.addString(getParty(trainer)[idx].getSpeciesName());
			informFaintFirst.sendResponse();

			ServerMessage informFaintSecond = new ServerMessage(m_players[1].getSession());
			informFaintSecond.init(ClientPacket.POKEMON_FAINTED.getValue());
			informFaintSecond.addString(getParty(trainer)[idx].getSpeciesName());
			informFaintSecond.sendResponse();
		}
	}

	@Override
	public void informPokemonHealthChanged(Pokemon poke, int change)
	{
		if(m_players != null && poke != null)
			if(poke.compareTo(getActivePokemon()[0]) == 0)
			{
				ServerMessage informHealthFirst = new ServerMessage(m_players[0].getSession());
				informHealthFirst.init(ClientPacket.POKEMON_HP.getValue());
				informHealthFirst.addInt(0);
				informHealthFirst.addString("0," + change);
				informHealthFirst.sendResponse();

				ServerMessage informHealthSecond = new ServerMessage(m_players[1].getSession());
				informHealthSecond.init(ClientPacket.POKEMON_HP.getValue());
				informHealthSecond.addInt(1);
				informHealthSecond.addString("1," + change);
				informHealthSecond.sendResponse();
			}
			else if(poke.compareTo(getActivePokemon()[1]) == 0)
			{
				ServerMessage informHealthFirst = new ServerMessage(m_players[1].getSession());
				informHealthFirst.init(ClientPacket.POKEMON_HP.getValue());
				informHealthFirst.addInt(0);
				informHealthFirst.addString("0," + change);
				informHealthFirst.sendResponse();

				ServerMessage informHealthSecond = new ServerMessage(m_players[0].getSession());
				informHealthSecond.init(ClientPacket.POKEMON_HP.getValue());
				informHealthSecond.addInt(1);
				informHealthSecond.addString("1," + change);
				informHealthSecond.sendResponse();
			}
			else
			{
				int index = getPokemonPartyIndex(0, poke);
				if(index > -1)
				{
					/* TODO: Add support for NPC pokemon healing whilst not in the battlefield. */
					ServerMessage informHealth = new ServerMessage(m_players[0].getSession());
					informHealth.init(ClientPacket.POKE_HP_CHANGE.getValue());
					informHealth.addInt(index);
					informHealth.addInt(poke.getHealth());
					informHealth.sendResponse();
					return;
				}
				index = getPokemonPartyIndex(1, poke);
				if(index > -1)
				{
					/* TODO: Add support for NPC pokemon healing whilst not in the battlefield. */
					ServerMessage informHealth = new ServerMessage(m_players[1].getSession());
					informHealth.init(ClientPacket.POKE_HP_CHANGE.getValue());
					informHealth.addInt(index);
					informHealth.addInt(poke.getHealth());
					informHealth.sendResponse();
					return;
				}
			}
	}

	@Override
	public void informStatusApplied(Pokemon poke, StatusEffect eff)
	{
		if(m_finished)
			return;
		if(m_players != null && poke != null)
			if(poke.compareTo(getActivePokemon()[0]) == 0)
			{
				ServerMessage receiveEffectFirst = new ServerMessage(m_players[0].getSession());
				receiveEffectFirst.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectFirst.addInt(0);
				receiveEffectFirst.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					receiveEffectFirst.addString("");
				else
					receiveEffectFirst.addString(eff.getName());
				receiveEffectFirst.sendResponse();

				ServerMessage receiveEffectSecond = new ServerMessage(m_players[1].getSession());
				receiveEffectSecond.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectSecond.addInt(1);
				receiveEffectSecond.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					receiveEffectSecond.addString("");
				else
					receiveEffectSecond.addString(eff.getName());
				receiveEffectSecond.sendResponse();
			}
			else if(poke.compareTo(getActivePokemon()[1]) == 0)
			{
				ServerMessage receiveEffectFirst = new ServerMessage(m_players[0].getSession());
				receiveEffectFirst.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectFirst.addInt(1);
				receiveEffectFirst.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					receiveEffectFirst.addString("");
				else
					receiveEffectFirst.addString(eff.getName());
				receiveEffectFirst.sendResponse();

				ServerMessage receiveEffectSecond = new ServerMessage(m_players[1].getSession());
				receiveEffectSecond.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectSecond.addInt(0);
				receiveEffectSecond.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					receiveEffectSecond.addString("");
				else
					receiveEffectSecond.addString(eff.getName());
				receiveEffectSecond.sendResponse();
			}
	}

	@Override
	public void informStatusRemoved(Pokemon poke, StatusEffect eff)
	{
		if(m_finished)
			return;
		if(poke != null && m_players != null)
			if(poke.compareTo(getActivePokemon()[0]) == 0 && !getActivePokemon()[0].isFainted())
			{
				ServerMessage removeEffectFirst = new ServerMessage(m_players[0].getSession());
				removeEffectFirst.init(ClientPacket.STATUS_REMOVED.getValue());
				removeEffectFirst.addInt(0);
				removeEffectFirst.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					removeEffectFirst.addString("");
				else
					removeEffectFirst.addString(eff.getName());
				removeEffectFirst.sendResponse();

				ServerMessage removeEffectSecond = new ServerMessage(m_players[1].getSession());
				removeEffectSecond.init(ClientPacket.STATUS_REMOVED.getValue());
				removeEffectSecond.addInt(1);
				removeEffectSecond.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					removeEffectSecond.addString("");
				else
					removeEffectSecond.addString(eff.getName());
				removeEffectSecond.sendResponse();
			}
			else if(poke.compareTo(getActivePokemon()[1]) == 0 && !getActivePokemon()[1].isFainted())
			{
				ServerMessage removeEffectFirst = new ServerMessage(m_players[0].getSession());
				removeEffectFirst.init(ClientPacket.STATUS_REMOVED.getValue());
				removeEffectFirst.addInt(1);
				removeEffectFirst.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					removeEffectFirst.addString("");
				else
					removeEffectFirst.addString(eff.getName());
				removeEffectFirst.sendResponse();

				ServerMessage removeEffectSecond = new ServerMessage(m_players[1].getSession());
				removeEffectSecond.init(ClientPacket.STATUS_REMOVED.getValue());
				removeEffectSecond.addInt(0);
				removeEffectSecond.addString(poke.getSpeciesName());
				if(eff.getName() == null)
					removeEffectSecond.addString("");
				else
					removeEffectSecond.addString(eff.getName());
				removeEffectSecond.sendResponse();
			}
	}

	@Override
	public void informSwitchInPokemon(int trainer, Pokemon poke)
	{
		int pokeIndex = getPokemonPartyIndex(trainer, poke);
		// Check if the enemy of the player switching has seen this pokemon before, if not, update the pokedex
		if(m_players != null)
		{
			if(!m_players[trainer].isPokemonSeen(poke.getPokedexNumber()))
				m_players[trainer].setPokemonSeen(poke.getPokedexNumber());

			if(trainer == 0)
			{
				ServerMessage switchInformFirst = new ServerMessage(m_players[0].getSession());
				switchInformFirst.init(ClientPacket.SWITCHED_POKE.getValue());
				switchInformFirst.addString(m_players[0].getName());
				switchInformFirst.addString(poke.getSpeciesName());
				switchInformFirst.addInt(0);
				switchInformFirst.addInt(pokeIndex);
				switchInformFirst.sendResponse();

				ServerMessage receiveEffectFirst = new ServerMessage(m_players[0].getSession());
				receiveEffectFirst.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectFirst.addInt(0);
				receiveEffectFirst.addString(poke.getSpeciesName());

				if(poke.hasEffect(BurnEffect.class))
					receiveEffectFirst.addString("Burn");
				else if(poke.hasEffect(FreezeEffect.class))
					receiveEffectFirst.addString("Freeze");
				else if(poke.hasEffect(ParalysisEffect.class))
					receiveEffectFirst.addString("paralysis");
				else if(poke.hasEffect(PoisonEffect.class))
					receiveEffectFirst.addString("Poison");
				else if(poke.hasEffect(SleepEffect.class))
					receiveEffectFirst.addString("Sleep");
				else
					receiveEffectFirst.addString("Normal");
				receiveEffectFirst.sendResponse();

				ServerMessage switchInformSecond = new ServerMessage(m_players[1].getSession());
				switchInformSecond.init(ClientPacket.SWITCHED_POKE.getValue());
				switchInformSecond.addString(m_players[0].getName());
				switchInformSecond.addString(poke.getSpeciesName());
				switchInformSecond.addInt(1);
				switchInformSecond.addInt(pokeIndex);
				switchInformSecond.sendResponse();

				ServerMessage receiveEffectSecond = new ServerMessage(m_players[1].getSession());
				receiveEffectSecond.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectSecond.addInt(1);
				receiveEffectSecond.addString(poke.getSpeciesName());

				if(poke.hasEffect(BurnEffect.class))
					receiveEffectSecond.addString("Burn");
				else if(poke.hasEffect(FreezeEffect.class))
					receiveEffectSecond.addString("Freeze");
				else if(poke.hasEffect(ParalysisEffect.class))
					receiveEffectSecond.addString("paralysis");
				else if(poke.hasEffect(PoisonEffect.class))
					receiveEffectSecond.addString("Poison");
				else if(poke.hasEffect(SleepEffect.class))
					receiveEffectSecond.addString("Sleep");
				else
					receiveEffectSecond.addString("Normal");
				receiveEffectSecond.sendResponse();

				poke.removeStatusEffects(false);
			}
			else
			{
				ServerMessage switchInformFirst = new ServerMessage(m_players[0].getSession());
				switchInformFirst.init(ClientPacket.SWITCHED_POKE.getValue());
				switchInformFirst.addString(m_players[1].getName());
				switchInformFirst.addString(poke.getSpeciesName());
				switchInformFirst.addInt(1);
				switchInformFirst.addInt(pokeIndex);
				switchInformFirst.sendResponse();

				ServerMessage receiveEffectFirst = new ServerMessage(m_players[0].getSession());
				receiveEffectFirst.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectFirst.addInt(1);
				receiveEffectFirst.addString(poke.getSpeciesName());

				if(poke.hasEffect(BurnEffect.class))
					receiveEffectFirst.addString("Burn");
				else if(poke.hasEffect(FreezeEffect.class))
					receiveEffectFirst.addString("Freeze");
				else if(poke.hasEffect(ParalysisEffect.class))
					receiveEffectFirst.addString("paralysis");
				else if(poke.hasEffect(PoisonEffect.class))
					receiveEffectFirst.addString("Poison");
				else if(poke.hasEffect(SleepEffect.class))
					receiveEffectFirst.addString("Sleep");
				else
					receiveEffectFirst.addString("Normal");
				receiveEffectFirst.sendResponse();

				ServerMessage switchInformSecond = new ServerMessage(m_players[1].getSession());
				switchInformSecond.init(ClientPacket.SWITCHED_POKE.getValue());
				switchInformSecond.addString(m_players[1].getName());
				switchInformSecond.addString(poke.getSpeciesName());
				switchInformSecond.addInt(0);
				switchInformSecond.addInt(pokeIndex);
				switchInformSecond.sendResponse();

				ServerMessage receiveEffectSecond = new ServerMessage(m_players[1].getSession());
				receiveEffectSecond.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffectSecond.addInt(0);
				receiveEffectSecond.addString(poke.getSpeciesName());

				if(poke.hasEffect(BurnEffect.class))
					receiveEffectSecond.addString("Burn");
				else if(poke.hasEffect(FreezeEffect.class))
					receiveEffectSecond.addString("Freeze");
				else if(poke.hasEffect(ParalysisEffect.class))
					receiveEffectSecond.addString("paralysis");
				else if(poke.hasEffect(PoisonEffect.class))
					receiveEffectSecond.addString("Poison");
				else if(poke.hasEffect(SleepEffect.class))
					receiveEffectSecond.addString("Sleep");
				else
					receiveEffectSecond.addString("Normal");
				receiveEffectSecond.sendResponse();

				poke.removeStatusEffects(false);
			}
		}
	}

	@Override
	public void informUseMove(Pokemon poke, String name)
	{
		if(m_players != null)
		{
			ServerMessage moveFirst = new ServerMessage(m_players[0].getSession());
			moveFirst.init(ClientPacket.MOVE_USED.getValue());
			moveFirst.addString(poke.getSpeciesName());
			moveFirst.addString(name);
			moveFirst.sendResponse();

			ServerMessage moveSecond = new ServerMessage(m_players[1].getSession());
			moveSecond.init(ClientPacket.MOVE_USED.getValue());
			moveSecond.addString(poke.getSpeciesName());
			moveSecond.addString(name);
			moveSecond.sendResponse();
		}
	}

	@Override
	public void informVictory(int winner)
	{
		m_finished = true;
		m_players[0].removeTempStatusEffects();
		m_players[1].removeTempStatusEffects();
		if(winner == 0)
		{
			ServerMessage victory = new ServerMessage(m_players[0].getSession());
			victory.init(ClientPacket.BATTLE_RESULT.getValue());
			victory.addInt(0);
			victory.sendResponse();

			ServerMessage loss = new ServerMessage(m_players[1].getSession());
			loss.init(ClientPacket.BATTLE_RESULT.getValue());
			loss.addInt(1);
			loss.sendResponse();
			m_players[1].lostBattle();
		}
		else
		{
			ServerMessage loss = new ServerMessage(m_players[0].getSession());
			loss.init(ClientPacket.BATTLE_RESULT.getValue());
			loss.addInt(0);
			loss.sendResponse();

			ServerMessage victory = new ServerMessage(m_players[1].getSession());
			victory.init(ClientPacket.BATTLE_RESULT.getValue());
			victory.addInt(1);
			victory.sendResponse();
			m_players[0].lostBattle();
		}
		m_players[0].setBattling(false);
		m_players[1].setBattling(false);
		dispose();
		if(m_dispatch != null)
		{
			/* This very bad programming but shoddy does it and forces us to do it */
			// Thread t = m_dispatch;
			m_dispatch = null;
			// t.stop(); let the thread manually return.
		}
	}

	@Override
	public void queueMove(int trainer, BattleTurn move) throws MoveQueueException
	{
		/* Checks the move exists */
		if(move.isMoveTurn() && move.getId() != -1 && getActivePokemon()[trainer].getMove(move.getId()) == null)
		{
			requestMove(trainer);
			return;
		}
		/* Handle forced switches */
		if(m_isWaiting && m_replace != null && m_replace[trainer])
		{
			if(!move.isMoveTurn())
				if(getActivePokemon()[trainer].compareTo(getParty(trainer)[move.getId()]) != 0)
				{
					switchInPokemon(trainer, move.getId());
					m_replace[trainer] = false;
					m_isWaiting = false;
					return;
				}
			requestPokemonReplacement(trainer);
			return;
		}
		// The trainer has no turn queued.
		if(m_turn[trainer] == null)
		{
			/* Handle Pokemon being unhappy and ignoring you */
			if(!getActivePokemon()[trainer].isFainted())
				if(getActivePokemon()[trainer].getHappiness() <= 40)
				{
					/* Pokemon is unhappy, they'll do what they feel like */
					showMessage(getActivePokemon()[trainer].getSpeciesName() + " is unhappy!");
					int moveID = getMechanics().getRandom().nextInt(4);
					while(getActivePokemon()[trainer].getMove(moveID) == null)
						moveID = getMechanics().getRandom().nextInt(4);
					move = BattleTurn.getMoveTurn(moveID);
				}
				else if(getActivePokemon()[trainer].getHappiness() < 70)
					/* Pokemon is partially unhappy, 50% chance they'll listen to you */
					if(getMechanics().getRandom().nextInt(2) == 1)
					{
						showMessage(getActivePokemon()[trainer].getSpeciesName() + " is unhappy!");
						int moveID = getMechanics().getRandom().nextInt(4);
						while(getActivePokemon()[trainer].getMove(moveID) == null)
							moveID = getMechanics().getRandom().nextInt(4);
						move = BattleTurn.getMoveTurn(moveID);
					}
			if(move.getId() == -1)
			{
				if(m_dispatch == null && (trainer == 0 && m_turn[1] != null || trainer == 1 && m_turn[0] != null))
				{
					m_dispatch = new Thread(new Runnable()
					{
						public void run()
						{
							executeTurn(m_turn);
							m_dispatch = null;
						}
					}, "BattleTurn-Thread");
					m_dispatch.start();
					return;
				}
			}
			else // Handle a fainted pokemon
			if(getActivePokemon()[trainer].isFainted())
			{
				if(!move.isMoveTurn() && getParty(trainer)[move.getId()] != null && getParty(trainer)[move.getId()].getHealth() > 0)
				{
					switchInPokemon(trainer, move.getId());
					requestMoves();
					return;
				}
				else // The player still has pokemon left
				if(getAliveCount(trainer) > 0)
				{
					requestPokemonReplacement(trainer);
					return;
				}
				else
				{
					// the player has no pokemon left. Announce winner
					if(trainer == 0)
						informVictory(1);
					else
						informVictory(0);
					return;
				}
			}
			else // The turn was used to attack!
			if(move.isMoveTurn())
			{
				// Handles Struggle
				if(getActivePokemon()[trainer].mustStruggle())
					m_turn[trainer] = BattleTurn.getMoveTurn(-1);
				else // The move has no more PP. Tell the client!
				if(getActivePokemon()[trainer].getPp(move.getId()) <= 0)
				{
					if(trainer == 0)
					{
						ServerMessage noPP = new ServerMessage(m_players[0].getSession());
						noPP.init(ClientPacket.NO_PP_LEFT.getValue());
						noPP.addString(getActivePokemon()[trainer].getMoveName(move.getId()));
						noPP.sendResponse();
					}
					else
					{
						ServerMessage noPP = new ServerMessage(m_players[1].getSession());
						noPP.init(ClientPacket.NO_PP_LEFT.getValue());
						noPP.addString(getActivePokemon()[trainer].getMoveName(move.getId()));
						noPP.sendResponse();
					}
					return;
				}
				else
					// Assign the move to the turn
					m_turn[trainer] = move;
			}
			else if(move.isItemTurn())
				return;
			else if(getActivePokemon()[trainer].isActive() && getParty(trainer)[move.getId()] != null && getParty(trainer)[move.getId()].getHealth() > 0)
				m_turn[trainer] = move;
			else
			{
				requestMove(trainer);
				return;
			}
		}
		if(m_dispatch != null)
			return;
		// Both turns are ready to be performed
		if(m_turn[0] != null && m_turn[1] != null)
		{
			m_dispatch = new Thread(new Runnable()
			{
				public void run()
				{
					executeTurn(m_turn);
					for(int i = 0; i < m_participants; ++i)
						m_turn[i] = null;
					m_dispatch = null;
				}
			}, "BattleTurn-Thread");
			m_dispatch.start();
		}
	}

	@Override
	public void refreshActivePokemon()
	{
		if(m_players != null)
		{
			ServerMessage informHealthFirst = new ServerMessage(m_players[0].getSession());
			informHealthFirst.init(ClientPacket.POKEMON_HP.getValue());
			informHealthFirst.addInt(0);
			informHealthFirst.addString("0," + getActivePokemon()[0].getHealth());
			informHealthFirst.sendResponse();
			ServerMessage informHealthFirstSecond = new ServerMessage(m_players[0].getSession());
			informHealthFirstSecond.init(ClientPacket.POKEMON_HP.getValue());
			informHealthFirstSecond.addInt(1);
			informHealthFirstSecond.addString("1," + getActivePokemon()[1].getHealth());
			informHealthFirstSecond.sendResponse();
			ServerMessage informHealthSecond = new ServerMessage(m_players[1].getSession());
			informHealthSecond.init(ClientPacket.POKEMON_HP.getValue());
			informHealthSecond.addInt(0);
			informHealthSecond.addString("0," + getActivePokemon()[1].getHealth());
			informHealthSecond.sendResponse();
			ServerMessage informHealthSecondS = new ServerMessage(m_players[1].getSession());
			informHealthSecondS.init(ClientPacket.POKEMON_HP.getValue());
			informHealthSecondS.addInt(1);
			informHealthSecondS.addString("1," + getActivePokemon()[0].getHealth());
			informHealthSecondS.sendResponse();
		}
	}

	@Override
	public void requestAndWaitForSwitch(int party)
	{
		int index = m_players[party].getPokemonIndex(getActivePokemon()[party]);
		int switchin = 0;
		for(int i = index + 1; i != index; i++)
		{
			if(i == 6)
			{
				i = 0;
			}
			if(m_players[party].getParty()[i] != null || !m_players[party].getParty()[i].isFainted())
			{
				switchin = i;
				break;
			}
		}
		if(getActivePokemon()[party].getLastMove().getName().equalsIgnoreCase("Baton Pass"))
		{
			// getActivePokemon()[party].hasEffect(MultipleStatChangeEffect.class);
			// getActivePokemon()[party].hasEffect(StatChangeEffect.class);
			System.out.println("last move was baton pass");
		}
		getActivePokemon()[party].switchOut();
		m_active[party] = switchin;
		replacementPokemonRequest(party, m_players[party].getParty()[switchin]);
		if(!m_replace[party])
			return;
		m_isWaiting = true;
		do
			synchronized(m_dispatch)
			{
				try
				{
					m_dispatch.wait(1000);
				}
				catch(InterruptedException e)
				{
				}
			}
		while(m_replace != null && m_replace[party]);
	}

	@Override
	public void showMessage(String message)
	{
		if(m_finished)
			return;
		if(m_players != null)
		{
			if(m_players[0] != null)
			{
				ServerMessage MessageFirst = new ServerMessage(m_players[0].getSession());
				MessageFirst.init(ClientPacket.MISC_BATTLE_MESSAGE.getValue());
				MessageFirst.addString(message);
				MessageFirst.sendResponse();
			}
			if(m_players[1] != null)
			{
				ServerMessage MessageSecond = new ServerMessage(m_players[1].getSession());
				MessageSecond.init(ClientPacket.MISC_BATTLE_MESSAGE.getValue());
				MessageSecond.addString(message);
				MessageSecond.sendResponse();
			}
		}
	}

	@Override
	protected void requestMove(int trainer)
	{
		ServerMessage moveRequest = new ServerMessage(m_players[trainer].getSession());
		moveRequest.init(ClientPacket.MOVE_REQUESTED.getValue());
		moveRequest.sendResponse();
	}

	@Override
	protected void requestMoves()
	{
		clearQueue();
		if(getActivePokemon()[0].isActive() && getActivePokemon()[1].isActive())
		{
			/* pp might be out of sync lets update it */
			ServerMessage ppUpdate0 = new ServerMessage(m_players[0].getSession());
			ppUpdate0.init(ClientPacket.BATTLE_PP_UPDATE.getValue());
			ppUpdate0.addInt(getActivePokemon()[0].getPp(0));
			ppUpdate0.addInt(getActivePokemon()[0].getPp(1));
			ppUpdate0.addInt(getActivePokemon()[0].getPp(2));
			ppUpdate0.addInt(getActivePokemon()[0].getPp(3));
			ppUpdate0.sendResponse();

			ServerMessage moveRequestFirst = new ServerMessage(m_players[0].getSession());
			moveRequestFirst.init(ClientPacket.MOVE_REQUESTED.getValue());
			moveRequestFirst.sendResponse();

			/* pp might be out of sync lets update it */
			ServerMessage ppUpdate1 = new ServerMessage(m_players[1].getSession());
			ppUpdate1.init(ClientPacket.BATTLE_PP_UPDATE.getValue());
			ppUpdate1.addInt(getActivePokemon()[1].getPp(0));
			ppUpdate1.addInt(getActivePokemon()[1].getPp(1));
			ppUpdate1.addInt(getActivePokemon()[1].getPp(2));
			ppUpdate1.addInt(getActivePokemon()[1].getPp(3));
			ppUpdate1.sendResponse();

			ServerMessage moveRequestSecond = new ServerMessage(m_players[1].getSession());
			moveRequestSecond.init(ClientPacket.MOVE_REQUESTED.getValue());
			moveRequestSecond.sendResponse();
		}
	}

	@Override
	protected void requestPokemonReplacement(int i)
	{
		// ServerMessage switchOccur = new ServerMessage(m_players[i].getSession());
		// switchOccur.init(ClientPacket.SWITCHED_POKE.getValue());
		// switchOccur.sendResponse();
	}

	protected void replacementPokemonRequest(int i, Pokemon poke)
	{
		informSwitchInPokemon(i, poke);
		poke.switchIn();
	}

	/**
	 * Sends pokemon data for Player p to receiver
	 * 
	 * @param p
	 * @param receiver
	 */
	private void sendPokemonData(Player p, Player receiver)
	{
		for(int i = 0; i < p.getParty().length; i++)
		{
			if(p.getParty()[i] != null)
			{
				Pokemon pp = p.getParty()[i];
				ServerMessage enemyData = new ServerMessage(receiver.getSession());
				enemyData.init(ClientPacket.RECEIVE_POKE_DATA.getValue());
				enemyData.addInt(i);
				enemyData.addString(pp.getName());
				enemyData.addInt(pp.getLevel());
				enemyData.addInt(pp.getGender());
				enemyData.addInt(pp.getStat(0));
				enemyData.addInt(pp.getHealth());
				enemyData.addInt(pp.getPokedexNumber());
				enemyData.addBool(pp.isShiny());
				enemyData.sendResponse();
			}
		}
	}
}
