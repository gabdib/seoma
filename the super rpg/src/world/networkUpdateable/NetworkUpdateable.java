package world.networkUpdateable;

import world.RelevantSet;
import world.World;

/**
 * defines an interface for objects that can be updated over the network
 * by either the server or a client, network updateable objects can interact
 * with the world by executing actions
 * @author Jack
 *
 */
public abstract class NetworkUpdateable
{
	private short id;
	private int updatePriority;
	private boolean broadcastDeath;
	private boolean ready = false;
	/**
	 * represents the objects state in regard to the control the engine has
	 * over it, if true then the object is a ghost object and its state is
	 * updated from another source
	 */
	private boolean isGhost;
	private boolean dead = false;
	
	public NetworkUpdateable(boolean isGhost, short id, int updatePriority, boolean broadcastDeath)
	{
		this.isGhost = isGhost;
		this.id = id;
		this.updatePriority = updatePriority;
		this.broadcastDeath = broadcastDeath;
	}
	/**
	 * tests to determine whether or not this network updateable is relevant
	 * to the passed id, relevant objects will be updated on clients by the server
	 * @param id
	 * @param set the relevant set maintained for the passed id
	 * @param w
	 * @return returns true if the object is relevant to the passed id, false otherwise
	 */
	public abstract boolean isRelevant(short id, RelevantSet set, World w);
	public boolean isDead()
	{
		return dead;
	}
	/**
	 * declares the object to be dead and removes it from the game world
	 */
	public void setDead()
	{
		dead = true;
	}
	/**
	 * checks to see if the network updateable should have its death broadcast to clients,
	 * network objects that are timed and will automatically be disposed by clients without
	 * an order from the server should not broadcast their deaths (ex. attacks)
	 * @return returns true if the network updateable object should broadcast its death to
	 * clients, false otherwise
	 */
	public boolean broadcastDeath()
	{
		return broadcastDeath;
	}
	/**
	 * loads the state specified by the data in the byte buffer
	 * @param b
	 */
	public abstract void loadState(byte[] b);
	/**
	 * gets the current state of the object, the state must be represented in
	 * less than 256 bytes
	 * @return returns a byte buffer containing the current state of the object
	 */
	public abstract byte[] getState();
	/**
	 * gets the id assigned to this network updateable object, each object
	 * has a unique id
	 * @return returns the unique id associated with this network object
	 */
	public short getID()
	{
		return id;
	}
	/**
	 * checks to see if this object is a ghost copy or an original, typically
	 * objects on the server are originals while objects on clients are ghosts,
	 * only non ghost objects are updated
	 * @return
	 */
	public boolean isGhost()
	{
		return isGhost;
	}
	/**
	 * updates the object, meant only to be called on non ghost objects
	 * @param w
	 * @param tdiff
	 */
	public abstract void update(World w, double tdiff);
	/**
	 * simulates an update to the object, a simulated update is not the same as
	 * a real update, ghost objects are simulated in between receiving update information
	 * from the server
	 * @param w
	 * @param tdiff
	 */
	public abstract void simulate(World w, double tdiff);
	/**
	 * gets the base update priority for the game object for determining
	 * the relevant set, update priority is dynamic and non fixed and is
	 * determined at the discretion of the game object itself, an object
	 * with a higher update priority will be updated more often than one
	 * with a lower priority, an update priority of 0 will guarantee that
	 * udpate information for the object is never sent to clients
	 * @return returns the update priority for the object
	 */
	public int getUpdatePriority()
	{
		return updatePriority;
	}
	/**
	 * cheacks to see if the network updateable object has all the information
	 * it needs to function, only after is has recevied its information should it be used
	 * @return returns true if the the object is ready to be used, false otherwise
	 */
	public boolean isReady()
	{
		return ready;
	}
	/**
	 * declares the object to be ready for use in the world
	 */
	protected void setReady()
	{
		ready = true;
	}
}
