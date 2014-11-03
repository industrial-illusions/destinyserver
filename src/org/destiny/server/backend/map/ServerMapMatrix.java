package org.destiny.server.backend.map;

import org.destiny.server.backend.entity.Character;
import org.destiny.server.backend.entity.Positionable.Direction;

/**
 * Stores all maps on the server in a 2D array
 * 
 * @author shadowkanji
 */
public class ServerMapMatrix
{
	private final ServerMap[][] m_mapMatrix;

	/**
	 * Default constructor
	 */
	public ServerMapMatrix()
	{
		m_mapMatrix = new ServerMap[100][100];
	}

	/**
	 * Returns a server map based on its in-game position (some maps are named negatively, e.g. -50.-50.tmx)
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public synchronized ServerMap getMapByGamePosition(int x, int y)
	{
		return m_mapMatrix[x + 50][y + 50];
	}

	/**
	 * Returns a server map based on its actual position in the server map array.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public synchronized ServerMap getMapByRealPosition(int x, int y)
	{
		return m_mapMatrix[x][y];
	}

	/**
	 * Moves a player between two maps.
	 * 
	 * @param player The player to be moved.
	 * @param origin The map the player came from.
	 * @param destination The map the player is moving towards.
	 */
	public void moveBetweenMaps(Character player, ServerMap origin, ServerMap dest)
	{
		Direction dir = null;
		/* Reposition player so they're on the correct edge */
		if(origin.getX() > dest.getX())
		{ // dest. map is to the left
			player.setX(dest.getWidth() * 32 - 32);
			player.setY(player.getY() + origin.getYOffsetModifier() - dest.getYOffsetModifier());
			dir = Direction.Left;
		}
		else if(origin.getX() < dest.getX())
		{ // to the right
			player.setX(0);
			player.setY(player.getY() + origin.getYOffsetModifier() - dest.getYOffsetModifier());
			dir = Direction.Right;
		}
		else if(origin.getY() > dest.getY())
		{// up
			player.setY(dest.getHeight() * 32 - 40);
			player.setX(player.getX() + origin.getXOffsetModifier() - dest.getXOffsetModifier());
			dir = Direction.Up;
		}
		else if(origin.getY() < dest.getY())
		{// down
			player.setY(-8);
			player.setX(player.getX() - dest.getXOffsetModifier() + origin.getXOffsetModifier());
			dir = Direction.Down;
		}
		/* Set the map */
		player.setMap(dest, dir);
	}

	/**
	 * Sets a map to the map matrixs
	 * 
	 * @param map
	 * @param x
	 * @param y
	 */
	public void setMap(ServerMap map, int x, int y)
	{
		m_mapMatrix[x][y] = map;
	}
}
