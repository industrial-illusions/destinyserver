package org.destiny.server.backend.entity;

/**
 * Data of something being traded
 * 
 * @author shadowkanji
 */
public class TradeOffer
{
	public enum TradeType
	{
		ITEM, MONEY, POKEMON
	}

	private int m_id;
	/* Stores Pokemon or Item name */
	private String m_information;
	private int m_quantity;
	private TradeType m_type;

	/**
	 * Returns the id
	 * 
	 * @return
	 */
	public int getId()
	{
		return m_id;
	}

	/**
	 * Returns information about this trade
	 * 
	 * @return
	 */
	public String getInformation()
	{
		return m_information;
	}

	/**
	 * Returns the quantity
	 * 
	 * @return
	 */
	public int getQuantity()
	{
		return m_quantity;
	}

	/**
	 * Returns the type
	 * 
	 * @return
	 */
	public TradeType getType()
	{
		return m_type;
	}

	/**
	 * Sets the id of the object
	 * 
	 * @param id
	 */
	public void setId(int id)
	{
		m_id = id;
	}

	/**
	 * Sets information for this offer
	 * 
	 * @param i
	 */
	public void setInformation(String i)
	{
		m_information = i;
	}

	/**
	 * Sets the quantity
	 * 
	 * @param q
	 */
	public void setQuantity(int q)
	{
		m_quantity = q;
	}

	/**
	 * Sets the trade type
	 * 
	 * @param t
	 */
	public void setType(TradeType t)
	{
		m_type = t;
	}
}
