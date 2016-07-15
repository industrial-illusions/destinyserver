package org.destiny.server.backend.item;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.destiny.server.Logger;
import org.destiny.server.network.MySqlManager;

/**
 * Stores a database of items for use in mechanics
 * 
 * @author Akkarinage
 * @param <ItemDatabase>
 */

public class ItemDatabase
{
	private MySqlManager db1;

	private static HashMap<Integer, Item> m_items;

		/**
		 * Returns the instance of items in the database
		 * 
		 * @return the instance of items in the database
		 */
		public static List<Item> getCategoryItems(String category)
		{
			List<Item> itemList = new ArrayList<Item>();
			for(int i = 0; i <= m_items.size(); i++)
				try
				{
					Item item = m_items.get(i);
					if(item.getCategory().equals(category))
						itemList.add(item);
				}
				catch(Exception e)
				{
				}
			return itemList;
		}

		/**
		 * Adds an item to the database
		 * 
		 * @param id
		 * @param i
		 */
		public void addItem(int id, Item i)
		{
			if(m_items == null)
				m_items = new HashMap<Integer, Item>();
			m_items.put(id, i);
		}

		/**
		 * Returns an item based on its id
		 * 
		 * @param id
		 * @return
		 */
		public Item getItem(int id)
		{
			return m_items.get(id);
		}

		/**
		 * Returns an item based on its name
		 * 
		 * @param name
		 * @return
		 */
		public Item getItem(String name)
		{
			Iterator<Item> it = m_items.values().iterator();
			Item i;
			while(it.hasNext())
			{
				i = it.next();
				if(i.getName().equalsIgnoreCase(name))
					return i;
			}
			return null;
		}

		public HashMap<Integer, Item> getItemsList()
		{
			return m_items;
		}

		/**
		 * Returns the ids of the items that should be added to the shop
		 * 
		 * @param type
		 * @return the ids of the items that should be added to the shop
		 */
		public List<Integer> getShopItems(int type)
		{
			List<Integer> shopItems = new ArrayList<Integer>();
			for(int i : m_items.keySet())
				if(m_items.get(i).getShop() > 0 && m_items.get(i).getShop() == type)
					shopItems.add(i);
			return shopItems;
		}

	/**
	 * Reinitialises the database
	 */
	public void initialise() {
		Thread t = new Thread(new Runnable() {
		public void run() {
			db1 = MySqlManager.getInstance();
			m_items = new HashMap<Integer, Item>();
			ResultSet rs1 = db1.query("SELECT * FROM `pn_item_db`");
			try {
				while (rs1.next()) {
					Item item = new Item();
					Integer id = rs1.getInt("id");
					String name = rs1.getString("name");
					String description = rs1.getString("description");
					String category = rs1.getString("category");
					Integer shop = rs1.getInt("shop");
					Integer price = rs1.getInt("price");
					String script = rs1.getString("script");
					
					// Start Parsing some Items!
					// m_id
					item.setId(id);

					// m_name
					item.setName(name);

					// m_description
					item.setDescription(description);

					// m_category
					item.setCategory(category);

					// m_shop
					item.setShop(shop);

					// m_price
					item.setPrice(price);

					// m_script
					item.setScript(script);

					m_items.put(item.getId(), item);

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			Logger.logInfo("Item database initialised from SQL.");
			}
		}, "ItemDatabase_Thread");
		t.start();
	}

}