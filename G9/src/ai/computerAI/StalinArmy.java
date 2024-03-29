package ai.computerAI;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.media.opengl.GL;

import ui.userIO.userInput.UserInput;
import utilities.Camera;
import utilities.MathUtil;
import world.World;
import world.owner.Owner;
import world.resource.ResourceDeposit;
import world.unit.Unit;
import world.unit.units.DefenseTurret;
import world.unit.units.Engineer;
import world.unit.units.Factory;
import world.unit.units.Harvester;
import world.unit.units.Leader;
import world.unit.units.Refinery;
import world.unit.units.Tank;
import ai.AI;
import ai.aiModule.CameraModule;

public class StalinArmy extends AI {
	final static boolean dEnabled = false;
	Random rand = null;
	Owner me;
	ArrayList<UnitGroup> swarmGroups;
	HashMap<ResourceDeposit, ResourceContext> rHash;
	CameraModule cm = new CameraModule('w', 'd', 's', 'a', 'r', 'f', 1000.0, 1.0);
	//HashMap<Class, Long> resourceLimits;
	int minHarvester = 10;
	
	int it = 0;
	
	int minSwarmSize = 100;
	
	final static int factorycost = 100;
	final static int refinerycost = 50;
	final static int turretcost = 70;
	
	/* Set to 5 to disable rect monitor */
	int currSet = 5;
	
	final static int borderMod = 0;
	
	HashMap<Unit, UnitContext> uHash;
	
	boolean starting = true;
	
	public StalinArmy(Owner o) {
		super(o);
		println("Initialized");
		swarmGroups = new ArrayList<UnitGroup>();
		uHash = new HashMap<Unit, UnitContext>();
		rHash = new HashMap<ResourceDeposit, ResourceContext>();
		registerAIModule(cm);
		//resourceLimits = new HashMap<Class, Long>();
		//initializeResourceLimits();
		me = o;
	}
	
	/*private void initializeResourceLimits()
	{
		resourceLimits.put(Harvester.class, new Long(10));
		resourceLimits.put(Tank.class, new Long(10));
		resourceLimits.put(Factory.class, new Long(10));
		resourceLimits.put(Refinery.class, new Long(10));
		resourceLimits.put(DefenseTurret.class, new Long(10));
		resourceLimits.put(Engineer.class, new Long(10));
	}
	
	private void modifyResourceLimitForClass(Class c, Long l)
	{
		resourceLimits.remove(c);
		resourceLimits.put(c, l);
	}*/
	
	private void println(String s)
	{
		if (dEnabled)
			System.out.println("StalinArmy: "+s);
	}

	public void drawUI(GL gl) {}
	public Camera getCamera() { return cm.getCamera(); }
	
