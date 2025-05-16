/**
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.model.PortletDescriptor;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.plugin.attachment.PortletInstanceAttachmentPlugin;
import io.meeds.layout.plugin.translation.PortletInstanceCategoryTranslationPlugin;
import io.meeds.layout.plugin.translation.PortletInstanceTranslationPlugin;
import io.meeds.layout.storage.PortletInstanceCategoryStorage;
import io.meeds.layout.storage.PortletInstanceLayoutStorage;
import io.meeds.layout.storage.PortletInstanceStorage;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@Service
public class PortletInstanceService {

  public static final String                           INSTANCE_CREATED_EVENT    = "layout.portletInstance.created";

  public static final String                           INSTANCE_UPDATED_EVENT    = "layout.portletInstance.updated";

  public static final String                           INSTANCE_DELETED_EVENT    = "layout.portletInstance.deleted";

  public static final String                           CATEGORY_CREATED_EVENT    = "layout.portletInstanceCategory.created";

  public static final String                           CATEGORY_UPDATED_EVENT    = "layout.portletInstanceCategory.updated";

  public static final String                           CATEGORY_DELETED_EVENT    = "layout.portletInstanceCategory.deleted";

  private static final List<String>                    EVERYONE_PERMISSIONS_LIST = Collections.singletonList(UserACL.EVERYONE);

  private static final Log                             LOG                       =
                                                           ExoLogger.getLogger(PortletInstanceService.class);

  private LayoutAclService                             layoutAclService;

  private LayoutService                                layoutService;

  private TranslationService                           translationService;

  private AttachmentService                            attachmentService;

  private LocaleConfigService                          localeConfigService;

  private PortletInstanceCategoryStorage               portletInstanceCategoryStorage;

  private PortletInstanceStorage                       portletInstanceStorage;

  private PortletInstanceLayoutStorage                 portletInstanceLayoutStorage;

  private PortletService                               portletService;

  private ListenerService                              listenerService;

  private Map<String, PortletInstancePreferencePlugin> preferencePlugins;

  public PortletInstanceService(LayoutAclService layoutAclService,
                                LayoutService layoutService,
                                TranslationService translationService,
                                AttachmentService attachmentService,
                                LocaleConfigService localeConfigService,
                                PortletInstanceCategoryStorage portletInstanceCategoryStorage,
                                PortletInstanceStorage portletInstanceStorage,
                                PortletInstanceLayoutStorage portletInstanceLayoutStorage,
                                PortletService portletService,
                                ListenerService listenerService) {
    this.layoutAclService = layoutAclService;
    this.layoutService = layoutService;
    this.translationService = translationService;
    this.attachmentService = attachmentService;
    this.localeConfigService = localeConfigService;
    this.portletInstanceCategoryStorage = portletInstanceCategoryStorage;
    this.portletInstanceStorage = portletInstanceStorage;
    this.portletInstanceLayoutStorage = portletInstanceLayoutStorage;
    this.portletService = portletService;
    this.listenerService = listenerService;
  }

  @PostConstruct
  public void init() {
    this.preferencePlugins = new ConcurrentHashMap<>();
  }

  public void addPortletInstancePreferencePlugin(PortletInstancePreferencePlugin plugin) {
    preferencePlugins.put(plugin.getPortletName(), plugin);
  }

  public void removePortletInstancePreferencePlugin(String portletName) {
    preferencePlugins.remove(portletName);
  }

  public List<PortletInstance> getPortletInstances(long categoryId, String username, Locale locale, boolean expand) {
    List<PortletInstance> portletInstances = categoryId < 1 ? portletInstanceStorage.getPortletInstances() :
                                                            portletInstanceStorage.getPortletInstances(categoryId);
    portletInstances = portletInstances.stream().filter(p -> this.hasPermission(p, username)).toList();
    if (expand) {
      portletInstances.stream().forEach(portletInstance -> computePortletInstanceAttributes(locale, portletInstance));
    }
    return portletInstances;
  }

  public List<PortletInstanceCategory> getPortletInstanceCategories() {
    return portletInstanceCategoryStorage.getPortletInstanceCategories();
  }

  public List<PortletInstanceCategory> getPortletInstanceCategories(String username, Locale locale, boolean expand) {
    List<PortletInstanceCategory> portletInstanceCategories = portletInstanceCategoryStorage.getPortletInstanceCategories();
    portletInstanceCategories = portletInstanceCategories.stream().filter(c -> this.hasPermission(c, username)).toList();
    if (expand && locale != null) {
      portletInstanceCategories.stream().forEach(c -> computePortletInstanceCategoryAttributes(locale, c));
    }
    return portletInstanceCategories;
  }

  public PortletInstance getPortletInstance(long id,
                                            String username,
                                            Locale locale,
                                            boolean expand) throws IllegalAccessException, ObjectNotFoundException {
    PortletInstance portletInstance = portletInstanceStorage.getPortletInstance(id);
    if (portletInstance == null) {
      throw new ObjectNotFoundException("Portlet instance not found");
    }
    if (!this.hasPermission(portletInstance, username)) {
      throw new IllegalAccessException();
    }
    if (expand && locale != null) {
      computePortletInstanceAttributes(locale, portletInstance);
    }
    return portletInstance;
  }

  public PortletInstanceCategory getPortletInstanceCategory(long id) {
    return portletInstanceCategoryStorage.getPortletInstanceCategory(id);
  }

  public PortletInstanceCategory getPortletInstanceCategory(long id,
                                                            String username,
                                                            Locale locale,
                                                            boolean expand) throws ObjectNotFoundException,
                                                                            IllegalAccessException {
    PortletInstanceCategory portletInstanceCategory = portletInstanceCategoryStorage.getPortletInstanceCategory(id);
    if (portletInstanceCategory == null) {
      throw new ObjectNotFoundException("Portlet instance category not found");
    }
    if (!this.hasPermission(portletInstanceCategory, username)) {
      throw new IllegalAccessException();
    }
    if (expand) {
      computePortletInstanceCategoryAttributes(locale, portletInstanceCategory);
    }
    return portletInstanceCategory;
  }

  public PortletInstance getPortletInstance(long id) {
    return portletInstanceStorage.getPortletInstance(id);
  }

  public PortletInstance createPortletInstance(PortletInstance portletInstance, String username) throws IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to create a Portlet instance");
    }
    PortletInstance createdPortletInstance = portletInstanceStorage.createPortletInstance(portletInstance);
    listenerService.broadcast(INSTANCE_CREATED_EVENT, username, createdPortletInstance);
    return createdPortletInstance;
  }

  public PortletInstance createPortletInstance(PortletInstance portletInstance) {
    PortletInstance createdPortletInstance = portletInstanceStorage.createPortletInstance(portletInstance);
    listenerService.broadcast(INSTANCE_CREATED_EVENT, null, createdPortletInstance);
    return createdPortletInstance;
  }

  public PortletInstanceCategory createPortletInstanceCategory(PortletInstanceCategory portletInstanceCategory,
                                                               String username) throws IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to create a Portlet instance Category");
    }
    PortletInstanceCategory createdPortletInstanceCategory =
                                                           portletInstanceCategoryStorage.createPortletInstanceCategory(portletInstanceCategory);
    listenerService.broadcast(CATEGORY_CREATED_EVENT, username, createdPortletInstanceCategory);
    return createdPortletInstanceCategory;
  }

  public PortletInstanceCategory createPortletInstanceCategory(PortletInstanceCategory category) {
    PortletInstanceCategory createdPortletInstanceCategory =
                                                           portletInstanceCategoryStorage.createPortletInstanceCategory(category);
    listenerService.broadcast(CATEGORY_CREATED_EVENT, null, createdPortletInstanceCategory);
    return createdPortletInstanceCategory;
  }

  public void deletePortletInstance(long id, String username) throws IllegalAccessException, ObjectNotFoundException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to delete a Portlet instance");
    }
    PortletInstance portletInstance = getPortletInstance(id);
    if (portletInstance == null) {
      throw new ObjectNotFoundException("Portlet instance doesn't exist");
    }
    if (portletInstance.isSystem()) {
      throw new IllegalAccessException("Can't delete a system Portlet instance");
    }
    deletePortletInstanceFromStore(id);
    listenerService.broadcast(INSTANCE_DELETED_EVENT, username, portletInstance);
  }

  public void deletePortletInstance(long id) throws ObjectNotFoundException {
    PortletInstance portletInstance = getPortletInstance(id);
    if (portletInstance == null) {
      throw new ObjectNotFoundException("Portlet instance doesn't exist");
    }
    deletePortletInstanceFromStore(id);
    listenerService.broadcast(INSTANCE_DELETED_EVENT, null, portletInstance);
  }

  public void deletePortletInstanceCategory(long id, String username) throws IllegalAccessException, ObjectNotFoundException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to create a Portlet instance Category");
    }
    PortletInstanceCategory portletInstanceCategory = getPortletInstanceCategory(id);
    if (portletInstanceCategory == null) {
      throw new ObjectNotFoundException("Portlet instance Category doesn't exist");
    }
    if (portletInstanceCategory.isSystem()) {
      throw new IllegalAccessException("Can't delete a system Portlet instance Category");
    }
    deletePortletInstanceCategoryFromStore(id);
    listenerService.broadcast(CATEGORY_DELETED_EVENT, username, portletInstanceCategory);
  }

  public void deletePortletInstanceCategory(long id) throws ObjectNotFoundException {
    PortletInstanceCategory portletInstanceCategory = getPortletInstanceCategory(id);
    if (portletInstanceCategory == null) {
      throw new ObjectNotFoundException("Portlet instance Category doesn't exist");
    }
    deletePortletInstanceCategoryFromStore(id);
    listenerService.broadcast(CATEGORY_DELETED_EVENT, null, portletInstanceCategory);
  }

  public PortletInstance updatePortletInstance(PortletInstance portletInstance, String username) throws ObjectNotFoundException,
                                                                                                 IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to update a Portlet instance");
    }
    PortletInstance updatedPortletInstance = portletInstanceStorage.updatePortletInstance(portletInstance);
    listenerService.broadcast(INSTANCE_UPDATED_EVENT, username, updatedPortletInstance);
    return updatedPortletInstance;
  }

  public PortletInstance updatePortletInstance(PortletInstance portletInstance) throws ObjectNotFoundException {
    PortletInstance updatedPortletInstance = portletInstanceStorage.updatePortletInstance(portletInstance);
    listenerService.broadcast(INSTANCE_UPDATED_EVENT, null, updatedPortletInstance);
    return updatedPortletInstance;
  }

  public PortletInstanceCategory updatePortletInstanceCategory(PortletInstanceCategory portletInstanceCategory,
                                                               String username) throws ObjectNotFoundException,
                                                                                IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to update a Portlet instance category");
    }
    PortletInstanceCategory updatedPortletInstanceCategory =
                                                           portletInstanceCategoryStorage.updatePortletInstanceCategory(portletInstanceCategory);
    listenerService.broadcast(CATEGORY_UPDATED_EVENT, username, updatedPortletInstanceCategory);
    return updatedPortletInstanceCategory;
  }

  public PortletInstanceCategory updatePortletInstanceCategory(PortletInstanceCategory portletInstanceCategory) throws ObjectNotFoundException {
    PortletInstanceCategory updatedPortletInstanceCategory =
                                                           portletInstanceCategoryStorage.updatePortletInstanceCategory(portletInstanceCategory);
    listenerService.broadcast(CATEGORY_UPDATED_EVENT, null, updatedPortletInstanceCategory);
    return updatedPortletInstanceCategory;
  }

  public Application getPortletInstanceApplication(long portletInstanceId,
                                                   long applicationStorageId,
                                                   String username) throws IllegalAccessException, ObjectNotFoundException {
    PortletInstance portletInstance =
                                    portletInstanceId <= 0 ? null : getPortletInstance(portletInstanceId, username, null, false);
    return portletInstanceLayoutStorage.getPortletInstanceApplication(portletInstance, applicationStorageId);
  }

  public List<PortletInstancePreference> getPortletInstancePreferences(long portletInstanceId,
                                                                       PortletInstanceContext portletInstanceContext,
                                                                       String username) throws IllegalAccessException,
                                                                                        ObjectNotFoundException {
    PortletInstance portletInstance = getPortletInstance(portletInstanceId, username, null, false);
    return getPortletInstancePreferences(portletInstance, portletInstanceContext);
  }

  public List<PortletInstancePreference> getApplicationPreferences(long applicationId,
                                                                   String username,
                                                                   PortletInstanceContext portletInstanceContext) throws IllegalAccessException,
                                                                                                                  ObjectNotFoundException {
    Application application = portletInstanceLayoutStorage.getApplication(applicationId);
    if (application == null) {
      throw new ObjectNotFoundException(String.format("Application with id %s wasn't found", applicationId));
    }
    if (!layoutAclService.isAdministrator(username)
        && Arrays.stream(application.getAccessPermissions())
                 .noneMatch(permission -> layoutAclService.hasPermission(username, permission))) {
      throw new IllegalAccessException(String.format("Application with id %s access denied", applicationId));
    }
    return getApplicationPreferences(application, portletInstanceContext);
  }

  public long getApplicationPortletInstanceId(long applicationId) {
    return portletInstanceLayoutStorage.getApplicationPortletInstanceId(applicationId);
  }

  public long getPortletInstanceApplicationId(long portletInstanceId) {
    return portletInstanceLayoutStorage.getPortletInstanceApplicationId(portletInstanceId);
  }

  public List<PortletInstance> getPortletInstances() {
    return portletInstanceStorage.getPortletInstances();
  }

  public void exportApplicationPreferences(Application application) {
    Portlet preferences = getApplicationPortletPreferences(application);
    String portletContentId = layoutService.getId(application.getState());
    application.setState(new TransientApplicationState(portletContentId, preferences));
  }

  public Portlet getApplicationPortletPreferences(Application application) {
    List<PortletInstancePreference> exportedPreferences = getApplicationPreferences(application,
                                                                                    new PortletInstanceContext(false, null));
    Map<String, Preference> preferencesMap = exportedPreferences.stream()
                                                                .collect(Collectors.toMap(PortletInstancePreference::getName,
                                                                                          p -> new Preference(p.getName(),
                                                                                                              p.getValue(),
                                                                                                              false)));
    return new Portlet(preferencesMap);
  }

  public void expandPortletPreferences(Application application, PortletInstanceContext portletInstanceContext) {
    String portletContentId = layoutService.getId(application.getState());
    List<PortletInstancePreference> preferences = getApplicationPreferences(application, portletInstanceContext);
    Map<String, Preference> preferencesMap = preferences.stream()
                                                        .collect(Collectors.toMap(PortletInstancePreference::getName,
                                                                                  p -> new Preference(p.getName(),
                                                                                                      p.getValue(),
                                                                                                      false)));
    TransientApplicationState applicationState = new TransientApplicationState(portletContentId, new Portlet(preferencesMap));
    application.setState(applicationState);
  }

  private void deletePortletInstanceFromStore(long id) throws ObjectNotFoundException {
    try {
      attachmentService.deleteAttachments(PortletInstanceAttachmentPlugin.OBJECT_TYPE, String.valueOf(id));
    } catch (Exception e) {
      LOG.debug("Error while deleting attachments of deleted Portlet instance", e);
    }
    try {
      translationService.deleteTranslationLabels(PortletInstanceTranslationPlugin.OBJECT_TYPE, id);
    } catch (Exception e) {
      LOG.debug("Error while deleting translation labels of deleted Portlet instance", e);
    }
    portletInstanceStorage.deletePortletInstance(id);
  }

  private void deletePortletInstanceCategoryFromStore(long id) throws ObjectNotFoundException {
    try {
      translationService.deleteTranslationLabels(PortletInstanceCategoryTranslationPlugin.OBJECT_TYPE, id);
    } catch (ObjectNotFoundException e) {
      LOG.debug("Error while deleting translation labels of deleted Portlet instance Category", e);
    }
    portletInstanceCategoryStorage.deletePortletInstanceCategory(id);
  }

  private void computePortletInstanceAttributes(Locale locale, PortletInstance portletInstance) {
    portletInstance.setName(getLabel(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                     portletInstance.getId(),
                                     PortletInstanceTranslationPlugin.TITLE_FIELD_NAME,
                                     locale));
    portletInstance.setDescription(getLabel(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                            portletInstance.getId(),
                                            PortletInstanceTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                            locale));
    List<String> attachmentFileIds = attachmentService.getAttachmentFileIds(PortletInstanceAttachmentPlugin.OBJECT_TYPE,
                                                                            String.valueOf(portletInstance.getId()));
    if (CollectionUtils.isNotEmpty(attachmentFileIds)) {
      portletInstance.setIllustrationId(Long.parseLong(attachmentFileIds.get(0)));
    }
    portletInstance.setApplicationId(getPortletInstanceApplicationId(portletInstance.getId()));
    PortletDescriptor portlet = portletService.getPortlet(portletInstance.getContentId());
    if (portlet != null) {
      portletInstance.setEditable(portlet.isEditable());
    }
  }

  private void computePortletInstanceCategoryAttributes(Locale locale, PortletInstanceCategory portletInstanceCategory) {
    portletInstanceCategory.setName(getLabel(PortletInstanceCategoryTranslationPlugin.OBJECT_TYPE,
                                             portletInstanceCategory.getId(),
                                             PortletInstanceCategoryTranslationPlugin.TITLE_FIELD_NAME,
                                             locale));
  }

  private List<PortletInstancePreference> getPortletInstancePreferences(PortletInstance portletInstance,
                                                                        PortletInstanceContext portletInstanceContext) throws ObjectNotFoundException {
    Application application = portletInstanceLayoutStorage.getOrCreatePortletInstanceApplication(portletInstance);
    if (application == null) {
      throw new ObjectNotFoundException(String.format("Portlet Instance with id %s wasn't found", portletInstance.getId()));
    }
    return getApplicationPreferences(application, portletInstanceContext);
  }

  private List<PortletInstancePreference> getApplicationPreferences(Application application,
                                                                    PortletInstanceContext portletInstanceContext) {
    String portletName = portletInstanceLayoutStorage.getApplicationPortletName(application);
    Portlet preferences;
    if (StringUtils.isNumeric(application.getStorageId())) {
      preferences = portletInstanceLayoutStorage.getApplicationPreferences(Long.parseLong(application.getStorageId()));
    } else if (application.getState() instanceof TransientApplicationState transientState) {
      // Can happen when application is coming from AddonContainer
      preferences = transientState.getContentState();
    } else {
      preferences = null;
    }
    PortletInstancePreferencePlugin plugin = preferencePlugins.get(portletName);
    if (plugin == null && preferences == null) {
      return Collections.emptyList();
    } else if (plugin != null) {
      return plugin.generatePreferences(application, preferences, portletInstanceContext);
    } else {
      return StreamSupport.stream(preferences.spliterator(), false)
                          .map(p -> new PortletInstancePreference(p.getName(), p.getValue()))
                          .toList();
    }
  }

  private String getLabel(String objectType, long objectId, String fieldName, Locale locale) {
    if (locale == null) {
      locale = localeConfigService.getDefaultLocaleConfig().getLocale();
    }
    try {
      TranslationField translationField = translationService.getTranslationField(objectType, objectId, fieldName);
      if (translationField != null && MapUtils.isNotEmpty(translationField.getLabels())) {
        String label = translationField.getLabels().get(locale);
        if (label == null) {
          Locale defaultLocale = localeConfigService.getDefaultLocaleConfig().getLocale();
          label = translationField.getLabels().get(defaultLocale);
        }
        if (label == null) {
          label = translationField.getLabels().values().iterator().next();
        }
        return label;
      } else {
        return null;
      }
    } catch (ObjectNotFoundException e) {
      return null;
    }
  }

  private boolean hasPermission(PortletInstance portletInstance, String username) {
    List<String> permissions = portletInstance.getPermissions();
    return CollectionUtils.isEmpty(permissions) || permissions.equals(EVERYONE_PERMISSIONS_LIST)
           || (StringUtils.isNotBlank(username)
               && permissions.stream().anyMatch(p -> layoutAclService.hasPermission(username, p)));
  }

  private boolean hasPermission(PortletInstanceCategory category, String username) {
    List<String> permissions = category.getPermissions();
    return CollectionUtils.isEmpty(permissions) || permissions.equals(EVERYONE_PERMISSIONS_LIST)
           || (StringUtils.isNotBlank(username)
               && permissions.stream().anyMatch(p -> layoutAclService.hasPermission(username, p)));
  }

}
