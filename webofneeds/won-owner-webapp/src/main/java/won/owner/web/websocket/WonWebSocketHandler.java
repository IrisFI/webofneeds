/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.web.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import won.protocol.message.WonMessageDecoder;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;

import java.io.IOException;

/**
 * User: syim
 * Date: 06.08.14
 */
public class WonWebSocketHandler extends TextWebSocketHandler
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private WonMessageDecoder wonMessageDecoder;


  @Autowired
  @Qualifier("default")
  private OwnerProtocolNeedServiceClientSide ownerService;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    logger.debug(message.getPayload());
    //WonMessage incomingMessage = wonMessageDecoder.decode(Lang.JSONLD,message.getPayload());
    //ownerService.processMessage(incomingMessage);
    WebSocketMessage<String> wonMessage = new TextMessage("from node");

    session.sendMessage(wonMessage);
  }
}
