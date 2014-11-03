/* IntrinsicAbility.java
 * Created on January 6, 2007, 5:47 PM
 * This file is a part of Shoddy Battle.
 * Copyright (C) 2006 Colin Fitzpatrick
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. */

package org.destiny.server.battle.mechanics.statuses.abilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.destiny.server.battle.BattleField;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.mechanics.JewelMechanics;
import org.destiny.server.battle.mechanics.PokemonType;
import org.destiny.server.battle.mechanics.moves.MoveList;
import org.destiny.server.battle.mechanics.moves.MoveListEntry;
import org.destiny.server.battle.mechanics.moves.PokemonMove;
import org.destiny.server.battle.mechanics.moves.RecoilMove;
import org.destiny.server.battle.mechanics.statuses.BurnEffect;
import org.destiny.server.battle.mechanics.statuses.ConfuseEffect;
import org.destiny.server.battle.mechanics.statuses.FreezeEffect;
import org.destiny.server.battle.mechanics.statuses.ParalysisEffect;
import org.destiny.server.battle.mechanics.statuses.PoisonEffect;
import org.destiny.server.battle.mechanics.statuses.SleepEffect;
import org.destiny.server.battle.mechanics.statuses.StatChangeEffect;
import org.destiny.server.battle.mechanics.statuses.StatusEffect;
import org.destiny.server.battle.mechanics.statuses.StatusListener;
import org.destiny.server.battle.mechanics.statuses.field.FieldEffect;
import org.destiny.server.battle.mechanics.statuses.field.HailEffect;
import org.destiny.server.battle.mechanics.statuses.field.RainEffect;
import org.destiny.server.battle.mechanics.statuses.field.SandstormEffect;
import org.destiny.server.battle.mechanics.statuses.field.SunEffect;
import org.destiny.server.battle.mechanics.statuses.field.WeatherEffect;
import org.destiny.server.battle.mechanics.statuses.items.HoldItem;

/**
 * @author Colin
 */
public class IntrinsicAbility extends StatusEffect implements Comparable<Object>
{

	/**
	 * Absorb 25% damage of one type.
	 */
	private static class AbsorbAbility extends IntrinsicAbility
	{
		private PokemonType m_type;

		public AbsorbAbility(String name, PokemonType type)
		{
			super(name);
			m_type = type;
		}

		@Override
		public boolean isMoveTransformer(boolean enemy)
		{
			return enemy;
		}

		@Override
		protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
		{
			PokemonMove move = entry.getMove();
			if(move.getType().equals(m_type) && move.isAttack())
			{
				p.getField().informUseMove(p.getOpponent(), entry.getName());
				int max = p.getStat(Pokemon.S_HP);
				if(p.getHealth() < max)
				{
					p.getField().showMessage(p.getName() + "'s " + getName() + " absorbed damage!");
					p.changeHealth(max / 4);
				}
				return null;
			}
			return entry;
		}

	}

	private static class AirLockAbility extends IntrinsicAbility
	{
		public AirLockAbility(String name)
		{
			super(name);
		}

		@Override
		public void switchIn(Pokemon p)
		{
			BattleField field = p.getField();
			FieldEffect eff = field.getEffectByType(WeatherEffect.class);
			if(eff != null)
			{
				eff.unapply(p);
				eff.unapply(p.getOpponent());
			}
		}

		@Override
		public boolean switchOut(Pokemon p)
		{
			p.setAbility(null, true);
			BattleField field = p.getField();
			FieldEffect eff = field.getEffectByType(WeatherEffect.class);
			if(eff != null)
			{
				eff.apply(p);
				eff.apply(p.getOpponent());
			}
			return super.switchOut(p);
		}
	}

	private static class UnburdenAbility extends IntrinsicAbility implements StatusListener
	{
		public UnburdenAbility()
		{
			super("Unburden");
		}

		public void informStatusApplied(Pokemon source, Pokemon poke, StatusEffect eff)
		{
		}

		public void informStatusRemoved(Pokemon p, StatusEffect eff)
		{
			if(eff instanceof HoldItem)
				p.addStatus(p, new StatusEffect()
				{
					@Override
					public boolean apply(Pokemon p)
					{
						p.getMultiplier(Pokemon.S_SPEED).multiplyBy(2.0);
						return super.apply(p);
					}

					@Override
					public String getDescription()
					{
						return " lost its burden!";
					}

					@Override
					public String getName()
					{
						return "Unburden";
					}

					@Override
					public boolean isPassable()
					{
						return false;
					}

					@Override
					public void unapply(Pokemon p)
					{
						p.getMultiplier(Pokemon.S_SPEED).divideBy(2.0);
					}
				});
		}
	}

	protected static HashMap<String, IntrinsicAbility> m_map = new HashMap<String, IntrinsicAbility>();

