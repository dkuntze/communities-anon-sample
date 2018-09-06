package com.adobe.apx.sample;

import com.adobe.cq.social.commons.comments.api.AbstractComment;
import com.adobe.cq.social.commons.comments.api.Comment;
import com.adobe.cq.social.commons.comments.api.CommentCollectionConfiguration;
import com.adobe.cq.social.commons.comments.listing.CommentSocialComponentListProviderManager;
import com.adobe.cq.social.scf.ClientUtilities;
import com.adobe.cq.social.scf.QueryRequestInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;


public class HugeComment extends AbstractComment<CommentCollectionConfiguration> implements Comment<CommentCollectionConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(HugeComment.class);


    public HugeComment(final Resource resource, final ClientUtilities clientUtils,
                                 final CommentSocialComponentListProviderManager listProviderManager) throws RepositoryException {
        super(resource, clientUtils, QueryRequestInfo.DEFAULT_QUERY_INFO_FACTORY.create(), listProviderManager);
    }

    public String getEmail() {
        ValueMap map = resource.adaptTo(ValueMap.class);
        return clientUtils.filterHTML(map.get("email").toString());
    }
}