	protected void performAIFunctions(World w,
			HashMap<Class<? extends UserInput>, ArrayList<UserInput>> ui)
	{
		LinkedList<Unit> units = getUnits(w);
		Iterator<Unit> i = units.iterator();
		while(i.hasNext())
		{
			Unit u = i.next();
			
			//This REALLY could be optimized
			if (uHash.get(u) == null)
			{
				ResourceDeposit ird = getClosestResourceDepositAbs(u.getLocation(), w);
				uHash.put(u, new UnitContext(u));
				uHash.get(u).assocR = rHash.get(ird);
				rHash.get(ird).units.add(u);
				if (u instanceof Engineer || u instanceof Leader)
				{
					ird = getClosestRDForEngineer(u.getLocation(), w);
					if (ird != null)
					{
						uHash.get(u).eR = rHash.get(ird);
						rHash.get(ird).eng = u;
					}
				}
			}
				
			
			if (u.getCurrentAction() != null) continue;
			
			if (u instanceof Harvester)
			{
				ResourceDeposit rd = getClosestResourceDeposit(u.getLocation(), w);
				if (rd == null)
					rd = getClosestResourceDepositAbs(u.getLocation(), w);
				double[][] rect = getEnemyRect(getEnemyOwners(w)[0], w);
				if (rect == null && uHash.get(u).special != 0)
					continue;
					
				if (getUnitCount(Harvester.class) > minHarvester)
				{
					if (currSet != 5)
					{
						uHash.get(u).special = currSet++;
					}
				}
				switch (uHash.get(u).special)
				{
				case 1:
					moveUnit(u, rect[0][0], rect[0][1], w);
					break;
				case 2:
					moveUnit(u, rect[1][0], rect[0][1], w);
					break;
				case 3:
					moveUnit(u, rect[1][0], rect[1][1], w);
					break;
				case 4:
					moveUnit(u, rect[0][0], rect[1][1], w);
					break;
				}
				if (uHash.get(u).special != 0)
				{
					/* We make special units indestructable */
					u.setSelected(true);
					u.setLife(1000000000);
					continue;
				}
				
				if (rd != null)
				{
					if (!gatherResources(u, rd, w))
						moveUnitSafely(u, rd.getLocation()[0], rd.getLocation()[1], w);
				}
				else
				{
					rect = getAllyRect(w);
					moveUnitRandomlyAroundRect(u, w, rect[0], rect[1][0], rect[1][1]);
				}
			}
			if (u instanceof Leader || u instanceof Engineer)
			{
				
				/*if (getUnitCount(Harvester.class) < minHarvester)
				{
					productionMax = 1;
					maintainUnitCount(w, u, Factory.class, 1, 0);
					double[] loc = uHash.get(u).assocR.r.getLocation();
					moveUnitSafely(u, loc[0], loc[1], w);
					//maintainUnitCount(w, u, DefenseTurret.class, 1, 0);
					maintainUnitCount(w, u, Refinery.class, 1, 0);
					continue;
				}*/
				
				//if (getUnitCount(Refinery.class) != 0 && uHash.get(u).eR != null)
				//	fc = maintainUnitCount(w, u, Factory.class, productionMax, 50);
				
				if (uHash.get(u).eR != null)
				{
					boolean missingR = true, missingF = true;
					
					for (int ii = 0; ii < uHash.get(u).eR.units.size(); ii++)
					{
						Unit eru = uHash.get(u).eR.units.get(ii);
						
						if (eru instanceof Factory)
							missingF = false;
						else if (eru instanceof Refinery)
							missingR = false;
						
						if (!missingR && !missingF)
							break;
					}
					
					//System.out.println("Found R: "+!missingR+" Found F: "+!missingF+" for RD: "+uHash.get(u).eR+" eng: "+u);
					
					if (missingR && getUnitCount(Factory.class) > 0)
					{
						double[] loc = uHash.get(u).eR.r.getLocation();
						moveUnitSafely(u, loc[0], loc[1], w);
						
						if (u.getLocation()[0] == loc[0] && u.getLocation()[1] == loc[1])
							maintainUnitCount(w, u, Refinery.class, getUnitCount(Refinery.class)+1, 0);
					}
					if (missingF)
					{
						double[] loc = uHash.get(u).eR.r.getLocation();
						moveUnitSafely(u, loc[0], loc[1], w);
						
						if (u.getLocation()[0] == loc[0] && u.getLocation()[1] == loc[1])
							maintainUnitCount(w, u, Factory.class, getUnitCount(Factory.class)+1, 0);
					}
					if (!missingF && !missingR)
					{
						if (maintainUnitCount(w, u, Factory.class, getUnitCount(Factory.class)+1, 0) == 0)
							moveUnitRandomlyAroundRD(u, w, uHash.get(u).eR.r);
					}
				}
				else
				{
					double[][] rect = getAllyRect(w);
					if (maintainUnitCount(w, u, DefenseTurret.class, getUnitCount(DefenseTurret.class)+1, 0) == 0)
						moveUnitRandomlyAroundRect(u, w, rect[0], rect[1][0], rect[1][1]);
				}
			}
			if (u instanceof Tank)
			{
				double[][] rect = null;
				
				if (uHash.get(u).swarming)
				{
					Unit target;
					
					target = getFirstEnemyUnit(w, Leader.class);
					if (target == null)
						target = getFirstEnemyUnit(w, Refinery.class);
					if (target == null)
						target = getFirstEnemyUnit(w, Factory.class);
					
					rect = getEnemyRect(getEnemyOwners(w)[0], w);
					if (rect == null)
					{
						uHash.get(u).swarming = false;
						u.setSelected(false);
					}
					else if (target != null)
					{
						moveUnitSafely(u, target.getLocation()[0], target.getLocation()[1], w);
						continue;
					}
				}
				
				if (rect == null)
					rect = getAllyRect(w);
				
				moveUnitRandomlyAroundRect(u, w, rect[0], rect[1][0], rect[1][1]);
			}
			if (u instanceof Factory)
			{
				boolean missingR = getUnitCount(Refinery.class) == 0;
				if (missingR && getUnitCount(Engineer.class) == 0 && getUnitCount(Leader.class) == 0)
					maintainUnitCount(w, u, Engineer.class, 1, 0);
				else if (!missingR)
				{
					maintainUnitCount(w, u, Harvester.class, minHarvester, 0);
					maintainUnitCount(w, u, Tank.class, getUnitCount(Tank.class)+1, 0);
					maintainUnitCount(w, u, Engineer.class, getResourceDeposits(w).size() - getUnitCount(Leader.class), 0);
					maintainUnitCount(w, u, Harvester.class, getResourceDeposits(w).size() * minHarvester, 0);
				}
			}
		}
		
		ArrayList<ResourceDeposit> rds = getResourceDeposits(w);
		for (ResourceDeposit prd : rds)
		{
			rHash.get(prd).eng = null;
			rHash.get(prd).units.clear();
		}
		
		int usableTankCount = 0;
		
		i = units.iterator();
		while (i.hasNext())
		{
			Unit un = i.next();
			if (uHash.get(un).eR != null)
			{
				if (uHash.get(un).eR.eng != null)
				{
					System.out.println("More than 1 engineer per deposit");
					//while(true);
				}
				uHash.get(un).eR.eng = un;
			}
			if (un instanceof Tank && !uHash.get(un).swarming)
				usableTankCount++;
			ResourceDeposit ird = getClosestResourceDepositAbs(un.getLocation(), w);
			uHash.get(un).assocR = rHash.get(ird);
			uHash.get(un).assocR.units.add(un);
		}

		if (it % 100 == 0)
			System.out.println(usableTankCount + " "+ w.getUnitEngine().getUnitList(getEnemyOwners(w)[0]).size() * 2 + " "+minSwarmSize);
		
		i = units.iterator();
		if ((usableTankCount >= minSwarmSize || 
			(getEnemyOwners(w)[0] != null && 
			 usableTankCount >= w.getUnitEngine().getUnitList(getEnemyOwners(w)[0]).size() * 2)) &&
			 getEnemyRect(getEnemyOwners(w)[0], w) != null)
		{
			System.out.println("Swarming with "+usableTankCount+" units against "+w.getUnitEngine().getUnitList(getEnemyOwners(w)[0]).size()+" units");
			while (i.hasNext())
			{
				Unit un = i.next();
				if (un instanceof Tank && !uHash.get(un).swarming)
				{
					Unit target;
					
					target = getFirstEnemyUnit(w, Leader.class);
					if (target == null)
						target = getFirstEnemyUnit(w, Refinery.class);
					if (target == null)
						target = getFirstEnemyUnit(w, Factory.class);
					
					double[][] rect = getEnemyRect(getEnemyOwners(w)[0], w);
					uHash.get(un).swarming = true;
					un.setSelected(true);
					usableTankCount--;
					un.clearActions();
					if (target != null)
						moveUnitSafely(un, target.getLocation()[0], target.getLocation()[1], w);
					else
						moveUnitRandomlyAroundRect(un, w, rect[0], rect[1][0], rect[1][1]);
				}
			}
			minSwarmSize += 50;
		}
	}
	
