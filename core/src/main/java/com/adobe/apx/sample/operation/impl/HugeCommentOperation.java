package com.adobe.apx.sample.operation.impl;

import com.adobe.cq.social.commons.CollabUtil;
import com.adobe.cq.social.commons.CommentException;
import com.adobe.cq.social.commons.annotation.Endpoint;
import com.adobe.cq.social.commons.annotation.Parameter;
import com.adobe.cq.social.commons.annotation.Parameters;
import com.adobe.cq.social.commons.comments.api.Comment;
import com.adobe.cq.social.commons.comments.endpoints.AbstractCommentOperation;
import com.adobe.cq.social.commons.comments.endpoints.AbstractCommentOperationService;
import com.adobe.cq.social.commons.comments.endpoints.CommentOperations;
import com.adobe.cq.social.scf.*;
import com.adobe.granite.security.user.UserProperties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

@Component (
        immediate = true,
        service = PostOperation.class,
        property = {
                "sling.post.operation=social:createComment"
        }
)
@Endpoint(
        name = "CommentPostOperation",
        resourceType = Comment.COMMENT_RESOURCETYPE,
        description = "Create a new Comment.  The user needs to have moderation permission or owns the specified resource.",
        example = "curl -X POST  -H \"Accept:application/json\" -d \"message=&id=nobot&:operation=social:createComment\" "
                + "-uaparker@geometrixx.info:aparker http:hostname:port/path/to/commentsystem.social.json")
@Parameters({
        @Parameter(name = SlingPostConstants.RP_OPERATION, value = HugeCommentOperation.CREATE_COMMENT_OPERATION,
                required = true),
        @Parameter(name = com.adobe.cq.social.commons.Comment.PARAM_BOTCHECK,
                value = com.adobe.cq.social.commons.Comment.VALUE_BOTCHECK, required = true),
        @Parameter(name = AbstractCommentOperationService.TAGS_PROPERTY, value = "Array of tag ids", required = true),
        @Parameter(name = AbstractCommentOperationService.CHARSET_PROPERTY,
                value = "UTF-8 or equivalent to support double byte characters", required = false),
        @Parameter(name = AbstractCommentOperationService.PROP_MESSAGE, value = "the new comment value.", required = true)})
public class HugeCommentOperation extends AbstractCommentOperation<CommentOperations> implements PostOperation {

    private static final Logger LOG = LoggerFactory.getLogger(HugeCommentOperation.class);

    @Reference( target = "(type=huge)" )
    private CommentOperations commentOperations; //this service is what we need to override

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SocialComponentFactoryManager componentFactoryManager;

    public static final String CREATE_COMMENT_OPERATION = "social:createComment";

    protected SocialOperationResult performOperation(final SlingHttpServletRequest request, final Session session)
            throws OperationException {

        LOG.debug("performing operations...");
        try {
            //use a service user here - using a admin rr is a bad idea
            ResourceResolver adminRR = resolverFactory.getAdministrativeResourceResolver(null);
            final Resource comment = getCommentOperationService().create(request, adminRR.adaptTo(Session.class));
            LOG.debug("comment name: " + comment.getName());

            //comment is good here

            LOG.debug("PerformOperation (request, session): " + request.getPathInfo()); //request is good

            LOG.debug(getSocialComponentForComment(comment, request).getResource().getPath()); //this kicks a null out

            SocialOperationResult operationResult = new SocialOperationResult(getSocialComponentForComment(comment, request), "created",
                    HttpServletResponse.SC_CREATED, comment.getPath());

            LOG.info("GET URL: " + operationResult.getResource().getUrl());
            adminRR.close();
            return operationResult;
        } catch (LoginException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    protected CommentOperations getCommentOperationService() {
        LOG.info(this.commentOperations.toString());
        return this.commentOperations;
    }

    protected String getUserIdFromRequest(final SlingHttpServletRequest request, final String defaultValue) {
        String userIdentifier;
        final UserProperties up = request.getResourceResolver().adaptTo(UserProperties.class);
        userIdentifier = (up == null) ? null : up.getAuthorizableID();
        if (userIdentifier == null) {
            userIdentifier = defaultValue;
        }
        return userIdentifier;
    }

    protected SocialComponent getSocialComponentForComment(final Resource comment,
                                                           final SlingHttpServletRequest request) {
        // resolving the resource again using the request session
        final Resource resource = request.getResourceResolver().getResource(comment.getPath());
        LOG.debug("getSocialComponentForComment resource, request");
        final SocialComponentFactory factory = componentFactoryManager.getSocialComponentFactory(resource);

        LOG.debug("factory name: " + factory.getClass().getSimpleName());
        return (factory != null) ? factory.getSocialComponent(resource, request) : null;
    }

    protected SocialOperationResult performOperation(final SlingHttpServletRequest request) throws OperationException {
        Session session;
        Session serviceSession = null;
        LOG.debug("performOperation (request): " + request.getRequestURI());
        if (CollabUtil.hasModeratePermissions(request.getResource())) {
            session = getSessionFromResource(request.getResource());
        } else {
            serviceSession = createAdminSession();
            session = serviceSession;
        }

        if (null == session) {
            throw new CommentException("session unavailable");
        }
        SocialOperationResult result;
        try {
            result = performOperation(request, session);
            return result;
        } finally {
            closeAdminSession(serviceSession);
        }
    }

}
