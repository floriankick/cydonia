/**
 * 
 */
package de.findus.cydonia.main;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import de.findus.cydonia.events.Event;
import de.findus.cydonia.events.EventListener;
import de.findus.cydonia.events.EventMachine;
import de.findus.cydonia.level.Flag;
import de.findus.cydonia.level.FlagFactory;
import de.findus.cydonia.level.Flube;
import de.findus.cydonia.level.WorldController;
import de.findus.cydonia.player.Beamer;
import de.findus.cydonia.player.Editor;
import de.findus.cydonia.player.EquipmentFactory;
import de.findus.cydonia.player.Picker;
import de.findus.cydonia.player.Player;
import de.findus.cydonia.player.PlayerController;

/**
 * @author Findus
 *
 */
public abstract class MainController extends Application implements PhysicsCollisionListener, EventListener {
		
	    protected static final String MAPFOLDER = "/de/findus/cydonia/level/";
	protected static final String MAPEXTENSION = ".xml";
		public static float PLAYER_SPEED = 5f;
	    public static float PHYSICS_ACCURACY = (1f / 192);
	    
	    public static Transform ROTATE90LEFT = new Transform(new Quaternion().fromRotationMatrix(new Matrix3f(1, 0, FastMath.HALF_PI, 0, 1, 0, -FastMath.HALF_PI, 0, 1)));
	    
	    private GameState gamestate;
	    
	    private GameConfig gameConfig;
	    
	    private WorldController worldController;
	    
	    private PlayerController playerController;
	    
	    private BulletAppState bulletAppState;
	    
	    private EventMachine eventMachine;
	    
	    private ConcurrentLinkedQueue<Event> eventQueue;
	    
	    
	    public MainController() {
	    	super();
	    	
	    	gameConfig = new GameConfig(true);
	    }

	    @Override
	    public void initialize() {
	        super.initialize();
	        
	        this.gamestate = GameState.LOADING;
	        
	        eventMachine = new EventMachine();
	        eventQueue = new ConcurrentLinkedQueue<Event>();
	        
	    	bulletAppState = new BulletAppState();
	    	bulletAppState.setEnabled(false);
	        bulletAppState.setThreadingType(ThreadingType.PARALLEL);
	        stateManager.attach(bulletAppState);
	        bulletAppState.getPhysicsSpace().setMaxSubSteps(16);
	        bulletAppState.getPhysicsSpace().setAccuracy(PHYSICS_ACCURACY);
	        bulletAppState.getPhysicsSpace().addCollisionListener(this);
	        
	        FlagFactory.init(assetManager);
	        
	        worldController = new WorldController(assetManager, bulletAppState.getPhysicsSpace());
	        eventMachine.registerListener(this);
			
			playerController = new PlayerController(assetManager, this);
	    }
	    
	    protected void cleanup() {
	    	bulletAppState.setEnabled(false);
	    	eventMachine.stop();
	    }
	    
	    @Override
	    public void update() {
	        super.update(); // makes sure to execute AppTasks
	        handleEvents();
	    }

	    protected abstract void handleEvent(Event e);

		private void handleEvents() {
			Event e = null;
			while ((e = eventQueue.poll()) != null) {
				this.handleEvent(e);
			}
		}

		@Override
		public void newEvent(Event e) {
			eventQueue.offer(e);
		}
		
		protected void returnFlag(Flag flag) {
			getWorldController().returnFlag(flag);
		}
		
		protected void takeFlag(Player p, Flag flag) {
			flag.setInBase(false);
			Node parent = flag.getModel().getParent();
			if(parent != null) {
				parent.detachChild(flag.getModel());
			}
			flag.getModel().setLocalTranslation(0, 1, 0);
//			flag.getModel().setLocalScale(Vector3f.UNIT_XYZ.divide(p.getModel().getLocalScale()));
			p.getNode().attachChild(flag.getModel());
			p.setFlag(flag);
			flag.setPlayer(p);
			System.out.println("takenflag");
		}
		
		protected void scoreFlag(Player p, Flag flag) {
			p.setFlag(null);
			flag.setPlayer(null);
			p.setScores(p.getScores() + 3);
			returnFlag(flag);
			// TODO: score team
			System.out.println("scoredflag");
		}
		
		protected void killPlayer(Player p) {
			if(p == null) return;
			if(p.getFlag() != null) {
				returnFlag(p.getFlag());
			}
			worldController.detachPlayer(p);
			p.setAlive(false);
		}
		
		protected void beam(Player p, Player victim) {
			p.setScores(p.getScores() + 1);
			killPlayer(victim);
		}

