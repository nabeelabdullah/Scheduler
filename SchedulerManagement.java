package com.apartmentV2.Utility;

import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class SchedulerManagement implements Runnable {

	private final TreeMap<Long, CopyOnWriteArrayList> tree = new TreeMap<>();

	private final ConcurrentHashMap<Long, Runnable> hash = new ConcurrentHashMap<>();

	private Long lastScedulingId = (long) 0;

	private Long currentTimeExecution = (long) 0;

	private boolean isRunning = false;

	private List list;

	private final ScheduledExecutorService service;

	private ScheduledFuture futureTask;

	public SchedulerManagement() {

		service = Executors.newScheduledThreadPool(0);

	}

	@Override
	public void run() {

		try {
			
			isRunning = true;

			synchronized (tree) {

				tree.remove(currentTimeExecution);

			}

			if (list != null) {

				Iterator<Long> it = list.iterator();

				while (it.hasNext()) {

					Long Id = it.next();

					Runnable task = hash.get(Id);

					if (task != null) {

						/**
						 * not making thread as of now *
						 */

						
						Thread thread = new Thread(task);

						thread.start();

						hash.remove(Id);
					}

				}

			}
		} finally {
			
			synchronized (tree) {

				Entry<Long, CopyOnWriteArrayList> entry = tree.firstEntry();

				if (entry != null) {

					manageSceduler(entry.getValue(), entry.getKey());

				} else {

					if (futureTask != null) {

						futureTask.cancel(true);

						futureTask = null;
					}
				}
			}

			System.out.println("End of Execution");

			isRunning = false;
		}
	}

	private void manageSceduler(List<Long> list, Long time) {

		/**
		 * if thread already running then do not interrupt its execution *
		 */

		currentTimeExecution = time;

		if (futureTask != null) {

			futureTask.cancel(true);

		}

		this.list = list;

		futureTask = service.schedule(this, time - Calendar.getInstance().getTimeInMillis(), TimeUnit.MILLISECONDS);

	}

	private Long addToInstantExecution(Long time, Runnable object) {

		service.schedule(object, time - Calendar.getInstance().getTimeInMillis(), TimeUnit.MILLISECONDS);

		return (long) -1;
	}

	public Long addToSchdule(Long time, Runnable object) {

		return addToSchdule(time, object, true);

	}

	public Long addToSchdule(Long time, Runnable object, boolean instantExecution) {

		lastScedulingId = lastScedulingId + 1;

		hash.put(lastScedulingId, object);

		CopyOnWriteArrayList<Long> idList;

		synchronized (tree) {

			idList = tree.get(time);

			if (idList == null) {

				idList = new CopyOnWriteArrayList();
			}

			idList.add(lastScedulingId);

			tree.put(time, idList);

		}

		if (futureTask == null || futureTask.isDone()) {

			manageSceduler(idList, time);

		}

		else if (currentTimeExecution == null || currentTimeExecution > time) {

			if (!isRunning) {

				manageSceduler(idList, time);

			}
		}

		return lastScedulingId;
	}

	public boolean deleteSchedule(Long id) {

		if (id == null || id == -1) {

			return false;
		}

		return hash.remove(id) != null;

	}

	public Long modifySchedule(Long id, Long newTIme) {

		Runnable task = hash.remove(id);

		return addToSchdule(newTIme, task);

	}

}
