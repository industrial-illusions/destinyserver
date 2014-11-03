package org.destiny.server.backend.entity;

import java.util.Timer;
import java.util.TimerTask;

import org.destiny.server.GameServer;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.protocol.ServerMessage;

/**
 * Represents HM Objects such as trees, boulders, whirlpools, etc.
 * 
 * @author ZombieBear
 */
public class HMObject extends NPC
{
	public enum ObjectType
	{
		CUT_TREE, ROCKSMASH_ROCK, STRENGTH_BOULDER, WHIRLPOOL
	}

	private static int HMObjectID = 0;
	private boolean addToMovementManager = true;
	private final HMObject hmObj = this;
	private ObjectType m_HMType;
	private int m_objId;
	private int originalX, originalY;
	private Timer timer = new Timer();

	public static ObjectType parseHMObject(String name) throws Exception
	{
		for(ObjectType oT : ObjectType.values())
			if(name.equalsIgnoreCase(oT.name()))
				return oT;
		throw new Exception("The HMObject requested is invalid.");
	}

	public int getObjId()
	{
		return m_objId;
	}

	public int getRequiredTrainerLevel(ObjectType oT)
	{
		int level = 0;
		switch(oT)
		{
			case CUT_TREE:
				level = 15;
				break;
			case ROCKSMASH_ROCK:
				level = 30;
				break;
			case STRENGTH_BOULDER:
				level = 35;
				break;
			case WHIRLPOOL:
				level = 40;
				break;
		}
		return level;
	}

	public ObjectType getType()
	{
		return m_HMType;
	}

	public void setOriginalX(int x)
	{
		originalX = x;
	}

	public void setOriginalY(int y)
	{
		originalY = y;
	}

	public void setType(ObjectType oT)
	{
		m_HMType = oT;
		if(oT == ObjectType.STRENGTH_BOULDER)
		{
			HMObjectID++;
			m_objId = HMObjectID;
		}
		switch(oT)
		{
			case CUT_TREE:
				setSprite(-2);
				break;
			case STRENGTH_BOULDER:
				setSprite(-3);
				break;
			case ROCKSMASH_ROCK:
				setSprite(-4);
				break;
			case WHIRLPOOL:
				setSprite(-5);
				break;
		}
	}

	@Override
	public void talkToPlayer(Player p)
	{
		/* Handle event.
		 * Returns the object to the map after 30 secs. */
		if(p.getTrainingLevel() >= getRequiredTrainerLevel(getType()))
			switch(m_HMType)
			{
				case STRENGTH_BOULDER:
					queueMovement(p.getFacing());
					if(addToMovementManager)
					{
						GameServer.getServiceManager().getMovementService().getMovementManager().addHMObject(this);
						addToMovementManager = false;
					}
					timer.schedule(new TimerTask()
					{
						@Override
						public void run()
						{
							hmObj.setX(originalX);
							hmObj.setY(originalY);
						}
					}, 30000);
					break;
				/* TODO: Fix and thoroughly test better implementation. */
				case CUT_TREE:
				case ROCKSMASH_ROCK:
				case WHIRLPOOL:
					getMap().removeChar(this);
					timer.schedule(new TimerTask()
					{
						@Override
						public void run()
						{
							m_map.addChar(hmObj);
						}
					}, 30000);
					break;
			}
		else
		{
			/* The player isn't strong enough to do this. Alert client. */
			ServerMessage message = new ServerMessage(ClientPacket.TRAIN_LVL_LOW);
			message.addInt(getRequiredTrainerLevel(m_HMType));
			p.getSession().Send(message);
		}
	}
}