package org.destiny.server.backend.map;

import tiled.core.TileLayer;

/**
 * Stores a tile layer
 * 
 * @author shadowkanji
 */
public class ServerTileLayer
{
	private final int m_height;
	private final byte[][] m_tiles;
	private final int m_width;

	/**
	 * Constructor
	 * 
	 * @param m
	 */
	public ServerTileLayer(TileLayer m)
	{
		m_tiles = new byte[m.getWidth()][m.getHeight()];
		m_width = m.getWidth();
		m_height = m.getHeight();

		for(int x = 0; x < m.getWidth(); x++)
			for(int y = 0; y < m.getHeight(); y++)
				if(m.getTileAt(x, y) != null)
					m_tiles[x][y] = '1';
				else
					m_tiles[x][y] = '0';
	}

	/**
	 * Returns the tile at x, y
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public byte getTileAt(int x, int y)
	{
		if(x < m_width && y < m_height)
			return m_tiles[x][y];
		return '0';
	}
}
