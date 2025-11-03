package io.meeds.layout.plugin.renderer;

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.ObjectAttachmentDetail;
import org.exoplatform.social.attachment.model.ObjectAttachmentList;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CMSPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {
  private static final Log LOG = ExoLogger.getLogger(SidebarLoginPortletInstancePreferencePlugin.class);

  private static final String DATA_INIT_PREFERENCE_NAME = "data.init";

  private static final String EXPORT_DATA_INIT_PREFERENCE_NAME = "export.data.init";

  private static final String EXPORT_DATA_ALT_INIT_PREFERENCE_NAME = "export.data.alt.init";

  private static final String EXPORT_DATA_TRANSLATION_INIT_PREFERENCE_NAME = "export.data.translation.init";

  private static final String CMS_SETTING_PREFERENCE_NAME = "name";

  @Autowired
  private AttachmentService attachmentService;

  @Autowired
  private FileService fileService;

  @Autowired
  private TranslationService translationService;

  public static final String OBJECT_TYPE = "cmsPortlet";

  @Override
  public String getPortletName() {
    return "CMSPortlet";
  }

  @Override
  @SneakyThrows
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    String settingName = getCmsSettingName(preferences);
    List<PortletInstancePreference> result = new ArrayList<>();
    preferences.forEach(preference -> {
      if (!preference.getName().equals("name") &&
          !preference.getName().equals(DATA_INIT_PREFERENCE_NAME)) {
        result.add(new PortletInstancePreference(preference.getName(), preference.getValue()));
      }
    });
    if (!StringUtils.isBlank(settingName)) {
      if (portletInstanceContext.isExport()) {
        //portlet is in export process
        //we need to put background images and translations in preferences
        result.addAll(computePreferencesToExport(settingName));
      } else {
        result.add(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME, settingName));
      }
    } else {

      if (portletInstanceContext.isExport() && preferences.getPreference(DATA_INIT_PREFERENCE_NAME) != null) {
        //portlet is in export process
        //we need to put background images and translations in preferences
        settingName = preferences.getPreference(DATA_INIT_PREFERENCE_NAME).getValue();
        result.addAll(computePreferencesToExport(settingName));
      }
    }


    return result;
  }

  private String getFileContent(String id) {
    try {
      FileItem
          file =
          fileService.getFile(Long.parseLong(id));
      if (file == null) {
        return null;
      } else {
        return Base64.encodeBase64String(file.getAsByte());
      }
    } catch (FileStorageException e) {
      LOG.warn("Unable to read file with id=", id, e);
      return null;
    }
  }

  private String getCmsSettingName(Portlet preferences) {
    if (preferences == null) {
      return null;
    }
    Preference settingNamePreference = preferences.getPreference(CMS_SETTING_PREFERENCE_NAME);
    return settingNamePreference == null ? null : settingNamePreference.getValue();
  }

  private List<PortletInstancePreference> computePreferencesToExport(String settingName) {
    List<PortletInstancePreference> preferencesToExport = new ArrayList<>();

    try {
      JSONObject translationsJson = new JSONObject();
      Map<String, TranslationField>
          translations = translationService.getAllTranslationFields(OBJECT_TYPE, settingName);
      translations.entrySet().forEach(entry -> {
        String translationKey = entry.getKey();
        TranslationField translationField = entry.getValue();
        if (!translationField.getLabels().isEmpty()) {
          translationsJson.put(translationKey, translationField.getLabels());
        }
      });
      if (!translationsJson.isEmpty()) {
        preferencesToExport.add(new PortletInstancePreference(EXPORT_DATA_TRANSLATION_INIT_PREFERENCE_NAME,
                                                              translationsJson.toString()));
      }
    } catch (ObjectNotFoundException o) {
      //nothing to do, no translations to copy
    }

    ObjectAttachmentList attachmentList = attachmentService.getAttachments(OBJECT_TYPE, settingName);
    ObjectAttachmentDetail file = attachmentList == null || attachmentList.getAttachments() == null || attachmentList.getAttachments().size() == 0 ?
                                  null :
                                  attachmentList.getAttachments().getFirst();
    if (file != null) {
      String fileContent = getFileContent(file.getId());
      preferencesToExport.add(new PortletInstancePreference(EXPORT_DATA_INIT_PREFERENCE_NAME, fileContent));
      if (file.getAltText() != null) {
        preferencesToExport.add(new PortletInstancePreference(EXPORT_DATA_ALT_INIT_PREFERENCE_NAME, file.getAltText()));
      }
    }
    return preferencesToExport;
  }
}
