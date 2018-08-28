package com.adobe.apx.sample;

import com.adobe.cq.social.commons.comments.api.Comment;
import com.adobe.cq.social.commons.comments.api.CommentSystemSocialComponentFactory;
import com.adobe.cq.social.commons.comments.listing.CommentSocialComponentListProviderManager;
import com.adobe.cq.social.scf.ClientUtilities;
import com.adobe.cq.social.scf.QueryRequestInfo;
import com.adobe.cq.social.scf.SocialComponent;
import com.adobe.cq.social.scf.SocialComponentFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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


    @Override
    public SocialComponent getSocialComponent(Resource resource) {
        log.info("GSC1");
        try {
            log.info("isLive: " + resource.getResourceResolver().getUserID());
            return new HugeComment(resource, getClientUtilities(resource.getResourceResolver()), commentSocialComponentListProviderManager);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public SocialComponent getSocialComponent(Resource resource, SlingHttpServletRequest slingHttpServletRequest) {
        log.info("GSC2");
        try {
            return new HugeComment(resource, getClientUtilities(slingHttpServletRequest), commentSocialComponentListProviderManager);
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

    public String getSupportedResourceType() {
        return Comment.COMMENT_RESOURCETYPE;
    }


}
