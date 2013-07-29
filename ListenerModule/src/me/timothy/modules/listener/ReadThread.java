package me.timothy.modules.listener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import me.timothy.dcrts.net.NetModule;

public class ReadThread extends Thread {
	private SocketChannel channel;
	private NetModule netModule;
	private volatile boolean running;
	private volatile boolean done;
	
	public ReadThread(NetModule net, SocketChannel sc) {
		this.channel = sc;
		this.netModule = net;
	}
	
	public void stopGracefully() {
		running = false;
		
		while(!done) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean running() {
		return running || !done;
	}
	
	@Override
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int nRead = 0;
		while(running) {
			try {
				nRead = channel.read(buffer);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if(nRead < 0) {
				System.out.println("ReadThread#run(): read " + nRead + " bytes from connected peer");
			}else if(nRead > 0) {
				buffer.flip();
				netModule.handleRead(buffer, null);
				buffer.clear();
				Thread.yield();
				continue;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		done = true;
	}
}
