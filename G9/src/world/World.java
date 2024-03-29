package world;

import geom.Boundable;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.media.opengl.GL;

import mapEditor.Map;
import pathFinder.PathFinder;
import pathFinder.epfv4.EPFV4;
import ui.userIO.userInput.UserInput;
import utilities.Polygon;
import utilities.region.RectRegion;
import utilities.region.Region;
import world.animation.AnimationEngine;
import world.owner.Owner;
import world.resource.ResourceEngine;
import world.shot.Shot;
import world.shot.ShotEngine;
import world.unit.Unit;
import world.unit.UnitEngine;
import ai.AI;

/**
 * holds information about the game world, presents a front that routes game information
 * to wherever it needs to be via the register method (shots automatically sent to shot engine,
 * units automatically sent to unit engine)
 * @author Jack
 *
 */
public class World
{
	String name;
	String description;
	
	ShotEngine se;
	UnitEngine ue;
	ResourceEngine re;
	AnimationEngine ae = new AnimationEngine();
	
	Owner[] o;
	ArrayList<Polygon> p = new ArrayList<Polygon>();
	ArrayList<Region> startLocations = new ArrayList<Region>();
	//Polygon[] p = new Polygon[10];
	PathFinder pf;
	HashMap<Owner, AI> ais;
	long seed;
	
	/**
	 * the ai whose camera is used to display the world
	 */
	AI cameraAI;
	
	int width;
	int height;
	
	long updateTime = 0;
	long updates = 0;
	
	long drawTime = 0;
	long drawUpdates = 0; //number of times the world has been drawn
	long gameTime = 0; //amount of time proccessed by the world
	
