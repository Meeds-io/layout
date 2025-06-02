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

export function getPageTemplates(expandContent) {
  return fetch(`/layout/rest/page/templates?expandContent=${expandContent || false}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving page templates');
    } else {
      return resp.json();
    }
  });
}

export function getPageTemplate(id, expandContent) {
  return fetch(`/layout/rest/page/templates/${id}?expandContent=${expandContent || false}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving page template');
    } else {
      return resp.json();
    }
  });
}

export function createPageTemplate(pageContent, disabled) {
  return fetch('/layout/rest/page/templates', {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      content: pageContent,
      disabled: disabled || false,
    }),
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating page template');
    }
  });
}

export function updatePageTemplate(pageTemplate) {
  return fetch(`/layout/rest/page/templates/${pageTemplate.id}`, {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(pageTemplate),
  }).then((resp) => {
    if (!resp?.ok) {
      throw new Error('Error when updating page template');
    }
  });
}

export function restorePageTemplate(id) {
  return fetch(`/layout/rest/page/templates/${id}/restore`, {
    credentials: 'include',
    method: 'PUT',
  }).then((resp) => {
    if (!resp?.ok) {
      throw new Error('Error while restoring page template');
    }
  });
}

export function deletePageTemplate(id) {
  return fetch(`/layout/rest/page/templates/${id}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then((resp) => {
    if (!resp?.ok) {
      throw new Error('Error when deleting page template');
    }
  });
}
