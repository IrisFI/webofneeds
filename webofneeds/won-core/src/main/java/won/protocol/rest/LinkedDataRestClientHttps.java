/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.rest;

import org.apache.http.conn.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.KeyStoreService;
import won.cryptography.service.TrustStoreService;
import won.cryptography.ssl.PrivateKeyStrategyGenerator;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 07.10.15
 */
public class LinkedDataRestClientHttps extends LinkedDataRestClient
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private RestTemplate restTemplateWithDefaultWebId;
  private HttpMessageConverter datasetConverter;
  String acceptHeaderValue = null;

  private Integer readTimeout;
  private Integer connectionTimeout;

  private PrivateKeyStrategyGenerator privateKeyStrategyGenerator;
  private KeyStoreService keyStoreService;
  private TrustStoreService trustStoreService;
  private TrustStrategy trustStrategy;



  public LinkedDataRestClientHttps(KeyStoreService keyStoreService, PrivateKeyStrategyGenerator
    privateKeyStrategyGenerator, TrustStoreService trustStoreService, TrustStrategy trustStrategy) {
    this.readTimeout = 20000;
    this.connectionTimeout = 20000; //DEF. TIMEOUT IS 20 sec
    this.keyStoreService = keyStoreService;
    this.privateKeyStrategyGenerator = privateKeyStrategyGenerator;
    this.trustStoreService = trustStoreService;
    this.trustStrategy = trustStrategy;
  }

  @PostConstruct
  public void initialize() {
    datasetConverter = new RdfDatasetConverter();
    HttpHeaders headers = new HttpHeaders();
    this.acceptHeaderValue = MediaType.toString(datasetConverter.getSupportedMediaTypes());
    try {
      restTemplateWithDefaultWebId = createRestTemplateForReadingLinkedData(this.keyStoreService
        .getDefaultAlias());
    } catch (Exception e) {
      logger.error("Failed to create ssl tofu rest template", e);
      throw new RuntimeException(e);
    }
  }

  private RestTemplate createRestTemplateForReadingLinkedData(String webID){
    RestTemplate template = null;
    try {
      template = CryptographyUtils.createSslRestTemplate(
        this.keyStoreService.getUnderlyingKeyStore(),
        this.keyStoreService.getPassword(),
        privateKeyStrategyGenerator.createPrivateKeyStrategy(webID),
        this.trustStoreService.getUnderlyingKeyStore(),
        this.trustStrategy,
        readTimeout, connectionTimeout);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create rest template for webID '" + webID +"'", e);
    }
    template.getMessageConverters().add(datasetConverter);
    return template;
  }

  @Override
  public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI, final URI requesterWebID) {

    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    RestTemplate restTemplate;
    try {
      restTemplate = getRestTemplateForReadingLinkedData(requesterWebID.toString());
    } catch (Exception e) {
      logger.error("Failed to create ssl tofu rest template", e);
      throw new RuntimeException(e);
    }
    restTemplate.getMessageConverters().add(datasetConverter);
    Map<String, String> requestHeaders = new HashMap<String, String>();
    requestHeaders.put(HttpHeaders.ACCEPT, this.acceptHeaderValue);
    return super.readResourceData(resourceURI, restTemplate, requestHeaders);
  }


  private RestTemplate getRestTemplateForReadingLinkedData(String webID) {

    if (webID.equals(keyStoreService.getDefaultAlias())) {
      return restTemplateWithDefaultWebId;
    }
    return createRestTemplateForReadingLinkedData(webID);
  }

  @Override
  public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI) {
    Map<String, String> requestHeaders = new HashMap<String, String>();
    requestHeaders.put(HttpHeaders.ACCEPT, this.acceptHeaderValue);
    return super.readResourceData(resourceURI, restTemplateWithDefaultWebId, requestHeaders);
  }

  @Override
  public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI, final
                                                                Map<String, String> requestHeaders) {
    requestHeaders.put(HttpHeaders.ACCEPT, this.acceptHeaderValue);
    return super.readResourceData(resourceURI, restTemplateWithDefaultWebId, requestHeaders);
  }

  @Override
  public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI, final URI requesterWebID, final
  Map<String, String> requestHeaders) {
    requestHeaders.put(HttpHeaders.ACCEPT, this.acceptHeaderValue);
    return super.readResourceData(resourceURI, getRestTemplateForReadingLinkedData(requesterWebID.toString()),
                                  requestHeaders);
  }

  public void setReadTimeout(final Integer readTimeout) {
    this.readTimeout = readTimeout;
  }

  public void setConnectionTimeout(final Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }
}
