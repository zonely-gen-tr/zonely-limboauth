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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 */

package net.elytrium.limboauth.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Resolves game names against the Zonely site nickname, never the profile's subname. */
public final class RegisteredPlayerLookup {

  private RegisteredPlayerLookup() {
  }

  public static RegisteredPlayer findByNickname(Dao<RegisteredPlayer, Integer> dao, String nickname)
      throws SQLException {
    if (nickname == null) {
      return null;
    }

    QueryBuilder<RegisteredPlayer, Integer> query = dao.queryBuilder();
    query.where().raw("LOWER(nick) = ?", new SelectArg(RegisteredPlayer.NICKNAME_FIELD,
        nickname.toLowerCase(Locale.ROOT)));
    // Pre-existing duplicate nicknames must resolve to the original website account.
    query.orderBy(RegisteredPlayer.ID_FIELD, true).limit(1L);
    List<RegisteredPlayer> matches = dao.query(query.prepare());
    return matches.isEmpty() ? null : matches.get(0);
  }
}
