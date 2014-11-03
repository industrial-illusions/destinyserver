/* HoldItem.java
 * Created on February 14, 2007, 9:16 PM
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

package org.destiny.server.battle.mechanics.statuses.items;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.destiny.server.battle.BattleField;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.mechanics.JewelMechanics;
import org.destiny.server.battle.mechanics.PokemonType;
import org.destiny.server.battle.mechanics.moves.MoveList;
import org.destiny.server.battle.mechanics.moves.MoveListEntry;
import org.destiny.server.battle.mechanics.moves.PokemonMove;
import org.destiny.server.battle.mechanics.statuses.BurnEffect;
import org.destiny.server.battle.mechanics.statuses.ConfuseEffect;
import org.destiny.server.battle.mechanics.statuses.FreezeEffect;
import org.destiny.server.battle.mechanics.statuses.ParalysisEffect;
import org.destiny.server.battle.mechanics.statuses.PoisonEffect;
import org.destiny.server.battle.mechanics.statuses.SleepEffect;
import org.destiny.server.battle.mechanics.statuses.StatChangeEffect;
import org.destiny.server.battle.mechanics.statuses.StatusEffect;
import org.destiny.server.battle.mechanics.statuses.StatusListener;
import org.destiny.server.battle.mechanics.statuses.ToxicEffect;
import org.destiny.server.battle.mechanics.statuses.abilities.IntrinsicAbility;
import org.destiny.server.battle.mechanics.statuses.field.FieldEffect;

/**
 * This class implements an item that can be held by a pokemon during battle.
 * These type of classes basically provide the same functionality as intrinsic
 * abilities, so the class inherits from IntrinsicAbility, even though the
 * semantic value of this is questionable.
 * 
 * @author Colin
 */
public class HoldItem extends IntrinsicAbility
{

	private static HoldItemData m_default = new HoldItemData();

	public HoldItem(boolean register, String name)
	{
		super(register, name);
	}

	/** Creates a new instance of HoldItem */
	public HoldItem(String name)
	{
		super(name);
	}

	/**
	 * Initialise a HoldItem that can be used only by certain named pokemon.
	 */
	public HoldItem(String name, String[] pokemon)
	{
		super(false, name);
		for(int i = 0; i < pokemon.length; ++i)
			m_default.addExclusiveItem(name, pokemon[i]);
	}

