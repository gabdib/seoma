package world.unit.units;

import world.owner.Owner;
import world.unit.Unit;
import world.unit.unitModifiers.Builder;

/**
 * a basic builder
 * @author Jack
 *
 */
public class Engineer extends Unit implements Builder
{

	public Engineer(Owner o, double x, double y)
	{
		super("engineer", o, x, y, 20, 17, 35, 80, null, 9, 30);
	}
	public boolean canBuild(Class<? extends Unit> c)
	{
		return c == Factory.class || c == Refinery.class || c == DefenseTurret.class;
	}
}