	public ShotEngine getShotEngine()
	{
		return se;
	}
	public Owner[] getOwners()
	{
		return o;
	}
	public ResourceEngine getResourceEngine()
	{
		return re;
	}
	public HashMap<Owner, AI> getAIs()
	{
		return ais;
	}
	/**
	 * gets the seed to be used for random values
	 * @return
	 */
	public long getSeed()
	{
		return seed;
	}
	/**
	 * sets the ai whose camera is to be used in displaying the world
	 * @param ai
	 */
	public void setCameraAI(AI ai)
	{
		cameraAI = ai;
	}
	public World(Owner[] owners, HashMap<Owner, AI> ais, long seed, String mapPath)
	{
		this.seed = seed;
		this.ais = ais;
		Map m = new Map();
		try
		{
			File f = new File(mapPath);
			FileInputStream fis = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(fis);
			System.out.println("loading "+f.getAbsolutePath());
			m.readMap(dis);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		width = m.getWidth();
		height = m.getHeight();
		this.o = owners;
		
		p = m.getPolygons();
		startLocations = m.getStartLocations();
		
		se = new ShotEngine(this, p.toArray(new Polygon[p.size()]));
		ue = new UnitEngine(this);
		re = new ResourceEngine(m.getResourceDeposits());
		
		
		pf = new EPFV4(m.getWidth(), m.getHeight(), p.toArray(new Polygon[p.size()]));
		for(Owner o: ais.keySet())
		{
			ais.get(o).initialize(this);
		}
	}
	public ArrayList<Region> getStartLocations()
	{
		return startLocations;
	}
	public PathFinder getPathFinder()
	{
		return pf;
	}
	public UnitEngine getUnitEngine()
	{
		return ue;
	}
	public int getMapWidth()
	{
		return width;
	}
	public int getMapHeight()
	{
		return height;
	}
	/**
	 * tests to see if the passed location is inside the game world
	 * @param x
	 * @param y
	 * @return returns true if the passed location is in the game world,
	 * false otherwise
	 */
	public boolean inWorld(double x, double y)
	{
		return new RectRegion(0, 0, width, height).contains(x, y);
	}
	public void updateWorld(double tdiff, HashMap<Byte, HashMap<Class<? extends UserInput>, ArrayList<UserInput>>> ui)
	{
		long start = System.currentTimeMillis();
		for(int i = o.length-1; i >= 0; i--)
		{
			try
			{
				ais.get(o[i]).performAIFunctions(this, ui.get(o[i].getID()), tdiff);
			}
			catch(Exception e)
			{
				//e.printStackTrace();
			}
		}
		ue.updateUnitEngine(tdiff, this);
		se.updateShotEngine(tdiff, ue);
		re.updateResourceEngine(tdiff, this);
		ae.updateAnimationEngine(tdiff);
		
		updateTime+=System.currentTimeMillis()-start;
		updates++;
		gameTime+=tdiff*1000;
		
		if(updates != 0 && updates%1500 == 0)
		{
			System.out.println("world time per update = "+updateTime+" [total time] / "+updates+" [updates] = "+(updateTime*1./updates));
			System.out.println("total game time simulated (ms) = "+gameTime);
			System.out.println("----------------------------------");
		}
	}
	public AnimationEngine getAnimationEngine()
	{
		return ae;
	}
	/**
	 * draws the world
	 * @param owner the owner of the game engine that is drawing the world,
	 * the ui of the ai belonging to this passed owner is drawn
	 * @param dwidth width of the displayed region of the screen
	 * @param dheight height of the displayed region of the screen
	 * @param gl
	 */
	public void drawWorld(Owner owner, int dwidth, int dheight, GL gl)
	{
		long start = System.currentTimeMillis();
		if(cameraAI != null && cameraAI.getCamera() != null)
		{
			/*gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrtho(0, dwidth, 0, dheight, -1, 1);
			
			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glLoadIdentity();*/
			
			cameraAI.getCamera().updateGL(gl, dwidth, dheight);
			cameraAI.getCamera().updateCamera(gl);
			
			gl.glColor4d(.4, .4, .4, .6);
			new RectRegion(0, 0, width, height).drawRegion(gl, -1);
			
			int x = (int)cameraAI.getCamera().getLocation()[0];
			int y = (int)cameraAI.getCamera().getLocation()[1];
			
			//double[] dim = cameraAI.getCamera().unproject(dwidth, 0);
			double[] dim = {dwidth*cameraAI.getCamera().getZoom(), dheight*cameraAI.getCamera().getZoom()};
			//System.out.println(dim[0]+", "+dim[1]);
			
			ae.drawAnimiations(gl, dwidth, dheight);
			try
			{
				HashSet<Boundable> r = ue.getAllUnits().intersects(x, y, dim[0], dim[1]);
				//HashSet<Region> r = ue.getAllUnits().getIntersections(x, y, dwidth, dheight);
				Iterator<Boundable> i = r.iterator();
				while(i.hasNext())
				{
					Unit u = (Unit)i.next();
					u.draw(gl);
				}
				gl.glColor4d(1, 1, 1, 1);
				//ue.getAllUnits().drawPartition(gl, width, height);
			}
			catch(ConcurrentModificationException e){}
			try
			{
				HashSet<Boundable> r = se.getShots().intersects(x, y, dim[0], dim[1]);
				//HashSet<Region> r = se.getShots().getIntersections(x, y, dwidth, dheight);
				Iterator<Boundable> i = r.iterator();
				while(i.hasNext())
				{
					Shot s = (Shot)i.next();
					s.drawShot(gl);
					if(s.isDead())
					{
						//System.out.println("dead and should not be drawn...");
					}
				}
				//se.getShotPartition().drawPartition(gl, width, height);
			}
			catch(ConcurrentModificationException e){}
			
			try
			{
				re.drawResourceDeposits(gl);
			}
			catch(ConcurrentModificationException e){}
		
			/*gl.glLineWidth(1);
			for(int a = 0; a < p.length; a++)
			{
				gl.glColor4d(0, 1, 0, .6);
				p[a].drawPolygon(gl, .1);
			}*/
			gl.glLineWidth(1);
			gl.glColor4d(0, 1, 0, .6);
			Iterator<Polygon> pi = p.iterator();
			while(pi.hasNext())
			{
				pi.next().drawPolygon(gl, .1);
			}
			
			//gl.glLineWidth(1);
			gl.glColor4d(1, 1, 1, .3);
			//pf.drawPathing(gl, false);
			
			if(ais.get(owner) != null)
			{
				ais.get(owner).drawUI(gl);
			}
			
			gl.glClearColor(0, 0, 0, 1);
		}
		
		drawTime+=System.currentTimeMillis()-start;
		drawUpdates++;
		if(drawUpdates%500 == 0)
		{
			System.out.println("average world draw time (ms) = "+drawTime+" [total time] / "+drawUpdates+" [draw updates] = "+(drawTime*1./drawUpdates));
			System.out.println("----------------");
		}
	}
}
