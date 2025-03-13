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

export function getPortletInstanceCategories () {
  return fetch('/layout/rest/portlet/instance/categories', {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving portlet instance categories');
    } else {
      return resp.json();
    }
  });
}

export function getPortletInstanceCategory (id) {
  return fetch(`/layout/rest/portlet/instance/categories/${id}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving portlet instance category');
    } else {
      return resp.json();
    }
  });
}

export function createPortletInstanceCategory (category) {
  return fetch('/layout/rest/portlet/instance/categories', {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(category),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating portlet instance categories');
    }
  });
}

export function updatePortletInstanceCategory (category) {
  return fetch(`/layout/rest/portlet/instance/categories/${category.id}`, {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(category),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when updating portlet instance categories');
    }
  });
}

export function deletePortletInstanceCategory (id) {
  return fetch(`/layout/rest/portlet/instance/categories/${id}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when deleting portlet instance categories');
    }
  });
}
