/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.layout.entity;

import java.io.Serializable;
import java.util.List;

import org.exoplatform.commons.utils.StringListConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import io.meeds.common.persistence.PortableSequence;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "LayoutApplication")
@Table(name = "PORTAL_APPLICATIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortletInstanceEntity implements Serializable {

  private static final long serialVersionUID = 4955770436068594917L;

  @Id
  @PortableSequence(name = "SEQ_GTN_APPLICATION_ID")
  @Column(name = "ID")
  private Long              id;

  @Column(name = "CATEGORY_ID")
  private long              categoryId;

  @Column(name = "CONTENT_ID")
  private String            contentId;

  @Convert(converter = StringListConverter.class)
  @Column(name = "PERMISSIONS")
  private List<String>      permissions;

  @Column(name = "PREFERENCES")
  private String            preferences;

  @Column(name = "IS_SYSTEM")
  private boolean           system;

  @Column(name = "IS_DISABLED")
  private boolean           disabled;

  @Column(name = "FOOTPRINT")
  private long              footprint;

}