	/**
	 * List of abilities nullified by Mold Breaker.
	 */
	private static final HashSet<String> m_moldBreaker = new HashSet<String>(Arrays.asList(new String[] { "Battle Armor", "Clear Body", "Damp", "Dry Skin", "Filter", "Flash Fire", "Flower Gift",
			"Heatproof", "Hyper Cutter", "Immunity", "Inner Focus", "Insomnia", "Keen Eye", "Leaf Guard", "Levitate", "Lightningrod", "Limber", "Magma Armor", "Marvel Scale", "Motor Drive",
			"Oblivious", "Own Tempo", "Sand Veil", "Shell Armor", "Shield Dust", "Simple", "Snow Cloak", "Solid Rock", "Soundproof", "Sticky Hold", "Storm Drain", "Suction Cups", "Tangled Feet",
			"Thick Fat", "Unaware", "Vital Spirit", "Volt Absorb", "Water Absorb", "Water Veil", "White Smoke", "Wonder Guard" }));

	/**
	 * List of moves that count as physical ("contact") attacks.
	 */
	private static final HashSet<String> m_physical = new HashSet<String>(Arrays.asList(new String[] { "Aerial Ace", "Ancientpower", "Arm Thrust", "Astonish", "Bide", "Bind", "Bite", "Blaze Kick",
			"Body Slam", "Bounce", "Brick Break", "Clamp", "Comet Punch", "Constrict", "Counter", "Crabhammer", "Cross Chop", "Crunch", "Crush Claw", "Cut", "Dig", "Dive", "Dizzy Punch",
			"Double Kick", "Double-Edge", "Doubleslap", "Dragon Claw", "Drill Peck", "Dynamicpunch", "Endeavor", "Extremespeed", "Facade", "False Swipe", "Fire Punch", "Flail", "Flame Wheel", "Fly",
			"Focus Punch", "Frustration", "Fury Attack", "Fury Cutter", "Fury Swipes", "Guillotine", "Headbutt", "Hi Jump Kick", "Horn Attack", "Horn Drill", "Hyper Fang", "Ice Ball", "Ice Punch",
			"Iron Tail", "Jump Kick", "Karate Chop", "Knock Off", "Leaf Blade", "Leech Life", "Lick", "Low Kick", "Mach Punch", "Mega Kick", "Mega Punch", "Megahorn", "Metal Claw", "Meteor Mash",
			"Needle Arm", "Outrage", "Overheat", "Peck", "Petal Dance", "Poison Fang", "Poison Tail", "Pound", "Pursuit", "Quick Attack", "Rage", "Rapid Spin", "Return", "Revenge", "Reversal",
			"Rock Smash", "Rolling Kick", "Rollout", "Scratch", "Seismic Toss", "Shadow Punch", "Skull Bash", "Sky Uppercut", "Slam", "Slash", "Smellingsalt", "Spark", "Steel Wing", "Stomp",
			"Strength", "Struggle", "Submission", "Super Fang", "Superpower", "Tackle", "Take Down", "Thief", "Thrash", "Thunderpunch", "Tickle", "Triple Kick", "Vice Grip", "Vine Whip",
			"Vital Throw", "Waterfall", "Wing Attack", "Wrap" }));

	private String m_name = null;

	/**
	 * Prevent external creation.
	 */
	protected IntrinsicAbility(boolean register, String name)
	{
		m_name = name;
		if(register && name != null)
			registerAbility();
	}

	/***
	 * Registration constructor.
	 */
	protected IntrinsicAbility(String name)
	{
		this(true, name);
	}

	/**
	 * Initialise the HaspMap of abilities.
	 */
	static
	{
		new IntrinsicAbility("Battle Armor");
		new IntrinsicAbility("Chlorophyll");
		new IntrinsicAbility("Rock Head");
		new IntrinsicAbility("Clear Body");
		new IntrinsicAbility("White Smoke");

		new IntrinsicAbility("Color Change")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				PokemonType type = move.getMove().getType();
				if(!target.isType(type))
				{
					target.setType(new PokemonType[] { type });
					target.getField().showMessage(target.getName() + " became the " + type + " type!");
				}
			}

			@Override
			public boolean isListener()
			{
				return true;
			}

