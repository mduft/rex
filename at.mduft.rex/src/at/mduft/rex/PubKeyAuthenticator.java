/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.common.util.Base64;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.mduft.rex.util.FileChangeListener;
import at.mduft.rex.util.FileChangeWatch;

/**
 * Public Key Authenticator that is capable of deciding whether a given public key is allowed to
 * login to the server. The user name has to be the one given on the server command line using
 * --user.
 */
public class PubKeyAuthenticator implements PublickeyAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(PubKeyAuthenticator.class);
    private final List<PublicKey> signatures = new ArrayList<>();
    private final FileChangeWatch watchService;
    private final String username;

    /**
     * Creates an authenticator that reads allowed keys from a file. A {@link WatchService} is set
     * up to track modifications on the given {@link File}. In case the {@link File} changes, it is
     * reloaded from disc.
     * 
     * @param user
     *            the user that is allowed to connect using the given keys.
     * @param pubKeyFile
     *            the public key file. format is the same as OpenSSH's authorized_keys2 file.
     * @throws IOException
     *             in case of an error while setting up the {@link WatchService}.
     */
    public PubKeyAuthenticator(String user, File pubKeyFile) throws IOException {
        username = user;

        Path toWatch = pubKeyFile.toPath();
        watchService = new FileChangeWatch(toWatch);
        watchService.addListener(new FileChangeListener() {
            @Override
            public void fileChanged(Path file) {
                reLoad(file);
            }
        });
        watchService.start();

        // initial load
        reLoad(toWatch);
    }

    /**
     * Loads public keys from the given files, decodes them and remembers the {@link PublicKey}
     * instance in a {@link List}.
     * 
     * @param pubKeyFile
     *            the public key file to parse.
     */
    private void reLoad(Path pubKeyFile) {
        log.info("(re-)loading keys from " + pubKeyFile);
        signatures.clear();

        try (BufferedReader r = new BufferedReader(new FileReader(pubKeyFile.toFile()))) {
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    for (String part : line.split(" ")) {
                        // AAAA -> base64 encoded key
                        if (!part.startsWith("AAAA")) {
                            continue;
                        }
                        Buffer b = new Buffer(Base64.decodeBase64(part.getBytes()));
                        signatures.add(b.getRawPublicKey());
                    }
                } catch (Exception e) {
                    log.error("failed to load a key: " + e);
                }
            }
        } catch (IOException e) {
            log.error("failed to load authorized keys", e);
        }
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        if (!this.username.equals(username)) {
            log.info("rejecting unauthorized username " + username);
            return false;
        }

        for (PublicKey allowed : signatures) {
            if (key.equals(allowed)) {
                return true;
            }
        }

        return false;
    }

}
