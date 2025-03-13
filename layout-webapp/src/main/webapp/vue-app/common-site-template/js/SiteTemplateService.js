/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

export function getSiteTemplates () {
  return fetch('/layout/rest/site/templates', {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving site templates');
    } else {
      return resp.json();
    }
  });
}

export function getSiteTemplate (id) {
  return fetch(`/layout/rest/site/templates/${id}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving site template');
    } else {
      return resp.json();
    }
  });
}

export function createSiteTemplate (siteTemplate) {
  return fetch('/layout/rest/site/templates', {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(siteTemplate),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating site template');
    }
  });
}

export function updateSiteTemplate (siteTemplate) {
  return fetch(`/layout/rest/site/templates/${siteTemplate.id}`, {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(siteTemplate),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when updating site template');
    }
  });
}

export function deleteSiteTemplate (id) {
  return fetch(`/layout/rest/site/templates/${id}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when deleting site template');
    }
  });
}

export function saveAsSiteTemplate (siteTemplate, siteId) {
  return fetch(`/layout/rest/site/templates/${siteId}`, {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(siteTemplate),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating a site template based on an existing site identifier');
    }
  });
}
