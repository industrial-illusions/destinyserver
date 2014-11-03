package org.destiny.server.battle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.destiny.server.backend.item.DropData;
import org.destiny.server.battle.Pokemon.ExpTypes;
import org.destiny.server.battle.mechanics.PokemonType;
import org.destiny.server.battle.mechanics.StatException;
import org.destiny.server.battle.mechanics.moves.MoveSet;
import org.destiny.server.battle.mechanics.moves.MoveSetData;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;

public class PokemonSpecies
{

	/**
	 * Gender constants.
	 */
	public static final int GENDER_NONE = 0;
	public static final int GENDER_MALE = 1;
	public static final int GENDER_FEMALE = 2;
	public static final int GENDER_BOTH = GENDER_MALE | GENDER_FEMALE;

	private static PokemonSpeciesData m_default = new PokemonSpeciesData();
	@ElementList
	protected String[] m_abilities;
	@ElementArray
	transient protected int[] m_base;

	@Element
	protected int m_baseEXP;

	@ElementArray
	protected int[] m_baseStats = new int[6];
	@Element
	protected String m_color;
	@ElementArray
	protected int[] m_compatibility = new int[2];
	@ElementArray
	protected DropData[] m_drops;
	@ElementArray
	protected int[] m_effortPoints = new int[6];

	@ElementArray
	protected String[] m_eggMoves;
	@ElementArray
	protected PokemonEvolution[] m_evolutions;

	@Element
	protected int m_femalePercentage;

	@Element
	transient protected int m_genders; // Possible genders.

	@Element
	protected ExpTypes m_growthRate;
	@Element(required = false)
	protected String m_habitat;
	@Element
	protected int m_happiness;
	@Element
	protected float m_height;

	@Element
	protected String m_internalName;
	@Element
	protected String m_kind;

	@ElementMap
	protected Map<Integer, String> m_levelMoves;
	@Element
	protected String m_name;

	@Element
	protected String m_pokedex;

	@Element
	protected int m_rareness;
	@Element
	protected int m_species;

	@ElementArray
	protected String[] m_starterMoves;

	@Element
	protected int m_stepsToHatch;
	@ElementArray
	protected String[] m_tmMoves;
	@ElementArray
	transient protected PokemonType[] m_type;
	@Element
	protected String m_type1;
	@Element(required = false)
	protected String m_type2;
	@Element
	protected float m_weight;
	private int m_number;
	protected int tier;

	// private int m_number;

	/** Constructor used for serialization */
	public PokemonSpecies()
	{
	}

	/**
	 * Construct a new pokemon species with arbitrary stats.
	 */
	public PokemonSpecies(int species, String name, int[] base, int gender)
	{
		m_species = species;
		m_name = name;
		m_base = base;
		m_genders = gender;
	}

	/**
	 * Allows for construction from another PokemonSpecies.
	 * 
	 * @param int m_species = id number of the pokemon
	 * @param String m_name = name of the species
	 * @param int[] m_base = base stats
	 * @param pokemon[] m_type which type(s) is this pokemon
	 * @param int m_genders
	 */
	public PokemonSpecies(PokemonSpecies i)
	{
		m_species = i.m_species;
		m_name = i.m_name;
		m_base = i.m_base;
		m_type = i.m_type;
		m_genders = i.m_genders;
		m_number = i.m_number;
	}

	/**
	 * Creates a new instance of PokemonSpecies
	 */
	public PokemonSpecies(PokemonSpeciesData data, int i) throws PokemonException
	{
		this(data.getSpecies(i));
	}

	/**
	 * Return the default species data.
	 */
	public static PokemonSpeciesData getDefaultData()
	{
		return m_default;
	}

	/**
	 * Read a PokemonSpecies from a stream, backed by an arbitrary
	 * PokemonSpeciesData object.
	 */
	public synchronized static Object readObject(PokemonSpeciesData data, ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		PokemonSpeciesData old = m_default;
		m_default = data;
		Object o = in.readObject();
		m_default = old;
		return o;
	}

	/**
	 * Set the default species data.
	 */
	public static void setDefaultData(PokemonSpeciesData data)
	{
		m_default = data;
	}

	/**
	 * Return whether this species can learn a particular move.
	 */
	public boolean canLearn(PokemonSpeciesData data, String move)
	{
		return data.canLearn(this, move);
	}