	static
	{
		new TypeBoostItem("Black Belt", PokemonType.T_FIGHTING);
		new TypeBoostItem("BlackGlasses", PokemonType.T_DARK);
		new TypeBoostItem("Charcoal", PokemonType.T_FIRE);
		new TypeBoostItem("Dragon Fang", PokemonType.T_DRAGON);
		new TypeBoostItem("Hard Stone", PokemonType.T_ROCK);
		new TypeBoostItem("Magnet", PokemonType.T_ELECTRIC);
		new TypeBoostItem("Metal Coat", PokemonType.T_STEEL);
		new TypeBoostItem("Miracle Seed", PokemonType.T_GRASS);
		new TypeBoostItem("Mystic Water", PokemonType.T_WATER);
		new TypeBoostItem("Nevermeltice", PokemonType.T_ICE);
		new TypeBoostItem("Poison Barb", PokemonType.T_POISON);
		new TypeBoostItem("Sharp Beak", PokemonType.T_FLYING);
		new TypeBoostItem("Silk Scarf", PokemonType.T_NORMAL);
		new TypeBoostItem("Silverpowder", PokemonType.T_BUG);
		new TypeBoostItem("Soft Sand", PokemonType.T_GROUND);
		new TypeBoostItem("Spell Tag", PokemonType.T_GHOST);
		new TypeBoostItem("Twisted Spoon", PokemonType.T_PSYCHIC);

		new StatBoostItem("Liechi Berry", Pokemon.S_ATTACK);
		new StatBoostItem("Ganlon Berry", Pokemon.S_DEFENCE);
		new StatBoostItem("Salac Berry", Pokemon.S_SPEED);
		new StatBoostItem("Petaya Berry", Pokemon.S_SPATTACK);
		new StatBoostItem("Apicot Berry", Pokemon.S_SPDEFENCE);
		// new StatBoostItem("Lansat Berry", Pokemon.S_CRITICAL_HITS);
		new StatBoostItem("Starf Berry", -1)
		{
			@Override
			public int getStages()
			{
				return 2;
			}

			@Override
			public int getStat(Pokemon p)
			{
				Random random = p.getField().getMechanics().getRandom();
				return random.nextInt(5) + 1;
			}
		};

		new HealthBoostItem("Figy Berry", Pokemon.S_ATTACK);
		new HealthBoostItem("Wiki Berry", Pokemon.S_SPATTACK);
		new HealthBoostItem("Mago Berry", Pokemon.S_SPEED);
		new HealthBoostItem("Aguav Berry", Pokemon.S_SPDEFENCE);
		new HealthBoostItem("Iapapa Berry", Pokemon.S_DEFENCE);
		new ConstantHealthBoostItem("Oran Berry", 10);
		new ConstantHealthBoostItem("Sitrus Berry", 30);

		new StatusCureItem("Aspear Berry", FreezeEffect.class);
		new StatusCureItem("Cheri Berry", ParalysisEffect.class);
		new StatusCureItem("Chesto Berry", SleepEffect.class);
		// Note: self-referential just for fun; parameter is unused.
		new StatusCureItem("Lum Berry", Class.class)
		{
			@Override
			public boolean isCurable(StatusEffect eff)
			{
				return eff.getLock() == StatusEffect.SPECIAL_EFFECT_LOCK || eff instanceof ConfuseEffect;
			}
		};
		new StatusCureItem("Pecha Berry", PoisonEffect.class);
		new StatusCureItem("Persim Berry", ConfuseEffect.class);
		new StatusCureItem("Rawst Berry", BurnEffect.class);
		new StatusCureItem("Mental Herb", MoveList.AttractEffect.class);
		new WhiteHerbItem();

		new HoldItem("Leftovers")
		{
			@Override
			public int getTier()
			{
				return 3;
			}

			@Override
			public boolean tick(Pokemon p)
			{
				int max = p.getStat(Pokemon.S_HP);
				if(p.getHealth() < max)
				{
					int restore = max / 16;
					p.getField().showMessage(p.getName() + "'s leftovers restored its health a little!");
					p.changeHealth(restore);
				}
				return false;
			}
		};

		new ChoiceBandItem("Choice Band", Pokemon.S_ATTACK);

		new HoldItem("Thick Club")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				if(isSuitable(p))
					p.getMultiplier(Pokemon.S_ATTACK).multiplyBy(2.0);
				return true;
			}

			public boolean isSuitable(Pokemon p)
			{
				String name = p.getSpeciesName();
				return name.equals("Cubone") || name.equals("Marowak");
			}

