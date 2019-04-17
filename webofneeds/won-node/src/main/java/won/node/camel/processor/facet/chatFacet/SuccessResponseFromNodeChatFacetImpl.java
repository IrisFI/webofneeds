package won.node.camel.processor.facet.chatFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.DefaultFacetMessageProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: quasarchimaere Date: 04.04.2019
 */
@Component
@DefaultFacetMessageProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_SUCCESS_RESPONSE_STRING)
@FacetMessageProcessor(facetType = WON.CHAT_FACET_STRING, direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_SUCCESS_RESPONSE_STRING)
public class SuccessResponseFromNodeChatFacetImpl extends AbstractCamelProcessor {
    @Override
    public void process(final Exchange exchange) {
        logger.debug("default facet implementation, not doing anything");
    }
}
