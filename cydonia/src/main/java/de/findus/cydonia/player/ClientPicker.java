/**
 * 
 */
package de.findus.cydonia.player;

import de.findus.cydonia.main.MainController;

/**
 * @author Findus
 *
 */
public class ClientPicker extends Picker {

	/**
	 * 
	 */
	public ClientPicker() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param range
	 * @param capacity
	 * @param player
	 * @param mainController
	 */
	public ClientPicker(String name, float range, int capacity, Player player,
			MainController mainController) {
		super(name, range, capacity, player, mainController);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void usePrimary(boolean activate) {
		
	}
	
	public void useSecondary(boolean activate) {
		
	}

}