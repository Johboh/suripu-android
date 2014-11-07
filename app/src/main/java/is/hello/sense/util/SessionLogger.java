package is.hello.sense.util;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public final class SessionLogger {
    public static final String FILENAME = "Sense-Session-Log.txt";

    private static final int ROLLOVER = 3;

    private static boolean initialized = false;
    private static File logFile;
    private static PrintWriter printWriter;
    private static AtomicInteger messagesWritten = new AtomicInteger();
    private static Handler handler;


    public static @NonNull String priorityToString(int priority) {
        switch (priority) {
            case Log.VERBOSE:
                return "V";
            case Log.DEBUG:
                return "D";
            case Log.INFO:
                return "I";
            case Log.WARN:
                return "W";
            case Log.ERROR:
                return "E";
            case Log.ASSERT:
                return "A";
            default:
                return "?";
        }
    }

    public static void println(int priority, @NonNull String tag, @NonNull String message) {
        if (!initialized)
            return;

        handler.post(() -> {
            try {
                int messagesWrittenSnapshot = messagesWritten.incrementAndGet();
                printWriter.printf("%s %s/%s: %s\n", DateTime.now(), priorityToString(priority), tag, message);
                if (priority == Log.ERROR || messagesWrittenSnapshot > ROLLOVER) {
                    messagesWritten.set(0);
                    printWriter.flush();
                }
            } catch (Exception e) {
                Log.wtf(SessionLogger.class.getSimpleName(), "Internal error.", e);
            }
        });
    }

    public static Observable<Void> flush() {
        if (!initialized) {
            return Observable.error(new NullPointerException());
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> {
            try {
                printWriter.flush();

                s.onNext(null);
                s.onCompleted();
            } catch (Exception e) {
                s.onError(e);
            }
        }).subscribeOn(AndroidSchedulers.handlerThread(handler));
    }

    public static Observable<Void> clearLog() {
        return Observable.create((Observable.OnSubscribe<Void>) s -> {
            if (logFile == null) {
                s.onError(new FileNotFoundException());
                return;
            }

            try {
                SessionLogger.initialized = false;

                Functions.safeClose(printWriter);
                SessionLogger.printWriter = new PrintWriter(new FileOutputStream(logFile, false));

                SessionLogger.initialized = true;

                println(Log.INFO, "Internal", "Log Cleared");
                printWriter.flush();

                s.onNext(null);
                s.onCompleted();
            } catch (Exception e) {
                s.onError(e);
            }
        }).subscribeOn(AndroidSchedulers.handlerThread(handler));
    }

    public static @NonNull String getLogFilePath(@NonNull Context context) {
        return context.getExternalCacheDir() + File.separator + FILENAME;
    }


    public static void init(@NonNull Context context) {
        File file = new File(getLogFilePath(context));
        init(file);
    }

    public static void init(@NonNull File logFile) {
        try {
            SessionLogger.printWriter = new PrintWriter(new FileOutputStream(logFile, true));
            SessionLogger.logFile = logFile;
            HandlerThread workerThread = new HandlerThread("SessionLogger.handlerThread");
            workerThread.start();
            SessionLogger.handler = new Handler(workerThread.getLooper());
            SessionLogger.initialized = true;

            println(Log.INFO, "Internal", "Session Began");
        } catch (IOException e) {
            Logger.error(SessionLogger.class.getSimpleName(), "Could not initialize session logger.", e);
        }
    }
}