		protected void joinPlayer(int playerid, String playername) {
			Player p = playerController.createNew(playerid);
			p.setName(playername);
			
			String gameMode = getGameConfig().getString("mp_gamemode");
			if("ctf".equals(gameMode)) {
				Picker picker1 = (Picker) getEquipmentFactory().create("Picker");
				picker1.setName("LongRangePicker");
				picker1.setRange(15);
				picker1.setCapacity(1);
				picker1.setPlayer(p);
				p.getEquips().add(picker1);
				
				Picker picker2 = (Picker) getEquipmentFactory().create("Picker");
				picker2.setName("ShortRangePicker");
				picker2.setRange(5);
				picker2.setCapacity(3);
				picker2.setPlayer(p);
				p.getEquips().add(picker2);
				
				Beamer beamer = (Beamer) getEquipmentFactory().create("Beamer");
				beamer.setName("Beamer");
				beamer.setRange(20);
				beamer.setPlayer(p);
				p.getEquips().add(beamer);
			}else if("editor".equals(gameMode)){
				Editor editor1 = (Editor) getEquipmentFactory().create("Editor");
				editor1.setName("EditorDarkGray");
				editor1.setRange(50);
				editor1.setObjectType(-2);
				editor1.setPlayer(p);
				p.getEquips().add(editor1);
				
				Editor editor2 = (Editor) getEquipmentFactory().create("Editor");
				editor2.setName("EditorLightGray");
				editor2.setRange(50);
				editor2.setObjectType(-1);
				editor2.setPlayer(p);
				p.getEquips().add(editor2);
				
				Editor editor3 = (Editor) getEquipmentFactory().create("Editor");
				editor3.setName("EditorWhite");
				editor3.setRange(50);
				editor3.setObjectType(0);
				editor3.setPlayer(p);
				p.getEquips().add(editor3);
				
				Editor editor4 = (Editor) getEquipmentFactory().create("Editor");
				editor4.setName("EditorBlue");
				editor4.setRange(50);
				editor4.setObjectType(1);
				editor4.setPlayer(p);
				p.getEquips().add(editor4);
				
				Editor editor5 = (Editor) getEquipmentFactory().create("Editor");
				editor5.setName("EditorRed");
				editor5.setRange(50);
				editor5.setObjectType(2);
				editor5.setPlayer(p);
				p.getEquips().add(editor5);
			}
		}
		
		protected void quitPlayer(Player p) {
			if(p == null) return;
			if(p.getFlag() != null) {
				returnFlag(p.getFlag());
			}
			worldController.detachPlayer(p);
			playerController.removePlayer(p.getId());
		}
		
		protected void respawn(final Player p) {
			if(p == null) return;
			playerController.setHealthpoints(p, 100);
			p.setAlive(true);

			p.getControl().setPhysicsLocation(worldController.getSpawnPoint(p.getTeam()).getPosition());
			worldController.attachPlayer(p);
		}
		
		protected void chooseTeam(Player p, int team) {
			if(p == null) return;
			playerController.setTeam(p, team);
		}
		
		protected void pickup(Player p, Flube flube) {
			if(flube != null) {
				getWorldController().detachFlube(flube);
				if(p != null) {
					if(p.getCurrentEquipment() instanceof Picker) {
						Picker picker = (Picker) p.getCurrentEquipment();
						picker.getRepository().add(flube);
					}
				}
			}
		}
		
		protected void place(Player p, Flube f, Vector3f loc) {
			f.getControl().setPhysicsLocation(loc);
			getWorldController().attachFlube(f);
			if(p != null) {
				if(p.getCurrentEquipment() instanceof Picker) {
					Picker picker = (Picker) p.getCurrentEquipment();
					picker.getRepository().remove(f);
				}
			}
			System.out.println("Place");
		}
		
		public GameState getGamestate() {
			return gamestate;
		}

	    /**
		 * @param gamestate the gamestate to set
		 */
		public void setGamestate(GameState gamestate) {
			this.gamestate = gamestate;
		}

		/**
		 * @return the gameConfig
		 */
		public GameConfig getGameConfig() {
			return gameConfig;
		}

		/**
		 * @return the bulletAppState
		 */
		public BulletAppState getBulletAppState() {
			return bulletAppState;
		}

		/**
		 * @return the worldController
		 */
		public WorldController getWorldController() {
			return worldController;
		}

		/**
		 * @return the playerController
		 */
		public PlayerController getPlayerController() {
			return playerController;
		}
		
		public abstract EquipmentFactory getEquipmentFactory();

		/**
		 * @return the eventMachine
		 */
		public EventMachine getEventMachine() {
			return eventMachine;
		}

		protected void addFlube(long id, int type, Vector3f origin) {
			Flube f = getWorldController().addNewFlube(id, type, origin);
			f.getControl().setPhysicsLocation(origin);
			getWorldController().attachFlube(f);
		}

		protected void removeFlube(Flube f) {
			getWorldController().removeFlube(f);
		}
}
