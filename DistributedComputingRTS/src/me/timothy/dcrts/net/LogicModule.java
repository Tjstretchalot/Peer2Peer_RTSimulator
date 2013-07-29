package me.timothy.dcrts.net;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public abstract class LogicModule extends Module {
	public static final Sigar sigar = new Sigar();
	
	protected int maxCPU;
	
	private double lastCPUPerc;
	private long lastCPUCall;
	
	/**
	 * Sets the maximum average cpu usage (+/- 1) in percent this
	 * logic module will use at any given point. Beyond that,
	 * the module will begin deflecting packets to other nodes.
	 * 
	 * @param max the maximum average cpu
	 */
	public void setMaxCPU(int max) {
		maxCPU = max;
	}
	
	public int getMaxCPU() {
		return maxCPU;
	}
	
	/**
	 * Checks if this logic module is accepting packets at this time.
	 * Common reasons include the maximum number of threads have been
	 * spawned and are in use or cpu usage has hit the max (as specified
	 * by {@link LogicModule#getMaxCPU()}
	 * 
	 * 
	 * @return if this computer is accepting logic packets at this time
	 */
	public boolean acceptingPackets() {
		return !isCPULimitReached();
	}
	
	/**
	 * Checks if the cpu limit has been reached. Polls a maximum
	 * of 10 times a second
	 * @return if the cpu limit has been reached
	 */
	protected boolean isCPULimitReached() {
		try {
			if(System.currentTimeMillis() - lastCPUCall < 100)
				return lastCPUPerc >= maxCPU;
			lastCPUPerc = sigar.getCpuPerc().getCombined();
			lastCPUCall = System.currentTimeMillis();
			return lastCPUPerc >= maxCPU;
		} catch (SigarException e) {
			e.printStackTrace();
			return true;
		}
	}
	
	
}
