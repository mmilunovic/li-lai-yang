package app.snapshot_bitcake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	//private Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();
	
	private Map<Integer, Map<Integer, Map<Integer, LYSnapshotResult>>> collectedLYValues = new ConcurrentHashMap<>();
	
	private BitcakeManager bitcakeManager;
		

	public SnapshotCollectorWorker() {
		bitcakeManager = new LaiYangBitcakeManager();
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			AppConfig.timestampedErrorPrint("Radim svoj marker event.");
			
			((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this);
			
			AppConfig.timestampedErrorPrint("Cekam na odgovor");
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				if (collectedLYValues.size() == AppConfig.getServentCount()) {
					waiting = false;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			AppConfig.timestampedErrorPrint("Dobio sam odgovor, ziveli!");
			int sum = 0;
			
			Integer collectorId = AppConfig.myServentInfo.getId();
			Integer version = AppConfig.initiatorsVersion.get(collectorId);

			AppConfig.timestampedErrorPrint("Ja sam inicijator, moj ID: " + collectorId);
			AppConfig.timestampedErrorPrint("Redni broj snapshot-a koji JA radim: " + version);

			AppConfig.timestampedErrorPrint("Cela jebena mapa " + collectedLYValues.toString());

//			AppConfig.timestampedErrorPrint("Ukupno elemenata u mapi: " + collectedLYValues.size());
//			AppConfig.timestampedErrorPrint("Ukupno elemenata u mapi za 0: " + collectedLYValues.get(0).size());
//			AppConfig.timestampedErrorPrint("Ukupno elemenata u mapi za 0 za mene: " + collectedLYValues.get(0).get(collectorId).toString());
//			AppConfig.timestampedErrorPrint("Ukupno elemenata u mapi za 0 za mene za trenutnu verziju: " + collectedLYValues.get(0).get(collectorId).get(version).toString());

			
			

			for(Entry<Integer, Map<Integer, Map<Integer, LYSnapshotResult>>> nodeResult: collectedLYValues.entrySet()) {
				sum += nodeResult.getValue().get(collectorId).get(version).getRecordedAmount();
				
				AppConfig.timestampedStandardPrint(
						"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().get(collectorId).get(version).getRecordedAmount());
			}
			
			for(int i = 0; i < AppConfig.getServentCount(); i++) {
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					if (i != j) {
						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
							int ijAmount = collectedLYValues.get(i).get(collectorId).get(version).getGiveHistory().get(j);
							int jiAmount = collectedLYValues.get(j).get(collectorId).get(version).getGetHistory().get(i);
							
							if (ijAmount != jiAmount) {
								String outputString = String.format(
										"Unreceived bitcake amount: %d from servent %d to servent %d",
										ijAmount - jiAmount, i, j);
								AppConfig.timestampedStandardPrint(outputString);
								sum += ijAmount - jiAmount;
							}
						}
					}
				}
			}
			

			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
			
			collectedLYValues.clear(); //reset for next invocation
			collecting.set(false);
			AppConfig.myServentInfo.clearEverything();

		}

	}
	
	public void addLYSnapshotInfo(int id, Map<Integer, Map<Integer, LYSnapshotResult>> givenHistoryForInitiatorForVersion) {
		collectedLYValues.put(id, givenHistoryForInitiatorForVersion);
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
