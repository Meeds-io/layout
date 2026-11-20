/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.model;

import org.exoplatform.commons.exception.ObjectNotFoundException;

/**
 * A layout write run under a guard of the layout services: the per-window
 * lock, or the wrapper that names a draft this call created when the write
 * that follows it fails
 *
 * @param <T> what the write returns
 */
@FunctionalInterface
public interface LayoutWrite<T> {

  T run() throws ObjectNotFoundException, IllegalAccessException;

}