			@Override
			public void unapply(Pokemon p)
			{
				if(isSuitable(p))
					p.getMultiplier(Pokemon.S_ATTACK).divideBy(2.0);
			}
		};

		new HoldItem("Metal Powder")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				if(isSuitable(p))
				{
					// TODO: these effects should be removed when transformed
					p.getMultiplier(Pokemon.S_DEFENCE).multiplyBy(2.0);
					p.getMultiplier(Pokemon.S_SPDEFENCE).multiplyBy(2.0);
				}
				return true;
			}

			public boolean isSuitable(Pokemon p)
			{
				return p.getSpeciesName().equals("Ditto");
			}

			@Override
			public void unapply(Pokemon p)
			{
				if(isSuitable(p))
				{
					p.getMultiplier(Pokemon.S_DEFENCE).multiplyBy(2.0);
					p.getMultiplier(Pokemon.S_SPDEFENCE).multiplyBy(2.0);
				}
			}
		};

		/** Diamond/Pearl-exclusive items begin here. */
		new ChoiceBandItem("Choice Specs", Pokemon.S_SPATTACK);
		// Arceus plates.
		new ArceusPlate("Draco Plate", PokemonType.T_DRAGON);
		new ArceusPlate("Dread Plate", PokemonType.T_DARK);
		new ArceusPlate("Earth Plate", PokemonType.T_GROUND);
		new ArceusPlate("Flame Plate", PokemonType.T_FIRE);
		new ArceusPlate("Fist Plate", PokemonType.T_FIGHTING);
		new ArceusPlate("Icicle Plate", PokemonType.T_ICE);
		new ArceusPlate("Insect Plate", PokemonType.T_BUG);
		new ArceusPlate("Iron Plate", PokemonType.T_STEEL);
		new ArceusPlate("Meadow Plate", PokemonType.T_GRASS);
		new ArceusPlate("Mind Plate", PokemonType.T_PSYCHIC);
		new ArceusPlate("Sky Plate", PokemonType.T_FLYING);
		new ArceusPlate("Splash Plate", PokemonType.T_WATER);
		new ArceusPlate("Stone Plate", PokemonType.T_ROCK);
		new ArceusPlate("Toxic Plate", PokemonType.T_POISON);
		new ArceusPlate("Zap Plate", PokemonType.T_ELECTRIC);
		/* TODO: Full Incense. What is meant and what is the actual Todo? */
		new SwitchInBoostItem("Lax Incense", Pokemon.S_EVASION, 1.05);
		new HoldItem("Luck Incense"); // Does nothing.
		new HoldItem("Pure Incense"); // Does nothing.
		new TypeBoostItem("Odd Incense", PokemonType.T_PSYCHIC);
		new TypeBoostItem("Rock Incense", PokemonType.T_ROCK);
		new TypeBoostItem("Rose Incense", PokemonType.T_GRASS);
		new TypeBoostItem("Sea Incense", PokemonType.T_WATER, 1.05);
		new TypeBoostItem("Wave Incense", PokemonType.T_WATER);
		new StabOrbItem("Adamant Orb", "Dialga");
		new StabOrbItem("Lustrous Orb", "Palkia");
		new HoldItem("Black Sludge")
		{
			@Override
			public int getTier()
			{
				return 3;
			}

			@Override
			public boolean tick(Pokemon p)
			{
				int max = p.getStat(Pokemon.S_HP);
				int delta = max / 16;
				boolean damage = !p.isType(PokemonType.T_POISON);
				if(damage)
				{
					p.getField().showMessage(p.getName() + " was damaged by Black Sludge!");
					p.changeHealth(-delta, true);
				}
				else if(p.getHealth() < max)
				{
					p.getField().showMessage(p.getName() + "'s Black Sludge restored a little health!");
					p.changeHealth(delta);
				}
				return false;
			}
		};

		new HoldItem("Blue Scarf"); // Does nothing.
		new SwitchInBoostItem("Brightpowder", Pokemon.S_EVASION, 1.10);

		new SpecialnessBoostItem("Muscle Band", false);

		new DestinyKnotItem();
		new HoldItem("Scope Lens");

		new HoldItem("Life Orb")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				p.getField().applyEffect(new FieldEffect()
				{
					private boolean[] m_damaged = new boolean[2];

					@Override
					public boolean apply(Pokemon p)
					{
						m_damaged[p.getParty()] = false;
						return true;
					}

					@Override
					public boolean applyToField(BattleField field)
					{
						return true;
					}

					@Override
					public void beginTick()
					{
						super.beginTick();
						Arrays.fill(m_damaged, false);
					}

					@Override
					public String getDescription()
					{
						return null;
					}

					@Override
					public String getName()
					{
						return null;
					}

					@Override
					public int getTier()
					{
						return -1;
					}

					@Override
					public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
					{
						if(p.getItemName().equals("Life Orb"))
						{
							PokemonMove move = entry.getMove();
							move.setPower((int) (move.getPower() * 1.3));
						}
						return entry;
					}

					@Override
					public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
					{
						if(source.getItemName().equals("Life Orb"))
						{
							int idx = source.getParty();
							if(!m_damaged[idx])
							{
								m_damaged[idx] = true;
								source.changeHealth(-source.getStat(Pokemon.S_HP) / 10, true);
							}
						}
					}

					@Override
					public boolean isListener()
					{
						return true;
					}

					@Override
					public boolean isMoveTransformer(boolean enemy)
					{
						return !enemy;
					}

					@Override
					public boolean tickField(BattleField field)
					{
						return false;
					}
				});
			}
		};

		new HoldItem("Light Ball")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				modifyStats(p, true);
				return super.apply(p);
			}

			public void modifyStats(Pokemon p, boolean apply)
			{
				if(!p.getSpeciesName().equals("Pikachu"))
					return;
				BattleField field = p.getField();
				boolean dp = field != null && field.getMechanics() instanceof JewelMechanics;
				if(apply)
				{
					if(dp)
						p.getMultiplier(Pokemon.S_ATTACK).multiplyBy(2.0);
					p.getMultiplier(Pokemon.S_SPATTACK).multiplyBy(2.0);
				}
				else
				{
					if(dp)
						p.getMultiplier(Pokemon.S_ATTACK).divideBy(2.0);
					p.getMultiplier(Pokemon.S_SPATTACK).divideBy(2.0);
				}
			}

			@Override
			public void unapply(Pokemon p)
			{
				modifyStats(p, false);
			}
		};

		new HoldItem("Wide Lens")
		{
			@Override
			public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				move.setAccuracy(move.getAccuracy() * 1.1);
				return entry;
			}

			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}
		};

		new HoldItem("Expert Belt")
		{
			@Override
			public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				PokemonMove move = entry.getMove();
				if(move.getEffectiveness(p, p.getOpponent()) > 1.0)
					move.setPower((int) (move.getPower() * 1.2));
				return entry;
			}

			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}
		};

		new SpecialnessBoostItem("Wise Glasses", true);

		new ChoiceBandItem("Choice Scarf", Pokemon.S_SPEED);

		new SpecialEffectItem("Toxic Orb", new ToxicEffect());
		new SpecialEffectItem("Flame Orb", new BurnEffect());

		new EffectiveMoveWeakener("Occa Berry", PokemonType.T_FIRE);
		new EffectiveMoveWeakener("Passho Berry", PokemonType.T_WATER);
		new EffectiveMoveWeakener("Wacan Berry", PokemonType.T_ELECTRIC);
		new EffectiveMoveWeakener("Rindo Berry", PokemonType.T_GRASS);
		new EffectiveMoveWeakener("Yache Berry", PokemonType.T_ICE);
		new EffectiveMoveWeakener("Chople Berry", PokemonType.T_FIGHTING);
		new EffectiveMoveWeakener("Kebia Berry", PokemonType.T_POISON);
		new EffectiveMoveWeakener("Shuca Berry", PokemonType.T_GROUND);
		new EffectiveMoveWeakener("Coba Berry", PokemonType.T_FLYING);
		new EffectiveMoveWeakener("Payapa Berry", PokemonType.T_PSYCHIC);
		new EffectiveMoveWeakener("Tanga Berry", PokemonType.T_BUG);
		new EffectiveMoveWeakener("Charti Berry", PokemonType.T_ROCK);
		new EffectiveMoveWeakener("Kasib Berry", PokemonType.T_GHOST);
		new EffectiveMoveWeakener("Haban Berry", PokemonType.T_DRAGON);
		new EffectiveMoveWeakener("Colbur Berry", PokemonType.T_DARK);
		new EffectiveMoveWeakener("Babiri Berry", PokemonType.T_STEEL);
		new EffectiveMoveWeakener("Chilan Berry", PokemonType.T_NORMAL);

		new HoldItem("Soul Dew")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				if(isSuitable(p))
				{
					p.getMultiplier(Pokemon.S_SPATTACK).multiplyBy(1.5);
					p.getMultiplier(Pokemon.S_SPDEFENCE).multiplyBy(1.5);
				}
				return true;
			}

			public boolean isSuitable(Pokemon p)
			{
				String name = p.getSpeciesName();
				return name.equals("Latios") || name.equals("Latias");
			}

			@Override
			public void unapply(Pokemon p)
			{
				if(isSuitable(p))
				{
					p.getMultiplier(Pokemon.S_SPATTACK).divideBy(1.5);
					p.getMultiplier(Pokemon.S_SPDEFENCE).divideBy(1.5);
				}
			}
		};

		/**
		 * The actual logic for these two is in shoddybattle.Pokemon
		 * It is a bit ugly, but oh well.
		 */
		new HoldItem("Focus Sash");
		new HoldItem("Focus Band");

		// The implementation of these is in WeatherMove.
		new HoldItem("Heat Rock");
		new HoldItem("Damp Rock");
		new HoldItem("Icy Rock");
		new HoldItem("Smooth Rock");

		// The implementation for this is in MoveList.TrappingEffect.
		new HoldItem("Shed Shell");

		/**
		 * The implementation for this is in the stupidly named
		 * MoveList.StatCutEffect class.
		 */
		new HoldItem("Light Clay");

		new DeepSeaItem("Deepseatooth", Pokemon.S_SPATTACK);
		new DeepSeaItem("Deepseascale", Pokemon.S_SPDEFENCE);

		new HoldItem("Metronome")
		{
			private MoveListEntry m_choice = null;
			private int m_level = 0;

			@Override
			public boolean apply(Pokemon p)
			{
				m_choice = null;
				return true;
			}

			@Override
			public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
			{
				if(m_choice == null)
				{
					m_choice = entry;
					m_level = 0;
				}
				else if(entry.equals(m_choice))
					++m_level;
				else
				{
					m_choice = null;
					return entry;
				}
				PokemonMove move = entry.getMove();
				move.setPower(move.getPower() * (10 + m_level) / 10);
				return entry;
			}

			@Override
			public boolean isMoveTransformer(boolean enemy)
			{
				return !enemy;
			}

			@Override
			public void switchIn(Pokemon p)
			{
				m_choice = null;
			}
		};

		new HoldItem("Macho Brace")
		{
			@Override
			public boolean apply(Pokemon p)
			{
				p.getMultiplier(Pokemon.S_SPEED).divideBy(2.0);
				return super.apply(p);
			}

			@Override
			public void unapply(Pokemon p)
			{
				p.getMultiplier(Pokemon.S_SPEED).multiplyBy(2.0);
			}
		};

		new HoldItem("Shell Bell")
		{
			@Override
			public void switchIn(Pokemon p)
			{
				p.getField().applyEffect(new FieldEffect()
				{
					private boolean[] m_damaged = new boolean[2];

					@Override
					public boolean apply(Pokemon p)
					{
						m_damaged[p.getParty()] = false;
						return true;
					}

					@Override
					public boolean applyToField(BattleField field)
					{
						return true;
					}

					@Override
					public void beginTick()
					{
						super.beginTick();
						Arrays.fill(m_damaged, false);
					}

					@Override
					public String getDescription()
					{
						return null;
					}

					@Override
					public String getName()
					{
						return null;
					}

					@Override
					public int getTier()
					{
						return -1;
					}

					@Override
					public void informDamaged(Pokemon source, Pokemon target, MoveListEntry move, int damage)
					{
						if(source.getItemName().equals("Shell Bell"))
						{
							int idx = source.getParty();
							if(!m_damaged[idx])
							{
								m_damaged[idx] = true;
								source.getField().showMessage(source.getName() + "'s Shell Bell " + "restored a little health!");
								int change = damage / 8;
								if(change < 1)
									change = 1;
								source.changeHealth(change);
							}
						}
					}

					@Override
					public boolean isListener()
					{
						return true;
					}

					@Override
					public boolean tickField(BattleField field)
					{
						return false;
					}
				});
			}
		};
	}

	/**
	 * Return the default item data.
	 */
	public static HoldItemData getDefaultData()
	{
		return m_default;
	}

	@Override
	protected void registerAbility()
	{
		super.registerAbility();
		m_default.m_items.add(getName());
	}

}

