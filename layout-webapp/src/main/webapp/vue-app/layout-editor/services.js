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

import * as layoutUtils from './js/LayoutUtils.js';
import * as coeditingService from './js/CoeditingService.js';

if (!Vue.prototype.$layoutUtils) {
  window.Object.defineProperty(Vue.prototype, '$layoutUtils', {
    value: layoutUtils,
  });
}
if (!Vue.prototype.$coeditingService) {
  window.Object.defineProperty(Vue.prototype, '$coeditingService', {
    value: coeditingService,
  });
}

Vue.prototype.$updateApplicationVisibility = function(visible, element) {
  if (!element) {
    element = this?.$root?.$el;
  }
  if (!element?.className?.includes?.('PORTLET-FRAGMENT')) {
    element = element?.parentElement;
  }
  if (element?.parentElement) {
    if (visible) {
      element.closest?.('.PORTLET-FRAGMENT')?.parentElement?.classList?.remove?.('hidden-sm-and-down');
    } else {
      element.closest?.('.PORTLET-FRAGMENT')?.parentElement?.classList?.add?.('hidden-sm-and-down');
    }
  }
};