	private int maintainUnitCount(World w, Unit u, Class<? extends Unit> c, int unitCount, int randMov)
	{
		boolean ret = true;
		int i = 0;
		
		if (getUnitCount(c) >= unitCount)
			return -1;
		
		/*if ((unitCount - getUnitCount(c)) * resPerUnit < (resourceLimits.get(c) / 100) * me.getResources())
		{
			println("Construction not allowed due to resource shortage");
			println("Units requested: "+(unitCount - getUnitCount(c)));
			println("Resource limit: "+(resourceLimits.get(c) / 100) * me.getResources());
			return -2;
		}*/
		
		for (int x = getUnitCount(c); x < unitCount && ret; x++)
		{
			if (c == Factory.class && me.getResources() < factorycost + turretcost)
				break;
			else if (c == Refinery.class && me.getResources() < refinerycost + turretcost)
				break;
			
			ret = buildUnit(c, u, w);
			if (ret)
			{
				if (c == Factory.class || c == Refinery.class)
					ret = buildUnit(DefenseTurret.class, u, w);
				
				i++;
			}
			
			//println("Building "+c+": " + (ret ? "Success" : "Failed"));
		}
		
		return i;
	}
	private ResourceDeposit getClosestRDForEngineer(double[] p, World w)
	{
		ArrayList<ResourceDeposit> deposits = getResourceDeposits(w);
		int closest = -1;
		double dist = Double.MAX_VALUE;
		for(int i = 0; i < deposits.size(); i++)
		{
			ResourceDeposit rd = deposits.get(i);
			if (rHash.get(rd) == null)
				rHash.put(rd, new ResourceContext(rd));
			double thisDist = MathUtil.distance(p[0], p[1], rd.getLocation()[0], rd.getLocation()[1]);
			if(thisDist < dist && rHash.get(rd).eng == null)
			{
				closest = i;
				dist = thisDist;
			}
		}
		
		if (closest == -1)
			return null;
		
		//println("New base index: "+closest);
		
		return deposits.get(closest);
	}
	private ResourceDeposit getClosestResourceDepositAbs(double[] p, World w)
	{
		ArrayList<ResourceDeposit> deposits = getResourceDeposits(w);
		int closest = -1;
		double dist = Double.MAX_VALUE;
		for(int i = 0; i < deposits.size(); i++)
		{
			ResourceDeposit rd = deposits.get(i);
			if (rHash.get(rd) == null)
				rHash.put(rd, new ResourceContext(rd));
			double thisDist = MathUtil.distance(p[0], p[1], rd.getLocation()[0], rd.getLocation()[1]);
			if(thisDist < dist)
			{
				closest = i;
				dist = thisDist;
			}
		}
		
		return deposits.get(closest);
	}
	private ResourceDeposit getClosestResourceDepositByLevelAndDanger(double[] p, World w, int level, boolean fearless)
	{
		ArrayList<ResourceDeposit> deposits = getResourceDeposits(w);
		int closest = -1;
		double dist = Double.MAX_VALUE;
		for(int i = 0; i < deposits.size(); i++)
		{
			ResourceDeposit rd = deposits.get(i);
			if (rHash.get(rd) == null)
				rHash.put(rd, new ResourceContext(rd));
			boolean danger = false;
			for (int j = 0; j < w.getOwners().length; j++)
			{
				Owner o = w.getOwners()[j];
				if (o == me) continue;
				
				double[][] rect = getEnemyRect(o, w);
				if (rect == null)
					continue;
				double[] loc = rd.getLocation();
				if (loc[0] >= rect[0][0] && loc[0] <= rect[1][0] &&
					loc[1] >= rect[0][1] && loc[1] <= rect[1][1])
				{
					//println("Dangerous rd: "+loc[0]+", "+loc[1]);
					danger = true;
					break;
				}
			}
			double thisDist = MathUtil.distance(p[0], p[1], rd.getLocation()[0], rd.getLocation()[1]);
			if(thisDist < dist && rd.getTotalResources() > level && (!danger || fearless))
			{
				closest = i;
				dist = thisDist;
			}
		}
		
		if (closest == -1)
			return null;
		
		return deposits.get(closest);
	}
	private ResourceDeposit getClosestResourceDeposit(double[] p, World w)
	{
		int i;
		ResourceDeposit rd;
		boolean fearless = false;
		
		for(;;)
		{
			i = 50;
			do
				rd = getClosestResourceDepositByLevelAndDanger(p, w, i--, fearless);
			while (i > 0 && rd == null);
			
			if (rd != null)
				return rd;
			
			fearless = !fearless;
			
			if (!fearless)
				return null;
		}
	}
	private Owner[] getEnemyOwners(World w)
	{
		for (;;)
		{
			int x = 0, i = 0, y = 0;
			
			for (Owner o : w.getOwners())
				if (o != me && w.getUnitEngine().getUnitList(o).size() != 0)
					x++;
			
			if (x == 0)
				return new Owner[] {null};
			
			Owner[] ret = new Owner[x];
			
			i = 1;
			while (x > 0)
			{
				for (Owner o : w.getOwners())
				{
					if (o == me)
						continue;
					
					if (w.getUnitEngine().getUnitList(o).size() == i)
					{
						if (y == ret.length)
						{
							println("!Correct in size: "+y+" "+ret.length);
							continue;
						}
						
						ret[y++] = o;
						x--;
					}
				}
				i++;
			}
			
			for (i = 0; i < ret.length; i++)
			{
				for (int j = 0; j < ret.length; j++)
				{
					if (j == i)
						continue;
					
					if (ret[j] == ret[i])
					{
						println("!Correct in ptr eval");
						continue;
					}
				}
			}
			
			if (++it % 500 == 0)
			{
				for (x = 0; x < ret.length; x++)
				{
					println("ret["+x+"] = "+ret[x]);
					println("Owner "+x+": "+w.getUnitEngine().getUnitList(ret[x]).size());
				}
			}
			
			return ret;
		}
	}
	private void moveUnitRandomlyAroundRD(Unit u, World w, ResourceDeposit rd)
	{
		double[] modloc = new double[] {rd.getLocation()[0], rd.getLocation()[1]};
		
		modloc[0] -= rd.getRadius();
		modloc[1] -= rd.getRadius();
		
		moveUnitRandomlyAroundArea(u, w, modloc, (int)(rd.getRadius() * 2));
	}
	public void moveUnitRandomly(Unit u, World w)
	{
		moveUnitSafely(u, rand.nextDouble()*w.getMapWidth(), rand.nextDouble()*w.getMapHeight(), w);
	}
	public void moveUnitRandomlyAroundArea(Unit u, World w, double[] p, int bounds)
	{
			moveUnitSafely(u, p[0]-bounds+rand.nextDouble()*bounds*2, p[1]-bounds+rand.nextDouble()*bounds*2, w);
	}
	public void moveUnitRandomlyAroundRect(Unit u, World w, double[] p, double xbound, double ybound)
	{
			moveUnitSafely(u, p[0]-xbound+rand.nextDouble()*xbound*2, p[1]-ybound+rand.nextDouble()*ybound*2, w);
	}
	private void moveUnitSafely(Unit u, double x, double y, World w)
	{
		for (Owner o : w.getOwners())
		{
			if (o == me) continue;
			double[][] rect = getEnemyRect(o, w);
			if (rect == null)
				continue;
			if (x >= rect[0][0] && x <= rect[1][0] &&
				y >= rect[0][1] && y <= rect[1][1])
			{
				if (uHash.get(u).swarming)
				{
					//println("Swarming intersection with hostile area allowed");
					moveUnit(u, x, y, w);
				}
				else
				{
					//println("Non-swarming intersection with hostile area denied");
				}
				return;
			}
		}
		
		moveUnit(u, x, y, w);
	}
	/*private double[][] getEnemyFrontLine(Owner o, World w, double[] loc)
	{
		double[][] recta = getEnemyRect(o, w), line = new double[2][2], rectb = new double[4][2];
		double closesta = Double.MAX_VALUE, closestb = Double.MAX_VALUE;
		
		if (recta == null)
			return null;
		
		rectb[0] = recta[0];
		rectb[1][0] = recta[1][0];
		rectb[1][1] = recta[0][1];
		rectb[2] = recta[1];
		rectb[3][0] = recta[0][0];
		rectb[3][1] = recta[1][1];
		
		for (int i = 0; i < rectb.length; i++)
		{
			double dist = MathUtil.distance(loc[0], loc[1], rectb[i][0], rectb[i][1]);
			if (dist < closesta)
			{
				closesta = dist;
				line[0] = rectb[i];
			}
			else if (dist < closestb)
			{
				closestb = dist;
				line[1] = rectb[i];
			}
		}
		
		if (closesta == Double.MAX_VALUE || closestb == Double.MAX_VALUE)
		{
			println("Problem detected: Front line code");
			while (true);
		}
		
		return line;
	}*/
	public double[][] getAllyRect(World w)
	{
		double[][] ret = new double[2][2];
		
		ret[0][0] = Double.MAX_VALUE;
		ret[0][1] = Double.MAX_VALUE;
		ret[1][0] = 0;
		ret[1][1] = 0;
		
		LinkedList<Unit> units = getUnits(w);
		for (Unit u : units)
		{
			if (u.getLocation()[0] < ret[0][0])
				ret[0][0] = u.getLocation()[0];
			
			if (u.getLocation()[1] < ret[0][1])
				ret[0][1] = u.getLocation()[1];

			if (u.getLocation()[0] > ret[1][0])
				ret[1][0] = u.getLocation()[0];
			
			if (u.getLocation()[1] > ret[1][1])
				ret[1][1] = u.getLocation()[1];
		}
		
		ret[0][0] -= borderMod;
		if (ret[0][0] < 0)
			ret[0][0] = 0;
		
		ret[0][1] -= borderMod;
		if (ret[0][1] < 0)
			ret[0][1] = 0;
		
		ret[1][0] += borderMod;
		if (ret[1][0] > w.getMapWidth())
			ret[1][0] = w.getMapWidth();
		
		ret[1][1] += borderMod;
		if (ret[1][1] > w.getMapHeight())
			ret[1][1] = w.getMapHeight();
		
		//println("("+ret[0][0]+","+ret[0][1]+") ("+ret[1][0]+","+ret[1][1]+")");
		
		return ret;
	}
	//Returns 2 arrays of 2 points which represent the upper-left most point and the lower-right most point
	public  double[][] getEnemyRect(Owner o, World w)
	{
		double[][] ret = new double[2][2];
		boolean valid = false;
		
		ret[0][0] = Double.MAX_VALUE;
		ret[0][1] = Double.MAX_VALUE;
		ret[1][0] = 0;
		ret[1][1] = 0;
		
		if (o == null)
			return null;
		
		LinkedList<Unit> units = w.getUnitEngine().getUnitList(o);
		for (Unit u : units)
		{
			/* We don't care about it if it is unarmed */
			if (u.getWeapon() == null)
				continue;
			
			valid = true;
			
			if (u.getLocation()[0] < ret[0][0])
				ret[0][0] = u.getLocation()[0];
			
			if (u.getLocation()[1] < ret[0][1])
				ret[0][1] = u.getLocation()[1];

			if (u.getLocation()[0] > ret[1][0])
				ret[1][0] = u.getLocation()[0];
			
			if (u.getLocation()[1] > ret[1][1])
				ret[1][1] = u.getLocation()[1];
		}
		
		if (!valid)
		{
			for (Unit u : units)
			{
				valid = true;
				
				if (u.getLocation()[0] < ret[0][0])
					ret[0][0] = u.getLocation()[0];
				
				if (u.getLocation()[1] < ret[0][1])
					ret[0][1] = u.getLocation()[1];

				if (u.getLocation()[0] > ret[1][0])
					ret[1][0] = u.getLocation()[0];
				
				if (u.getLocation()[1] > ret[1][1])
					ret[1][1] = u.getLocation()[1];
			}
		}
		
		if (!valid)
			return null;
		
		ret[0][0] -= borderMod;
		if (ret[0][0] < 0)
			ret[0][0] = 0;
		
		ret[0][1] -= borderMod;
		if (ret[0][1] < 0)
			ret[0][1] = 0;
		
		ret[1][0] += borderMod;
		if (ret[1][0] > w.getMapWidth())
			ret[1][0] = w.getMapWidth();
		
		ret[1][1] += borderMod;
		if (ret[1][1] > w.getMapHeight())
			ret[1][1] = w.getMapHeight();
		
		//println("("+ret[0][0]+","+ret[0][1]+") ("+ret[1][0]+","+ret[1][1]+")");
		
		return ret;
	}