/**
 * An Arceus plate: boosts the type of one move, and also changes Arceus to
 * said type.
 * 
 * @author Colin
 */
class ArceusPlate extends HoldItem
{
	private double m_factor = 1.1;
	private PokemonType[] m_oldType;
	private PokemonType m_type;

	public ArceusPlate(String name, PokemonType type)
	{
		super(name);
		m_type = type;
	}

	@Override
	public boolean apply(Pokemon p)
	{
		if(p.hasAbility("Multitype"))
		{
			m_oldType = p.getTypes();
			p.setType(new PokemonType[] { m_type });
		}
		return true;
	}

	@Override
	public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
	{
		PokemonMove move = entry.getMove();
		if(move.getType().equals(m_type))
			move.setPower((int) (move.getPower() * m_factor));
		if(entry.getName().equals("Judgment"))
			move.setType(m_type);
		return entry;
	}

	@Override
	public boolean isMoveTransformer(boolean enemy)
	{
		return !enemy;
	}

	@Override
	public void switchIn(Pokemon p)
	{
		BattleField field = p.getField();
		if(p.hasAbility("Multitype") && field != null)
			field.showMessage("The foe's " + p.getName() + " transformed into the " + m_type + " type!");
	}

	@Override
	public void unapply(Pokemon p)
	{
		if(p.hasAbility("Multitype"))
			p.setType(m_oldType);
	}

}

