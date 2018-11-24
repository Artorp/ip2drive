package no.artorp.ip2drive;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UpdatePeriodically implements Runnable {

	private long initialTime;
	private final long cycle_time;
	private ExecutorService executorService;
	private Callable<Void> updateSpreadsheet;
	private Future<Void> myFuture;

	public UpdatePeriodically(long cycle_seconds, Callable<Void> updateSpreadsheet) {
		this.cycle_time = cycle_seconds * 60 * 1_000_000_000L;
		this.updateSpreadsheet = updateSpreadsheet;
	}
	
	@Override
	public void run() {
		// set up single-threaded executor service
		this.executorService = Executors.newSingleThreadExecutor();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nShutting down executor service...");
			executorService.shutdownNow();
		}));
		
		this.initialTime = System.nanoTime();
		System.out.println("Initial run:");
		myFuture = this.executorService.submit(updateSpreadsheet);

		SimpleDateFormat sdfMinSec = new SimpleDateFormat("mm:ss");
		try {
			while (!Thread.interrupted()) {
				try {
					// wait for future / promise to resolve
					myFuture.get(45, TimeUnit.SECONDS);
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				String lastTime = "";
				while (System.nanoTime() - initialTime < cycle_time) {
					Date remainingTime = new Date((initialTime - System.nanoTime() + cycle_time) / 1_000_000);
					String currentTime = sdfMinSec.format(remainingTime);
					if (!currentTime.equals(lastTime)) {
						System.out.print("Next update in: " + currentTime + "\r");
						lastTime = currentTime;
					}
					Thread.sleep(100);
				}
				initialTime = System.nanoTime();
				System.out.println();
				System.out.println("Updating spreadsheet.");
				// assign new future / promise
				myFuture = executorService.submit(updateSpreadsheet);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("Interrupted, shutting down executor service.");
			executorService.shutdownNow();
		}
	}

}
