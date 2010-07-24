package network.operationExecutor.clientOperation;

import java.nio.ByteBuffer;

import network.Connection;
import network.IOConstants;
import network.client.Client;
import network.operationExecutor.Operation;
import world.World;
import world.controller.Controller;
import world.unit.Avatar;
import world.unit.UnitInitializer;

/**
 * the operation for setting up the controller on clients
 * and creating the avatar for them to control
 * @author Jack
 *
 */
public class ControllerSetup extends Operation
{
	World w;
	Client client;
	
	public ControllerSetup(World w, Client client)
	{
		super(IOConstants.controllerSetup);
		this.w = w;
		this.client = client;
	}
	public void performOperation(ByteBuffer buff, Connection c)
	{
		short id = buff.getShort();
		short x = buff.getShort();
		short y = buff.getShort();
		
		Avatar a = new Avatar(false, id, new double[]{x, y});
		w.initialize(UnitInitializer.createInitializer(a, Byte.MIN_VALUE));
		
		
		Controller controller = new Controller(a);
		System.out.println("client controller set, avatar id = "+id);
		w.setController(controller);
		client.connected(a);
	}
}
