/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple base class for commands that can be represented as a {@link Callable}.
 */
public abstract class SimpleCommand implements Command, Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(SimpleCommand.class);

    protected InputStream in;
    protected OutputStream out;
    protected OutputStream err;
    protected Environment env;
    private ExitCallback exit;

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.exit = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        this.env = env;
        int status = 127;
        try {
            status = call();
        } catch (Exception e) {
            log.warn("exception in simple command", e);
            status = 255;
        } finally {
            exit.onExit(status);
        }
    }

    @Override
    public void destroy() {
        // nothing
    }

}
