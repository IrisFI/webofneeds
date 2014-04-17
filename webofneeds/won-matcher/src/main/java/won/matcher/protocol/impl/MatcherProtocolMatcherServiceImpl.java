package won.matcher.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.matcher.protocol.MatcherProtocolMatcherService;
import won.matcher.protocol.MatcherProtocolMatcherServiceCallback;
import won.matcher.protocol.NopMatcherProtocolMatcherServiceCallback;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:12
 */
    //TODO: refactor service interfaces.
public class MatcherProtocolMatcherServiceImpl implements MatcherProtocolMatcherService {

  final Logger logger = LoggerFactory.getLogger(getClass());



  //handler for incoming won protocol messages. The default handler does nothing.
  @Autowired(required = false)
  private MatcherProtocolMatcherServiceCallback matcherServiceCallback = new NopMatcherProtocolMatcherServiceCallback();


  //TODO: refactor this to use DataAccessService

  @Override
  public void onNewNeed(URI needURI, Model content) {
      logger.debug("matcher from need: need created event for needURI {}",needURI);
      if (needURI == null) throw new IllegalArgumentException("needURI is not set");
      matcherServiceCallback.onNewNeed(needURI, content);
  }

  @Override
  public void onNeedActivated(final URI needURI) {
    logger.debug("matcher from need: need activated event for needURI {}", needURI);
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    matcherServiceCallback.onNeedActivated(needURI);
  }

  @Override
  public void onNeedDeactivated(final URI needURI) {
    logger.debug("matcher from need: need deactivated event for needURI {}", needURI);
    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    matcherServiceCallback.onNeedDeactivated(needURI);
  }


}
