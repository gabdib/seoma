package mapEditor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL;
import utilities.MathUtil;
import utilities.Polygon;
import utilities.region.RectRegion;
import utilities.region.Region;
import world.resource.ResourceDeposit;

/**
 * represents a game map
 * @author Jack
 *
 */
public class Map
{
	String name = new String("Untitled");
	String description = new String();
	
	int width = 700;
	int height = 700;
	
	ArrayList<Polygon> p = new ArrayList<Polygon>();
	ArrayList<Region> startLocations = new ArrayList<Region>();
	
	ArrayList<ResourceDeposit> resourceDeposits = new ArrayList<ResourceDeposit>();
	
	public void addPolygon(Polygon polygon)
	{
		p.add(polygon);
	}
	public ArrayList<ResourceDeposit> getResourceDeposits()
	{
		return resourceDeposits;
	}
	public ArrayList<Region> getStartLocations()
	{
		return startLocations;
	}
	public ArrayList<Polygon> getPolygons()
	{
		return p;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getName()
	{
		return name;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public String getDescription()
	{
		return description;
	}
	/**
	 * removes polygons from the list if they contain
	 * the passed point
	 * @param x
	 * @param y
	 */
	public void removePolygon(double x, double y)
	{
		Iterator<Polygon> i = p.iterator();
		while(i.hasNext())
		{
			Polygon p = i.next();
			if(p.contains(x, y))
			{
				i.remove();
			}
		}
	}
	public int getWidth()
	{
		return width;
	}
	public int getHeight()
	{
		return height;
	}
	/**
	 * adds a starting location, starting units will start in the centers of their
	 * repsective regions
	 * @param r
	 */
	public void addStartLocation(Region r)
	{
		startLocations.add(r);
	}
	/**
	 * removes any start locations that contain the passed point
	 * @param x
	 * @param y
	 */
	public void removeStartLocation(double x, double y)
	{
		Iterator<Region> i = startLocations.iterator();
		while(i.hasNext())
		{
			Region r = i.next();
			if(r.contains(x, y))
			{
				i.remove();
			}
		}
	}
	/**
	 * removes a resource deposit if it contains the passed point
	 * @param x
	 * @param y
	 */
	public void removeResourceDeposit(double x, double y)
	{
		Iterator<ResourceDeposit> i = resourceDeposits.iterator();
		while(i.hasNext())
		{
			ResourceDeposit r = i.next();
			if(MathUtil.distance(x, y, r.getLocation()[0], r.getLocation()[1]) <= r.getRadius())
			{
				i.remove();
			}
		}
	}
	/**
	 * clears all the stored polygons for this map
	 */
	public void clearPolygons()
	{
		p = new ArrayList<Polygon>();
	}
	/**
	 * clears all resource deposits stored for this map
	 */
	public void clearResourceDeposits()
	{
		resourceDeposits = new ArrayList<ResourceDeposit>();
	}
	/**
	 * writes the map to the passed stream
	 * @param dos
	 * @throws IOException
	 */
	public void writeMap(DataOutputStream dos) throws IOException
	{
		int version = 4;
		dos.writeInt(version);
		
		dos.writeInt(name.length());
		dos.writeChars(name);
		dos.writeInt(description.length());
		dos.writeChars(description);
		
		dos.writeInt(width);
		dos.writeInt(height);
		
		dos.writeInt(p.size());
		Iterator<Polygon> pi = p.iterator();
		while(pi.hasNext())
		{
			Polygon.writePolygon(pi.next(), dos);
		}
		dos.writeInt(startLocations.size());
		Iterator<Region> ri = startLocations.iterator();
		while(ri.hasNext())
		{
			ri.next().writeRegion(dos);
			//Region.writeRegion(ri.next(), dos);
		}
		dos.writeInt(resourceDeposits.size());
		Iterator<ResourceDeposit> di = resourceDeposits.iterator(); //deposit iterator
		while(di.hasNext())
		{
			di.next().writeResourceDeposit(dos);
		}
	}
	public void setSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	public void readMap(DataInputStream dis) throws IOException
	{
		int version = dis.readInt();
		if(version == 1)
		{
			readMapVersion1(dis);
		}
		else if(version == 2)
		{
			readMapVersion2(dis);
		}
		else if(version == 3)
		{
			readMapVersion3(dis);
		}
		else if(version == 4)
		{
			readMapVersion4(dis);
		}
	}
	/**
	 * reads the map name, description, width, height, polygons,
	 * starting locations, resource deposits
	 * @param dis
	 * @throws IOException
	 */
	private void readMapVersion2(DataInputStream dis) throws IOException
	{
		readMapVersion1(dis);
		int length = dis.readInt();
		resourceDeposits = new ArrayList<ResourceDeposit>();
		for(int i = 0; i < length; i++)
		{
			ResourceDeposit rd = new ResourceDeposit(dis);
			resourceDeposits.add(rd);
		}
	}
	/**
	 * reads the map name, description, width, height, polygons,
	 * starting locations, resource deposits
	 * @param dis
	 * @throws IOException
	 */
	private void readMapVersion4(DataInputStream dis) throws IOException
	{
		readMapVersion1(dis);
		int length = dis.readInt();
		resourceDeposits = new ArrayList<ResourceDeposit>();
		for(int i = 0; i < length; i++)
		{
			String name = "";
			int nlength = dis.readInt();
			for(int a = 0; a < nlength; a++)
			{
				name+=dis.readChar();
			}
			System.out.print("loading "+name+"... ");
			
			try
			{
				Class<?> rd = getClass().getClassLoader().loadClass(name);
				double[] l = new double[2]; //location
				for(int q = 0; q < 2; q++)
				{
					l[q] = dis.readDouble();
				}
				Constructor<?> c = rd.getConstructor(l.getClass());
				ResourceDeposit temp = (ResourceDeposit)c.newInstance(l);
				resourceDeposits.add(temp);
			}
			catch(Exception e)
			{
				System.out.println("failed");
				e.printStackTrace();
			}
			System.out.println("done!");
		}
	}
	/**
	 * reads the map name, description, width, height, polygons,
	 * starting locations, resource deposits
	 * @param dis
	 * @throws IOException
	 */
	private void readMapVersion3(DataInputStream dis) throws IOException
	{
		readMapVersion1(dis);
		int length = dis.readInt();
		resourceDeposits = new ArrayList<ResourceDeposit>();
		for(int i = 0; i < length; i++)
		{
			String name = "";
			int nlength = dis.readInt();
			for(int a = 0; a < nlength; a++)
			{
				name+=dis.readChar();
			}
			
			/*String[] ntemp = name.split("."); //temporary name
			name = "";
			for(int q = 1; q < ntemp.length; q++)
			{
				name+=ntemp[q];
				if(q != ntemp.length-1)
				{
					name+='.';
				}
			}*/
			//removes the "gameEngine." from the path name to the resource deposits
			name = name.substring(11);
			
			System.out.print("loading "+name+"... ");
			
			try
			{
				Class<?> rd = getClass().getClassLoader().loadClass(name);
				double[] l = new double[2]; //location
				for(int q = 0; q < 2; q++)
				{
					l[q] = dis.readDouble();
				}
				Constructor<?> c = rd.getConstructor(l.getClass());
				ResourceDeposit temp = (ResourceDeposit)c.newInstance(l);
				resourceDeposits.add(temp);
			}
			catch(Exception e)
			{
				System.out.println("failed");
				e.printStackTrace();
			}
			System.out.println("done!");
		}
	}
	/**
	 * reads the map name, description, width, height, polygons, and
	 * starting locations
	 * @param dis
	 * @throws IOException
	 */
	private void readMapVersion1(DataInputStream dis) throws IOException
	{
		int length = dis.readInt();
		name = new String();
		for(int i = 0; i < length; i++)
		{
			name+=dis.readChar();
		}
		length = dis.readInt();
		description = new String();
		for(int i = 0; i < length; i++)
		{
			description+=dis.readChar();
		}
		
		width = dis.readInt();
		height = dis.readInt();
		//System.out.println("w="+width+", h="+height);

		length = dis.readInt();
		p = new ArrayList<Polygon>();
		for(int i = 0; i < length; i++)
		{
			Polygon polygon = Polygon.readPolygon(dis);
			p.add(polygon);
		}
		length = dis.readInt();
		startLocations = new ArrayList<Region>();
		for(int i = 0; i < length; i++)
		{
			RectRegion r = new RectRegion(dis);
			startLocations.add(r);
		}
	}
	public void drawMapElements(GL gl)
	{
		try
		{
			gl.glLineWidth(2);
			gl.glColor3d(0, 1, 0);
			Iterator<Polygon> pi = p.iterator();
			while(pi.hasNext())
			{
				gl.glColor3d(0, 1, 0);
				Polygon p = pi.next();
				p.drawPolygon(gl, 0);
				/*gl.glColor3d(1, 0, 0);
				p.drawNormalVectors(gl, 20, .1);
				p.drawRegion(gl);*/
			}
			gl.glColor3d(1, 0, 1);
			Iterator<Region> ri = startLocations.iterator();
			while(ri.hasNext())
			{
				ri.next().drawRegion(gl, 1);
			}
			gl.glColor3d(1, 0, 0);
			new RectRegion(0, 0, width, height).drawRegion(gl, 1);
			
			Iterator<ResourceDeposit> di = resourceDeposits.iterator(); //deposit iterator
			while(di.hasNext())
			{
				di.next().draw(gl, 1);
				
			}
		}
		catch(Exception e){}
	}
}