	private int getUnitCount(Class<? extends Unit> c)
	{
		int i = 0;
	
		//Easy case
		if (getUnits().get(c) == null ||
			getUnits().get(c).size() == 0)
			return 0;
		
		for (Unit u : getUnits().get(c))
			if (!u.isDead())
				i++;

		return i;
	}

	private Unit getFirstEnemyUnit(World w, Class<? extends Unit> c)
	{
		//Easy case
		if (getEnemyUnits(w).get(c) == null ||
			getEnemyUnits(w).get(c).size() == 0)
			return null;
		
		for (Unit u : getEnemyUnits(w).get(c))
			return u;

		return null;
	}

	public void initialize(World w) {
		rand = new Random(w.getSeed());
	}
}

class ResourceContext {
	public ArrayList<Unit> units;
	public ResourceDeposit r;
	public Unit eng;
	
	public ResourceContext(ResourceDeposit r)
	{
		this.r = r;
		units = new ArrayList<Unit>();
	}
}

class UnitContext {
	public Unit u;
	public UnitGroup swarmGroup;
	public ResourceContext eR;
	public ResourceContext assocR;
	public boolean swarming;
	public int special;
	
	public UnitContext(Unit u)
	{
		this.u = u;
		this.swarmGroup = null;
		this.assocR = null;
	}
}

class UnitGroup {
	public ArrayList<Unit> u;
	public long stTime;
	
	public UnitGroup(ArrayList<Unit> u)
	{
		this.u = u;
	}
}
