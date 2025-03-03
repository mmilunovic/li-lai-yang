package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.LYMarkerHandler;
import servent.handler.snapshot.LYTellHandler;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
		
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;
				
				/*
				 * Lai-Yang stuff. Process any red messages we got before we got the marker.
				 * The marker contains the collector id, so we need to process that as our first
				 * red message. 
				 */				
				Socket clientSocket = listenerSocket.accept();
				
				clientMessage = MessageUtil.readMessage(clientSocket);

				synchronized (AppConfig.colorLock) {
					
					if(clientMessage.getMessageType() == MessageType.LY_MARKER) {				
						int initiatorDoingSnapshot = AppConfig.versionRelation((BasicMessage)clientMessage);
												
						if (initiatorDoingSnapshot != -1) {
								AppConfig.myServentInfo.setParentId(clientMessage.getOriginalSenderInfo().getId());
	
								AppConfig.myServentInfo.setMyCollectorId(initiatorDoingSnapshot);
	
								AppConfig.initiatorsVersion.putAll(((BasicMessage)clientMessage).getInitiatorsVersion());
								
								AppConfig.timestampedErrorPrint("Moj kolektor i parent: " + initiatorDoingSnapshot + " " + clientMessage.getOriginalSenderInfo().getId());
	
								LaiYangBitcakeManager lyBitcakeManager = (LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();
								
								lyBitcakeManager.markerEvent(Integer.parseInt(clientMessage.getMessageText()), snapshotCollector);
						}
					}
				}
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case TRANSACTION:
					messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
					break;
				case LY_MARKER:
					messageHandler = new LYMarkerHandler();
					break;
				case LY_TELL:
					messageHandler = new LYTellHandler(clientMessage, snapshotCollector);
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