/**
 * A berry that restores a fixed amount of health.
 */
class ConstantHealthBoostItem extends HoldItem implements Berry
{
	private int m_change;

	public ConstantHealthBoostItem(String name, int change)
	{
		super(name);
		m_change = change;
	}

	public void executeEffects(Pokemon p)
	{
		p.getField().showMessage(p.getName() + "'s" + getName() + " restored health!");
		p.changeHealth(m_change);
	}

	@Override
	public void informDamaged(Pokemon source, Pokemon target, MoveListEntry entry, int damage)
	{
		if(target.getHealth() <= target.getStat(Pokemon.S_HP) / 2)
		{
			executeEffects(target);
			target.removeStatus(this);
		}
	}

	@Override
	public boolean isListener()
	{
		return true;
	}
}

/* Raises one of Clamperl's stats. */
class DeepSeaItem extends HoldItem
{
	private int m_stat;

	public DeepSeaItem(String name, int stat)
	{
		super(name);
		m_stat = stat;
	}

	@Override
	public boolean apply(Pokemon p)
	{
		if(isSuitable(p))
			p.getMultiplier(m_stat).multiplyBy(2.0);
		return super.apply(p);
	}

	public boolean isSuitable(Pokemon p)
	{
		return p.getSpeciesName().equals("Clamperl");
	}

