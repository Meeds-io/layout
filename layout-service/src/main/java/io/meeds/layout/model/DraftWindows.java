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

import java.util.List;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageKey;

/**
 * The windows a write addresses on the user's layout, resolved without a
 * write: the layout read (the user's draft when their revision names one,
 * the published layout otherwise), the stored windows the ids name and
 * their contexts. One of the two groups of fields is set, the page's or the
 * site's.
 *
 * @param livePageKey the live page, null for a site
 * @param liveNodeId the live page's navigation node, null for a site
 * @param draftPageKey the user's draft page, null when none exists or for a
 *          site
 * @param liveSite the live site, null for a page
 * @param draftSite the user's draft site, null when none exists or for a
 *          page
 * @param storedLayout the stored tree the windows were resolved in, the
 *          draft's when one exists, the live layout otherwise
 * @param windows the stored windows, in the order of the ids
 * @param contexts the windows' contexts on that layout, in the same order
 */
public record DraftWindows(PageKey livePageKey,
                           Long liveNodeId,
                           PageKey draftPageKey,
                           PortalConfig liveSite,
                           PortalConfig draftSite,
                           ModelObject storedLayout,
                           List<Application> windows,
                           List<ApplicationSettingsContext> contexts) {

  /**
   * @return whether the user has no draft yet, so that a write creates one
   *         first and addresses the windows on it
   */
  public boolean createDraft() {
    return isPage() ? draftPageKey == null : draftSite == null;
  }

  /**
   * @return whether the layout is a page
   */
  public boolean isPage() {
    return livePageKey != null;
  }

}
