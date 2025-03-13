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

import * as siteLayoutService from './js/SiteLayoutService.js';
import * as navigationLayoutService from './js/NavigationLayoutService.js';
import * as pageLayoutService from './js/PageLayoutService.js';
import * as pageTemplateService from './js/PageTemplateService.js';
import * as applicationUtils from './js/ApplicationUtils.js';

if (!eXo.$navigationLayoutService) {
  window.Object.defineProperty(eXo, '$navigationLayoutService', {
    value: navigationLayoutService,
  });
}

if (!eXo.$siteLayoutService) {
  window.Object.defineProperty(eXo, '$siteLayoutService', {
    value: siteLayoutService,
  });
}

if (!eXo.$pageLayoutService) {
  window.Object.defineProperty(eXo, '$pageLayoutService', {
    value: pageLayoutService,
  });
}

if (!eXo.$pageTemplateService) {
  window.Object.defineProperty(eXo, '$pageTemplateService', {
    value: pageTemplateService,
  });
}

if (!eXo.$applicationUtils) {
  window.Object.defineProperty(eXo, '$applicationUtils', {
    value: applicationUtils,
  });
}
