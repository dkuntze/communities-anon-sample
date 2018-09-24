package com.adobe.apx.sample.operation.impl;

import com.adobe.cq.social.commons.CommentException;
import com.adobe.cq.social.commons.CommentSystem;
import com.adobe.cq.social.commons.bundleactivator.Activator;
import com.adobe.cq.social.commons.comments.api.Comment;
import com.adobe.cq.social.commons.comments.endpoints.AbstractCommentOperationService;
import com.adobe.cq.social.commons.comments.endpoints.CommentOperationExtension;
import com.adobe.cq.social.commons.comments.endpoints.CommentOperations;
import com.adobe.cq.social.commons.events.CommentEvent;
import com.adobe.cq.social.commons.ugclimiter.api.UGCLimiterService;
import com.adobe.cq.social.scf.OperationException;
import com.adobe.cq.social.scf.SocialComponent;
import com.adobe.cq.social.scf.SocialComponentFactory;
import com.adobe.cq.social.scf.SocialComponentFactoryManager;
import com.adobe.cq.social.ugcbase.SocialUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataSource;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
        service = CommentOperations.class,
        name = "Custom Comment OPS Service",
        property = {
                "type=huge",
                "attachmentTypeBlacklist=DEFAULT",
                AbstractCommentOperationService.PROPERTY_FIELD_WHITELIST + "=fname",
                AbstractCommentOperationService.PROPERTY_FIELD_WHITELIST + "=tags",
        }
)
public class HugeCommentOperationService extends AbstractCommentOperationService<CommentOperationExtension, CommentOperationExtension.CommentOperation, Comment>
        implements CommentOperations {

    private static final Logger LOG = LoggerFactory.getLogger(HugeCommentOperationService.class);

    @Reference
    private SocialComponentFactoryManager factoryManager;

    @Reference
    private SocialUtils socialUtils;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private static final String ANONYMOUS = "anonymous";

    protected Comment getSocialComponentForResource(final Resource resource) {
        if (resource == null) {
            return null;
        }
        final SocialComponentFactory factory = this.factoryManager.getSocialComponentFactory(resource);
        final SocialComponent component = factory.getSocialComponent(resource);
        if (component instanceof Comment) {
            return (Comment) component;
        } else {
            return null;
        }
    }

    protected void postCreateEvent(final com.adobe.cq.social.commons.comments.api.Comment comment, final String userId) {

        LOG.info("something is null here: " + userId);
        try {
            postEvent(new CommentEvent(comment, userId, comment.isTopLevel() ? CommentEvent.CommentActions.CREATED
                    : CommentEvent.CommentActions.REPLIED));
        } catch (NullPointerException npe) {
            LOG.error(npe.getMessage());
        }

    }

    protected boolean mayPost(final SlingHttpServletRequest request, final CommentSystem cs, final String userId) {
        LOG.info("mayPost = true");
        return true;
    }

    // again - bad idea to create admin RR here. Use a svc user
    protected ResourceResolver getResourceResolver(final Session session) throws LoginException {
        //final Map<String, Object> authInfo = new HashMap<String, Object>();
        //authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
        ResourceResolver adminRR = resolverFactory.getAdministrativeResourceResolver(null);
        return adminRR;
    }

    @Override
    protected void postUpdateEvent(Comment comment, String userId) {
        postEvent(new CommentEvent(comment, userId, CommentEvent.CommentActions.EDITED));
    }

    @Override
    protected void postChangeStateEvent(Comment comment, String authorId) {
        postEvent(new CommentEvent(comment, authorId, CommentEvent.CommentActions.CHANGESTATE));
    }

    @Override
    protected void postDeleteEvent(Comment comment, String userId) {
        postEvent(new CommentEvent(comment, userId, CommentEvent.CommentActions.DELETED));
    }

    @Override
    protected CommentOperationExtension.CommentOperation getCreateOperation() {
        return CommentOperationExtension.CommentOperation.CREATE;
    }

    @Override
    protected CommentOperationExtension.CommentOperation getDeleteOperation() {
        return CommentOperationExtension.CommentOperation.DELETE;
    }

    @Override
    protected CommentOperationExtension.CommentOperation getUpdateOperation() {
        return CommentOperationExtension.CommentOperation.UPDATE;
    }

    @Override
    protected CommentOperationExtension.CommentOperation getUploadImageOperation() {
        return CommentOperationExtension.CommentOperation.UPLOADIMAGE;
    }

    @Override
    protected CommentOperationExtension.CommentOperation getChangeStateOperation() {
        return CommentOperationExtension.CommentOperation.CHANGESTATE;
    }

    protected String getResourceType(Resource resource) {
        return com.adobe.cq.social.commons.comments.api.Comment.COMMENT_RESOURCETYPE;
    }

    public Resource create(final SlingHttpServletRequest request, final Session session) throws OperationException {
        final Resource resource = request.getResource();

        final CommentSystem cs = getCommentSystem(resource, session);

        validateParameters(request);

        final String name = "admin"; //set this to something other than anonymous to post
        if (name.equals(ANONYMOUS)) {
            throw new OperationException("Session is invalid",
                    HttpServletResponse.SC_UNAUTHORIZED);
        }

        if (ugcLimiterService == null) {
            // this throws IllegalStateException if reference can't be found for the service
            ugcLimiterService = Activator.getService(UGCLimiterService.class);
        }

        if (!ugcLimiterService.mayCreateUGC(request.getResourceResolver(), name)) {
            // Dont change the exception message. This exception is used in UI and it may stop working
            // Refer to CQ-97142
            throw new OperationException("Exceeded contribution limit of " + ugcLimiterService.getUgcLimit()
                    + " pieces of content per 500. " + 500,
                    HttpServletResponse.SC_FORBIDDEN);
        }

        if (!mayPost(request, cs, name)) {
            throw new OperationException("User not allowed to post to forum at " + cs.getPath(),
                    HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        final Map<String, Object> props = new HashMap<>();
        try {
            getDefaultProperties(request, props, session);
            getCustomProperties(request, props, session);
        } catch (final CommentException e) {
            throw new OperationException("Failed to create comment", e,
                    HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
        } catch (final RepositoryException e) {
            throw new OperationException("Failed to get comment properties", e,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        // create comment
        final List<DataSource> attachments = getAttachmentsFromRequest(request, cs);
        return create(resource, cs, name, props, attachments, session);

    }


}