	/**
	 * Return whether a pokemon can have a particular ability.
	 */
	public boolean canUseAbility(PokemonSpeciesData data, String ability)
	{
		return data.canUseAbility(m_name, ability);
	}

	public String[] getAbilities()
	{
		return m_abilities;
	}

	/**
	 * Get a "balanced" level for this species using this formula:
	 * level = 113 - 0.074 * [base stat total]
	 * This formula places the pokemon's level within the interval [60, 100]
	 * based on base stats.
	 */
	public int getBalancedLevel()
	{
		int total = 0;
		for(int i = 0; i < m_base.length; ++i)
			total += m_base[i];
		int level = (int) Math.round(113.0 - 0.074 * total);
		if(level < 0)
			level = 0;
		else if(level > 100)
			level = 100;
		return level;
	}

	public int[] getBase()
	{
		return m_base;
	}

	public int getBase(int i) throws StatException
	{
		if(i < 0 || i > 5)
			throw new StatException();
		return m_base[i];
	}

	public int getBaseEXP()
	{
		return m_baseEXP;
	}

	public int[] getBaseStats()
	{
		return m_baseStats;
	}

	public String getColor()
	{
		return m_color;
	}

	public int[] getCompatibility()
	{
		return m_compatibility;
	}

	public DropData[] getDropData()
	{
		return m_drops;
	}

	public int[] getEffortPoints()
	{
		return m_effortPoints;
	}

	public String[] getEggMoves()
	{
		return m_eggMoves;
	}

	public PokemonEvolution[] getEvolutions()
	{
		return m_evolutions;
	}

	public int getFemalePercentage()
	{
		return m_femalePercentage;
	}

	public int getGenders()
	{
		return m_genders;
	}

	public ExpTypes getGrowthRate()
	{
		return m_growthRate;
	}

	public String getHabitat()
	{
		return m_habitat;
	}

	public int getHappiness()
	{
		return m_happiness;
	}

	public float getHeight()
	{
		return m_height;
	}

	public String getInternalName()
	{
		return m_internalName;
	}

	public String getKind()
	{
		return m_kind;
	}

	public Map<Integer, String> getLevelMoves()
	{
		return m_levelMoves;
	}

	/**
	 * Get the MoveSet associated with this species.
	 */
	public MoveSet getMoveSet(MoveSetData data)
	{
		return data.getMoveSet(m_species);
	}

	public String getName()
	{
		return m_name;
	}

	public String getPokedexInfo()
	{
		return m_pokedex;
	}

	/**
	 * @return The number of the pokemon starting from 0 (bulbasaur) to 492 (arceus)
	 */
	public int getPokemonNumber()
	{
		return m_number;
	}

	/**
	 * @return The number of the pokemon as registered in the pokedex 1 (bulbasaur) to 493 (arceus)
	 */
	public int getPokedexNumber()
	{
		return m_number + 1;
	}

	/**
	 * Return a TreeSet of possible abilities.
	 */
	public String[] getPossibleAbilities(PokemonSpeciesData data)
	{
		return data.getPossibleAbilities(m_name);
	}

	/**
	 * Return the possible genders for this species.
	 */
	public int getPossibleGenders()
	{
		return m_genders;
	}

	/**
	 * Returns a random item dropped by the Pokemon, -1 if no item was dropped
	 * 
	 * @return
	 */
	public int getRandomItem()
	{
		if(m_drops == null)
		{
			System.err.println("INFO: Drop data null for " + m_name);
			return -1;
		}
		if(DataService.getBattleMechanics().getRandom().nextInt(99) < 30)
		{
			int r = 100;
			ArrayList<Integer> m_result = new ArrayList<Integer>();
			for(int i = 0; i < m_drops.length; i++)
			{
				r = DataService.getBattleMechanics().getRandom().nextInt(100) + 1;
				if(m_drops[i] != null && r < m_drops[i].getProbability())
					m_result.add(m_drops[i].getItemNumber());
			}
			return m_result.size() > 0 ? m_result.get(DataService.getBattleMechanics().getRandom().nextInt(m_result.size())) : -1;
		}
		return -1;
	}

	public int getRareness()
	{
		return m_rareness;
	}

	public int getSpecies()
	{
		return m_species;
	}

	/**
	 * Returns the number of the pokemon, not taking into account multiple forms of pokemon (eg deoxys ranges from 385-388)
	 * 
	 * @return
	 */
	public int getSpeciesNumber()
	{
		return m_species;
	}

	public String[] getStarterMoves()
	{
		return m_starterMoves;
	}

