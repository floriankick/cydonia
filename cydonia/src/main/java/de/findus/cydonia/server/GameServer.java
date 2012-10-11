/**
 * 
 */
package de.findus.cydonia.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;

import de.findus.cydonia.bullet.Bullet;
import de.findus.cydonia.level.Level;
import de.findus.cydonia.level.Level1;
import de.findus.cydonia.level.Level2;
import de.findus.cydonia.messages.AttackMessage;
import de.findus.cydonia.messages.BulletPhysic;
import de.findus.cydonia.messages.HitMessage;
import de.findus.cydonia.messages.PlayerInputMessage;
import de.findus.cydonia.messages.PlayerJoinMessage;
import de.findus.cydonia.messages.PlayerPhysic;
import de.findus.cydonia.messages.PlayerQuitMessage;
import de.findus.cydonia.messages.RespawnMessage;
import de.findus.cydonia.messages.WorldStateMessage;
import de.findus.cydonia.player.Player;
import de.findus.cydonia.player.PlayerInputState;

/**
 * @author Findus
 *
 */
public class GameServer extends Application implements MessageListener<HostedConnection>, ConnectionListener, PhysicsCollisionListener {
	
	public static float MAX_STEP_HEIGHT = 0.2f;
	public static float PLAYER_SPEED = 5f;
	public static float PHYSICS_ACCURACY = (1f / 192);
	
	private static final int RELOAD_TIME = 500;
	
	public static Transform ROTATE90LEFT = new Transform(new Quaternion().fromRotationMatrix(new Matrix3f(1, 0, FastMath.HALF_PI, 0, 1, 0, -FastMath.HALF_PI, 0, 1)));

	public static void main(String[] args) {
		GameServer gameServer = new GameServer();
		gameServer.start(JmeContext.Type.Headless);
	}
	
	private Server server;
	
	private Thread senderLoop;
	
	protected Node rootNode = new Node("Root Node");
	
	private RigidBodyControl landscape;
    
    private HashMap<Integer, Player> players;
    
    private HashMap<Long, Bullet> bullets;
    
	private BulletAppState bulletAppState;
    
    private boolean senderRunning;
    
    private ConcurrentLinkedQueue<Message> updateQueue;
    
    private Level level;
    
    /**
     * Used for moving players.
     * Allocated only once and reused for performance reasons.
     */
    private Vector3f walkdirection = new Vector3f();
	
