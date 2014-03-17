/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a {@link Thread} that watches for file modifications based on the Java7
 * {@link WatchService} infrastructure. Whenever the file changes, all registered listeners are
 * notified.
 */
public class FileChangeWatch extends Thread {

    private static final Logger log = LoggerFactory.getLogger(FileChangeWatch.class);
    private final List<FileChangeListener> listeners = new ArrayList<>();
    private final Path toWatch;
    private final WatchService service;

    /**
     * Creates a new {@link FileChangeWatch} that will observe the given path for changes.
     * 
     * @param toWatch
     *            the file to watch
     * @throws IOException
     *             in case setting up the {@link WatchService} fails.
     */
    public FileChangeWatch(Path toWatch) throws IOException {
        this.toWatch = toWatch;
        this.service = toWatch.getFileSystem().newWatchService();
        this.toWatch.getParent().register(service, StandardWatchEventKinds.ENTRY_MODIFY);

        setName("Watch " + toWatch.getFileName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        log.debug("start watching " + toWatch + " for modifications");
        try {
            WatchKey key = service.take();
            while (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        if (pathEvent.context().getFileName().equals(toWatch.getFileName())) {
                            notifyChange(toWatch);
                        }
                    }
                }
                if (!key.reset()) {
                    log.warn("directory inaccessible: " + toWatch.getParent());
                    return;
                }

                key = service.take();
            }
        } catch (InterruptedException e) {
            log.info("watch service interupted: ", e);
        }
    }

    /**
     * Notify all listeners about a change.
     * 
     * @param changed
     *            the file that changed.
     */
    private void notifyChange(Path changed) {
        for (FileChangeListener l : listeners) {
            try {
                l.fileChanged(changed);
            } catch (RuntimeException e) {
                log.warn("failed to notify " + l, e);
            }
        }
    }

    /**
     * Adds an additional listener that is to be notified when the watched file changes.
     * 
     * @param listener
     *            the listener to be notified on modifications.
     */
    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }

}
