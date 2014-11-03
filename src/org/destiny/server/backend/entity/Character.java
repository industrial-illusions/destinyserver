package org.destiny.server.backend.entity;

import java.util.LinkedList;
import java.util.Queue;

import org.destiny.server.backend.map.ServerMap;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.protocol.ServerMessage;

/**
 * Base class for a character. Note: Originally this implemented Battleable but not all chars are battleable
 * 
 * @author shadowkanji
 */
public class Character implements Positionable
{

	private static final int WALKING_DISTANCE = 32;
	protected ServerMap m_map;
	/* Stores a queue of movements for processing */
	protected Queue<Direction> m_movementQueue = new LinkedList<Direction>();
	protected String m_name;
	protected int m_sprite, m_mapX, m_mapY, m_x, m_y, m_id;
	private boolean m_boostPriority = false;
	protected Direction m_facing = Direction.Down;
	private boolean m_isVisible, m_isSurfing, isMoving = false;
	private int TRANSPORT_MULTIPLIER = 1;

	/**
	 * Boost the char's movement priority
	 */
	public void boostPriority()
	{
		m_boostPriority = true;
	}

	/**
	 * Disposes of this char
	 */
	public void dispose()
	{
		m_map = null;
	}

	/**
	 * Returns if two chars are the same based on id.
	 * 
	 * @param obj
	 * @return True when both objects are equal, otherwise false.
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof Character)
		{
			Character c = (Character) obj;
			return (m_id == c.getId()) ? true : false;
		}
		return false;
	}

	/**
	 * Returns the hashcode for this object.
	 */
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + m_id;
		return result;
	}

	/**
	 * Returns the direction this char is facing
	 */
	public Direction getFacing()
	{
		return m_facing;
	}

	/**
	 * Returns this char's id
	 */
	public int getId()
	{
		return m_id;
	}

	/**
	 * Returms the map this char is on
	 */
	public ServerMap getMap()
	{
		return m_map;
	}

	/**
	 * Returns the mapX of this char
	 */
	public int getMapX()
	{
		return m_mapX;
	}

	/**
	 * Returns the mayY of this char
	 */
	public int getMapY()
	{
		return m_mapY;
	}

	/**
	 * Returns the name of the char
	 */
	public String getName()
	{
		return m_name.equalsIgnoreCase("SPRITER") ? "" : m_name;
	}

	/**
	 * Returns next movement to be checked
	 * 
	 * @return
	 */
	public Direction getNextMovement()
	{
		return m_movementQueue.poll();
	}
	
	public Direction peekNextMovement()
	{
		return m_movementQueue.peek();
	}
	
	public boolean isMoving()
	{
		return isMoving;
	}

	/**
	 * Returns the priority of this player to be move checked
	 * 
	 * @return
	 */
	public int getPriority()
	{
		int priority = m_movementQueue.size();
		if(m_boostPriority)
		{
			m_boostPriority = false;
			priority += 100;
		}
		return priority;
	}

	/**
	 * Returns the raw sprite (ignoring surf)
	 * 
	 * @return
	 */
	public int getRawSprite()
	{
		return m_sprite;
	}

	/**
	 * Returns the sprite of this char. Will return the surf sprite if the char is surfing
	 */
	public int getSprite()
	{
		return m_isSurfing ? -1 : m_sprite;
	}

	/**
	 * Returns the x co-ordinate of this char
	 */
	public int getX()
	{
		return m_x;
	}

	/**
	 * Returns the y co-ordinate of this char
	 */
	public int getY()
	{
		return m_y;
	}

	/**
	 * Returns true if this char is surfing
	 * 
	 * @return
	 */
	public boolean isSurfing()
	{
		return m_isSurfing;
	}

	/**
	 * Returns if this char is visible
	 */
	public boolean isVisible()
	{
		return m_isVisible;
	}

	/**
	 * Processes and checks the top movement queued
	 * @return 
	 */
	public boolean move()
	{
		/* Moves player with the movement with the most priority*/
		if(m_facing == peekNextMovement())
		{
			move(getNextMovement());
			isMoving = true;
			return true;
		}
		else
		{
			isMoving = false;
			return false;			
		}		
	}

	/**
	 * Returns true if the char was successfully moved in direction d
	 * 
	 * @param d - Direction to be moved in
	 */
	public boolean move(Direction d)
	{
		if(d != null && m_map != null)
		{
			// Change direction if needs be
			if(m_facing != d)
			{
				setFacing(d);
				return true;
			}
			// Move the player
			if(m_map.moveChar(this, d))
			{
				/* Update co-ordinates and inform other players of movement */
				switch(d)
				{
					case Up:
						m_y -= WALKING_DISTANCE * TRANSPORT_MULTIPLIER;
						m_facing = Direction.Up;
						m_map.sendMovementToAll(d, this);
						break;
					case Down:
						m_y += WALKING_DISTANCE * TRANSPORT_MULTIPLIER;
						m_facing = Direction.Down;
						m_map.sendMovementToAll(d, this);
						break;
					case Left:
						m_x -= WALKING_DISTANCE * TRANSPORT_MULTIPLIER;
						m_facing = Direction.Left;
						m_map.sendMovementToAll(d, this);
						break;
					case Right:
						m_x += WALKING_DISTANCE * TRANSPORT_MULTIPLIER;
						m_facing = Direction.Right;
						m_map.sendMovementToAll(d, this);
						break;
				}
				return true;
			}
			else
			{
				if(this instanceof Player)
				{
					// If its a player, resync them
					Player p = (Player) this;
					ServerMessage message = new ServerMessage(ClientPacket.UPDATE_COORDS);
					message.addInt(getX());
					message.addInt(getY());
					p.getSession().Send(message);
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * Queues a movement to be checked
	 * 
	 * @param d
	 */
	public void queueMovement(Direction d)
	{
		m_movementQueue.offer(d);
	}

	/**
	 * Changes the direction of the NPC.
	 * 
	 * @param direction The direction to change to.
	 */
	public void setFacing(Direction direction)
	{
		m_facing = direction;
		if(m_map != null)
			m_map.sendMovementToAll(direction, this);
	}

	/**
	 * Changes the direction of the NPC.
	 * 
	 * @param direction The direction to change to.
	 * @param map The map the Character is located in.
	 */
	public void setFacing(Direction direction, ServerMap map)
	{
		if(m_facing != direction)
		{
			m_facing = direction;
			map.sendMovementToAll(direction, this);
		}
	}

	/**
	 * Sets this char's id.
	 * NOTE: Players ids are permanent, given upon registration
	 * NOTE: NonPlayers ids are dynamic, based on how many other npcs are on the same map
	 */
	public void setId(int id)
	{
		m_id = id;
	}

	/**
	 * Sets the map this player is and handles all networking to client that is involved with it
	 */
	public void setMap(ServerMap map, Direction dir)
	{
		// Remove the char from their old map
		if(m_map != null)
			m_map.removeChar(this);
		// Set their current map to the new map
		m_map = map;
		m_mapX = map.getX();
		m_mapY = map.getY();
		// Add the char to the map
		m_map.addChar(this);
	}

	/**
	 * Sets the map x co-ordinate
	 * 
	 * @param x
	 */
	public void setMapX(int x)
	{
		m_mapX = x;
	}

	/**
	 * Sets the map y co-ordinate
	 * 
	 * @param y
	 */
	public void setMapY(int y)
	{
		m_mapY = y;
	}

	/**
	 * Sets the name of this char
	 * NOTE: For Player's this is their username
	 * 
	 * @param name
	 */
	public void setName(String name)
	{
		m_name = name;
	}

	/**
	 * Set the sprite of this char
	 */
	public void setSprite(int sprite)
	{
		m_sprite = sprite;
		// Inform everyone of sprite change
		if(m_map != null)
		{
			// m_map.sendToAll(new SpriteChangeMessage(m_id, this.getSprite()));
			ServerMessage message = new ServerMessage(ClientPacket.SPRITE_CHANGE);
			message.addInt(m_id);
			message.addInt(getSprite());
			m_map.sendToAll(message);
		}
	}

	/**
	 * Sets if this char is surfing or not and sends the sprite change information to everyone
	 * 
	 * @param b
	 */
	public void setSurfing(boolean b)
	{
		m_isSurfing = b;
		if(m_map != null)
		{
			// m_map.sendToAll(new SpriteChangeMessage(m_id, this.getSprite()));
			ServerMessage message = new ServerMessage(ClientPacket.SPRITE_CHANGE);
			message.addInt(m_id);
			message.addInt(getSprite());
			m_map.sendToAll(message);
		}
	}

	/**
	 * Set if this char is visible
	 */
	public void setVisible(boolean visible)
	{
		m_isVisible = visible;
	}

	/**
	 * Sets the x co-ordinate of this character on the map
	 */
	public void setX(int x)
	{
		m_x = x;
	}

	/**
	 * Sets the y co-ordinate of this character on the map
	 */
	public void setY(int y)
	{
		m_y = y;
	}
}
