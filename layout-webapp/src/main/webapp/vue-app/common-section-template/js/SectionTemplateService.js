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

export function getSectionTemplates() {
  return fetch('/layout/rest/sections', {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving section templates');
    } else {
      return resp.json();
    }
  });
}

export function getSectionTemplate(id) {
  return fetch(`/layout/rest/sections/${id}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving section template');
    } else {
      return resp.json();
    }
  });
}

export function generateSectionTemplateNodeId(id) {
  return fetch(`/layout/rest/sections/${id}/nodeId`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving section template working node identifier');
    } else {
      return resp.json();
    }
  });
}

export function generateSectionTemplateContent(id) {
  return fetch(`/layout/rest/sections/${id}/content`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when retrieving section template content');
    } else {
      return resp.json();
    }
  });
}

export function saveAsSectionTemplate(pageRef, containerId) {
  return fetch(`/layout/rest/sections/container/${containerId}`, {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: `pageRef=${pageRef}`,
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating a section template based on an existing section container identifier');
    }
  });
}

export function createSectionTemplate(sectionTemplate) {
  return fetch('/layout/rest/sections', {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(sectionTemplate),
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating section template');
    }
  });
}

export function updateSectionTemplate(sectionTemplate) {
  return fetch(`/layout/rest/sections/${sectionTemplate.id}`, {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(sectionTemplate),
  }).then((resp) => {
    if (!resp?.ok) {
      throw new Error('Error when updating section template');
    }
  });
}

export function restoreSectionTemplate(id) {
  return fetch(`/layout/rest/sections/${id}/restore`, {
    credentials: 'include',
    method: 'PUT',
  }).then((resp) => {
    if (!resp?.ok) {
      throw new Error('Error when restoring section template');
    }
  });
}

export function deleteSectionTemplate(id) {
  return fetch(`/layout/rest/sections/${id}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then((resp) => {
    if (!resp?.ok) {
      throw new Error('Error when deleting section template');
    }
  });
}
