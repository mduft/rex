/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.util;

import java.nio.file.Path;

/**
 * Implementors can register with a {@link FileChangeWatch} to get notify on file modifications on
 * disc.
 */
public interface FileChangeListener {

    /**
     * Called whenever the file changes the the underlying {@link FileChangeWatch} is bound to.
     * 
     * @param file
     *            the file that changed to allow binding the listener to multiple
     *            {@link FileChangeWatch}es.
     */
    public void fileChanged(Path file);

}
