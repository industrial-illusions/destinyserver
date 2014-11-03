package org.destiny.server.feature;

import org.simpleframework.xml.Element;

/**
 * Stores exp and level information for a Fish Pokemon(one found by fishing)
 * 
 * @author Fshy
 */
public class FishPokemon
{

	@Element
	private int m_experience;
	@Element
	private int m_levelReq;
	@Element
	private int m_rodReq;

	/**
	 * Constructor
	 */
	public FishPokemon()
	{
	}

	/**
	 * Alternative constructor
	 * 
	 * @param levelReq
	 * @param experience
	 */
	public FishPokemon(int levelReq, int experience, int rodReq)
	{
		m_experience = experience;
		m_levelReq = levelReq;
		m_rodReq = rodReq;
	}

	/**
	 * Returns the experience for fishing this pogey
	 * 
	 * @return
	 */
	public int getExperience()
	{
		return m_experience;
	}

	/**
	 * Returns the level required to fish up this pogey
	 * 
	 * @return
	 */
	public int getReqLevel()
	{
		return m_levelReq;
	}

	/**
	 * Returns the rod required to fish up this pogey
	 * 0 is old rod, 15 is good rod, 50 is great rod, 75 is ultra rod.
	 * 
	 * @return
	 */
	public int getReqRod()
	{
		return m_rodReq;
	}

	/**
	 * Sets the experience gained from fishing this pogey
	 * 
	 * @param p
	 */
	public void setExperience(int p)
	{
		m_experience = p;
	}

	/**
	 * Sets the level required to encounter/fish this pogey
	 * 
	 * @param i
	 */
	public void setLevelReq(int s)
	{
		m_levelReq = s;
	}
}
