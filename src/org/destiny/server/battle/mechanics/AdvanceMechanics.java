package org.destiny.server.battle.mechanics;

import org.destiny.server.battle.BattleField;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.mechanics.moves.MoveList;
import org.destiny.server.battle.mechanics.moves.PokemonMove;
import org.destiny.server.battle.mechanics.statuses.StatChangeEffect;
import org.destiny.server.battle.mechanics.statuses.field.FieldEffect;
import org.destiny.server.battle.mechanics.statuses.field.HailEffect;
import org.destiny.server.battle.mechanics.statuses.field.RainEffect;
import org.destiny.server.battle.mechanics.statuses.field.SandstormEffect;
import org.destiny.server.battle.mechanics.statuses.field.SunEffect;

/**
 * This class represents the mechanics in the advanced generation of pokemon.
 * 
 * @author Colin
 */
public class AdvanceMechanics extends BattleMechanics
{

	@SuppressWarnings("unused")
	private static int m_log = 0;
	private static final long serialVersionUID = -2238204671194997172L;

	/** Creates a new instance of AdvanceMechanics */
	public AdvanceMechanics(int bytes)
	{
		super(bytes);
	}

	/**
	 * Return whether the move hit.
	 */
	@Override
	public boolean attemptHit(PokemonMove move, Pokemon user, Pokemon target)
	{
		BattleField field = user.getField();
		double accuracy = 0;
		switch(move.getMoveListEntry().getName())
		{
			case "Thunder":
				if(field.getEffectByType(RainEffect.class) != null)
					accuracy = 1;
				else if(field.getEffectByType(SunEffect.class) != null)
					accuracy = 0.5;
				else
					accuracy = move.getAccuracy();
				break;
			case "Hurricane":
				if(field.getEffectByType(RainEffect.class) != null)
					accuracy = 1;
				else if(field.getEffectByType(SunEffect.class) != null)
					accuracy = 0.5;
				else
					accuracy = move.getAccuracy();
				break;
			case "Blizzard":
				if(field.getEffectByType(HailEffect.class) != null)
					accuracy = 1;
				else
					accuracy = move.getAccuracy();
				break;
			default:
				accuracy = move.getAccuracy();
				break;
		}

		boolean hit;
		if(accuracy != 0.0 && (user.hasAbility("No Guard") || target.hasAbility("No Guard") || user.hasEffect(MoveList.LockOnEffect.class)))
			hit = true;
		else
		{
			double evasion = 1;

			if(target.hasAbility("Snow Cloak") && field.getEffectByType(HailEffect.class) != null)
			{
				evasion = target.getEvasion().getMultiplier() * 1.2;
			}
			else if(target.hasAbility("Sand Veil") && field.getEffectByType(SandstormEffect.class) != null)
			{
				evasion = target.getEvasion().getMultiplier() * 1.2;
			}
			else
			{
				evasion = target.getEvasion().getMultiplier();
			}

			// check if items make a difference
			if(target.hasItem("Lax Incense"))
			{
				evasion *= 1.05;
			}
			else if(user.hasItem("Wide Lens"))
			{
				accuracy *= 1.1;
			}
			else if(target.hasItem("BrightPowder"))
			{
				evasion *= 1.1;
			}
			else if(user.hasItem("Zoom Lens"))
			{
				accuracy *= 1.2;
			}

			double effective = accuracy * user.getAccuracy().getMultiplier() / evasion;
			if(effective > 1.0)
				effective = 1.0;

			hit = field.getRandom().nextDouble() <= effective;
		}
		if(!hit)
			field.showMessage(user.getName() + "'s attack missed!");
		return hit;
	}

