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

export function createNode (parentNodeId,
  previousNodeId,
  nodeLabel,
  nodeId,
  icon,
  visible,
  scheduled,
  startScheduleDate,
  endScheduleDate,
  labels,
  pageRef,
  target,
  isPasteMode) {
  return fetch('/layout/rest/navigations', {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      parentNodeId,
      previousNodeId,
      nodeLabel,
      nodeId,
      icon,
      visible,
      scheduled,
      isPasteMode,
      startScheduleDate: startScheduleDate?.getTime?.() || 0,
      endScheduleDate: endScheduleDate?.getTime?.() || 0,
      pageRef,
      target,
      labels,
    }),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw resp;
    }
  });
}

export function createDraftNode (nodeId) {
  return fetch(`/layout/rest/navigations/${nodeId}/draft`, {
    credentials: 'include',
    method: 'POST',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw resp;
    }
  });
}

export function updateNode (nodeId,
  nodeLabel,
  pageRef,
  visible,
  scheduled,
  startScheduleDate,
  endScheduleDate,
  labels,
  target,
  icon) {
  return fetch(`/layout/rest/navigations/${nodeId}`, {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      nodeLabel,
      icon,
      visible,
      scheduled,
      startScheduleDate: startScheduleDate?.getTime?.() || 0,
      endScheduleDate: endScheduleDate?.getTime?.() || 0,
      pageRef,
      target,
      labels,
    }),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when updating navigation node');
    }
  });
}

export function deleteNode (nodeId, delay) {
  localStorage.setItem('deletedNode', nodeId);
  return fetch(`/layout/rest/navigations/${nodeId}?delay=${delay || 0}`, {
    credentials: 'include',
    method: 'DELETE',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when deleting navigation node');
    }
  });
}

export function undoDeleteNode (nodeId) {
  return fetch(`/layout/rest/navigations/${nodeId}/undoDelete`, {
    method: 'POST',
    credentials: 'include',
  }).then(resp => {
    if (resp?.ok) {
      localStorage.removeItem('deletedNode');
    } else {
      throw new Error('Error when undoing deleting navigation node');
    }
  });
}

export function moveNode (nodeId, destinationParentId, previousNodeId) {
  const formData = new FormData();
  if (destinationParentId) {
    formData.append('destinationParentId', destinationParentId);
  }
  if (previousNodeId) {
    formData.append('previousNodeId', previousNodeId);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`/layout/rest/navigations/${nodeId}/move`, {
    credentials: 'include',
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params,
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when moving navigation node');
    }
  });
}

export function getNode (nodeId) {
  return fetch(`/layout/rest/navigations/${nodeId}`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when retrieving node');
    }
  });
}

export function getNodeLabels (nodeId) {
  return fetch(`/layout/rest/navigations/${nodeId}/labels`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when retrieving node labels');
    }
  });
}

export function getNodeUri (nodeId) {
  return fetch(`/layout/rest/navigations/${nodeId}/uri`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => {
    if (resp?.ok) {
      return resp.text();
    } else {
      throw new Error('Error when retrieving node uri');
    }
  });
}