	@Override
	public void unapply(Pokemon p)
	{
		if(isSuitable(p))
			p.getMultiplier(m_stat).divideBy(2.0);
		super.unapply(p);
	}
}

/**
 * Destiny Knot: If attract is applied, foe is also attracted.
 */
class DestinyKnotItem extends HoldItem implements StatusListener
{
	public DestinyKnotItem()
	{
		super("Destiny Knot");
	}

	public void informStatusApplied(Pokemon source, Pokemon p, StatusEffect eff)
	{
		if(eff instanceof MoveList.AttractEffect && source != null)
			source.addStatus(p, eff);
	}

	public void informStatusRemoved(Pokemon p, StatusEffect eff)
	{

	}
}

/**
 * Weakens the super-effective moves of one type used against the holder.
 */
class EffectiveMoveWeakener extends HoldItem implements Berry
{
	private boolean m_suitable;
	private PokemonType m_type;

	public EffectiveMoveWeakener(String name, PokemonType type)
	{
		super(name);
		m_type = type;
	}

	public void executeEffects(Pokemon p)
	{
		// Activating this wouldn't make very much sense.
	}

	@Override
	public MoveListEntry getEnemyTransformedMove(Pokemon p, MoveListEntry entry)
	{
		PokemonMove move = entry.getMove();
		m_suitable = isAppropriateMove(p, move) && !p.hasSubstitute();
		if(m_suitable)
			move.setPower((int) (move.getPower() * 0.5));
		return entry;
	}

	@Override
	public void informDamaged(Pokemon source, Pokemon target, MoveListEntry entry, int damage)
	{
		@SuppressWarnings("unused")
		PokemonMove move = entry.getMove();
		if(m_suitable)
		{
			target.getField().showMessage("The " + getName() + " weakened " + entry.getName() + "'s power!");
			target.removeStatus(this);
		}
	}

	@Override
	public boolean isListener()
	{
		return true;
	}

	@Override
	public boolean isMoveTransformer(boolean enemy)
	{
		return enemy;
	}

