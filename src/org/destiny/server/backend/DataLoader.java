package org.destiny.server.backend;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import org.destiny.server.Logger;
import org.destiny.server.backend.entity.HMObject;
import org.destiny.server.backend.entity.NPC;
import org.destiny.server.backend.entity.TradeChar;
import org.destiny.server.backend.entity.Positionable.Direction;
import org.destiny.server.backend.map.ServerMap;
import org.destiny.server.backend.map.WarpTile;

/**
 * Handles data loading for maps (NPCs, Warps, HMObjects, etc.)
 * 
 * @author shadowkanji
 */
public class DataLoader implements Runnable
{
	private final File m_file;
	private final ServerMap m_map;

	/**
	 * Constructor
	 * 
	 * @param f
	 */
	public DataLoader(File file, ServerMap map) {
		m_file = file;
		m_map = map;
		new Thread(this, "MapDataLoader-Thread").start();
	}

	/**
	 * Called by starting the thread
	 */
	public void run() {
		try(Scanner reader = new Scanner(m_file)) {
			NPC npc = null;
			WarpTile warp = null;
			HMObject hmObject = null;
			TradeChar t = null;
			String line;
			String[] details;
			String direction = "Down";
			while(reader.hasNextLine())	{
				line = reader.nextLine();
				switch(line){
				case "[npc]":
					npc = new NPC();
					npc.setName(reader.nextLine());
					direction = reader.nextLine();
					if(direction.equalsIgnoreCase("UP")) {
						npc.setFacing(Direction.Up);
					} else if(direction.equalsIgnoreCase("LEFT")) {
						npc.setFacing(Direction.Left);
					} else if(direction.equalsIgnoreCase("RIGHT")) {
						npc.setFacing(Direction.Right);
					} else {
						npc.setFacing(Direction.Down);
					}
					npc.setSprite(Integer.parseInt(reader.nextLine()));
					if(npc.getName().equalsIgnoreCase("NULL") && npc.getSprite() != 0)
						npc.setName("NPC");
					npc.setX(Integer.parseInt(reader.nextLine()) * 32);
					npc.setY(Integer.parseInt(reader.nextLine()) * 32 - 8);
					npc.setOriginalDirection(npc.getFacing());
					// Load possible Pokemons
					line = reader.nextLine();
					if(!line.equalsIgnoreCase("NULL")) {
						details = line.split(",");
						HashMap<String, Integer> pokes = new HashMap<String, Integer>();
						for(int i = 0; i < details.length; i = i + 2)
							pokes.put(details[i], Integer.parseInt(details[i + 1]));
						npc.setPossiblePokemon(pokes);
					}
					npc.setPartySize(Integer.parseInt(reader.nextLine()));	// Set minimum party level
					npc.setBadge(Integer.parseInt(reader.nextLine()));		// Minimum Badge requirement
					line = reader.nextLine();								// Add all speech, if any
					if(!line.equalsIgnoreCase("NULL")) {
						details = line.split(",");
						for(int i = 0; i < details.length; i++)
							npc.addSpeech(Integer.parseInt(details[i]));
					}
					npc.setHealer(Boolean.parseBoolean(reader.nextLine().toLowerCase()));
					npc.setBox(Boolean.parseBoolean(reader.nextLine().toLowerCase()));

					// Setting ShopKeeper as an int.
					String shop = reader.nextLine();
					try	{
						npc.setShopKeeper(Integer.parseInt(shop.trim()));
					}
					catch(Exception e) {
						try {
							/* Must be an old shop */
							if(Boolean.parseBoolean(shop.trim().toLowerCase())) {
								npc.setShopKeeper(1); // Its an old shop! Yay!
								Logger.logError("Found old style Shop NPC (" + m_map.getX() + "." + m_map.getY() + ".txt)", "Change TRUE to Shop Number in " + m_map.getX() + "." + m_map.getY() + ".txt");
							} else {
								npc.setShopKeeper(0); // Its an old npc. Not a shop.
								Logger.logError("Found old style NPC (" + m_map.getX() + "." + m_map.getY() + ".txt)", "Change FALSE to 0 in " + m_map.getX() + "." + m_map.getY() + ".txt");
							}
						}
						catch(Exception ex)	{
							npc.setShopKeeper(0);// Dunno what the hell it is, but its not a shop.
							Logger.logError("Unknown Shop/NPC (" + m_map.getX() + "." + m_map.getY() + ".txt)", "Better have a look " + m_map.getX() + "." + m_map.getY() + ".txt - maybe take a hammer to it??");
						}
					}
					break;
				case "[/npc]":
					m_map.addChar(npc);
					break;
					case "[warp]":
					warp = new WarpTile();
					warp.setX(Integer.parseInt(reader.nextLine()));
					warp.setY(Integer.parseInt(reader.nextLine()));
					warp.setWarpX(Integer.parseInt(reader.nextLine()) * 32);
					warp.setWarpY(Integer.parseInt(reader.nextLine()) * 32 - 8);
					warp.setWarpMapX(Integer.parseInt(reader.nextLine()));
					warp.setWarpMapY(Integer.parseInt(reader.nextLine()));
					warp.setBadgeRequirement(Integer.parseInt(reader.nextLine()));
					break;
				case "[/warp]":
					m_map.addWarp(warp);
					break;
				case "[hmobject]":
					hmObject = new HMObject();
					hmObject.setName(reader.nextLine());
					hmObject.setType(HMObject.parseHMObject(hmObject.getName()));
					hmObject.setX(Integer.parseInt(reader.nextLine()) * 32);
					hmObject.setOriginalX(hmObject.getX());
					hmObject.setY(Integer.parseInt(reader.nextLine()) * 32 - 8);
					hmObject.setOriginalY(hmObject.getY());
					break;
				case "[/hmobject]":
					hmObject.setMap(m_map, Direction.Down);
					break;
				case "[trade]":
					t = new TradeChar();
					t.setName(reader.nextLine());
					direction = reader.nextLine();
					if(direction.equalsIgnoreCase("UP"))
						t.setFacing(Direction.Up);
					else if(direction.equalsIgnoreCase("LEFT"))
						t.setFacing(Direction.Left);
					else if(direction.equalsIgnoreCase("RIGHT"))
						t.setFacing(Direction.Right);
					else
						t.setFacing(Direction.Down);
					t.setSprite(Integer.parseInt(reader.nextLine()));
					t.setX(Integer.parseInt(reader.nextLine()) * 32);
					t.setY(Integer.parseInt(reader.nextLine()) * 32 - 8);
					t.setRequestedPokemon(reader.nextLine(), Integer.parseInt(reader.nextLine()), reader.nextLine());
					t.setOfferedSpecies(reader.nextLine(), Integer.parseInt(reader.nextLine()));
					break;
				case "[/trade]":
					m_map.addChar(t);
					break;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Logger.logError("Invalid NPC, HM Object or WarpTile (" + m_map.getX() + "." + m_map.getY() + ".txt)", "Check syntax in " + m_map.getX() + "." + m_map.getY() + ".txt");
		}
	}
}