	@Override
	public strictfp int calculateDamage(PokemonMove move, Pokemon attacker, Pokemon defender, boolean silent)
	{

		final BattleField field = attacker.getField();
		PokemonType moveType = move.getType();
		final boolean special = isMoveSpecial(move);
		int power = move.getPower();
		boolean isCritical = move.canCriticalHit() && isCriticalHit(move, attacker, defender);

		double attack = attacker.getStat(special ? Pokemon.S_SPATTACK : Pokemon.S_ATTACK);

		if((attacker.getName().equalsIgnoreCase("Cubone") || attacker.getName().equalsIgnoreCase("Marowak")) && attacker.hasItem("Thick Club"))
		{
			if(!special)
				attack *= 2;
		}
		int defStat = special ? Pokemon.S_SPDEFENCE : Pokemon.S_DEFENCE;

		StatMultiplier mul = defender.getMultiplier(defStat);
		double defMultiplier = mul.getMultiplier();
		if(isCritical && defMultiplier > 1.0)
			defMultiplier = mul.getSecondaryMultiplier();
		double defence = defender.getStat(defStat, defMultiplier);

		final int random = field.getRandom().nextInt(16) + 85;

		double multiplier = move.getEffectiveness(attacker, defender);

		if(multiplier > 1.0)
		{
			if(!silent)
				field.showMessage("It's super effective!");
			if(attacker.getItemName().equalsIgnoreCase("Expert Belt"))
				power *= 1.2;
		}
		else if(multiplier == 0.0)
		{
			if(!silent)
				field.showMessage("It doesn't affect " + defender.getName() + "...");
			// Just return now to prevent a critical hit from occurring.
			return 0;
		}
		else if(multiplier < 1.0)
			if(!silent)
				field.showMessage("It's not very effective...");

		final boolean stab = attacker.isType(moveType);
		double stabFactor = attacker.hasAbility("Adaptability") ? 2.0 : 1.5;

		// check if the user is holding an item
		// and if the item it is holding effects his power
		switch(attacker.getItemName())
		{
			case "Adamant Orb":
				if(attacker.getName().equalsIgnoreCase("Dialga") && (move.getType() == PokemonType.T_DRAGON || move.getType() == PokemonType.T_STEEL))
					power *= 1.2;
				break;
			case "Black Belt":
				if(move.getType() == PokemonType.T_FIGHTING)
					power *= 1.2;
				break;
			case "BlackGlasses":
				if(move.getType() == PokemonType.T_DARK)
					power *= 1.2;
				break;
			case "Charcoal":
				if(move.getType() == PokemonType.T_FIRE)
					power *= 1.2;
				break;
			case "Draco Plate":
				if(move.getType() == PokemonType.T_DRAGON)
					power *= 1.2;
				break;
			case "Dragon Fang":
				if(move.getType() == PokemonType.T_DRAGON)
					power *= 1.2;
				break;
			case "Dread Plate":
				if(move.getType() == PokemonType.T_DARK)
					power *= 1.2;
				break;
			case "Earth Plate":
				if(move.getType() == PokemonType.T_GROUND)
					power *= 1.2;
				break;
			case "Fist Plate":
				if(move.getType() == PokemonType.T_FIGHTING)
					power *= 1.2;
				break;
			case "Flame Plate":
				if(move.getType() == PokemonType.T_FIRE)
					power *= 1.2;
				break;
			case "Griseous Orb":
				if(attacker.getName().equalsIgnoreCase("Giratina") && (move.getType() == PokemonType.T_DRAGON || move.getType() == PokemonType.T_GHOST))
					power *= 1.2;
				break;
			case "Hard Stone":
				if(move.getType() == PokemonType.T_ROCK)
					power *= 1.2;
				break;
			case "Icicle Plate":
				if(move.getType() == PokemonType.T_ICE)
					power *= 1.2;
				break;
			case "Insect Plate":
				if(move.getType() == PokemonType.T_BUG)
					power *= 1.2;
				break;
			case "Iron Plate":
				if(move.getType() == PokemonType.T_STEEL)
					power *= 1.2;
				break;
			case "Light Ball":
				if(attacker.getName().equalsIgnoreCase("Pikachu"))
					attack *= 2;
				break;
			case "Lustrous Orb":
				if(attacker.getName().equalsIgnoreCase("Palkia") && (move.getType() == PokemonType.T_DRAGON || move.getType() == PokemonType.T_WATER))
					power *= 1.2;
				break;
			case "Magnet":
				if(move.getType() == PokemonType.T_ELECTRIC)
					power *= 1.2;
				break;
			case "Meadow Plate":
				if(move.getType() == PokemonType.T_GRASS)
					power *= 1.2;
				break;
			case "Metal Coat":
				if(move.getType() == PokemonType.T_STEEL)
					power *= 1.2;
				break;
			case "Mind Plate":
				if(move.getType() == PokemonType.T_PSYCHIC)
					power *= 1.2;
				break;
			case "Miracle Seed":
				if(move.getType() == PokemonType.T_GRASS)
					power *= 1.2;
				break;
			case "Muscle Band":
				if(!special)
					power *= 1.1;
				break;
			case "Mystic Water":
				if(move.getType() == PokemonType.T_WATER)
					power *= 1.2;
				break;
			case "NeverMeltIce":
				if(move.getType() == PokemonType.T_ICE)
					power *= 1.2;
				break;
			case "Odd Incense":
				if(move.getType() == PokemonType.T_PSYCHIC)
					power *= 1.2;
				break;
			case "Pink Bow":
				if(move.getType() == PokemonType.T_NORMAL)
					power *= 1.1;
				break;
			case "Poison Barb":
				if(move.getType() == PokemonType.T_BUG)
					power *= 1.2;
				break;
			case "Polkadot Bow":
				if(move.getType() == PokemonType.T_BUG)
					power *= 1.1;
				break;
			case "Rock Incense":
				if(move.getType() == PokemonType.T_ROCK)
					power *= 1.2;
				break;
			case "Sea Incense":
				if(move.getType() == PokemonType.T_WATER)
					power *= 1.2;
				break;
			case "Sharp Beak":
				if(move.getType() == PokemonType.T_FLYING)
					power *= 1.2;
				break;
			case "Silk Scarf":
				if(move.getType() == PokemonType.T_NORMAL)
					power *= 1.2;
				break;
			case "SilverPowder":
				if(move.getType() == PokemonType.T_BUG)
					power *= 1.2;
				break;
			case "Sky Plate":
				if(move.getType() == PokemonType.T_FLYING)
					power *= 1.2;
				break;
			case "Soft Sand":
				if(move.getType() == PokemonType.T_GROUND)
					power *= 1.2;
				break;
			case "Spell Tag":
				if(move.getType() == PokemonType.T_GHOST)
					power *= 1.2;
				break;
			case "Splash Plate":
				if(move.getType() == PokemonType.T_WATER)
					power *= 1.2;
				break;
			case "Spooky Plate":
				if(move.getType() == PokemonType.T_GHOST)
					power *= 1.2;
				break;
			case "Stone Plate":
				if(move.getType() == PokemonType.T_ROCK)
					power *= 1.2;
				break;
			case "Toxic Plate":
				if(move.getType() == PokemonType.T_POISON)
					power *= 1.2;
				break;
			case "TwistedSpoon":
				if(move.getType() == PokemonType.T_PSYCHIC)
					power *= 1.2;
				break;
			case "Wave Incense":
				if(move.getType() == PokemonType.T_WATER)
					power *= 1.2;
				break;
			case "Wise Glasses":
				if(special)
					power *= 1.1;
				break;
			case "Zap Plate":
				if(move.getType() == PokemonType.T_ELECTRIC)
					power *= 1.2;
				break;
			default:
				break;
		}

		int damage = (int) (((int) ((int) ((int) (2 * attacker.getLevel() / 5.0 + 2.0) * attack * power / defence) / 50.0) + 2) * (random / 100.0) * (stab ? stabFactor : 1.0) * multiplier);

		if(isCritical)
		{
			damage *= attacker.hasAbility("Sniper") ? 3 : 2;
			if(defender.hasAbility("Anger Point"))
			{
				if(!silent)
					field.showMessage(defender.getName() + "'s Anger Point raised its attack!");
				StatChangeEffect eff = new StatChangeEffect(Pokemon.S_ATTACK, true, 12);
				eff.setDescription(null);
				defender.addStatus(defender, eff);
			}
			if(!silent)
				field.showMessage("A critical hit!");
		}

		return damage < 1 ? 1 : damage;
	}

