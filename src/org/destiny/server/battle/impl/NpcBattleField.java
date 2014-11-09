package org.destiny.server.battle.impl;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.NPC;
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
import org.destiny.server.constants.UserClasses;
import org.destiny.server.feature.TimeService;
import org.destiny.server.protocol.ServerMessage;

/**
 * A battlefield for NPC battles
 * 
 * @author shadowkanji
 */
public class NpcBattleField extends BattleField
{
	private static final int BASE_GOLD_REWARD = 30;
	private boolean m_finished = false;
	private NPC m_npc;
	private Player m_player;
	private BattleTurn[] m_turn = new BattleTurn[2];

	/**
	 * Constructor
	 * 
	 * @param mech
	 * @param p
	 * @param n
	 */
	public NpcBattleField(BattleMechanics mech, Player p, NPC n)
	{
		super(mech, new Pokemon[][] { p.getParty(), n.getParty(p) });
		/* Store the player and npc */
		m_player = p;
		m_npc = n;

		/* Start the battle */
		m_player.ensureHealthyPokemon();
		ServerMessage startBattle = new ServerMessage(m_player.getSession());
		startBattle.init(ClientPacket.BATTLE_STARTED.getValue());
		startBattle.addBool(false);
		startBattle.addInt(getAliveCount(1));
		startBattle.sendResponse();
		/* Check if this player has seen this wild pokemon before, if not, update pokedex */
		if(!m_player.isPokemonSeen(getActivePokemon()[1].getPokedexNumber()))
			m_player.setPokemonSeen(getActivePokemon()[1].getPokedexNumber());
		/* Send enemy name */
		ServerMessage enemyName = new ServerMessage(m_player.getSession());
		enemyName.init(ClientPacket.ENEMY_TRAINER_NAME.getValue());
		enemyName.addString(m_npc.getName());
		enemyName.sendResponse();
		/* Send enemy's Pokemon data */
		sendPokemonData(p);
		/* Set the player's battle id */
		m_player.setBattleId(0);
		/* Apply weather and request moves */
		applyWeather();
		requestMoves();
	}

