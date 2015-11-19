package org.destiny.server.battle.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.destiny.server.Logger;
import org.destiny.server.backend.entity.NPC;

/**
 * A thread which wakes sleeping NPCs (NPCs sleep for ~15 minutes after battle)
 * 
 * @author XtremeJedi
 */
public class NpcSleepTimer extends Thread
{
	private static List<NPC> m_npcSleeping = new ArrayList<NPC>();
	private boolean m_running = true;

	public NpcSleepTimer()
	{
		super("NPCSleepTimer-Thread");
	}

	public static void addNPC(NPC npc)
	{
		m_npcSleeping.add(npc);
	}

	@Override
	public void run()
	{
		Logger.logInfo("Npc sleep timer started.");
		Random r = new Random();
		while(m_running)
		{
			/* Loop through every sleeping NPC. */
			Iterator<NPC> iterator = m_npcSleeping.iterator();
			while(iterator.hasNext())
			{
				NPC npc = iterator.next();
				if(!npc.canBattle() && System.currentTimeMillis() - npc.getLastBattleTime() >= 5 * 60 * 1000 + r.nextInt(10 * 60 * 1000))
				{
					npc.resetLastBattleTime();
					iterator.remove();
				}
			}
			try
			{
				Thread.sleep(r.nextInt(5 * 60 * 1000)); // ~5 minutes
			}
			catch(InterruptedException e)
			{
			}
		}
		Logger.logInfo("Npc sleep timer stopped.");
	}

	public void finish()
	{
		m_running = false;
	}
}
