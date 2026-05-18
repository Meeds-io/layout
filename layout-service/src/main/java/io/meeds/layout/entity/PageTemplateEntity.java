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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import io.meeds.common.persistence.PortableSequence;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "LayoutPageTemplate")
@Table(name = "LAYOUT_PAGE_TEMPLATES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageTemplateEntity {

  @Id
  @PortableSequence(name = "SEQ_PAGE_TEMPLATE_ID")
  @Column(name = "ID")
  protected Long  id;

  @Column(name = "DISABLED")
  private boolean disabled;

  @Column(name = "SYSTEM_PAGE_TEMPLATE")
  private boolean system;

  @Column(name = "CATEGORY")
  private String  category;

  @Column(name = "CONTENT")
  private String  content;

}
