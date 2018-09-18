package com.adobe.apx.sample;

import com.adobe.cq.social.commons.comments.api.Comment;
import com.adobe.cq.social.commons.comments.api.CommentSystemSocialComponentFactory;
import com.adobe.cq.social.commons.comments.listing.CommentSocialComponentListProviderManager;
import com.adobe.cq.social.scf.*;
import com.adobe.cq.social.ugcbase.SocialUtils;
import com.adobe.granite.xss.XSSAPI;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = SocialComponentFactory.class,
        name = "Huge Comment Social Component Factory"
)
public class HugeSocialComponentFactory extends CommentSystemSocialComponentFactory implements SocialComponentFactory {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private CommentSocialComponentListProviderManager commentSocialComponentListProviderManager;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    ClientUtilityFactory clientUtilFactory;

    @Reference
    private SocialUtils socialUtils;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    private XSSAPI xss;


    @Override
    public SocialComponent getSocialComponent(Resource resource) {
        log.debug("GSC1");
        try {
            log.debug("GSC1 isLive: " + resource.getResourceResolver().getUserID());
            HugeComment returnComment = new HugeComment(resource, getClientUtilities(resource.getResourceResolver()), commentSocialComponentListProviderManager);
            log.debug("GSC1 returning comment");
            return returnComment;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public SocialComponent getSocialComponent(Resource resource, SlingHttpServletRequest slingHttpServletRequest) {
        log.debug("GSC2 getSocialComponent");
        try {
            log.debug(getClientUtilities(slingHttpServletRequest).getAuthorizedUserId());
            HugeComment hugeComment = new HugeComment(resource, getClientUtilities(slingHttpServletRequest), commentSocialComponentListProviderManager);

            log.debug(hugeComment.getEmail());
            return hugeComment;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public SocialComponent getSocialComponent(Resource resource, ClientUtilities clientUtilities, QueryRequestInfo queryRequestInfo) {
        log.info("GSC3");
        try {
            return new HugeComment(resource, clientUtilities, commentSocialComponentListProviderManager);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getPriority() {
        log.info("PRIORITY");
        return 10;
    }

    protected ClientUtilities getClientUtilities(final SlingHttpServletRequest request) {
        return clientUtilFactory.getClientUtilities(xss, request, socialUtils);
    }

    protected ClientUtilities getClientUtilities(final ResourceResolver resourceResolver) {
        return clientUtilFactory.getClientUtilities(this.xss, resourceResolver, socialUtils);
    }

        public String getSupportedResourceType() {
        return Comment.COMMENT_RESOURCETYPE;
    }


}
