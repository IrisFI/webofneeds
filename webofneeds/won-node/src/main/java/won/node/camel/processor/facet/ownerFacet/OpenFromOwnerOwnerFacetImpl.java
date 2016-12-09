package won.node.camel.processor.facet.ownerFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.DefaultFacetMessageProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component
@DefaultFacetMessageProcessor(direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_OPEN_STRING)
@FacetMessageProcessor(facetType = WON.OWNER_FACET_STRING,direction=WONMSG.TYPE_FROM_OWNER_STRING,messageType =
  WONMSG.TYPE_OPEN_STRING)
public class OpenFromOwnerOwnerFacetImpl extends AbstractFromOwnerCamelProcessor
{
  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public void process(final Exchange exchange) {
    logger.debug("default facet implementation, not doing anything");
  }
}
