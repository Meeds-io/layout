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

export function getPortletInstances (categoryId) {
  return fetch(`/layout/rest/portlet/instances${categoryId && `?categoryId=${categoryId}` || ''}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving portlet instances');
    } else {
      return resp.json();
    }
  });
}

export function getPortletInstance (id) {
  return fetch(`/layout/rest/portlet/instances/${id}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving portlet instance');
    } else {
      return resp.json();
    }
  });
}

export function getPortletInstancePreferences (id) {
  return fetch(`/layout/rest/portlet/instances/${id}/preferences`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving portlet instance preferences');
    } else {
      return resp.json();
    }
  });
}

export function createPortletInstance (portletInstance) {
  return fetch('/layout/rest/portlet/instances', {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(portletInstance),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating portlet instance');
    }
  });
}

export function updatePortletInstance (portletInstance) {
  return fetch(`/layout/rest/portlet/instances/${portletInstance.id}`, {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(portletInstance),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when updating portlet instance');
    }
  });
}

export function deletePortletInstance (id) {
  return fetch(`/layout/rest/portlet/instances/${id}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when deleting portlet instance');
    }
  });
}
