/* AbsorbMove.java
 * Created on January 19, 2006, 3:19 PM
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

package org.destiny.server.battle.mechanics.moves;

import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.mechanics.BattleMechanics;
import org.destiny.server.battle.mechanics.PokemonType;

/**
 * Creates a move that increases the user's health by a percentage
 * of the damage done to the opponent.
 * 
 * @author Ben
 */
public class AbsorbMove extends PokemonMove
{

	private double m_percent;

	/**
	 * Creates a new instance of RecoilMove
	 */
	public AbsorbMove(PokemonType type, int power, double accuracy, int pp, double percent)
	{

		super(type, power, accuracy, pp);
		m_percent = percent;
	}

	@Override
	public int use(BattleMechanics mech, Pokemon user, Pokemon target)
	{
		int damage = mech.calculateDamage(this, user, target);
		if(user.getItemName().equalsIgnoreCase("big root"))
			damage *= 1.3;
		int health = target.getHealth();
		if(damage > health)
			damage = health;

		int absorb = (int) (damage * m_percent);
		if(absorb < 1)
			absorb = 1;
		String message;
		if(target.hasAbility("Liquid Ooze"))
		{
			absorb = -absorb;
			message = user.getName() + " sucked up liquid ooze!";
		}
		else
			message = user.getName() + " absorbed health!";
		user.getField().showMessage(message);
		user.changeHealth(absorb);
		target.changeHealth(-damage);
		return damage;
	}
}
