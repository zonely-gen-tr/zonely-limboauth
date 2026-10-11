/*
 * Copyright (C) 2021 - 2025 Elytrium
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.compat;

import com.j256.ormlite.dao.Dao;
import java.sql.SQLException;
import java.util.Objects;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.elytrium.limboauth.model.RegisteredPlayerLookup;

/** Reads password hashes from shared web/game storage to invalidate stale sessions. */
public final class CredentialSessionVerifier {

  private CredentialSessionVerifier() {
  }

  public static String readHash(Dao<RegisteredPlayer, Integer> dao, String lowercaseNickname) throws SQLException {
    RegisteredPlayer account = RegisteredPlayerLookup.findByNickname(dao, lowercaseNickname);
    return account == null ? null : account.getHash();
  }

  public static boolean isCurrent(Dao<RegisteredPlayer, Integer> dao, String lowercaseNickname, String sessionHash)
      throws SQLException {
    RegisteredPlayer account = RegisteredPlayerLookup.findByNickname(dao, lowercaseNickname);
    return account != null && Objects.equals(sessionHash, account.getHash());
  }
}
