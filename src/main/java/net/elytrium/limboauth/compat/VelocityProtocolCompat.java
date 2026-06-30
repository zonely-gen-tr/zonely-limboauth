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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class VelocityProtocolCompat {

  private static final String PLUGIN_MESSAGE_PACKET = "com.velocitypowered.proxy.protocol.packet.PluginMessagePacket";
  private static final String PLUGIN_MESSAGE = "com.velocitypowered.proxy.protocol.packet.PluginMessage";

  private VelocityProtocolCompat() {

  }

  public static boolean isPluginMessage(Object packet) {
    String className = packet.getClass().getName();
    return PLUGIN_MESSAGE_PACKET.equals(className) || PLUGIN_MESSAGE.equals(className);
  }

  public static String getPluginMessageChannel(Object packet) {
    try {
      return (String) packet.getClass().getMethod("getChannel").invoke(packet);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  public static ByteBuf getPluginMessageContent(Object packet) {
    return ((ByteBufHolder) packet).content();
  }

  public static Object createPluginMessage(String channel, ByteBuf content) {
    try {
      Constructor<?> constructor = pluginMessageClass().getConstructor(String.class, ByteBuf.class);
      return constructor.newInstance(channel, content);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Class<?> pluginMessageClass() {
    try {
      return Class.forName(PLUGIN_MESSAGE_PACKET);
    } catch (ClassNotFoundException ignored) {
      try {
        return Class.forName(PLUGIN_MESSAGE);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