	public int getStepsToHatch()
	{
		return m_stepsToHatch;
	}

	public String[] getTMMoves()
	{
		return m_tmMoves;
	}

	public PokemonType[] getType()
	{
		return m_type;
	}

	public String getType1()
	{
		return m_type1;
	}

	public String getType2()
	{
		return m_type2;
	}

	public PokemonType[] getTypes()
	{
		return m_type;
	}

	public float getWeight()
	{
		return m_weight;
	}

	public void setAbilities(String[] mAbilities)
	{
		m_abilities = mAbilities;
	}

	public void setBase(int[] mBase)
	{
		m_base = mBase;
	}

	public void setBaseEXP(int mBaseEXP)
	{
		m_baseEXP = mBaseEXP;
	}

	public void setBaseStats(int[] mBaseStats)
	{
		m_baseStats = mBaseStats;
	}

	public void setColor(String mColor)
	{
		m_color = mColor;
	}

	public void setCompatibility(int[] mCompatibility)
	{
		m_compatibility = mCompatibility;
	}

	public void setDropData(DropData[] d)
	{
		m_drops = d;
	}

	public void setEffortPoints(int[] mEffortPoints)
	{
		m_effortPoints = mEffortPoints;
	}

	public void setEggMoves(String[] mEggMoves)
	{
		m_eggMoves = mEggMoves;
	}

	public void setEvolutions(PokemonEvolution[] mEvolutions)
	{
		m_evolutions = mEvolutions;
	}

	public void setFemalePercentage(int mFemalePercentage)
	{
		m_femalePercentage = mFemalePercentage;
	}

	public void setGenders(int mGenders)
	{
		m_genders = mGenders;
	}

	public void setGrowthRate(ExpTypes mGrowthRate)
	{
		m_growthRate = mGrowthRate;
	}

	public void setHabitat(String mHabitat)
	{
		m_habitat = mHabitat;
	}

	public void setHappiness(int mHappiness)
	{
		m_happiness = mHappiness;
	}

	public void setHeight(float mHeight)
	{
		m_height = mHeight;
	}

	public void setInternalName(String mInternalName)
	{
		m_internalName = mInternalName;
	}

	public void setKind(String mKind)
	{
		m_kind = mKind;
	}

	public void setLevelMoves(HashMap<Integer, String> mMoves)
	{
		m_levelMoves = mMoves;
	}

	public void setName(String mName)
	{
		m_name = mName;
	}

	public void setPokedexInfo(String mPokedex)
	{
		m_pokedex = mPokedex;
	}

	public void setPokemonNumber(int pokemonNumber)
	{
		m_number = pokemonNumber;
	}

	/**
	 * Set the possible genders for this species.
	 */
	public void setPossibleGenders(int genders)
	{
		m_genders = genders;
	}

	public void setRareness(int mRareness)
	{
		m_rareness = mRareness;
	}

	public void setSpecies(int mSpecies)
	{
		m_species = mSpecies;
	}

	public void setStarterMoves(String[] m)
	{
		m_starterMoves = m;
	}

	public void setStepsToHatch(int mStepsToHatch)
	{
		m_stepsToHatch = mStepsToHatch;
	}

	public void setTMMoves(String[] mPossibleMoves)
	{
		m_tmMoves = mPossibleMoves;
	}

	public void setType(PokemonType[] mType)
	{
		m_type = mType;
	}

	public void setType1(String mType1)
	{
		m_type1 = mType1;
	}

	public void setType2(String mType2)
	{
		m_type2 = mType2;
	}

	public void setWeight(float mWeight)
	{
		m_weight = mWeight;
	}

	/**
	 * This methods prevents pokemon with arbitrary base stats from being
	 * loaded. Pokemon are unserialised only by id and their stats are loaded
	 * from that id.
	 * This method creatively throws an IOException if the species id does not
	 * correspond to a valid pokemon species.
	 * This method works from the default species data. To use this with
	 * arbitrary species data, use the <code>readFromStream</code> method.
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		PokemonSpecies species;
		try
		{
			species = m_default.getSpecies(m_species);
		}
		catch(PokemonException e)
		{
			throw new IOException();
		}
		m_name = species.m_name;
		m_base = species.m_base;
		m_type = species.m_type;
		m_genders = species.m_genders;
	}

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();
	}

	public int getTier()
	{
		return tier;
	}

	public void setTier(int t)
	{
		tier = t;
	}
}
