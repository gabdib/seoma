package world.animation;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.media.opengl.GL;

public class AnimationEngine
{
	LinkedList<Animation> a = new LinkedList<Animation>();
	
	long totalTime = 0;
	long updates = 0;
	
	public synchronized void updateAnimationEngine(double tdiff)
	{
		long start = System.currentTimeMillis();
		Iterator<Animation> i = a.iterator();
		while(i.hasNext())
		{
			Animation a = i.next();
			a.update(tdiff, this);
			if(a.isDead())
			{
				i.remove();
			}
		}
		totalTime+=System.currentTimeMillis()-start;
		updates++;
		
		if(updates % 1500 == 0 && updates != 0)
		{
			System.out.println("animation engine update time (ms) = "+totalTime+" [total time] / "+updates+" [updates] = "+(totalTime/updates));
			System.out.println("-------------------");
		}
	}
	public synchronized void registerAnimation(Animation animation)
	{
		a.add(animation);
	}
	public synchronized void drawAnimiations(GL gl, int width, int height)
	{
		try
		{
			Iterator<Animation> i = a.iterator();
			while(i.hasNext())
			{
				i.next().drawAnimation(gl, width, height);
			}
		}
		catch(ConcurrentModificationException e)
		{
			e.printStackTrace();
			System.out.println("exception caught, continuing...");
			System.out.println("---------------------");
		}
	}
}
