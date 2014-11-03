package org.destiny.server.battle;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class PokemonEvolution
{

	public enum EvolutionTypes
	{
		AtkDefEqual, AttackGreater, Beauty, Cascoon, DayHoldItem, DefenseGreater, Happiness, HappinessDay, HappinessNight, HasInParty, HasMove, Item, Level, LevelFemale, LevelMale, Ninjask, Shedinja, Silcoon, Trade, TradeItem
	}

	@Element(required = false)
	private String m_attribute;
	@Element
	private String m_evolveTo;
	@Element(required = false)
	private int m_level;
	@Element
	private EvolutionTypes m_type;

	public String getAttribute()
	{
		return m_attribute;
	}

	public String getEvolveTo()
	{
		return m_evolveTo;
	}

	public int getLevel()
	{
		return m_level;
	}

	public EvolutionTypes getType()
	{
		return m_type;
	}

	public void setAttribute(String m_attribute)
	{
		this.m_attribute = m_attribute;
	}

	public void setEvolveTo(String m_evolveTo)
	{
		this.m_evolveTo = m_evolveTo;
	}

	public void setLevel(int m_level)
	{
		this.m_level = m_level;
	}

	public void setType(EvolutionTypes m_type)
	{
		this.m_type = m_type;
	}
}
