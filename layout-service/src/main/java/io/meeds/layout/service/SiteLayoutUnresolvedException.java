/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.service;

/**
 * Thrown when the layout of a site couldn't be read: the site is read with an
 * empty layout, which an edit or a copy of the site would write over a stored
 * one.
 */
public class SiteLayoutUnresolvedException extends RuntimeException {

  private static final long serialVersionUID = 4519807625417936781L;

  public SiteLayoutUnresolvedException(String siteType, String siteName) {
    super(String.format("The layout of site %s:%s couldn't be read", siteType, siteName));
  }

}