	@Override
	public void applyWeather()
	{
		if(m_player.getMap().isWeatherForced())
			switch(m_player.getMap().getWeather())
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

	@Override
	public void executeItemTurn(int i)
	{
		if(m_turn[0] == null)
			m_turn[0] = BattleTurn.getItemTurn(i);
		if(m_turn[1] == null)
			m_turn[1] = BattleTurn.getItemTurn(i);
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
		if(idx == 0)
			return m_player.getName();
		else
			return m_npc.getName();
	}

	@Override
	public void informPokemonFainted(int trainer, int idx)
	{
		if(m_player != null)
		{
			ServerMessage informFaint = new ServerMessage(m_player.getSession());
			informFaint.init(ClientPacket.POKEMON_FAINTED.getValue());
			informFaint.addString(getParty(trainer)[idx].getSpeciesName());
			informFaint.sendResponse();
		}
	}

	@Override
	public void informPokemonHealthChanged(Pokemon poke, int change)
	{
		if(m_player != null)
			if(getActivePokemon()[0] == poke)
			{
				ServerMessage informHealth = new ServerMessage(m_player.getSession());
				informHealth.init(ClientPacket.POKEMON_HP.getValue());
				informHealth.addInt(0);
				informHealth.addString("0," + change);
				informHealth.sendResponse();
			}
			else if(getActivePokemon()[1] == poke)
			{
				ServerMessage informHealth = new ServerMessage(m_player.getSession());
				informHealth.init(ClientPacket.POKEMON_HP.getValue());
				informHealth.addInt(1);
				informHealth.addString("1," + change);
				informHealth.sendResponse();
			}
			else
			{
				int index = getPokemonPartyIndex(0, poke);
				if(index > -1)
				{
					ServerMessage informHealth = new ServerMessage(m_player.getSession());
					informHealth.init(ClientPacket.POKE_HP_CHANGE.getValue());
					informHealth.addInt(index);
					informHealth.addInt(poke.getHealth());
					informHealth.sendResponse();
					return;
				}
				/* TODO: Add support for NPC pokemon healing whilst not in the battlefield. */
			}
	}

	@Override
	/* TODO: check this code with checking moves. */
	public void informStatusApplied(Pokemon pokemon, StatusEffect eff)
	{
		if(m_finished)
			return;

		if(m_player != null)
			if(getActivePokemon()[0].equals(pokemon))
			{
				ServerMessage receiveEffect = new ServerMessage(m_player.getSession());
				receiveEffect.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffect.addInt(0);
				receiveEffect.addString(pokemon.getSpeciesName());
				if(eff == null)
					receiveEffect.addString("");
				else
					receiveEffect.addString(eff.getName());
				receiveEffect.sendResponse();
			}
			else if(pokemon.equals(getActivePokemon()[1]))
			{
				ServerMessage receiveEffect = new ServerMessage(m_player.getSession());
				receiveEffect.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffect.addInt(1);
				receiveEffect.addString(pokemon.getSpeciesName());
				// System.out.println(pokemon.getSpeciesName() + ", " + eff.getName());
				if(eff.getName() == null)
					receiveEffect.addString("");
				else
					receiveEffect.addString(eff.getName());
				receiveEffect.sendResponse();
			}
	}

	@Override
	public void informStatusRemoved(Pokemon pokemon, StatusEffect eff)
	{
		if(m_finished)
			return;
		if(m_player != null)
			if(getActivePokemon()[0].equals(pokemon) && !getActivePokemon()[0].isFainted())
			{
				ServerMessage removeEffect = new ServerMessage(m_player.getSession());
				removeEffect.init(ClientPacket.STATUS_REMOVED.getValue());
				removeEffect.addInt(0);
				removeEffect.addString(pokemon.getSpeciesName());
				if(eff.getName() == null)
					removeEffect.addString("");
				else
					removeEffect.addString(eff.getName());
				removeEffect.sendResponse();
			}
			else if(pokemon.equals(getActivePokemon()[1]) && !getActivePokemon()[1].isFainted())
			{
				ServerMessage removeEffect = new ServerMessage(m_player.getSession());
				removeEffect.init(ClientPacket.STATUS_REMOVED.getValue());
				removeEffect.addInt(1);
				removeEffect.addString(pokemon.getSpeciesName());
				if(eff.getName() == null)
					removeEffect.addString("");
				else
					removeEffect.addString(eff.getName());
				removeEffect.sendResponse();
			}
	}

	@Override
	public void informSwitchInPokemon(int trainer, Pokemon poke)
	{
		if(m_player != null)
			if(trainer == 0)
			{
				ServerMessage switchInform = new ServerMessage(m_player.getSession());
				switchInform.init(ClientPacket.SWITCHED_POKE.getValue());
				switchInform.addString(m_player.getName());
				switchInform.addString(poke.getSpeciesName());
				switchInform.addInt(trainer);
				switchInform.addInt(getPokemonPartyIndex(trainer, poke));
				switchInform.sendResponse();

				ServerMessage receiveEffect = new ServerMessage(m_player.getSession());
				receiveEffect.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffect.addInt(0);
				receiveEffect.addString(poke.getSpeciesName());

				if(poke.hasEffect(BurnEffect.class))
					receiveEffect.addString("Burn");
				else if(poke.hasEffect(FreezeEffect.class))
					receiveEffect.addString("Freeze");
				else if(poke.hasEffect(ParalysisEffect.class))
					receiveEffect.addString("paralysis");
				else if(poke.hasEffect(PoisonEffect.class))
					receiveEffect.addString("Poison");
				else if(poke.hasEffect(SleepEffect.class))
					receiveEffect.addString("Sleep");
				else
					receiveEffect.addString("Normal");
				receiveEffect.sendResponse();

				poke.removeStatusEffects(false);
			}
			else
			{
				ServerMessage switchInform = new ServerMessage(m_player.getSession());
				switchInform.init(ClientPacket.SWITCHED_POKE.getValue());
				switchInform.addString(m_npc.getName());
				switchInform.addString(poke.getSpeciesName());
				switchInform.addInt(trainer);
				switchInform.addInt(getPokemonPartyIndex(trainer, poke));
				switchInform.sendResponse();

				ServerMessage receiveEffect = new ServerMessage(m_player.getSession());
				receiveEffect.init(ClientPacket.STATUS_RECEIVED.getValue());
				receiveEffect.addInt(1);
				receiveEffect.addString(poke.getSpeciesName());

				if(poke.hasEffect(BurnEffect.class))
					receiveEffect.addString("Burn");
				else if(poke.hasEffect(FreezeEffect.class))
					receiveEffect.addString("Freeze");
				else if(poke.hasEffect(ParalysisEffect.class))
					receiveEffect.addString("paralysis");
				else if(poke.hasEffect(PoisonEffect.class))
					receiveEffect.addString("Poison");
				else if(poke.hasEffect(SleepEffect.class))
					receiveEffect.addString("Sleep");
				else
					receiveEffect.addString("Normal");
				receiveEffect.sendResponse();

				poke.removeStatusEffects(false);
			}
	}

	@Override
	public void informUseMove(Pokemon poke, String name)
	{
		if(m_player != null)
		{
			ServerMessage move = new ServerMessage(m_player.getSession());
			move.init(ClientPacket.MOVE_USED.getValue());
			move.addString(poke.getSpeciesName());
			move.addString(name);
			move.sendResponse();
		}
	}

	/* Ends the battle and the player gets rewarded with gold and experience.
	 * If the gym leader was already beaten the player gets triple the experience and 100 extra gold.
	 * If the enemy trainer was a Gym Leader the player is also rewarded with the badge. */
	@Override
	public void informVictory(int winner)
	{
		m_finished = true;
		int trainerExp = 0;
		double texp = 1.0;
		double tmoneyrate = 1.0;
		if(m_player.getAdminLevel() >= UserClasses.VIP){
			texp = trainerExp * GameServer.RATE_EXP_TRAINER_VIP;
			tmoneyrate = GameServer.RATE_GOLD_VIP;
		} else {
			texp = trainerExp * GameServer.RATE_EXP_TRAINER;
			tmoneyrate = GameServer.RATE_GOLD;
		}
		int money = (int) ((BASE_GOLD_REWARD * (getMechanics().getRandom().nextInt(5) + 1)) * tmoneyrate);	// The magic cookie is used as a base to reward gold (replace later).
		if(winner == 0)
		{
			for(int i = 0; i < getParty(1).length; i++)
				if(getParty(1)[i] != null)
					trainerExp += getParty(1)[i].getLevel() / 2;
			if(m_npc.isGymLeader() && !m_player.hasBadge(m_npc.getBadge()))
			{
				trainerExp *= 3;
				money += 100;
			}
			if(trainerExp > 0)
				m_player.addTrainingExp((int) (trainerExp * texp));
			if(m_npc.isGymLeader())
				m_player.addBadge(m_npc.getBadge());
			ServerMessage reward = new ServerMessage(m_player.getSession());
			reward.init(ClientPacket.MONEY_CHANGED.getValue());
			reward.addInt(money);
			reward.sendResponse();
			m_player.setMoney(m_player.getMoney() + money);
			m_player.removeTempStatusEffects();
			ServerMessage victory = new ServerMessage(m_player.getSession());
			victory.init(ClientPacket.BATTLE_RESULT.getValue());
			victory.addInt(0);
			victory.sendResponse();
			ServerMessage message = new ServerMessage(ClientPacket.UPDATE_COORDS);
			message.addInt(m_player.getX());
			message.addInt(m_player.getY());
			m_player.getSession().Send(message);
		}
		else
		{
			if(m_player.getMoney() - money >= 0)
				m_player.setMoney(m_player.getMoney() - money);
			else
				m_player.setMoney(0);
			ServerMessage loss = new ServerMessage(m_player.getSession());
			loss.init(ClientPacket.BATTLE_RESULT.getValue());
			loss.addInt(1);
			loss.sendResponse();
			m_player.lostBattle();
		}
		m_player.updateClientMoney();
		m_player.setBattling(false);
		m_player.setTalking(false);
		dispose();
		if(m_dispatch != null)
		{
			/* TODO: This programming so bad it's in comments. Rewrite or remove?
			 * This very bad programming but shoddy does it and forces us to do it */
			/* Thread t = m_dispatch;
			 * m_dispatch = null;
			 * t.stop(); let the thread manually return. */
		}
	}

	@Override
	public void queueMove(int trainer, BattleTurn move) throws MoveQueueException
	{
		/* Check if move exists */
		if(move.isMoveTurn() && move.getId() != -1 && getActivePokemon()[trainer].getMove(move.getId()) == null)
		{
			requestMove(trainer);
			return;
		}
		/* Handle forced switches */
		if(m_isWaiting && m_replace != null && m_replace[trainer])
		{
			if(!move.isMoveTurn())
				if(!getActivePokemon()[trainer].equals(getParty(trainer)[move.getId()]))
				{
					switchInPokemon(trainer, move.getId());
					m_replace[trainer] = false;
					m_isWaiting = false;
					return;
				}
			requestPokemonReplacement(trainer);
			return;
		}
		/* Queue the move */
		if(m_turn[trainer] == null)
		{
			/* Handle Pokemon being unhappy and ignoring you */
			if(trainer == 0 && !getActivePokemon()[0].isFainted())
				if(getActivePokemon()[0].getHappiness() <= 40)
				{
					/* Pokemon is unhappy, they'll do what they feel like */
					showMessage(getActivePokemon()[0].getSpeciesName() + " is unhappy!");
					int moveID = getMechanics().getRandom().nextInt(4);
					while(getActivePokemon()[0].getMove(moveID) == null)
						moveID = getMechanics().getRandom().nextInt(4);
					move = BattleTurn.getMoveTurn(moveID);
				}
				else if(getActivePokemon()[0].getHappiness() < 70)
					/* Pokemon is partially unhappy, 50% chance they'll listen to you */
					if(getMechanics().getRandom().nextInt(2) == 1)
					{
						showMessage(getActivePokemon()[0].getSpeciesName() + " is unhappy!");
						int moveID = getMechanics().getRandom().nextInt(4);
						while(getActivePokemon()[0].getMove(moveID) == null)
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
				else // The move has no more PP
				if(getActivePokemon()[trainer].getPp(move.getId()) <= 0)
				{
					if(trainer == 0)
					{
						// TcpProtocolHandler.writeMessage(m_player.getTcpSession(), new NoPPMessage(this.getActivePokemon()[trainer].getMoveName(move.getId())));
						ServerMessage noPP = new ServerMessage(m_player.getSession());
						noPP.init(ClientPacket.NO_PP_LEFT.getValue());
						noPP.addString(getActivePokemon()[trainer].getMoveName(move.getId()));
						noPP.sendResponse();
						requestMove(0);
					}
					else
						/* Get another move from the npc */
						requestMove(1);
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
		/* Ensures the npc selected a move */
		if(trainer == 0 && m_turn[0] != null && m_turn[1] == null)
		{
			requestMove(1);
			return;
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
		ServerMessage informHealthFirst = new ServerMessage(m_player.getSession());
		informHealthFirst.init(ClientPacket.POKEMON_HP.getValue());
		informHealthFirst.addInt(0);
		informHealthFirst.addString("0," + getActivePokemon()[0].getHealth());
		informHealthFirst.sendResponse();
		ServerMessage informHealthSecond = new ServerMessage(m_player.getSession());
		informHealthSecond.init(ClientPacket.POKEMON_HP.getValue());
		informHealthSecond.addInt(1);
		informHealthSecond.addString("1," + getActivePokemon()[1].getHealth());
		informHealthSecond.sendResponse();
	}

	@Override
	public void requestAndWaitForSwitch(int party)
	{

		if(party == 0)
		{
			int index = m_player.getPokemonIndex(getActivePokemon()[party]);
			int switchin = 0;
			for(int i = index + 1; i != index; i++)
			{
				if(i == 6)
				{
					i = 0;
				}
				if(m_player.getParty()[i] != null || !m_player.getParty()[i].isFainted())
				{
					switchin = i;
					break;
				}
			}
			getActivePokemon()[party].switchOut();
			m_active[party] = switchin;
			replacementPokemonRequest(party, m_player.getParty()[switchin]);
			/* Request a switch from the player */
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
		else
		{
			int index = 0;
			for(int i = 0; i < m_npc.getParty(m_player).length; i++)
			{
				if(m_npc.getParty(m_player)[i].isActive())
				{
					index = i;
					break;
				}
			}
			// int index = m_npc.getParty(m_player).getPokemonIndex(getActivePokemon()[party]);
			int switchin = 0;
			for(int i = index + 1; i != index; i++)
			{
				if(i == 6)
				{
					i = 0;
				}
				if(m_npc.getParty(m_player)[i] != null || !m_npc.getParty(m_player)[i].isFainted())
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
			replacementPokemonRequest(party, m_npc.getParty(m_player)[switchin]);
		}
	}

	@Override
	public void showMessage(String message)
	{
		if(m_finished)
			return;
		if(m_player != null)
		{
			ServerMessage Message = new ServerMessage(m_player.getSession());
			Message.init(ClientPacket.MISC_BATTLE_MESSAGE.getValue());
			Message.addString(message);
			Message.sendResponse();
		}
	}

	@Override
	protected void requestMove(int trainer)
	{
		if(trainer == 0)
		{
			/* pp might be out of sync lets update it */
			ServerMessage ppUpdate = new ServerMessage(m_player.getSession());
			ppUpdate.init(ClientPacket.BATTLE_PP_UPDATE.getValue());
			ppUpdate.addInt(getActivePokemon()[0].getPp(0));
			ppUpdate.addInt(getActivePokemon()[0].getPp(1));
			ppUpdate.addInt(getActivePokemon()[0].getPp(2));
			ppUpdate.addInt(getActivePokemon()[0].getPp(3));
			ppUpdate.sendResponse();
			/* Request move from player */
			ServerMessage moveRequest = new ServerMessage(m_player.getSession());
			moveRequest.init(ClientPacket.MOVE_REQUESTED.getValue());
			moveRequest.sendResponse();
		}
		else
		{
			/* Request move from npc */
			try
			{
				if(getActivePokemon()[1].hasTypeWeakness(getActivePokemon()[0]) && getAliveCount(1) >= 3)
					/* The npc should switch out a different Pokemon */
					/* 50:50 chance they will switch */
					if(getMechanics().getRandom().nextInt(3) == 0)
					{
						int index = 0;
						while(getParty(1)[index] == null || getParty(1)[index].isFainted() || getParty(1)[index].compareTo(getActivePokemon()[1]) == 0)
						{
							try
							{
								Thread.sleep(100);
							}
							catch(Exception e)
							{
							}
							index = getMechanics().getRandom().nextInt(6);
						}
						queueMove(1, BattleTurn.getSwitchTurn(index));
						return;
					}
				/* If they did not switch, select a move */
				int moveID = getMechanics().getRandom().nextInt(4);
				while(getActivePokemon()[1].getMove(moveID) == null)
					moveID = getMechanics().getRandom().nextInt(4);
				queueMove(1, BattleTurn.getMoveTurn(moveID));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void requestMoves()
	{
		clearQueue();
		requestMove(1);
		requestMove(0);
	}

	@Override
	protected void requestPokemonReplacement(int i)
	{
		if(i == 0)
		{
			/* Request Pokemon replacement from player */
			// TcpProtocolHandler.writeMessage(m_player.getTcpSession(), new SwitchRequest());
			// ServerMessage switchOccur = new ServerMessage(m_player.getSession());
			// switchOccur.Init(32);
			// switchOccur.sendResponse();
		}
		else /* Request Pokemon replacement from npc */
		if(getAliveCount(1) == 0)
			informVictory(0);
		else
			try
			{
				int index = 0;

				while(getParty(1)[index] == null || getParty(1)[index].isFainted())
				{
					try
					{
						Thread.sleep(100);
					}
					catch(Exception e)
					{
					}
					index = getMechanics().getRandom().nextInt(6);
				}
				switchInPokemon(1, BattleTurn.getSwitchTurn(index).getId());

				// Check if this player has seen this wild pokemon before, if not, update pokedex
				if(!m_player.isPokemonSeen(m_pokemon[1][index].getPokedexNumber()))
					m_player.setPokemonSeen(m_pokemon[1][index].getPokedexNumber());
				requestMoves();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	protected void replacementPokemonRequest(int i, Pokemon poke)
	{
		/* TcpProtocolHandler.writeMessage(m_players[i].getTcpSession(), new SwitchRequest()); */
		informSwitchInPokemon(i, poke);
		poke.switchIn();
	}

	/**
	 * Sends pokemon data to the client
	 * 
	 * @param receiver
	 */
	private void sendPokemonData(Player receiver)
	{
		for(int i = 0; i < getParty(1).length; i++)
			if(getParty(1)[i] != null)
			{
				Pokemon p = getParty(1)[i];
				ServerMessage enemyData = new ServerMessage(m_player.getSession());
				enemyData.init(ClientPacket.RECEIVE_POKE_DATA.getValue());
				enemyData.addInt(i);
				enemyData.addString(p.getName());
				enemyData.addInt(p.getLevel());
				enemyData.addInt(p.getGender());
				enemyData.addInt(p.getHealth());
				enemyData.addInt(p.getHealth());
				enemyData.addInt(p.getPokedexNumber());
				enemyData.addBool(p.isShiny());
				enemyData.sendResponse();
			}
	}

}
