/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

import at.mduft.rex.command.DefaultCommand;

/**
 * {@link Factory} used to create a shell (i.e. no command given during connect). This simply
 * returns the {@link DefaultCommand} which prints out some help and quits with an error code.
 */
public class RexShellFactory implements Factory<Command> {

    @Override
    public Command create() {
        return new DefaultCommand();
    }

}
