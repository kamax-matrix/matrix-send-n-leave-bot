/*
 * Matrix Send'n'Leave Bot - Join rooms we are invited to, send one message than leave.
 * Copyright (C) 2018 Maxime Dor
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.kamax.matrix.bots.send_n_leave;

import io.kamax.matrix.MatrixErrorInfo;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.client.*;
import io.kamax.matrix.client.regular.MatrixHttpClient;
import io.kamax.matrix.client.regular.SyncOptions;
import io.kamax.matrix.hs.MatrixHomeserver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Objects;

public class SendNLeaveBot {

    private final Logger log = LoggerFactory.getLogger(SendNLeaveBot.class);

    private _MatrixID mxisd;
    private String password;
    private String baseUrl;
    private String message;

    private _MatrixClient client;

    public void setMxisd(String mxisd) {
        this.mxisd = MatrixID.asValid(mxisd);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void start() {
        if (Objects.isNull(mxisd)) {
            throw new RuntimeException("Matrix ID of the user is not set");
        }

        if (StringUtils.isBlank(baseUrl)) {
            throw new RuntimeException("Homeserver base URL is not set");
        }

        if (StringUtils.isBlank(message)) {
            throw new RuntimeException("Message is not set");
        }

        try {
            MatrixHomeserver hs = new MatrixHomeserver(mxisd.getDomain(), baseUrl);
            MatrixClientContext context = new MatrixClientContext(hs);
            client = new MatrixHttpClient(context);
            client.login(new MatrixPasswordLoginCredentials(mxisd.getLocalPart(), password));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Thread t = new Thread(() -> {
            log.info("Send'n'Leave bot is running");
            String syncToken = null;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    _SyncData data = client.sync(SyncOptions.build().setTimeout(30000).setSince(syncToken).get());
                    log.debug("Sync call returned");

                    data.getRooms().getInvited().stream().map(r -> client.getRoom(r.getId())).forEach(r -> {
                        log.info("Invited to {}", r.getAddress());
                        try {
                            r.join();
                        } catch (MatrixClientRequestException e) {
                            MatrixErrorInfo err = e.getError().orElseGet(() -> new MatrixErrorInfo("Unknown error"));
                            log.warn("Unable to join {}: {} - {}", r.getAddress(), err.getErrcode(), err.getError());
                            r.leave();
                        }
                    });

                    data.getRooms().getJoined().stream().map(r -> client.getRoom(r.getId())).forEach(r -> {
                        log.info("Joined to {}", r.getAddress());
                        r.sendText(message);
                        log.info("{} - Sending message", r.getAddress());
                        r.leave();
                        log.info("{} - Leaving", r.getAddress());
                    });

                    data.getRooms().getLeft().forEach(r -> {
                        log.info("Left {}", r.getId());
                    });

                    syncToken = data.nextBatchToken();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.setName("sync");

        t.start();
    }

}
