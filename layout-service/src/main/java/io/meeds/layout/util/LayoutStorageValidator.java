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
package io.meeds.layout.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.CloneApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PersistentApplicationState;

/**
 * Checks that the storage ids a layout draft save carries name windows and
 * containers the edited page or site already holds. The storage saves an
 * element carrying a positive storage id as an update of the stored window or
 * container with that id, so a draft save may only carry the ids of the
 * layout it replaces; an element without such an id is created.
 */
public final class LayoutStorageValidator {

  private LayoutStorageValidator() {
    // Static utility
  }

  /**
   * Refuses a submitted layout that carries a window or container storage id
   * the stored layout does not hold. Windows and containers are stored under
   * two id sequences, so a window id is only accepted on a window and a
   * container id (a page or site body placeholder included) only on a
   * container. A window's state id must name the same held window as its
   * storage id: the storage updates a window by its storage id, while a
   * submitted layout used as is, the meta portal's after its save, renders the
   * window and stores its preferences through its state id. The layout REST
   * builds the state id from the storage id. Ids are read as the storage reads
   * them: an id that does not parse to a positive number is not an update and
   * is not checked.
   *
   * @param storedElements the root elements of the stored layout, null
   *          elements ignored
   * @param submittedElements the root elements of the layout to save, null
   *          elements ignored
   * @param errorCode the message code of the refusal
   * @throws IllegalArgumentException when a submitted id is not held by the
   *           stored layout
   */
  public static void validateHeldStorageIds(Collection<? extends ModelObject> storedElements,
                                            Collection<? extends ModelObject> submittedElements,
                                            String errorCode) {
    if (submittedElements == null || submittedElements.isEmpty()) {
      return;
    }
    Set<Long> windowIds = new HashSet<>();
    Set<Long> containerIds = new HashSet<>();
    if (storedElements != null) {
      storedElements.forEach(element -> collectHeldIds(element, windowIds, containerIds));
    }
    submittedElements.forEach(element -> checkHeldIds(element, windowIds, containerIds, errorCode));
  }

  private static void collectHeldIds(ModelObject element, Set<Long> windowIds, Set<Long> containerIds) {
    if (element == null) {
      return;
    }
    if (element instanceof Application application) {
      addId(windowIds, toId(application.getStorageId()));
      addId(windowIds, toStateId(application.getState()));
    } else {
      addId(containerIds, toId(element.getStorageId()));
      if (element instanceof Container container && container.getChildren() != null) {
        container.getChildren().forEach(child -> collectHeldIds(child, windowIds, containerIds));
      }
    }
  }

  private static void checkHeldIds(ModelObject element, Set<Long> windowIds, Set<Long> containerIds, String errorCode) {
    if (element == null) {
      return;
    }
    if (element instanceof Application application) {
      long applicationId = toId(application.getStorageId());
      long stateId = toStateId(application.getState());
      if ((applicationId > 0 && !windowIds.contains(applicationId))
          || (stateId > 0 && !windowIds.contains(stateId))
          || (applicationId > 0 && stateId > 0 && applicationId != stateId)) {
        throw new IllegalArgumentException(errorCode);
      }
    } else {
      long containerId = toId(element.getStorageId());
      if (containerId > 0 && !containerIds.contains(containerId)) {
        throw new IllegalArgumentException(errorCode);
      }
      if (element instanceof Container container && container.getChildren() != null) {
        container.getChildren().forEach(child -> checkHeldIds(child, windowIds, containerIds, errorCode));
      }
    }
  }

  private static long toStateId(ApplicationState state) {
    if (state instanceof PersistentApplicationState persistentState) {
      return toId(persistentState.getStorageId());
    } else if (state instanceof CloneApplicationState cloneState) {
      return toId(cloneState.getStorageId());
    } else {
      return 0;
    }
  }

  private static long toId(String storageId) {
    // Same parsing as the storage, which updates the element when it is positive
    return Safe.parseLong(storageId);
  }

  private static void addId(Set<Long> ids, long id) {
    if (id > 0) {
      ids.add(id);
    }
  }

}