    @Override
    public void initialize() {
        super.initialize();

        this.players = new HashMap<Integer, Player>();
        this.bullets = new HashMap<Long, Bullet>();
        updateQueue = new ConcurrentLinkedQueue<Message>();
        
        Bullet.setAssetManager(assetManager);

    	bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setMaxSubSteps(16);
        bulletAppState.getPhysicsSpace().setAccuracy(PHYSICS_ACCURACY);
        
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
        
        level = new Level2();
        Spatial scene = null;
        //scene = assetManager.loadModel("Scenes/firstworld.j3o");
        scene = level.getScene(assetManager);
        
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(scene);
        landscape = new RigidBodyControl(sceneShape, 0);
        scene.addControl(landscape);
        
        rootNode.attachChild(scene);
        bulletAppState.getPhysicsSpace().add(landscape);
        
        Bullet.preloadTextures();
        
        try {
			server = Network.createServer(6173);
			server.start();
			Serializer.registerClass(WorldStateMessage.class);
			Serializer.registerClass(PlayerPhysic.class);
			Serializer.registerClass(BulletPhysic.class);
			Serializer.registerClass(PlayerInputMessage.class);
			Serializer.registerClass(PlayerInputState.class);
			Serializer.registerClass(AttackMessage.class);
			Serializer.registerClass(HitMessage.class);
			Serializer.registerClass(RespawnMessage.class);
			Serializer.registerClass(PlayerJoinMessage.class);
			Serializer.registerClass(PlayerQuitMessage.class);
			
			server.addMessageListener(this);
			server.addConnectionListener(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        bulletAppState.setEnabled(true);
		senderLoop = new Thread(new WorldStateSenderLoop());
		senderRunning = true;
		senderLoop.start();
    }
    
    @Override
    public void update() {
        super.update(); // makes sure to execute AppTasks
        if (speed == 0 || paused) {
            return;
        }

        float tpf = timer.getTimePerFrame() * speed;

        // update states
        stateManager.update(tpf);

        // update game specific things
        handleMessages();
        movePlayers(tpf);
        
        // update world and gui
        rootNode.updateLogicalState(tpf);
        rootNode.updateGeometricState();

        stateManager.render(renderManager);
        renderManager.render(tpf, context.isRenderable());
        stateManager.postRender();
    }

	private void handleMessages() {
		while (!updateQueue.isEmpty()) {
			Message m = updateQueue.poll();

			if (m instanceof PlayerInputMessage) {
				PlayerInputMessage inputUpdate = (PlayerInputMessage) m;
				Player p = players.get(inputUpdate.getPlayerId());
				if(p != null) {
					p.setInputState(inputUpdate.getInputs());
					p.getControl().setViewDirection(inputUpdate.getViewDir());
				}
			}else if(m instanceof AttackMessage) {
				AttackMessage attack = (AttackMessage) m;
				Player p = players.get(attack.getPlayerid());
				long passedTime = System.currentTimeMillis() - p.getLastShot();
				if(passedTime >= RELOAD_TIME) {
					p.setLastShot(System.currentTimeMillis());
					Vector3f pos = p.getControl().getPhysicsLocation();
					Vector3f dir = p.getControl().getViewDirection();

					Bullet bul = Bullet.createBullet(p.getId());
					bul.getModel().setLocalTranslation(pos.add(dir.normalize().mult(1.1f)));
					rootNode.attachChild(bul.getModel());
					bul.getControl().setPhysicsLocation(pos.add(dir.normalize().mult(1.1f)));
					bulletAppState.getPhysicsSpace().add(bul.getControl());
					bul.getControl().setLinearVelocity(dir.normalize().mult(25));

					bullets.put(bul.getId(), bul);

					BulletPhysic physic = new BulletPhysic();
					physic.setId(bul.getId());
					physic.setTranslation(bul.getControl().getPhysicsLocation());
					physic.setVelocity(bul.getControl().getLinearVelocity());
					attack.setPhysic(physic);
					for(HostedConnection con : server.getConnections()) {
						con.send(attack);
					}
				}
			}else if(m instanceof PlayerJoinMessage) {
				PlayerJoinMessage join = (PlayerJoinMessage) m;
				int playerid = join.getId();
				Player p = new Player(playerid, assetManager);
				p.setName(join.getName());
				p.setTeam(join.getTeam());
				players.put(playerid, p);
				bulletAppState.getPhysicsSpace().add(p.getControl());
				p.getControl().setPhysicsLocation(new Vector3f(0, 10, 0));
				rootNode.attachChild(p.getModel());
				
				for (HostedConnection con : server.getConnections()) {
					con.send(join);
				}
			}else if(m instanceof RespawnMessage) {
				RespawnMessage respawn = (RespawnMessage) m;
				int playerid = respawn.getPlayerid();
				Player p = players.get(playerid);
				p.setHealthpoints(100);
				p.setAlive(true);
				bulletAppState.getPhysicsSpace().add(p.getControl());
				p.getControl().setPhysicsLocation(level.getSpawnPoint(p.getTeam()).getPosition());
				rootNode.attachChild(p.getModel());

				for (HostedConnection con : server.getConnections()) {
					con.send(respawn);
				}
			}
		}
	}

	private void movePlayers(float tpf) {
		for (Player p : this.players.values()) {
			Vector3f viewDir = p.getControl().getViewDirection().clone().setY(0).normalizeLocal();
			Vector3f viewLeft = new Vector3f();
			ROTATE90LEFT.transformVector(viewDir, viewLeft);
			
			walkdirection.set(0, 0, 0);
			if(p.getInputState().isLeft()) walkdirection.addLocal(viewLeft);
			if(p.getInputState().isRight()) walkdirection.addLocal(viewLeft.negate());
			if(p.getInputState().isForward()) walkdirection.addLocal(viewDir);
			if(p.getInputState().isBack()) walkdirection.addLocal(viewDir.negate());

			walkdirection.normalizeLocal().multLocal(PHYSICS_ACCURACY * PLAYER_SPEED);

			p.getControl().setWalkDirection(walkdirection);
		}
	}

	@Override
	public void collision(PhysicsCollisionEvent e) {
		Spatial bullet = null;
		Spatial other = null;

		if(e.getNodeA() != null) {
			Boolean sticky = e.getNodeA().getUserData("Sticky");
			if (sticky != null && sticky.booleanValue() == true) {
				bullet = e.getNodeA();
				other = e.getNodeB();
			}
		}
		if (e.getNodeB() != null) {
			Boolean sticky = e.getNodeB().getUserData("Sticky");
			if (sticky != null && sticky.booleanValue() == true) {
				bullet = e.getNodeB();
				other = e.getNodeA();
			}
		}

		if(bullet != null && other != null) {
			rootNode.detachChild(bullet);
			bulletAppState.getPhysicsSpace().remove(bullet.getControl(RigidBodyControl.class));
			bullet.removeControl(RigidBodyControl.class);
			if(other.getName().startsWith("player") && bullet.getName().startsWith("bullet")) {
				int victimid = Integer.parseInt(other.getName().substring(6));
				long bulletid = Long.parseLong(bullet.getName().substring(6));
				Bullet bul = bullets.get(bulletid);
				this.hitPlayer(bul.getPlayerid(), victimid, 20);
			}else {
				if(other != null) {
					if (other instanceof Node) {
						((Node) other).attachChild(bullet);
					}else {
						other.getParent().attachChild(bullet);
					}
				}
			}
		}
	}
	
	private void hitPlayer(int sourceid, int victimid, double hitpoints) {
		Player p = players.get(victimid);
		if(p == null) {
			return;
		}
		double hp = p.getHealthpoints();
		hp -= hitpoints;
		if(hp <= 0) {
			hp = 0;
			this.killPlayer(victimid);
			Player source = players.get(sourceid);
			if(source != null) {
				source.setKills(source.getKills() + 1);
			}
		}
		p.setHealthpoints(hp);
		
		HitMessage hit = new HitMessage();
		hit.setVictimPlayerid(victimid);
		hit.setHitpoints(20);
		hit.setSourcePlayerid(sourceid);
		
		for(HostedConnection con : server.getConnections()) {
			con.send(hit);
		}
	}
	
	private void killPlayer(int id) {
		Player p = players.get(id);
		if(p != null) {
			bulletAppState.getPhysicsSpace().remove(p.getControl());
			rootNode.detachChild(p.getModel());
			p.setAlive(false);
		}
	}

	@Override
	public void messageReceived(HostedConnection source, Message m) {
		updateQueue.add(m);
	}

	@Override
	public void connectionAdded(Server server, HostedConnection conn) {
		for (Player p : players.values()) {
			PlayerJoinMessage join = new PlayerJoinMessage();
			join.setId(p.getId());
			join.setName(p.getName());
			join.setTeam(p.getTeam());
			conn.send(join);
			if(p.isAlive()) {
				RespawnMessage respawn = new RespawnMessage();
				respawn.setPlayerid(p.getId());
				conn.send(respawn);
			}
		}
	}

	@Override
	public void connectionRemoved(Server server, HostedConnection conn) {
		Player p = players.get(conn.getId());
		bulletAppState.getPhysicsSpace().remove(p.getControl());
		players.remove(conn.getId());
		rootNode.detachChild(p.getModel());
		
		PlayerQuitMessage quit = new PlayerQuitMessage();
		quit.setId(conn.getId());
		for(HostedConnection client : server.getConnections()) {
			client.send(quit);
		}
	}
	
	/**
	 * This class is used to send the current state of the virtual world to all clients in constant intervals.
	 * @author Findus
	 *
	 */
	private class WorldStateSenderLoop implements Runnable {
		@Override
		public void run() {
			while(senderRunning) {
				WorldStateMessage m = WorldStateMessage.getUpdate(players.values(), bullets.values());
				m.setReliable(false);
				
				for (HostedConnection conn : server.getConnections()) {
					conn.send(m);
				}
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

}