	@Override
	/* TODO: http://bulbapedia.bulbagarden.net/wiki/Individual_values#Generation_III Check please! */
	public int calculateStat(Pokemon p, int i) throws StatException
	{
		if(i < 0 || i > 5)
			throw new StatException();
		if(i == Pokemon.S_HP)
		{
			/* HP is calculated differently. */
			double ivAlgorithmHP = (((p.getIv(i) + (2.0 * p.getBase(i)) + (p.getEv(i) / 4.0) + 100) * p.getLevel()) / 100.0) + 10;
			if(p.getSpeciesName().equals("Shedinja"))
			{
				/* Shedinja always has 1 hp. */
				return 1;
			}
			return (int) ivAlgorithmHP;
		}
		/* Calculate value for other stats. */
		double ivAlgorithm = ((((p.getIv(i) + (2.0 * p.getBase(i)) + (p.getEv(i) / 4.0)) * p.getLevel()) / 100.0) + 5) * p.getNature().getEffect(i);
		return (int) ivAlgorithm;
	}

	public boolean isCriticalHit(PokemonMove move, Pokemon user, Pokemon target)
	{
		if(target.isCriticalImmune())
			return false;

		FieldEffect effect = user.getField().getEffectByType(MoveList.LuckyChantEffect.class);
		if(effect != null)
		{
			MoveList.LuckyChantEffect eff = (MoveList.LuckyChantEffect) effect;
			if(eff.isActive(target.getParty()))
				return false;
		}

		int moveFactor = 0;
		if(move.hasHighCriticalHitRate())
			moveFactor = this instanceof JewelMechanics ? 1 : 3;

		int factor = user.getCriticalHitFactor() + (user.hasItem("Scope Lens") ? 1 : 0) + (user.hasItem("Stick") && user.getName().equalsIgnoreCase("Farfetch'd") ? 2 : 0) /* TODO: + (FE/L * 1) */
				+ moveFactor;
		double chance = 0.0;
		switch(factor)
		{
			case 1:
				chance = 0.0625;
				break;
			case 2:
				chance = 0.125;
				break;
			case 3:
				chance = 0.25;
				break;
			case 4:
				chance = 0.332;
				break;
			default:
				chance = 0.5;
				break;
		}
		return user.getField().getRandom().nextDouble() <= chance;
	}

