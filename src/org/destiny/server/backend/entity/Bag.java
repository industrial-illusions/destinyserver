package org.destiny.server.backend.entity;

import java.util.ArrayList;

import org.destiny.server.GameServer;

/**
 * Represents a player's bag
 * 
 * @author shadowkanji
 */
public class Bag
{
	private static final int BAG_SIZE = 600; // 30 is the artificial bag size, right? // sadhi set it to 600 because then it should prevent your bag becomming full
	private final ArrayList<BagItem> m_items;
	private final int m_memberId;

	/**
	 * Default constructor
	 */
	public Bag(int memberid)
	{
		m_memberId = memberid;
		m_items = new ArrayList<BagItem>();
	}

	/**
	 * Adds an item to the bag. Returns true on success
	 * 
	 * @param itemNumber
	 * @param quantity
	 */
	public boolean addItem(int itemNumber, int quantity)
	{
		int bagIndex = containsItem(itemNumber);
		if(bagIndex > -1)
		{
			m_items.get(bagIndex).increaseQuantity(quantity);
			return true;
		}
		else if(m_items.size() < BAG_SIZE)
		{
			m_items.add(new BagItem(itemNumber, quantity));
			return true;
		}
		return false;
	}

	/**
	 * Checks if item is in bag. Returns bagIndex if true, else returns -1.
	 * 
	 * @param itemNumber
	 * @param quantity
	 */
	public int containsItem(int itemNumber)
	{
		int bagIndex = -1;
		for(int i = 0; i < m_items.size(); i++)
			if(m_items.get(i).getItemNumber() == itemNumber)
			{
				bagIndex = i;
				break;// End for loop. We found what we're looking for.
			}
		return bagIndex;
	}

	/**
	 * Checks if item is in bag. Returns quantity of items.
	 * 
	 * @param itemNumber
	 * @param quantity
	 */
	public int getItemQuantity(int itemNumber)
	{
		int quantity = 0;
		for(int i = 0; i < m_items.size(); i++)
			if(m_items.get(i).getItemNumber() == itemNumber)
			{
				quantity = m_items.get(i).getQuantity();
				break;// End for loop. We found what we're looking for.
			}
		return quantity;
	}

	/**
	 * Returns all the items in the bag
	 */
	public ArrayList<BagItem> getItems()
	{
		return m_items;
	}

	/**
	 * Returns this bag's member id
	 * 
	 * @return
	 */
	public int getMemberId()
	{
		return m_memberId;
	}

	/**
	 * Returns true if there is space in the bag for that item
	 * 
	 * @param id
	 * @return
	 */
	public boolean hasSpace(int itemid)
	{
		if(containsItem(itemid) >= 0 || m_items.size() < BAG_SIZE)
			return true;
		return false;
	}

	/**
	 * Removes an item.
	 * 
	 * @param itemNumber
	 * @param quantity
	 * @return Returns true if the item is succesfully removed, otherwise false.
	 */
	public boolean removeItem(int itemNumber, int quantity)
	{
		for(int i = 0; i < m_items.size(); i++)
		{
			BagItem item = m_items.get(i);
			if(item.getItemNumber() == itemNumber)
			{
				if(item.getQuantity() - quantity > 0)
				{
					item.decreaseQuantity(quantity);
					GameServer.getServiceManager().getNetworkService().getSaveManager().updateBagItem(m_memberId, item.getItemNumber(), item.getQuantity());
				}
				else
				{
					m_items.remove(item);
					GameServer.getServiceManager().getNetworkService().getSaveManager().removeBagItem(m_memberId, item.getItemNumber());
				}
				return true;
			}
		}
		return false;
	}
}
