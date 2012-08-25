package com.gelakinetic.mtgfam.helpers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

public abstract class QueuedAsyncTask<Params, Progress, Result> {
	private static final String LOG_TAG = "QueuedAsyncTask";

	private static final int CORE_POOL_SIZE = 5;// start up to 5 synchronous
												// threads by default (since
												// 1.6)
	private static final int MAXIMUM_POOL_SIZE = 6;// if our queue fills (and it
													// never will), start up to
													// 6 threads
	private static final int KEEP_ALIVE = 1;// default since 2.1 - it was 10
											// before, and this just makes them
											// more responsive

	private static final ThreadFactory sThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r) {
			return new Thread(r, "QueuedAsyncTask #" + mCount.getAndIncrement());
		}
	};

	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>();
	// there was a queue size of 10 by default to prevent starvation;
	// we remove that limit because we'll be queuing a bunch of small tasks

	public static final PausableThreadPoolExecutor THREAD_POOL_EXECUTOR = new PausableThreadPoolExecutor(
			CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
			sPoolWorkQueue, sThreadFactory);

	private static final int MESSAGE_POST_RESULT = 0x1;
	private static final int MESSAGE_POST_PROGRESS = 0x2;

	private static final InternalHandler sHandler = new InternalHandler();

	private static volatile Executor sDefaultExecutor = THREAD_POOL_EXECUTOR;
	private final WorkerRunnable<Params, Result> mWorker;
	private final FutureTask<Result> mFuture;

	private volatile Status mStatus = Status.PENDING;

	private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

	public enum Status {
		PENDING, RUNNING, FINISHED,
	}

	public static void init() {
		sHandler.getLooper();
	}

	public static void setDefaultExecutor(Executor exec) {
		sDefaultExecutor = exec;
	}

	public QueuedAsyncTask() {
		mWorker = new WorkerRunnable<Params, Result>() {
			public Result call() throws Exception {
				mTaskInvoked.set(true);

				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				return postResult(doInBackground(mParams));
			}
		};

		mFuture = new FutureTask<Result>(mWorker) {
			@Override
			protected void done() {
				try {
					final Result result = get();

					postResultIfNotInvoked(result);
				} catch (InterruptedException e) {
					android.util.Log.w(LOG_TAG, e);
				} catch (ExecutionException e) {
					throw new RuntimeException(
							"An error occured while executing doInBackground()",
							e.getCause());
				} catch (CancellationException e) {
					postResultIfNotInvoked(null);
				} catch (Throwable t) {
					throw new RuntimeException(
							"An error occured while executing "
									+ "doInBackground()", t);
				}
			}
		};
	}

	private void postResultIfNotInvoked(Result result) {
		final boolean wasTaskInvoked = mTaskInvoked.get();
		if (!wasTaskInvoked) {
			postResult(result);
		}
	}

	private Result postResult(Result result) {
		Message message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
				new QueuedAsyncTaskResult<Result>(this, result));
		message.sendToTarget();
		return result;
	}

	public final Status getStatus() {
		return mStatus;
	}

	protected abstract Result doInBackground(Params... params);

	protected void onPreExecute() {
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	protected void onPostExecute(Result result) {
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	protected void onProgressUpdate(Progress... values) {
	}

	@SuppressWarnings({ "UnusedParameters" })
	protected void onCancelled(Result result) {
		onCancelled();
	}

	protected void onCancelled() {
	}

	public final boolean isCancelled() {
		return mFuture.isCancelled();
	}

	public final boolean cancel(boolean mayInterruptIfRunning) {
		return mFuture.cancel(mayInterruptIfRunning);
	}

	public final Result get() throws InterruptedException, ExecutionException {
		return mFuture.get();
	}

	public final Result get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return mFuture.get(timeout, unit);
	}

	public final QueuedAsyncTask<Params, Progress, Result> execute(
			Params... params) {
		return executeOnExecutor(sDefaultExecutor, params);
	}

	public final QueuedAsyncTask<Params, Progress, Result> executeOnExecutor(
			Executor exec, Params... params) {
		if (mStatus != Status.PENDING) {
			switch (mStatus) {
			case RUNNING:
				throw new IllegalStateException("Cannot execute task:"
						+ " the task is already running.");
			case FINISHED:
				throw new IllegalStateException("Cannot execute task:"
						+ " the task has already been executed "
						+ "(a task can be executed only once)");
			}
		}

		mStatus = Status.RUNNING;

		onPreExecute();

		mWorker.mParams = params;
		exec.execute(mFuture);

		return this;
	}

	public static void execute(Runnable runnable) {
		sDefaultExecutor.execute(runnable);
	}

	protected final void publishProgress(Progress... values) {
		if (!isCancelled()) {
			sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
					new QueuedAsyncTaskResult<Progress>(this, values))
					.sendToTarget();
		}
	}

	private void finish(Result result) {
		if (isCancelled()) {
			onCancelled(result);
		} else {
			onPostExecute(result);
		}
		mStatus = Status.FINISHED;
	}

	private static class InternalHandler extends Handler {
		@SuppressWarnings({ "unchecked", "RawUseOfParameterizedType" })
		@Override
		public void handleMessage(Message msg) {
			QueuedAsyncTaskResult result = (QueuedAsyncTaskResult) msg.obj;
			switch (msg.what) {
			case MESSAGE_POST_RESULT:
				// There is only one result
				result.mTask.finish(result.mData[0]);
				break;
			case MESSAGE_POST_PROGRESS:
				result.mTask.onProgressUpdate(result.mData);
				break;
			}
		}
	}

	private static abstract class WorkerRunnable<Params, Result> implements
			Callable<Result> {
		Params[] mParams;
	}

	@SuppressWarnings({ "RawUseOfParameterizedType" })
	private static class QueuedAsyncTaskResult<Data> {
		final QueuedAsyncTask mTask;
		final Data[] mData;

		QueuedAsyncTaskResult(QueuedAsyncTask task, Data... data) {
			mTask = task;
			mData = data;
		}
	}

	public static void shutdownGracefully() {
		THREAD_POOL_EXECUTOR.shutdown(); // Disable new tasks from being
											// submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!THREAD_POOL_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
				THREAD_POOL_EXECUTOR.shutdownNow(); // Cancel currently
													// executing tasks
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			THREAD_POOL_EXECUTOR.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	public static class PausableThreadPoolExecutor extends ThreadPoolExecutor {
		private boolean isPaused;
		private ReentrantLock pauseLock = new ReentrantLock();
		private Condition unpaused = pauseLock.newCondition();

		public PausableThreadPoolExecutor(int corePoolSize,
				int maximumPoolSize, long keepAliveTime, TimeUnit unit,
				BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
					workQueue, threadFactory);
		}

		protected void beforeExecute(Thread t, Runnable r) {
			super.beforeExecute(t, r);
			pauseLock.lock();
			try {
				while (isPaused)
					unpaused.await();
			} catch (InterruptedException ie) {
				t.interrupt();
			} finally {
				pauseLock.unlock();
			}
		}

		public void pause() {
			pauseLock.lock();
			try {
				isPaused = true;
			} finally {
				pauseLock.unlock();
			}
		}

		public void resume() {
			pauseLock.lock();
			try {
				isPaused = false;
				unpaused.signalAll();
			} finally {
				pauseLock.unlock();
			}
		}
	}
}