	/**
	 * Return whether a given move deals special damage.
	 */
	@Override
	public boolean isMoveSpecial(PokemonMove move)
	{
		return move.getType().isSpecial();
	}

	/**
	 * There are several conditions to validate. The total number of effort
	 * points must be less than or equal to 510. There can be no more than 255
	 * effort points per stat. There can be no more than 31 individual
	 * points per stat. The pokemon's level must be in the interval [1, 100].
	 */
	@Override
	public void validateHiddenStats(Pokemon p) throws ValidationException
	{
		int level = p.getLevel();
		if(level < 1 || level > 100)
			throw new ValidationException("Level must be between 1 and 100.");

		int evs = 0;
		for(int i = 0; i < 6; ++i)
		{
			int ev = p.getEv(i);
			evs += ev;
			if(ev > 255)
				throw new ValidationException("No stat can be allocated more than 255 EVs.");
			if(ev < 0)
				throw new ValidationException("EVs cannot be negative.");

			int iv = p.getIv(i);
			if(iv > 31)
				throw new ValidationException("No stat can be given more than 31 IVs.");
			if(iv < 0)
				throw new ValidationException("IVs cannot be negative.");
		}
		if(evs > 510)
			throw new ValidationException("A pokemon cannot have more than 510 EVs in total.");
	}

}
