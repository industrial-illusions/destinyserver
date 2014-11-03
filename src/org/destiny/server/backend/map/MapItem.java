package org.destiny.server.backend.map;

/**
 * A map item
 * 
 * @author shadowkanji
 */
public class MapItem
{
	private final int m_id;
	private final int m_x;
	private final int m_y;

	/**
	 * Constructor
	 * 
	 * @param x
	 * @param y
	 * @param id
	 */
	public MapItem(int x, int y, int id)
	{
		m_x = x;
		m_y = y;
		m_id = id;
	}

	/**
	 * Returns the item id of this item
	 * 
	 * @return
	 */
	public int getId()
	{
		return m_id;
	}

	/**
	 * Returns the x co-ordinate of this item
	 * 
	 * @return
	 */
	public int getX()
	{
		return m_x;
	}

	/**
	 * Returns the y co-ordinate of this item
	 * 
	 * @return
	 */
	public int getY()
	{
		return m_y;
	}
}
