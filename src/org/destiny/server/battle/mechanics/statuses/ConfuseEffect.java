/* ConfuseEffect.java
 * Created on December 23, 2006, 12:12 PM
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

package org.destiny.server.battle.mechanics.statuses;

import org.destiny.server.battle.BattleField;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.mechanics.BattleMechanics;
import org.destiny.server.battle.mechanics.PokemonType;
import org.destiny.server.battle.mechanics.moves.PokemonMove;

/**
 * @author Colin
 */
public class ConfuseEffect extends StatusEffect
{

	private int m_turns = 0;

	@Override
	public boolean apply(Pokemon p)
	{
		if(p.hasSubstitute())
			return false;
		if(p.hasAbility("Own Tempo"))
			return false;
		if(p.hasAbility("Tangled Feet"))
			p.getMultiplier(Pokemon.S_EVASION).increaseMultiplier();
		m_turns = p.getField().getRandom().nextInt(4) + 2;
		return true;
	}

	@Override
	public String getDescription()
	{
		return " became confused!";
	}

	@Override
	public String getName()
	{
		return "Confusion";
	}

	@Override
	public int getTier()
	{
		// Not applicable.
		return 1;
	}

	/**
	 * Confusion has a 50% chance of immobolising the afflicted pokemon.
	 */
	@Override
	public boolean immobilises(Pokemon poke)
	{
		if(poke.hasEffect(SleepEffect.class))
			return false;

		if(--m_turns <= 0)
		{
			poke.removeStatus(this);
			poke.getField().showMessage(poke.getName() + " snapped out of confusion!");
			return false;
		}

		BattleField field = poke.getField();

		field.showMessage(poke.getName() + " is confused!");
		if(field.getRandom().nextDouble() <= 0.5)
			return false;

		field.showMessage("It hurt itself in its confusion!");
		poke.useMove(new PokemonMove(PokemonType.T_TYPELESS, 40, 1.0, 1)
		{
			@Override
			public boolean attemptHit(BattleMechanics mech, Pokemon source, Pokemon target)
			{
				return true;
			}

			@Override
			public boolean canCriticalHit()
			{
				return false;
			}

			@Override
			public int use(BattleMechanics mech, Pokemon source, Pokemon target)
			{
				int damage = mech.calculateDamage(this, source, target);
				target.changeHealth(-damage, true);
				return damage;
			}
		}, poke);
		return true;
	}

	@Override
	public void informDuplicateEffect(Pokemon p)
	{
		p.getField().showMessage(p.getName() + " is already confused!");
	}

	@Override
	public boolean switchOut(Pokemon p)
	{
		return true;
	}

	@Override
	public boolean tick(Pokemon p)
	{
		return false;
	}

	@Override
	public void unapply(Pokemon p)
	{
		if(p.hasAbility("Tangled Feet"))
			p.getMultiplier(Pokemon.S_EVASION).decreaseMultiplier();
	}
}
