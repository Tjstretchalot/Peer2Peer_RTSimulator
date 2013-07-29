package me.timothy.modules.distribute;

import me.timothy.dcrts.net.LogicModule;

/**
 * 
 * @author Timothy
 * @deprecated Now real node nodules are being used in the connection state, so this isn't necessary
 */
public class DistributeModule extends LogicModule {

	@Override
	public void onActivate() {
		System.out.println("DistributeModule activated!");
	}

	@Override
	public void onDeactivate() {
		System.out.println("DistributeModule deactivated!");
	}

}
