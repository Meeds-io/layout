<%@page import="io.meeds.layout.service.LayoutAclService"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@page import="org.exoplatform.portal.webui.util.Util"%>
<%@page import="org.exoplatform.portal.mop.SiteKey"%>
<%
  SiteKey siteKey = Util.getUIPortal().getSiteKey();
  LayoutAclService aclService = ExoContainerContext.getService(LayoutAclService.class);
  boolean isAdministrator = aclService.isAdministrator(request.getRemoteUser());
  boolean canManageSiteNavigation = aclService.canEditNavigation(siteKey, request.getRemoteUser());
  if (canManageSiteNavigation) {
%>
<div class="VuetifyApp">
  <div id="siteNavigation">
    <script type="text/javascript">
      eXo.env.portal.isAdministrator = <%=isAdministrator%>;
      eXo.env.portal.canManageSiteNavigation = <%=canManageSiteNavigation%>;
      require(['PORTLET/layout/SiteNavigation'], app => app.init(<%=canManageSiteNavigation%>));
    </script>
  </div>
</div>
<% } %>