			@Override
			public void switchIn(Pokemon p)
			{
				p.setType(new PokemonType[] { PokemonType.T_NORMAL });
			}
		};

		new CriticalTypeAbility("Blaze", PokemonType.T_FIRE);
		new CriticalTypeAbility("Overgrow", PokemonType.T_GRASS);
		new CriticalTypeAbility("Swarm", PokemonType.T_BUG);
		new CriticalTypeAbility("Torrent", PokemonType.T_WATER);
		new CompoundEyesAbility();
		new IntrinsicAbility("Damp");

		new IntrinsicAbility("Drought")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				p.getField().showMessage(p.getName() + "'s Drought intensified the sun's rays!");
				p.getField().applyEffect(new SunEffect(0));
			}
		};

		new IntrinsicAbility("Drizzle")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				p.getField().showMessage(p.getName() + "'s Drizzle caused a storm!");
				p.getField().applyEffect(new RainEffect(0));
			}
		};

		new IntrinsicAbility("Sand Stream")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				BattleField field = p.getField();
				field.showMessage(p.getName() + "'s Sand Stream whipped up a sandstorm!");
				field.applyEffect(new SandstormEffect(0));
			}
		};

		new IntrinsicAbility("Early Bird");

		new IntrinsicAbility("Effect Spore")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				if(!isPhysical(move))
					return;

				StatusEffect eff = null;
				String description = "";
				String name = source.getName();
				BattleField field = source.getField();
				final double random = field.getRandom().nextDouble();
				if(random <= 1.0)
				{
					eff = new ParalysisEffect();
					description = "paralysed " + name;
				}
				else if(random <= 2.0)
				{
					eff = new PoisonEffect();
					description = "poisoned " + name;
				}
				else if(random <= 3.0)
				{
					eff = new SleepEffect();
					description = "put " + name + " to sleep";
				}
				if(eff != null)
				{
					field.setNarrationEnabled(false);
					boolean applied = source.addStatus(target, eff) != null;
					field.setNarrationEnabled(true);
					if(applied)
						source.getField().showMessage(target.getName() + "'s Effect Spore " + description + "!");
				}
			}

			@Override
			public boolean isListener()
			{
				return true;
			}
		};

		new IntrinsicAbility("Flame Body")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				if(!isPhysical(move))
					return;
				if(source.getField().getRandom().nextDouble() <= 0.3)
					if(source.addStatus(target, new BurnEffect()) != null)
						source.getField().showMessage(target.getName() + "'s Flame Body burned " + source.getName() + "!");
			}

			@Override
			public boolean isListener()
			{
				return true;
			}
		};

		new IntrinsicAbility("Poison Point")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				if(!isPhysical(move))
					return;
				if(source.getField().getRandom().nextDouble() <= 0.3)
					if(source.addStatus(target, new PoisonEffect()) != null)
						source.getField().showMessage(target.getName() + "'s Poison Point poisoned " + source.getName() + "!");
			}

			@Override
			public boolean isListener()
			{
				return true;
			}
		};

		new IntrinsicAbility("Static")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				if(!isPhysical(move))
					return;
				if(source.getField().getRandom().nextDouble() <= 0.3)
					if(source.addStatus(target, new ParalysisEffect()) != null)
						source.getField().showMessage(target.getName() + "'s Static paralysed " + source.getName() + "!");
			}

			@Override
			public boolean isListener()
			{
				return true;
			}
		};

		new IntrinsicAbility("Cute Charm")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				if(!isPhysical(move))
					return;
				if(source.getField().getRandom().nextDouble() <= 0.3)
					if(source.addStatus(target, new MoveList.AttractEffect()) != null)
						source.getField().showMessage(target.getName() + "'s Cute Charm infatuated " + source.getName() + "!");
			}

			@Override
			public boolean isListener()
			{
				return true;
			}
		};

		new IntrinsicAbility("Liquid Ooze");

		IntrinsicAbility power = new IntrinsicAbility("Huge Power")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				p.getMultiplier(Pokemon.S_ATTACK).multiplyBy(2.0);
				return true;
			}

			@Override
			public void unapply(Pokemon p)
			{
				p.getMultiplier(Pokemon.S_ATTACK).divideBy(2.0);
			}
			/**
			 * According to AA, you were totally wrong about these not
			 * working with Skill Swap and Role Play, Ben.
			 */
		};

		power = (IntrinsicAbility) power.clone();
		power.m_name = "Pure Power";
		power.registerAbility();

		new IntrinsicAbility("Hustle")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				p.getMultiplier(Pokemon.S_ATTACK).multiplyBy(1.5);
				p.getAccuracy().multiplyBy(0.8);
				return true;
			}

			@Override
			public void unapply(Pokemon p)
			{
				p.getMultiplier(Pokemon.S_ATTACK).divideBy(1.5);
				p.getAccuracy().divideBy(0.8);
			}
		};

		new IntrinsicAbility("Hyper Cutter");
		new IntrinsicAbility("Illuminate");
		new IntrinsicAbility("Immunity");
		new IntrinsicAbility("Inner Focus");
		new IntrinsicAbility("Insomnia");
		new IntrinsicAbility("Forecast");

		new IntrinsicAbility("Intimidate")
		{
			@Override
			public void switchIn(Pokemon a)
			{
				Pokemon b = a.getOpponent();
				if(b.hasAbility("Clear Body") || b.hasAbility("White Smoke") || b.hasAbility("Hyper Cutter") || b.hasSubstitute())
					return;
				a.getField().showMessage(a.getName() + "'s intimidate cut " + b.getName() + "'s attack!");
				StatChangeEffect effect = new StatChangeEffect(Pokemon.S_ATTACK, false, 1);
				effect.setDescription(null);
				b.addStatus(a, effect);
			}
		};

		new IntrinsicAbility("Keen Eye");
		new IntrinsicAbility("Limber");
		new IntrinsicAbility("Magma Armor");
		new IntrinsicAbility("Own Tempo");
		new IntrinsicAbility("Pickup");
		new IntrinsicAbility("Serene Grace");
		new IntrinsicAbility("Shell Armor");
		new IntrinsicAbility("Shield Dust");
		new IntrinsicAbility("Stench");
		new IntrinsicAbility("Sticky Hold");
		new IntrinsicAbility("Water Veil");

		new IntrinsicAbility("Truant")
		{
			private boolean m_inactive = true;

			@Override
			public boolean immobilises(Pokemon p)
			{
				if(m_inactive == !m_inactive)
				{
					p.getField().showMessage(p.getName() + " is loafing around!");
					return true;
				}
				return false;
			}

			@Override
			public boolean switchOut(Pokemon p)
			{
				m_inactive = true;
				return super.switchOut(p);
			}
		};

		new IntrinsicAbility("Natural Cure")
		{
			@Override
			public boolean switchOut(Pokemon p)
			{
				p.removeStatus(StatusEffect.SPECIAL_EFFECT_LOCK);
				return super.switchOut(p);
			}
		};

		new IntrinsicAbility("Pressure")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				if(p.getField().getMechanics() instanceof JewelMechanics)
					p.getField().showMessage(p.getName() + " is exerting its pressure!");
			}
		};
		new IntrinsicAbility("Run Away");

		new IntrinsicAbility("Shed Skin")
		{
			@Override
			public int getTier()
			{
				// TODO: Get the correct tier!
				return 0;
			}

			@Override
			public boolean tick(Pokemon p)
			{
				if(p.getField().getRandom().nextDouble() * 3.0 <= 1.0)
					if(p.hasEffect(StatusEffect.SPECIAL_EFFECT_LOCK))
					{
						p.removeStatusEffects(true);
						p.getField().showMessage(p.getName() + " shed its skin!");
					}
				return false;
			}
		};

		new IntrinsicAbility("Sturdy");
		new IntrinsicAbility("Vital Spirit");
		new IntrinsicAbility("Lightningrod"); // Has no effect in 1 v. 1.
		new IntrinsicAbility("Plus"); // Has no effect in 1 v. 1.
		new IntrinsicAbility("Minus"); // Has no effect in 1 v. 1.

		new IntrinsicAbility("Rough Skin")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				if(!isPhysical(move))
					return;
				int part = damage / 16;
				if(part < 1)
					part = 1;
				source.changeHealth(-part, true);
			}

			@Override
			public boolean isListener()
			{
				return true;
			}
		};

		new IntrinsicAbility("Levitate")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getType().equals(PokemonType.T_GROUND) && move.isAttack())
				{
					BattleField field = p.getField();
					field.showMessage(p.getName() + " makes ground moves miss with Levitate!");
					move.setAccuracy(0);
					return entry;
				}
				return entry;
			}
		};

		new IntrinsicAbility("Thick Fat")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getType().equals(PokemonType.T_FIRE) || move.getType().equals(PokemonType.T_ICE))
					move.setPower(move.getPower() / 2);
				return entry;
			}
		};

		new IntrinsicAbility("Trace")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				Pokemon target = p.getOpponent();
				IntrinsicAbility ability = target.getAbility();
				if(ability == null)
					return;
				if(ability.getName().equals("Multitype"))
					// Multitype cannot be traced.
					return;
				p.setAbility(ability, true);
				BattleField field = p.getField();
				field.showMessage(p.getName() + " traced " + target.getName() + "'s " + ability.getName() + "!");
				if(field.getMechanics() instanceof JewelMechanics)
				{
					IntrinsicAbility ablility = p.getAbility();
					if(!ablility.equals(this))
						ablility.switchIn(p);
				}
			}
		};

		new IntrinsicAbility("Speed Boost")
		{
			@Override
			public int getTier()
			{
				return 5;
			}

			@Override
			public boolean tick(Pokemon p)
			{
				StatChangeEffect eff = new StatChangeEffect(Pokemon.S_SPEED, true);
				eff.setDescription("'s Speed Boost raised its speed!");
				p.addStatus(p, eff);
				return true;
			}
		};

		new IntrinsicAbility("Sand Veil");
		new IntrinsicAbility("Swift Swim");

		new IntrinsicAbility("Rain Dish")
		{
			@Override
			public int getTier()
			{
				return 2;
			}

			@Override
			public boolean tick(Pokemon p)
			{
				if(!p.hasEffect(RainEffect.class))
					return false;
				int maximum = p.getStat(Pokemon.S_HP);
				int raise = maximum / 16;
				p.getField().showMessage(p.getName() + "'s Rain Dish restored its health a little!");
				p.changeHealth(raise);
				return true;
			}
		};

		new OpponentEffectAbility("Mold Breaker")
		{
			@Override
			public void applyToOpponent(Pokemon owner, Pokemon p)
			{
				IntrinsicAbility ability = p.getAbility();
				if(ability != null && ability.isActive() && m_moldBreaker.contains(ability.getName()))
				{
					// Nullify this ability.
					ability.unapply(p);
					ability.deactivate();
				}
			}

			@Override
			public boolean switchOut(Pokemon p)
			{
				/* This will intefere with Gastro Acid, but whatever; the
				 * interaction is unlikely to come up. */
				Pokemon opponent = p.getOpponent();
				IntrinsicAbility ability = opponent.getAbility();
				if(ability != null && !ability.isActive() && !ability.isRemovable())
				{
					ability.apply(opponent);
					ability.activate();
				}
				return super.switchOut(p);
			}
		};

		new IntrinsicAbility("Wonder Guard")
		{
			private Pokemon m_pokemon;

			@Override
			public boolean apply(Pokemon p)
			{
				m_pokemon = p;
				return true;
			}

			@Override
			public double getEnemyTransformedEffectiveness(PokemonType move, PokemonType target)
			{
				double actual = PokemonMove.getEffectiveness(move, null, m_pokemon);
				if(actual <= 1.0)
					return 0.0;
				return actual;
			}

			@Override
			public boolean isEffectivenessTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			public boolean isSwappable()
			{
				return false;
			}
		};

		new AbsorbAbility("Volt Absorb", PokemonType.T_ELECTRIC);
		new AbsorbAbility("Water Absorb", PokemonType.T_WATER);

		new IntrinsicAbility("Flash Fire")
		{
			private boolean m_boosted = false;

			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return true;
			}

			@Override
			public boolean switchOut(Pokemon p)
			{
				m_boosted = false;
				return super.switchOut(p);
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(!move.getType().equals(PokemonType.T_FIRE))
					return entry;
				BattleField field = p.getField();
				if(!move.isAttack() || !(field.getMechanics() instanceof JewelMechanics))
					return entry;
				field.informUseMove(p.getOpponent(), entry.getName());
				field.showMessage(p.getName() + "'s Flash Fire raised its fire power!");
				m_boosted = true;
				return null;
			}

			@Override
			protected MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(m_boosted && move.getType().equals(PokemonType.T_FIRE))
					move.setPower(move.getPower() * 3 / 2);
				return entry;
			}
		};

		new TrappingAbility("Arena Trap")
		{
			@Override
			public boolean isTrappable(Pokemon p)
			{
				return PokemonMove.getEffectiveness(PokemonType.T_GROUND, p.getOpponent(), p) != 0.0 && !p.hasAbility("Levitate");
			}
		};

		new TrappingAbility("Magnet Pull")
		{
			@Override
			public boolean isTrappable(Pokemon p)
			{
				return p.isType(PokemonType.T_STEEL);
			}
		};

		new TrappingAbility("Shadow Tag");

		new IntrinsicAbility("Oblivious");

		// DP abilities

		new IntrinsicAbility("Anticipation")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				Pokemon target = p.getOpponent();
				for(int i = 0; i < 4; i++)
				{
					MoveListEntry entry = target.getMove(i);
					if(entry == null)
						continue;
					PokemonMove move = entry.getMove();
					if(move.getEffectiveness(p.getOpponent(), p) > 1.0 && move.isAttack())
					{
						p.getField().showMessage(p.getName() + "'s Anticipation made it shudder!");
						return;
					}
				}
			}
		};

		new IntrinsicAbility("Forewarn")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				Pokemon target = p.getOpponent();
				MoveListEntry mostPowerful = null;
				int power = 0;
				for(int i = 0; i < 4; i++)
				{
					MoveListEntry move = target.getMove(i);
					if(move == null)
						continue;
					int movePower = move.getMove().getPower();
					if(movePower > power)
					{
						power = movePower;
						mostPowerful = move;
					}
				}
				if(mostPowerful != null)
					p.getField().showMessage(p.getName() + " foresaw " + mostPowerful.getName() + "!");
			}
		};

		new IntrinsicAbility("Aftermath")
		{
			@Override
			public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
			{
				if(!isPhysical(move))
					return;
				if(target.isFainted())
				{
					source.getField().showMessage(source.getName() + " was hurt by " + target.getName() + "'s Aftermath!");
					int hp = source.getStat(Pokemon.S_HP) / 4;
					if(hp < 1)
						hp = 1;
					source.changeHealth(-hp, true);
				}
			}

			@Override
			public boolean isListener()
			{
				return true;
			}
		};

		new IntrinsicAbility("Bad Dreams")
		{
			@Override
			public int getTier()
			{
				// todo: find the right teir
				return 3;
			}

			@Override
			public boolean tick(Pokemon p)
			{
				Pokemon target = p.getOpponent();
				if(target.hasEffect(SleepEffect.class))
				{
					int damage = target.getStat(Pokemon.S_HP) / 8;
					if(damage < 1)
						damage = 1;
					target.changeHealth(-damage, true);
				}
				return false;
			}
		};

		new IntrinsicAbility("Suction Cups");

		new IntrinsicAbility("Download")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				Pokemon target = p.getOpponent();
				int stat = target.getStat(Pokemon.S_DEFENCE) < target.getStat(Pokemon.S_SPDEFENCE) ? Pokemon.S_ATTACK : Pokemon.S_SPATTACK;
				p.getField().showMessage(p.getName() + "'s Download raised its stats!");
				p.addStatus(p, new StatChangeEffect(stat, true));
			}
		};

		new AbsorbAbility("Dry Skin", PokemonType.T_WATER)
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				if(super.getEnemyTransformedMove(p, entry) == null)
					return null;
				PokemonMove move = entry.getMove();
				if(move.getType().equals(PokemonType.T_FIRE))
					move.setPower(move.getPower() * 5 / 4);
				return entry;
			}
		};

		new IntrinsicAbility("Filter")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getEffectiveness(p, p.getOpponent()) > 1.0)
					move.setPower(move.getPower() * 3 / 4);
				return entry;
			}
		};

		new IntrinsicAbility("Solid Rock")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getEffectiveness(p, p.getOpponent()) > 1.0)
					move.setPower(move.getPower() * 3 / 4);
				return entry;
			}
		};

		new IntrinsicAbility("Frisk")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				Pokemon target = p.getOpponent();
				HoldItem item = target.getItem();
				if(item != null)
					p.getField().showMessage(p.getName() + " found " + target.getName() + "'s " + item.getName() + "!");
			}
		};

		new IntrinsicAbility("Gluttony");
		// Doesn't do anything in battle
		new IntrinsicAbility("Honey Gather");
		// Doesn't do anything in 1 v 1
		new IntrinsicAbility("Storm Drain");
		new IntrinsicAbility("Hyrdation");
		new IntrinsicAbility("Ice Body");
		new IntrinsicAbility("Skill Link");
		new IntrinsicAbility("Poison Heal");
		new IntrinsicAbility("Sniper");
		new IntrinsicAbility("Snow Cloak");
		new IntrinsicAbility("Solar Power");
		new IntrinsicAbility("Tangled Feet");
		new IntrinsicAbility("Steadfast");
		new IntrinsicAbility("Hydration");

		new IntrinsicAbility("Heatproof")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getType().equals(PokemonType.T_FIRE))
					move.setPower(move.getPower() / 2);
				return entry;
			}
		};

		new IntrinsicAbility("Magic Guard");

		new IntrinsicAbility("Iron Fist")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}

			@Override
			protected MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				String[] moves = { "Comet Punch", "Mach Punch", "Bullet Punch", "Thunderpunch", "Ice Punch", "Fire Punch", "Sky Uppercut", "Mega Punch", "Focus Punch", "Drain Punch" };
				String name = entry.getName();
				PokemonMove move = entry.getMove();
				for(int i = 0; i < moves.length; i++)
					if(name.equals(moves[i]))
						move.setPower(move.getPower() * 6 / 5);
				return entry;
			}
		};

		new IntrinsicAbility("Motor Drive")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}

			@Override
			protected MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getType().equals(PokemonType.T_ELECTRIC) && move.isAttack())
				{
					p.getField().informUseMove(p.getOpponent(), entry.getName());
					p.addStatus(p, new StatusEffect()
					{
						@Override
						public boolean apply(Pokemon p)
						{
							p.getMultiplier(Pokemon.S_SPEED).multiplyBy(1.5);
							return true;
						}

						@Override
						public String getDescription()
						{
							return "'s Motor Drive increased its speed!";
						}

						@Override
						public String getName()
						{
							return "Motor Drive";
						}

						@Override
						public int getTier()
						{
							return -1;
						}

						@Override
						public boolean isPassable()
						{
							return false;
						}

						@Override
						public boolean switchOut(Pokemon p)
						{
							p.getMultiplier(Pokemon.S_SPEED).divideBy(1.5);
							return true;
						}

						@Override
						public boolean tick(Pokemon p)
						{
							return false;
						}
					});
					return null;
				}
				return entry;
			}
		};

		new IntrinsicAbility("Normalize")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}

			@Override
			protected MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				entry.getMove().setType(PokemonType.T_NORMAL);
				return entry;
			}
		};

		new IntrinsicAbility("Rivalry")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}

			@Override
			protected MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				@SuppressWarnings("unused")
				Pokemon target = p.getOpponent();
				int g1 = p.getGender();
				int g2 = p.getOpponent().getGender();
				if(g1 == PokemonSpecies.GENDER_NONE || g2 == PokemonSpecies.GENDER_NONE)
					return entry;
				int numerator = 5;
				if(g1 != g2)
					numerator = 3;
				entry.getMove().setPower(entry.getMove().getPower() * numerator / 4);
				return entry;
			}
		};

		new IntrinsicAbility("Snow Warning")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				p.getField().showMessage(p.getName() + "'s Snow Warning started a blizzard!");
				p.getField().applyEffect(new HailEffect(0));
			}
		};

		new IntrinsicAbility("Slow Start")
		{
			private int m_turns = 0;

			@Override
			public String getName()
			{
				return "Slow Start";
			}

			@Override
			public int getTier()
			{
				return 5;
			}

			@Override
			public void switchIn(Pokemon p)
			{
				if(m_turns == 0)
				{
					p.getMultiplier(Pokemon.S_ATTACK).multiplyBy(0.5);
					p.getMultiplier(Pokemon.S_SPEED).multiplyBy(0.5);
				}
				m_turns = 5;
				p.getField().showMessage(p.getName() + " can't get going due to its Slow Start!");
			}

			@Override
			public boolean tick(Pokemon p)
			{
				if(--m_turns == 0)
				{
					p.getMultiplier(Pokemon.S_ATTACK).multiplyBy(2.0);
					p.getMultiplier(Pokemon.S_SPEED).multiplyBy(2.0);
					p.getField().showMessage(p.getName() + " finally got its act together!");
				}
				return false;
			}
		};

		new IntrinsicAbility("Scrappy")
		{
			@Override
			public double getTransformedEffectiveness(PokemonType move, PokemonType defender)
			{
				if(defender.equals(PokemonType.T_GHOST))
					if(move.equals(PokemonType.T_NORMAL) || move.equals(PokemonType.T_FIGHTING))
						return 1.0;
				return super.getTransformedEffectiveness(move, defender);
			}

			@Override
			public boolean isEffectivenessTransformer(boolean enemy)
			{
				return !enemy;
			}
		};

		new IntrinsicAbility("Technician")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}

			@Override
			protected MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getPower() <= 60)
					entry.getMove().setPower(move.getPower() * 3 / 2);
				return entry;
			}
		};

		new IntrinsicAbility("Tinted Lens")
		{
			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}

			@Override
			protected MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getEffectiveness(p, p.getOpponent()) < 1.0)
					move.setPower(move.getPower() * 2);
				return entry;
			}
		};

		new IntrinsicAbility("Klutz")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				HoldItem item = p.getItem();
				if(item != null && item.isActive())
				{
					item.unapply(p);
					item.deactivate();
				}
				return true;
			}

			@Override
			public void unapply(Pokemon p)
			{
				HoldItem item = p.getItem();
				if(item != null && !item.isRemovable())
				{
					item.activate();
					item.apply(p);
				}
			}
		};

		new IntrinsicAbility("Reckless")
		{
			@Override
			public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move instanceof RecoilMove)
					move.setPower((int) (move.getPower() * 1.2));
				return entry;
			}

			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}
		};

		new StatusReactionAbility("Guts", Pokemon.S_ATTACK);
		new StatusReactionAbility("Quick Feet", Pokemon.S_SPEED);
		new StatusReactionAbility("Marvel Scale", Pokemon.S_DEFENCE);

		new IntrinsicAbility("Multitype")
		{
			@Override
			public boolean isSwappable()
			{
				return false;
			}
		};
		new IntrinsicAbility("Super Luck");
		new IntrinsicAbility("Adaptability");
		new IntrinsicAbility("No Guard");
		new SynchronizeAbility();
		new IntrinsicAbility("Flower Gift");
		new IntrinsicAbility("Anger Point");
		new AirLockAbility("Air Lock");
		new AirLockAbility("Cloud Nine");
		new UnburdenAbility();

		new IntrinsicAbility("Soundproof")
		{
			@Override
			public MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
			{
				Set<String> set = new HashSet<String>(Arrays.asList(new String[] { "Grasswhistle", "Growl", "Hyper Voice", "Metal Sound", "Roar", "Sing", "Supersonic", "Screech", "Snore", "Uproar" }));
				String name = entry.getName();
				if(set.contains(name))
				{
					BattleField field = p.getField();
					field.informUseMove(p, name);
					field.showMessage("But it failed!");
					return null;
				}
				return entry;
			}

			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return enemy;
			}
		};

		m_map.put("Cacophony", getInstance("Soundproof"));

		new IntrinsicAbility("Leaf Guard")
		{
			@Override
			public boolean allowsStatus(StatusEffect eff, Pokemon source, Pokemon target)
			{
				if(source == null || target == null)
					return true;
				BattleField field = source.getField();
				if(field == null)
					return true;
				if(field.getEffectByType(SunEffect.class) == null)
					return true;
				if(source == target)
					return true;
				return !(eff instanceof BurnEffect || eff instanceof FreezeEffect || eff instanceof ParalysisEffect || eff instanceof PoisonEffect || eff instanceof SleepEffect || eff instanceof ConfuseEffect);
			}
		};
	}

	public static Set<String> getAbilityNames()
	{
		Set<String> result = new HashSet<String>();
		Iterator<String> it = m_physical.iterator();
		while(it.hasNext())
			result.add(it.next());
		it = m_moldBreaker.iterator();
		while(it.hasNext())
			result.add(it.next());
		it = m_map.keySet().iterator();
		while(it.hasNext())
			result.add(it.next());
		return result;
	}

	/**
	 * Get an intrinsic ability by name.
	 */
	public static IntrinsicAbility getInstance(String ability)
	{
		Iterator<String> it = m_map.keySet().iterator();
		while(it.hasNext())
		{
			String a = it.next();
			if(a.equalsIgnoreCase(ability))
				return m_map.get(a);
		}
		return null;
	}

	/**
	 * Return whether a move is a physical attack.
	 */
	public static boolean isPhysical(MoveListEntry entry)
	{
		return m_physical.contains(entry.getName());
	}

	@Override
	public boolean apply(Pokemon p)
	{
		return true;
	}

	/**
	 * Compare this object to another IntrinsicAbility.
	 */
	public int compareTo(Object obj)
	{
		IntrinsicAbility ability = (IntrinsicAbility) obj;
		return m_name.compareTo(ability.m_name);
	}

	/**
	 * Returns if two abilities are the same based on name.
	 * 
	 * @param obj
	 * @return True when both objects are equal, otherwise false.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if(!super.equals(obj))
			return false;
		IntrinsicAbility ability = (IntrinsicAbility) obj;
		return ability.m_name.equals(m_name);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
		return result;
	}

	/**
	 * Intrinsic abilities do not employ descriptions.
	 */
	@Override
	public String getDescription()
	{
		return null;
	}

	/**
	 * Get this ability's name.
	 */
	@Override
	public String getName()
	{
		return m_name;
	}

	/**
	 * Because they do not tick, intrinsic abilities do not need a tier.
	 */
	@Override
	public int getTier()
	{
		return -1;
	}

	/**
	 * Intrinsic abilities generally do not immobilise.
	 */
	@Override
	public boolean immobilises(Pokemon p)
	{
		return false;
	}

	/**
	 * Returns whether an opponent claims the effect of this intrinsic
	 * ability after taking it by way of a move (etc.).
	 */
	public boolean isEffectTransferrable()
	{
		return true;
	}

	/**
	 * Return whether this ability works with role play and skill swap.
	 */
	public boolean isSwappable()
	{
		return true;
	}

	@Override
	public boolean switchOut(Pokemon p)
	{
		return false;
	}

	/**
	 * Intrinsic abilities generally do not tick.
	 */
	@Override
	public boolean tick(Pokemon p)
	{
		return false;
	}

	@Override
	public void unapply(Pokemon p)
	{

	}

	/**
	 * Register an intrinsic ability.
	 */
	protected void registerAbility()
	{
		m_map.put(m_name, this);
	}
}

