package com.cnblogs.hoojo.concurrency.service;

import java.util.List;

import javax.annotation.concurrent.GuardedBy;

import org.junit.Assert;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;

/**
 * <b>function:</b> 自定义的线程管理
 * 
 * doStart和doStop方法的实现需要考虑下性能，尽可能的低延迟。
 * 如果初始化的开销较大，如读文件，打开网络连接，
 * 或者其他任何可能引起阻塞的操作，建议移到另外一个单独的线程去处理。
 * <br/>
 * 注意：
 * 		1、不允许重复启动<br/>
 * 		2、_notifyStarted 不能重复调用；若重复调用时，State = STARTING 才可以
 * 
 * @author hoojo
 * @createDate 2017年11月14日 下午5:35:50
 * @file MyService.java
 * @package com.cnblogs.hoojo.concurrency.service
 * @project guava-example
 * @blog http://hoojo.cnblogs.com
 * @email hoojo_@126.com
 * @version 1.0
 */
public class MyService extends AbstractService {

	String fail;
	
	public MyService() {
	}
	
	public MyService(String fail) {
		this.fail = fail;
	}
	
	// 首次调用startAsync()时会同时调用doStart(), doStart()内部需要处理所有的初始化工作
	// 如果启动成功则调用notifyStarted()方法；启动失败则调用notifyFailed()
	@SuppressWarnings("unused")
	@Override
	protected void doStart() {
		System.out.println("doStart--->state: " + this.state() + ", isRunning: " + this.isRunning());
		
		if ("start".equals(fail)) {
			Assert.fail(fail);
			int a = 1/0;
		}
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// 首次调用stopAsync()会同时调用doStop(),doStop()要做的事情就是停止服务，
	// 如果停止成功则调用 notifyStopped()方法；停止失败则调用 notifyFailed()方法。
	@SuppressWarnings("unused")
	@Override
	protected void doStop() {
		System.out.println("doStop--->state: " + this.state() + ", isRunning: " + this.isRunning());
		
		if ("stop".equals(fail)) {
			Assert.fail(fail);
			int a = 1/0;
		}
	}
	
	public static class RecordingListener extends Listener {
		static RecordingListener record(Service service) {
			RecordingListener listener = new RecordingListener(service);
			service.addListener(listener, MoreExecutors.directExecutor());
			return listener;
		}

		final Service service;

		RecordingListener(Service service) {
			this.service = service;
		}

		@GuardedBy("this")
		final List<State> stateHistory = Lists.newArrayList();

		@Override
		public synchronized void starting() {
			System.out.println("Listener.starting: " + service.state() + ", isRunning: " + service.isRunning());
		}

		@Override
		public synchronized void running() {
			System.out.println("Listener.running: " + service.state() + ", isRunning: " + service.isRunning());
		}

		@Override
		public synchronized void stopping(State from) {
			System.out.println("Listener.stopping: " + service.state() + ", from:" + from + ", isRunning: " + service.isRunning());
		}

		@Override
		public synchronized void terminated(State from) {
			System.out.println("Listener.terminated: " + service.state() + ", from:" + from + ", isRunning: " + service.isRunning());
		}

		@Override
		public synchronized void failed(State from, Throwable failure) {
			System.out.println("Listener.failed: " + service.state() + ", from:" + from + ", isRunning: " + service.isRunning());
			System.out.println(failure.getMessage());
		}
	}
	
	public void _notifyStarted() {
		super.notifyStarted();
	}
	
	public void _notifyStopped() {
		super.notifyStopped();
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("--------new--------");
		MyService service = new MyService();
		RecordingListener.record(service);

		System.out.println(service.state()); // State.NEW
		System.out.println(service.isRunning()); // false

		System.out.println("--------startAsync--------");
		service.startAsync();
		System.out.println(service.state()); // State.STARTING
		System.out.println(service.isRunning()); // false
		
		System.out.println("--------notifyStarted--------");
		service.notifyStarted();
		System.out.println(service.state()); // State.RUNNING
		System.out.println(service.isRunning()); // true
		
		System.out.println("--------awaitRunning--------");
		service.awaitRunning();
		System.out.println(service.state()); // State.RUNNING
		System.out.println(service.isRunning()); // true
		
		System.out.println("--------stopAsync--------");
		service.stopAsync();
		System.out.println(service.state()); // State.STOPPING
		System.out.println(service.isRunning()); // false
		
		System.out.println("--------notifyStopped--------");
		service.notifyStopped();
		System.out.println(service.state()); // State.TERMINATED
		System.out.println(service.isRunning()); // false
		
		System.out.println("--------awaitTerminated--------");
		service.awaitTerminated();
		System.out.println(service.state()); // State.TERMINATED
		System.out.println(service.isRunning()); // false
	}
}
