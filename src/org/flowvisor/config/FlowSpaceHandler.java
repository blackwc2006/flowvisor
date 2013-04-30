package org.flowvisor.config;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowvisor.api.APIUserCred;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowMap;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

public class FlowSpaceHandler extends ConcurrentHashMap<Integer, List<FlowEntry>> implements Runnable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private FlowMap flowSpace = null;
	private AtomicInteger nextId = null;
	private boolean shutdown;
	
	public FlowSpaceHandler() {
		nextId = new AtomicInteger(0);
		this.shutdown = false;
	}
	
	@Override
	public void run() {
		while (!shutdown) {
			if (!isEmpty()) {
				//flowSpace = getFlowMap();
				Iterator<Entry<Integer, List<FlowEntry>>> it = this.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Integer, List<FlowEntry>> entry = it.next();
					for (FlowEntry fentry : entry.getValue()) {
						try {
							FlowSpaceImpl.getProxy().addRule(fentry);
							flowSpace.addRule(fentry);
							String logMsg = "User " + APIUserCred.getUserName() + 
									flowspaceAddChangeLogMessage(fentry.getDpid(), 
											fentry.getRuleMatch(), fentry.getPriority(),
											fentry.getActionsList(), fentry.getName());
							FVLog.log(LogLevel.INFO, null, logMsg);
						} catch (ConfigError e) {
							/*
							 * TODO handle exception to log failure which will be used to create 
							 * FSStatus message
							 */
							FVLog.log(LogLevel.WARN, null, e.getMessage());
						}
					}
					FlowSpaceImpl.getProxy().notifyChange(flowSpace);
					it.remove();
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				FVLog.log(LogLevel.DEBUG, null, "Shutting down FlowSpace handler; exiting.");
				break;
			}
		}
		
	}
	
	public Integer add(List<FlowEntry> fes) {
		int next = nextId.getAndIncrement();
		this.put(next, fes);
		return next;
	}
	
	public boolean status(Integer id) {
		return this.containsKey(id);
	}
	
	public void shutdown() {
		this.shutdown = true;
		
	}
	
	private FlowMap getFlowMap() {
		if (flowSpace == null)
			flowSpace = FVConfig.getFlowSpaceFlowMap();
		return flowSpace; 
			
	}
	
	private static String flowspaceAddChangeLogMessage(long dpid, OFMatch match,
			int priority, List<OFAction> actions, String name ){
		return " for dpid=" + FlowSpaceUtil.dpidToString(dpid) + " match=" + match +
		" priority=" + priority + " actions=" + FlowSpaceUtil.toString(actions) + " name=" + name;
	}

	
	
}