/**
 * Implementation of Guts: boosts attack whilst a status effect is present.
 */
class StatusReactionAbility extends IntrinsicAbility implements StatusListener
{
	private int m_stat;

	public StatusReactionAbility(String name, int stat)
	{
		super(name);
		m_stat = stat;
	}

	public void informStatusApplied(Pokemon source, Pokemon poke, StatusEffect eff)
	{
		if(eff.getLock() == StatusEffect.SPECIAL_EFFECT_LOCK)
			poke.getMultiplier(m_stat).increaseMultiplier();
	}

	public void informStatusRemoved(Pokemon poke, StatusEffect eff)
	{
		if(eff.getLock() == StatusEffect.SPECIAL_EFFECT_LOCK)
			poke.getMultiplier(m_stat).decreaseMultiplier();
	}
}

/**
 * Implementation of Synchronize: applies status to enemy that is
 * inflicted on user.
 */
class SynchronizeAbility extends IntrinsicAbility implements StatusListener
{
	public SynchronizeAbility()
	{
		super("Synchronize");
	}

	public void informStatusApplied(Pokemon source, Pokemon poke, StatusEffect eff)
	{
		if(eff.getLock() == StatusEffect.SPECIAL_EFFECT_LOCK)
		{
			if(eff instanceof SleepEffect || eff instanceof FreezeEffect)
				return;
			// Synchronize requires that the opponent applied the effect.
			if(source != poke && source != null)
				source.addStatus(poke, eff);
		}

	}

	public void informStatusRemoved(Pokemon poke, StatusEffect eff)
	{

	}
}
