package org.destiny.server.backend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.destiny.server.backend.entity.Character;
import org.destiny.server.backend.entity.HMObject;
import org.destiny.server.backend.entity.HMObject.ObjectType;

/**
 * Loops through all players and moves them if they request to be moved
 * 
 * @author shadowkanji
 */
public class MovementManager implements Runnable
{
	/** Comparator for comparing chars */
	private final ReentrantReadWriteLock queueLock = new ReentrantReadWriteLock(true);
	private final ReadLock qReadLock = queueLock.readLock();
	private final WriteLock qWriteLock = queueLock.writeLock();
	private Comparator<Character> m_comp;
	private boolean m_isRunning = true;
	private Queue<Character> m_done;
	private int m_pLoad = 0;
	private Thread m_thread;
	private Queue<Character> m_waiting;
	private Queue<Character> m_moving;

	/**
	 * Default constructor.
	 */
	public MovementManager()
	{
		m_comp = new Comparator<Character>()
		{
			public int compare(Character arg0, Character arg1)
			{
				return arg0.getPriority() - arg1.getPriority();
			}
		};
		m_waiting = new PriorityQueue<Character>(11, m_comp);
		m_done = new PriorityQueue<Character>(10, m_comp);
		m_moving = new PriorityQueue<Character>(1, m_comp);
	}

	public void addHMObject(HMObject obj)
	{
		qWriteLock.lock();
		try
		{
			if(obj.getType() == ObjectType.STRENGTH_BOULDER)
			{
				m_pLoad++;
				m_waiting.add(obj);
			}
		}
		finally
		{
			qWriteLock.unlock();
		}
	}

	/**
	 * Adds a player to this movement service
	 * 
	 * @param player
	 */
	public void addPlayer(Character player)
	{
		qWriteLock.lock();
		try
		{
			m_pLoad++;
			m_waiting.offer(player);
		}
		finally
		{
			qWriteLock.unlock();
		}
	}

	/**
	 * Returns how many players are in this thread (the processing load)
	 */
	public int getProcessingLoad()
	{
		return m_pLoad;
	}

	/**
	 * Returns true if the movement manager is running
	 * 
	 * @return
	 */
	public boolean isRunning()
	{
		return m_thread != null && m_thread.isAlive();
	}

	/**
	 * Removes a player from this movement service, returns true if the player was in the thread and was removed. Otherwise, returns false.
	 * 
	 * @param player
	 */
	public boolean removePlayer(String player)
	{
		/* Check waiting list */
		synchronized(m_waiting)
		{
			for(Character c : m_waiting)
				if(c.getName().equalsIgnoreCase(player))
				{
					m_waiting.remove(c);
					m_pLoad--;
					return true;
				}
		}
		/* Check done list */
		synchronized(m_done)
		{
			for(Character c : m_done)
				if(c.getName().equalsIgnoreCase(player))
				{
					m_done.remove(c);
					m_pLoad--;
					return true;
				}
		}
		/* Check moving list */
		synchronized(m_moving)
		{
			for(Character c : m_moving)
				if(c.getName().equalsIgnoreCase(player))
				{
					m_moving.remove(c);
					m_pLoad--;
					return true;
				}
		}
		return false;
	}

	/**
	 * Called by m_thread.start().
	 * Looping through the waiting and moving queues and moving the character with highest priority from both queues.
	 */
	public void run()
	{
		Character tmp = null;
		while(m_isRunning)
		{
			/* Pull char of highest priority */
			if(m_waiting != null && !m_waiting.isEmpty())
			{
				synchronized(m_waiting)
				{
					tmp = m_waiting.poll();
				}
				/* Move character */
				if(tmp.move())
				{
					/* Place him in moving queue */
					qReadLock.lock();
					try
					{
						m_moving.offer(tmp);
					}
					finally
					{
						qReadLock.unlock();
					}
				}
				if(!m_moving.isEmpty())
					synchronized(m_moving)
					{
						/* Get character */
						tmp = m_moving.poll();
					}
				/* Move character */
				if(!tmp.move())
				{
					/* Place him in the done queue */
					qReadLock.lock();
					try
					{
						m_done.offer(tmp);
					}
					finally
					{
						qReadLock.unlock();
					}
				}
				else
				{
					/* Keep him in the Moving queue, but place him last */
					qReadLock.lock();
					try
					{
						m_moving.offer(tmp);
					}
					finally
					{
						qReadLock.unlock();
					}
				}

			}
			if(m_waiting != null && m_done != null)
			{
				if(m_waiting.isEmpty())
				{
					/* If waiting is empty transfer the characters with movement remaining */
					ArrayList<Character> transfer = new ArrayList<Character>();
					for(int i = 0; i < m_done.size(); i++)
					{
						if(m_done.peek().peekNextMovement() != null)
							/* Character has movement remaining. Adding to the transfer list */
							transfer.add(m_done.poll());
						else
							/* Character has no movement remaining. Removing from movement service */
							m_done.poll();
					}
					qReadLock.lock();
					try
					{
						m_waiting.addAll(transfer);
						/* the done queue should be clear, this is just to be sure! */
						m_done.clear();
					}
					finally
					{
						qReadLock.unlock();
					}
				}
			}
			try
			{
				Thread.sleep(3);
			}
			catch(InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}
	}

	/**
	 * Starts the movement thread
	 */
	public void start()
	{
		m_thread = new Thread(this, "MovementManager-Thread");
		m_thread.start();
	}

	/**
	 * Stops the movement thread
	 */
	public void stop()
	{
		m_moving.clear();
		m_done.clear();
		m_waiting.clear();
		m_isRunning = false;
	}
}