	private boolean isAppropriateMove(Pokemon p, PokemonMove move)
	{
		return (PokemonType.T_NORMAL.equals(m_type) || move.getEffectiveness(p.getOpponent(), p) > 1.0) && move.getType().equals(m_type) && move.isDamaging();
	}
}

/**
 * A hold item that restores health so long as the holder does not have a
 * particular kind of nature; if he does then the item also confuses.
 */
class HealthBoostItem extends HoldItem implements Berry
{
	private int m_stat;

	public HealthBoostItem(String name, int stat)
	{
		super(name);
		m_stat = stat;
	}

	public void executeEffects(Pokemon p)
	{
		p.getField().showMessage(p.getName() + "'s " + getName() + " restored its health a little!");

		// Restore a little health.
		p.changeHealth(p.getStat(Pokemon.S_HP) / 8);

		// Check if the pokemon hates the berry.
		if(p.getNature().getEffect(m_stat) < 1.0)
		{
			// Confuse the pokemon if it hated it.
			p.getField().showMessage("The berry was the wrong flavour for " + p.getName() + "!");
			p.addStatus(p, new ConfuseEffect());
		}
	}

	@Override
	public int getTier()
	{
		return 3;
	}

	@Override
	public boolean tick(Pokemon p)
	{
		int hp = p.getHealth();
		int max = p.getStat(Pokemon.S_HP);
		if(hp * 2 <= max)
		{
			executeEffects(p);
			p.removeStatus(this);
			return true;
		}
		return false;
	}
}

/**
 * Item that applies a special effect to the user.
 */
class SpecialEffectItem extends HoldItem
{
	private StatusEffect m_effect;

	public SpecialEffectItem(String name, StatusEffect effect)
	{
		super(name);
		m_effect = effect;
	}

	@Override
	public int getTier()
	{
		return 3;
	}

	@Override
	public boolean tick(Pokemon p)
	{
		if(!p.hasEffect(StatusEffect.SPECIAL_EFFECT_LOCK))
			p.addStatus(p, m_effect);
		return false;
	}

}

/**
 * Raise the damage done by one type of move (physical/special).
 */
class SpecialnessBoostItem extends HoldItem
{
	private boolean m_special;

	public SpecialnessBoostItem(String name, boolean special)
	{
		super(name);
		m_special = special;
	}

	@Override
	public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
	{
		PokemonMove move = entry.getMove();
		if(move.isSpecial(p.getField().getMechanics()) == m_special)
			move.setPower((int) (move.getPower() * 1.1));
		return entry;
	}

	@Override
	public boolean isMoveTransformer(boolean enemy)
	{
		return !enemy;
	}
}

/**
 * Raises the power of STAB moves for a particular pokemon.
 */
class StabOrbItem extends HoldItem
{
	private String m_pokemon;

	public StabOrbItem(String name, String pokemon)
	{
		super(name);
		m_pokemon = pokemon;
	}

	@Override
	public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
	{
		if(!p.getSpeciesName().equals(m_pokemon))
			// No effect for Pokemon other than the specified one.
			return entry;
		PokemonMove move = entry.getMove();
		PokemonType type = move.getType();
		if(p.isType(type))
			move.setPower((int) (move.getPower() * 1.2));
		return entry;
	}

	@Override
	public boolean isMoveTransformer(boolean enemy)
	{
		return !enemy;
	}
}

/**
 * A hold item that boosts a stat once health falls below 25%.
 */
class StatBoostItem extends HoldItem implements Berry
{
	private int m_stat;

	public StatBoostItem(String name, int stat)
	{
		super(name);
		m_stat = stat;
	}

	public void executeEffects(Pokemon p)
	{
		final int stat = getStat(p);
		final int stages = getStages();
		StatChangeEffect eff = new StatChangeEffect(stat, true, stages);
		eff.setDescription("'s " + getName() + (stages > 1 ? " sharply " : "") + " raised its " + Pokemon.getStatName(stat) + "!");
		p.addStatus(p, eff);
	}

	public int getStages()
	{
		return 1;
	}

	public int getStat(Pokemon p)
	{
		return m_stat;
	}

