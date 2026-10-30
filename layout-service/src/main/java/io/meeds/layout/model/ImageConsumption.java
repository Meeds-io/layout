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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;

/**
 * The upload ids one layout write consumes: each id is claimed once while the
 * call's description is applied, before any write, and stands in the layout
 * as a placeholder until the attachments are saved and their URLs take its
 * place. The object id of an image is the call's key prefix, the live page or
 * site as the editor keys its attachments, then the element's storage id, or
 * the upload id for an element the call creates.
 */
public class ImageConsumption {

  public static final String        UPLOAD_NOT_FOUND  = "does not exist: upload";

  public static final String        UPLOAD_USED_TWICE = "upload id used twice";

  public static final String        REFUSAL_PREFIX    = "value is not accepted for styling.";

  public static final String        PLACEHOLDER       = "upload_";

  private final String              keyPrefix;

  private final Predicate<String>   staged;

  private final Map<String, ImageTarget> targets          = new LinkedHashMap<>();

  private final Map<String, String> urls              = new LinkedHashMap<>();

  private final List<AttachedImage> attached          = new ArrayList<>();

  /**
   * @param keyPrefix the live page's or site's storage id, page_&lt;id&gt; or
   *          site_&lt;id&gt;
   * @param staged whether an upload id names a staged resource
   */
  public ImageConsumption(String keyPrefix, Predicate<String> staged) {
    this.keyPrefix = keyPrefix;
    this.staged = staged;
  }

  /**
   * @return a consumption that accepts no upload id: the writers of a call
   *         that carries none
   */
  public static ImageConsumption none() {
    return new ImageConsumption(null, id -> false);
  }

  /**
   * @param uploadId the id the call gave
   * @param objectType the attachment object type of the background
   * @param elementStorageId the styled element's storage id, null for one the
   *          call creates
   * @param path the image block's path, named by the refusal of a blank id or
   *          of a call that takes no image
   * @return the placeholder written in the layout until the URL is known
   * @throws ObjectNotFoundException when no staged resource has the id
   */
  public String claim(String uploadId, String objectType, String elementStorageId, String path) throws ObjectNotFoundException {
    String id = StringUtils.trimToNull(uploadId);
    if (id == null || keyPrefix == null) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + path);
    } else if (!staged.test(id)) {
      throw new ObjectNotFoundException(UPLOAD_NOT_FOUND);
    } else if (targets.containsKey(id)) {
      throw new IllegalArgumentException(UPLOAD_USED_TWICE);
    }
    targets.put(id, new ImageTarget(objectType, keyPrefix + "_" + (elementStorageId == null ? id : elementStorageId)));
    return PLACEHOLDER + id;
  }

  public Map<String, ImageTarget> getTargets() {
    return Collections.unmodifiableMap(targets);
  }

  public boolean isEmpty() {
    return targets.isEmpty();
  }

  public void setUrl(String uploadId, String url) {
    urls.put(uploadId, url);
  }

  /**
   * @param value a stored image value
   * @return the URL the placeholder stands for, the value itself otherwise
   */
  public String resolve(String value) {
    if (value != null && value.startsWith(PLACEHOLDER)) {
      String url = urls.get(value.substring(PLACEHOLDER.length()));
      if (url != null) {
        return url;
      }
    }
    return value;
  }

  public void addAttached(AttachedImage image) {
    attached.add(image);
  }

  public List<AttachedImage> getAttached() {
    return Collections.unmodifiableList(attached);
  }

}
