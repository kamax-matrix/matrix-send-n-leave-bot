/*
 * Matrix Send'n'Leave Bot - Join rooms we are invited to, send one message than leave.
 * Copyright (C) 2018 Kamax Sarl
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

import java.util.Optional;

public class SendNLeaveBotApp {

    public static void main(String[] args) {
        SendNLeaveBot bot = new SendNLeaveBot();

        bot.setMxid(System.getenv("SNL_USER_MXID"));
        bot.setPassword(System.getenv("SNL_USER_PASSWORD"));
        bot.setMessage(System.getenv("SNL_MESSAGE"));
        Optional.ofNullable(System.getenv("SNL_HS_URL")).ifPresent(bot::setBaseURL);

        bot.start();
    }

}