	@Override
	public int getTier()
	{
		return 3;
	}

	@Override
	public boolean tick(Pokemon p)
	{
		int hp = p.getHealth();
		int max = p.getStat(Pokemon.S_HP);
		if(hp * 4 <= max || p.hasAbility("Gluttony") && hp * 2 <= max)
		{
			// Boost the stat.
			executeEffects(p);
			p.removeStatus(this);
			return true;
		}
		return false;
	}
}

/**
 * A hold item that cures a status effect.
 */
class StatusCureItem extends HoldItem implements StatusListener, Berry
{
	private Class<?> m_effect;

	public StatusCureItem(String name, Class<?> eff)
	{
		super(name);
		m_effect = eff;
	}

	public void cureEffect(Pokemon p, StatusEffect eff)
	{
		p.removeStatus(eff);
		p.removeStatus(this);
		displayCureMessage(p, eff);
	}

	public void displayCureMessage(Pokemon p, StatusEffect eff)
	{
		p.getField().showMessage(p.getName() + "'s " + getName() + " cured its " + eff.getName() + "!");
	}

	public void executeEffects(Pokemon p)
	{
		List<StatusEffect> statuses = p.getNormalStatuses(StatusEffect.SPECIAL_EFFECT_LOCK);
		Iterator<StatusEffect> i = statuses.iterator();
		while(i.hasNext())
		{
			StatusEffect effect = i.next();
			if(isCurable(effect))
				cureEffect(p, effect);
		}
	}

	public void informStatusApplied(Pokemon source, Pokemon p, StatusEffect eff)
	{
		if(isCurable(eff))
			cureEffect(p, eff);
	}

	public void informStatusRemoved(Pokemon p, StatusEffect eff)
	{

	}

	public boolean isCurable(StatusEffect eff)
	{
		return m_effect.isAssignableFrom(eff.getClass());
	}
}

/**
 * An item that cuts a stat of the opponent when the pokemon with the item
 * switches in.
 */
class SwitchInBoostItem extends HoldItem
{
	private double m_mul;
	private int m_stat;

	public SwitchInBoostItem(String name, int stat, double mul)
	{
		super(name);
		m_stat = stat;
		m_mul = mul;
	}

	@Override
	public boolean apply(Pokemon p)
	{
		p.getMultiplier(m_stat).multiplyBy(m_mul);
		return true;
	}

	@Override
	public void unapply(Pokemon p)
	{
		p.getMultiplier(m_stat).divideBy(m_mul);
	}
}

/**
 * A hold item that makes one type of move more powerful.
 * 
 * @author Colin
 */
class TypeBoostItem extends HoldItem
{

	protected double m_factor = 1.1;
	protected PokemonType m_type;

	/** Creates a new instance of TypeBoostItem */
	public TypeBoostItem(String name, PokemonType type)
	{
		super(name);
		m_type = type;
	}

	public TypeBoostItem(String name, PokemonType type, double factor)
	{
		this(name, type);
		m_factor = factor;
	}

	/**
	 * Boost the power of one type of move by 10%.
	 */
	@Override
	public MoveListEntry getTransformedMove(Pokemon p, MoveListEntry entry)
	{
		PokemonMove move = entry.getMove();
		if(move.getType().equals(m_type))
			move.setPower((int) (move.getPower() * m_factor));
		return entry;
	}

	@Override
	public boolean isMoveTransformer(boolean enemy)
	{
		return !enemy;
	}
}

class WhiteHerbItem extends StatusCureItem
{
	public WhiteHerbItem()
	{
		super("White Herb", StatChangeEffect.class);
	}

	@Override
	public void displayCureMessage(Pokemon p, StatusEffect eff)
	{
		p.getField().showMessage(p.getName() + "'s White Herb cured its " + "status!");
	}

	@Override
	public void executeEffects(Pokemon p)
	{
		List<StatusEffect> statuses = p.getNormalStatuses(0);
		Iterator<StatusEffect> i = statuses.iterator();
		while(i.hasNext())
		{
			StatusEffect effect = i.next();
			if(isCurable(effect))
				cureEffect(p, effect);
		}
	}
}
