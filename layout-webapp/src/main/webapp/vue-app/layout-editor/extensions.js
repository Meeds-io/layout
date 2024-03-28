/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

extensionRegistry.registerExtension('layout-editor', 'container', {
  rank: 1000,
  type: 'default',
  isValid: container => container?.template === 'Container',
  containerType: 'layout-editor-container',
});

extensionRegistry.registerExtension('layout-editor', 'container', {
  rank: 200,
  type: 'page-layout',
  isValid: container => container?.template === 'system:/groovy/portal/webui/container/UIPageLayout.gtmpl',
  containerType: 'layout-editor-container',
});

extensionRegistry.registerExtension('layout-editor', 'container', {
  rank: 100,
  type: 'page',
  isValid: container => container.type === 'PAGE',
  containerType: 'layout-editor-container-page',
});

extensionRegistry.registerExtension('layout-editor', 'container', {
  rank: 300,
  type: 'grid-section',
  isValid: container => container?.template === 'GridContainer',
  containerType: 'layout-editor-container-section',
});

extensionRegistry.registerExtension('layout-editor', 'container', {
  rank: 400,
  type: 'flex-section',
  isValid: container => container?.template === 'FlexContainer',
  containerType: 'layout-editor-container-section',
});

extensionRegistry.registerExtension('layout-editor', 'container', {
  rank: 500,
  type: 'cell',
  isValid: container => container?.template === 'CellContainer',
  containerType: 'layout-editor-container-cell',
});

extensionRegistry.registerExtension('layout-editor', 'container', {
  rank: 600,
  type: 'application',
  isValid: container => !container.template && (container.contentId?.includes?.('/') || container.storageId),
  containerType: 'layout-editor-container-application',
});
