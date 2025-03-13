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

export function createSite (siteName, siteId, siteLabel, siteDescription, displayed, displayOrder, bannerUploadId, icon, accessPermissions, editPermission) {
  const createModel = {
    siteId,
    portalConfig: {
      name: siteName,
      label: siteLabel,
      description: siteDescription,
      displayed,
      displayOrder,
      bannerUploadId,
      accessPermissions,
      editPermission,
      properties: {
        icon: icon || 'fa-globe',
      },
    },
  };
  return fetch('/layout/rest/sites', {
    credentials: 'include',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(createModel),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating site');
    }
  });
}

export function createDraftSite (siteType, siteName) {
  const formData = new FormData();
  if (siteType) {
    formData.append('siteType', siteType);
  }
  if (siteName) {
    formData.append('siteName', siteName);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`/layout/rest/sites/draft?${params}`, {
    credentials: 'include',
    method: 'POST',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating site');
    }
  });
}

export function getSiteLayout (siteType, siteName, expand) {
  const formData = new FormData();
  if (siteType) {
    formData.append('siteType', siteType);
  }
  if (siteName) {
    formData.append('siteName', siteName);
  }
  if (expand) {
    formData.append('expand', expand);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`/layout/rest/sites/layout?${params}`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when creating site');
    }
  });
}

export function getSiteById (siteId, lang) {
  const formData = new FormData();
  if (lang) {
    formData.append('lang', lang);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`/layout/rest/sites/${siteId}${params.length && '?' || ''}${params.length && params || ''}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when Getting site');
    }
  });
}

export function getSite (siteType, siteName, lang) {
  const formData = new FormData();
  formData.append('siteName', siteName);
  formData.append('siteType', siteType || 'portal');
  if (lang) {
    formData.append('lang', lang);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`/layout/rest/sites?${params}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when Getting site');
    }
  });
}

export function getSiteLabels (siteId) {
  if (!siteId) {
    return Promise.resolve({ en: null });
  }
  return fetch(`/layout/rest/sites/${siteId}/labels`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when retrieving site labels');
    }
  });
}

export function getSiteDescriptions (siteId) {
  if (!siteId) {
    return Promise.resolve({ en: null });
  }
  return fetch(`/layout/rest/sites/${siteId}/descriptions`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when retrieving site descriptions');
    }
  });
}

export function updateSite (siteName, siteType, siteLabel, siteDescription, displayed, displayOrder, bannerUploadId, bannerRemoved, siteIcon) {
  const updateModel = {
    siteType,
    siteName,
    siteLabel,
    siteDescription,
    siteIcon: siteIcon || 'fa-globe',
    displayed,
    displayOrder,
    bannerRemoved: bannerRemoved || false,
  };
  if (bannerUploadId) {
    updateModel.bannerUploadId = bannerUploadId;
  }
  return fetch('/layout/rest/sites', {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(updateModel),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when updating site');
    }
  });
}

export function updateSiteLayout (siteType, siteName, layout, expand, publish) {
  return fetch(`/layout/rest/sites/layout?siteType=${siteType}&siteName=${siteName}&expand=${expand || ''}&publish=${publish || false}`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(layout),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else if (resp.status === 400) {
      return resp.json()
        .then(e => {
          throw new Error(e?.message);
        });
    } else {
      throw new Error(resp.status);
    }
  });
}

export function restoreSite ({
  siteName,
  siteType,
  importMode,
  siteLayout,
  sitePages,
  siteNavigation,
}) {
  const formData = new FormData();
  if (siteName) {
    formData.append('siteName', siteName);
  }
  if (siteType) {
    formData.append('siteType', siteType);
  }
  if (importMode) {
    formData.append('importMode', importMode);
  }
  if (siteLayout) {
    formData.append('siteLayout', siteLayout);
  }
  if (sitePages) {
    formData.append('pagesLayout', sitePages);
  }
  if (siteNavigation) {
    formData.append('navigation', siteNavigation);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch('/layout/rest/sites/restore', {
    credentials: 'include',
    method: 'PUT',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params,
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when updating site');
    }
  });
}

export function deleteSite (siteType, siteName) {
  const formData = new FormData();
  formData.append('siteName', siteName);
  formData.append('siteType', siteType || 'portal');
  const params = new URLSearchParams(formData).toString();
  return fetch(`/layout/rest/sites?${params}`, {
    method: 'DELETE',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when deleting site');
    }
  });
}

export function updateSitePermissions (siteType, siteName, editPermission, accessPermissions) {
  return fetch('/layout/rest/sites/permissions', {
    method: 'PATCH',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      siteType,
      siteName,
      editPermission: editPermission || null,
      accessPermissions: accessPermissions || null,
    }),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when updating site permissions');
    }
  });
}

export function getMembershipTypes () {
  return fetch(`/${eXo.env.portal.rest}/v1/membershipTypes`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error when retrieving membership types');
    }
  });
}
