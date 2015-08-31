/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.rest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.biz.TagDBStore;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.store.TagValidator;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServiceTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;

import java.util.List;

@Path(TagRESTConstants.TAGDEF_NAME_AND_VERSION)
@Component
@Scope("request")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TagREST {

    private static final Log LOG = LogFactory.getLog(TagREST.class);

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
	ServiceDBStore svcStore;

	@Autowired
	TagDBStore tagStore;

    TagValidator validator;

    public TagREST() {
	}

	@PostConstruct
	public void initStore() {
		validator = new TagValidator();

        tagStore.setServiceStore(svcStore);
        validator.setTagStore(tagStore);
	}

    @POST
    @Path(TagRESTConstants.TAGDEFS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef createTagDef(RangerTagDef tagDef) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTagDef(" + tagDef + ")");
        }

        RangerTagDef ret;

        try {
            ret = tagStore.createTagDef(tagDef);
        } catch(Exception excp) {
            LOG.error("createTagDef(" + tagDef + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.createTagDef(" + tagDef + "): " + ret);
        }

        return ret;
    }

    @PUT
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef updateTagDef(@PathParam("id") Long id, RangerTagDef tagDef) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateTagDef(" + id + ")");
        }
        if (tagDef.getId() == null) {
            tagDef.setId(id);
        } else if (!tagDef.getId().equals(id)) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST , "tag name mismatch", true);
        }

        RangerTagDef ret;

        try {
            ret = tagStore.updateTagDef(tagDef);
        } catch (Exception excp) {
            LOG.error("updateTagDef(" + id + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateTagDef(" + id + ")");
        }

        return ret;
    }

    @DELETE
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagDef(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagDef(" + id + ")");
        }

        try {
            tagStore.deleteTagDef(id);
        } catch(Exception excp) {
            LOG.error("deleteTagDef(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagDef(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagDefByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagDefByGuid(" + guid + ")");
        }

        try {
            RangerTagDef exist = tagStore.getTagDefByGuid(guid);
            tagStore.deleteTagDef(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteTagDef(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagDefByGuid(" + guid + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef getTagDef(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagDef(" + id + ")");
        }

        RangerTagDef ret;

        try {
            ret = tagStore.getTagDef(id);
        } catch(Exception excp) {
            LOG.error("getTagDef(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagDef(" + id + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef getTagDefByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagDefByGuid(" + guid + ")");
        }

        RangerTagDef ret;

        try {
            ret = tagStore.getTagDefByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getTagDefByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagDefByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGDEF_RESOURCE + "name/{name}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagDef getTagDefByName(@PathParam("name") String name) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagDefByName(" + name + ")");
        }

        RangerTagDef ret;

        try {
            ret = tagStore.getTagDefByName(name);
        } catch(Exception excp) {
            LOG.error("getTagDefByName(" + name + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagDefByName(" + name + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGDEFS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTagDef> getAllTagDefs() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllTagDefs()");
        }

        List<RangerTagDef> ret;

        try {
            ret = tagStore.getTagDefs(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllTagDefs() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(ret == null) {
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, "Not found", true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllTagDefs()");
        }

        return ret;
    }

    @POST
    @Path(TagRESTConstants.TAGS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag createTag(RangerTag tag) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTag(" + tag + ")");
        }

        RangerTag ret;

        try {
            validator.preCreateTag(tag);
            ret = tagStore.createTag(tag);
        } catch(Exception excp) {
            LOG.error("createTag(" + tag + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.createTag(" + tag + "): " + ret);
        }

        return ret;
    }

    @PUT
    @Path(TagRESTConstants.TAG_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag updateTag(@PathParam("id") Long id, RangerTag tag) {

        RangerTag ret;

        try {
            validator.preUpdateTag(id, tag);
            ret = tagStore.updateTag(tag);
        } catch (Exception excp) {
            LOG.error("updateTag(" + id + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateTag(" + id + "): " + ret);
        }

        return ret;
    }

    @PUT
    @Path(TagRESTConstants.TAG_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag updateTagByGuid(@PathParam("guid") String guid, RangerTag tag) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateTagByGuid(" + guid + ")");
        }

        RangerTag ret;

        try {
            validator.preUpdateTagByGuid(guid, tag);
            ret = tagStore.updateTag(tag);
        } catch (Exception excp) {
            LOG.error("updateTagByGuid(" + guid + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateTagByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @DELETE
    @Path(TagRESTConstants.TAG_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTag(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTag(" + id +")");
        }

        try {
            validator.preDeleteTag(id);
            tagStore.deleteTag(id);
        } catch(Exception excp) {
            LOG.error("deleteTag(" + id + ") failed", excp);
            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTag(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAG_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagByGuid(" + guid + ")");
        }

        try {
            RangerTag exist = validator.preDeleteTagByGuid(guid);
            tagStore.deleteTag(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteTagByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagByGuid(" + guid + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.TAG_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag getTag(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTag(" + id + ")");
        }
        RangerTag ret;

        try {
            ret = tagStore.getTag(id);
        } catch(Exception excp) {
            LOG.error("getTag(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTag(" + id + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAG_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTag getTagByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagByGuid(" + guid + ")");
        }
        RangerTag ret;

        try {
            ret = tagStore.getTagByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getTagByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGS_RESOURCE + "type/{type}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTag> getTagsByType(@PathParam("type") String type) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagsByType(" + type + ")");
        }
        List<RangerTag> ret;

        try {
            ret = tagStore.getTagsByType(type);
        } catch(Exception excp) {
            LOG.error("getTagsByType(" + type + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagsByType(" + type + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTag> getAllTags() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllTags()");
        }

        List<RangerTag> ret;

        try {
            ret = tagStore.getTags(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllTags() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if (CollectionUtils.isEmpty(ret)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getAllTags() - No tags found");
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllTags(): " + ret);
        }

        return ret;
    }

    @POST
    @Path(TagRESTConstants.RESOURCES_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource createServiceResource(RangerServiceResource resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createServiceResource(" + resource + ")");
        }

        RangerServiceResource ret;

        try {
            validator.preCreateServiceResource(resource);
            ret = tagStore.createServiceResource(resource);
        } catch(Exception excp) {
            LOG.error("createServiceResource(" + resource + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.createServiceResource(" + resource + "): " + ret);
        }

        return ret;
    }

    @PUT
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource updateServiceResource(@PathParam("id") Long id, RangerServiceResource resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateServiceResource(" + id + ")");
        }
        RangerServiceResource ret;

        try {
            validator.preUpdateServiceResource(id, resource);
            ret = tagStore.updateServiceResource(resource);
        } catch(Exception excp) {
            LOG.error("updateServiceResource(" + resource + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateServiceResource(" + id + "): " + ret);
        }
        return ret;
    }

    @PUT
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource updateServiceResourceByGuid(@PathParam("guid") String guid, RangerServiceResource resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.updateServiceResourceByGuid(" + guid + ", " + resource + ")");
        }
        RangerServiceResource ret;
        try {
            validator.preUpdateServiceResourceByGuid(guid, resource);
            ret = tagStore.updateServiceResource(resource);
        } catch(Exception excp) {
            LOG.error("updateServiceResourceByGuid(" + guid + ", " + resource + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.updateServiceResourceByGuid(" + guid + ", " + resource + "): " + ret);
        }
        return ret;
    }

    @DELETE
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteServiceResource(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteServiceResource(" + id + ")");
        }
        try {
            validator.preDeleteServiceResource(id);
            tagStore.deleteServiceResource(id);
        } catch (Exception excp) {
            LOG.error("deleteServiceResource() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteServiceResource(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteServiceResourceByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteServiceResourceByGuid(" + guid + ")");
        }

        try {
            RangerServiceResource exist = validator.preDeleteServiceResourceByGuid(guid);
            tagStore.deleteServiceResource(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteServiceResourceByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteServiceResourceByGuid(" + guid + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource getServiceResource(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResource(" + id + ")");
        }
        RangerServiceResource ret;
        try {
            ret = tagStore.getServiceResource(id);
        } catch(Exception excp) {
            LOG.error("getServiceResource(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResource(" + id + "): " + ret);
        }
        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource getServiceResourceByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResourceByGuid(" + guid + ")");
        }
        RangerServiceResource ret;
        try {
            ret = tagStore.getServiceResourceByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getServiceResourceByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResourceByGuid(" + guid + "): " + ret);
        }
        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCES_RESOURCE + "service/{serviceName}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerServiceResource> getServiceResourcesByService(@PathParam("serviceName") String serviceName) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResourcesByService(" + serviceName + ")");
        }

        List<RangerServiceResource> ret = null;

        try {
            ret = tagStore.getServiceResourcesByService(serviceName);
        } catch(Exception excp) {
            LOG.error("getServiceResourcesByService(" + serviceName + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if (CollectionUtils.isEmpty(ret)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getServiceResourcesByService(" + serviceName + ") - No service-resources found");
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResourcesByService(" + serviceName + "): count=" + (ret == null ? 0 : ret.size()));
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCE_RESOURCE + "signature/{resourceSignature}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerServiceResource getServiceResourceByResourceSignature(@PathParam("resourceSignature") String resourceSignature) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceResourceByResourceSignature(" + resourceSignature + ")");
        }

        RangerServiceResource ret = null;

        try {
            ret = tagStore.getServiceResourceByResourceSignature(resourceSignature);
        } catch(Exception excp) {
            LOG.error("getServiceResourceByResourceSignature(" + resourceSignature + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getServiceResourceByResourceSignature(" + resourceSignature + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.RESOURCES_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerServiceResource> getAllServiceResources() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllServiceResources()");
        }

        List<RangerServiceResource> ret;

        try {
            ret = tagStore.getServiceResources(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllServiceResources() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllServiceResources(): count=" + (ret == null ? 0 : ret.size()));
        }

        return ret;
    }

    @POST
    @Path(TagRESTConstants.TAGRESOURCEMAPS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap createTagResourceMap(@QueryParam("tag-guid") String tagGuid, @QueryParam("resource-guid") String resourceGuid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTagResourceMap(" + tagGuid + ", " +  resourceGuid + ")");
        }

        RangerTagResourceMap tagResourceMap;

        try {
            tagResourceMap = validator.preCreateTagResourceMap(tagGuid, resourceGuid);
            tagResourceMap = tagStore.createTagResourceMap(tagResourceMap);
        } catch(Exception excp) {
            LOG.error("createTagResourceMap(" + tagGuid + ", " +  resourceGuid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.createTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }

        return tagResourceMap;
    }

    @DELETE
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagResourceMap(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMap(" + id + ")");
        }
        try {
            validator.preDeleteTagResourceMap(id);
            tagStore.deleteTagResourceMap(id);
        } catch (Exception excp) {
            LOG.error("deleteTagResourceMap() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagResourceMap(" + id + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagResourceMapByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMapByGuid(" + guid + ")");
        }

        try {
            RangerTagResourceMap exist = validator.preDeleteTagResourceMapByGuid(guid);
            tagStore.deleteServiceResource(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteTagResourceMapByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.deleteTagResourceMapByGuid(" + guid + ")");
        }
    }

    @DELETE
    @Path(TagRESTConstants.TAGRESOURCEMAPS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public void deleteTagResourceMap(@QueryParam("tag-guid") String tagGuid, @QueryParam("resource-guid") String resourceGuid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }

        try {
            RangerTagResourceMap exist = validator.preDeleteTagResourceMap(tagGuid, resourceGuid);
            tagStore.deleteTagResourceMap(exist.getId());
        } catch(Exception excp) {
            LOG.error("deleteTagResourceMap(" + tagGuid + ", " +  resourceGuid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.deleteTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "{id}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap getTagResourceMap(@PathParam("id") Long id) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMap(" + id + ")");
        }
        RangerTagResourceMap ret;

        try {
            ret = tagStore.getTagResourceMap(id);
        } catch(Exception excp) {
            LOG.error("getTagResourceMap(" + id + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagResourceMap(" + id + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "guid/{guid}")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap getTagResourceMapByGuid(@PathParam("guid") String guid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMapByGuid(" + guid + ")");
        }
        RangerTagResourceMap ret;

        try {
            ret = tagStore.getTagResourceMapByGuid(guid);
        } catch(Exception excp) {
            LOG.error("getTagResourceMapByGuid(" + guid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagResourceMapByGuid(" + guid + "): " + ret);
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAP_RESOURCE + "tag-resource-guid")
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public RangerTagResourceMap getTagResourceMap(@QueryParam("tagGuid") String tagGuid, @QueryParam("resourceGuid") String resourceGuid) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }
        
        RangerTagResourceMap ret = null;

        try {
            ret = tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);
        } catch(Exception excp) {
            LOG.error("getTagResourceMap(" + tagGuid + ", " +  resourceGuid + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagResourceMap(" + tagGuid + ", " + resourceGuid + ")");
        }

        return ret;
    }

    @GET
    @Path(TagRESTConstants.TAGRESOURCEMAPS_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<RangerTagResourceMap> getAllTagResourceMaps() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getAllTagResourceMaps()");
        }

        List<RangerTagResourceMap> ret;

        try {
            ret = tagStore.getTagResourceMaps(new SearchFilter());
        } catch(Exception excp) {
            LOG.error("getAllTagResourceMaps() failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if (CollectionUtils.isEmpty(ret)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getAllTagResourceMaps() - No tag-resource-maps found");
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getAllTagResourceMaps(): " + ret);
        }

        return ret;
    }


    // This API is typically used by plug-in to get selected tagged resources from RangerAdmin
    @GET
    @Path(TagRESTConstants.TAGS_DOWNLOAD + "{serviceName}")
    @Produces({ "application/json", "application/xml" })
    public ServiceTags getServiceTagsIfUpdated(@PathParam("serviceName") String serviceName,
                                                   @QueryParam(TagRESTConstants.LAST_KNOWN_TAG_VERSION_PARAM) Long lastKnownVersion, @QueryParam("pluginId") String pluginId) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + pluginId + ")");
        }

        ServiceTags ret = null;

        try {
            ret = tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion);
        } catch(Exception excp) {
            LOG.error("getServiceTagsIfUpdated(" + serviceName + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<==> TagREST.getServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + pluginId + ")");
        }

        return ret;
    }

    // This API is typically used by GUI to get all available tags from RangerAdmin

    @GET
    @Path(TagRESTConstants.TAGTYPES_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    @PreAuthorize("hasRole('ROLE_SYS_ADMIN')")
    public List<String> getTagTypes(@QueryParam(TagRESTConstants.SERVICE_NAME_PARAM) String serviceName) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.getTagTypes(" + serviceName + ")");
        }
        List<String> tagTypes = null;

        try {
            tagTypes = tagStore.getTagTypes(serviceName);
        } catch(Exception excp) {
            LOG.error("getTags(" + serviceName + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.getTagTypes(" + serviceName + ")");
        }
        return tagTypes;
    }

    // This API is typically used by GUI to help lookup available tags from RangerAdmin to help tag-policy writer. It
    // may also be used to validate configuration parameters of a tag-service

    @GET
    @Path(TagRESTConstants.TAGTYPES_LOOKUP_RESOURCE)
    @Produces({ "application/json", "application/xml" })
    public List<String> lookupTagTypes(@QueryParam(TagRESTConstants.SERVICE_NAME_PARAM) String serviceName,
                                       @DefaultValue(".*") @QueryParam(TagRESTConstants.PATTERN_PARAM) String pattern) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> TagREST.lookupTagTypes(" + serviceName  + ", " + pattern + ")");
        }
        List<String> matchingTagTypes = null;

        try {
            matchingTagTypes = tagStore.lookupTagTypes(serviceName, pattern);
        } catch(Exception excp) {
            LOG.error("lookupTags(" + serviceName + ") failed", excp);

            throw restErrorUtil.createRESTException(HttpServletResponse.SC_BAD_REQUEST, excp.getMessage(), true);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== TagREST.lookupTagTypes(" + serviceName + ")");
        }
        return matchingTagTypes;
    }

}