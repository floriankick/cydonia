/**
 * 
 */
package de.encala.cydonia.share.events;

import com.jme3.network.serializing.Serializable;

/**
 * @author encala
 * 
 */
@Serializable
public class AttackEvent extends AbstractEvent {

	private int playerid;

	private long bulletid;

	public AttackEvent() {
		super();
	}

	public AttackEvent(int playerid, long bulletid, boolean forward) {
		super(forward);
		this.playerid = playerid;
		this.bulletid = bulletid;
	}

	public int getPlayerid() {
		return playerid;
	}

	public void setPlayerid(int playerid) {
		this.playerid = playerid;
	}

	/**
	 * @return the bulletid
	 */
	public long getBulletid() {
		return bulletid;
	}

	/**
	 * @param bulletid
	 *            the bulletid to set
	 */
	public void setBulletid(long bulletid) {
		this.bulletid = bulletid;
	}
}
