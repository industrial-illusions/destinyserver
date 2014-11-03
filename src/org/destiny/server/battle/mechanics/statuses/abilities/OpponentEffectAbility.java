/* OpponentEffectAbility.java
 * Created on July 28, 2007, 11:39 PM
 * This file is a part of Shoddy Battle.
 * Copyright (C) 2007 Colin Fitzpatrick
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * The Free Software Foundation may be visited online at http://www.fsf.org. */

package org.destiny.server.battle.mechanics.statuses.abilities;

import org.destiny.server.battle.BattleField;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.mechanics.statuses.field.FieldEffect;

/**
 * @author Colin
 */
public abstract class OpponentEffectAbility extends IntrinsicAbility
{

	private FieldEffect m_effect;

	/** Creates a new instance of TrappingAbility */
	public OpponentEffectAbility(String name)
	{
		super(name);
	}

	@Override
	public void switchIn(final Pokemon owner)
	{
		class OpponentFieldEffect extends FieldEffect
		{
			@Override
			public boolean apply(Pokemon p)
			{
				if(owner.getParty() != p.getParty())
					applyToOpponent(owner, p);
				return true;
			}

			@Override
			public boolean applyToField(BattleField field)
			{
				m_effect = this;
				return true;
			}

			@Override
			public boolean equals(Object o2)
			{
				if(!(o2 instanceof OpponentFieldEffect))
					return false;
				OpponentFieldEffect e2 = (OpponentFieldEffect) o2;
				return owner.getParty() == e2.getOwner().getParty();
			}

			public Pokemon getOwner()
			{
				return owner;
			}

			@Override
			public boolean tickField(BattleField field)
			{
				return false;
			}

			@Override
			public void unapplyToField(BattleField field)
			{
				m_effect = null;
			}
		}
		owner.getField().applyEffect(new OpponentFieldEffect());
	}

	@Override
	public boolean switchOut(Pokemon p)
	{
		unapply(p);
		return super.switchOut(p);
	}

	@Override
	public void unapply(Pokemon p)
	{
		try
		{
			if(p != null)
				if(m_effect != null)
					if(p.getField() != null)
						p.getField().removeEffect(m_effect);
		}
		catch(NullPointerException npe)
		{
			npe.printStackTrace();
		}
	}

	protected abstract void applyToOpponent(Pokemon owner, Pokemon p);

}
