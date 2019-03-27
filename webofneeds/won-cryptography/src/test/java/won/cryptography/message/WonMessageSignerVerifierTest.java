package won.cryptography.message;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import won.cryptography.rdfsign.SignatureVerificationState;
import won.cryptography.service.keystore.FileBasedKeyStoreService;
import won.cryptography.utils.TestSigningUtils;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.impl.WonMessageSignerVerifier;
import won.protocol.util.RdfUtils;

/**
 * User: ypanchenko Date: 09.04.2015
 */
@Ignore
public class WonMessageSignerVerifierTest {
    private static final String RESOURCE_FILE_SIG = "/won-signed-messages/create-need-msg.trig";
    private static final String RESOURCE_OWNER_FILE_NOSIG = "/won-signed-messages/need-owner-msg-nosig.trig";
    private static final String RESOURCE_NODE_FILE_NOSIG = "/won-signed-messages/need-node-msg-nosig.trig";
    private static final String NEED_CORE_DATA_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data";
    private static final String NEED_CORE_DATA_SIG_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data-sig";
    private static final String EVENT_ENV1_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data";
    private static final String EVENT_ENV1_SIG_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data-sig";
    private static final String EVENT_ENV2_URI = "http://localhost:8080/won/resource/event/7719577021233193000#envelope-s7gl";
    private static final String EVENT_ENV2_SIG_URI = "http://localhost:8080/won/resource/event/7719577021233193000#envelope-s7gl-sig";
    Map<String, PublicKey> pubKeysMap = new HashMap<String, PublicKey>();
    private PrivateKey needKey;
    private PrivateKey ownerKey;
    private PrivateKey nodeKey;

    @Before
    public void init() throws Exception {
        // load public keys:
        Security.addProvider(new BouncyCastleProvider());
        File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
        FileBasedKeyStoreService storeService = new FileBasedKeyStoreService(keysFile, "temp");
        storeService.init();
        pubKeysMap.put(TestSigningUtils.needCertUri,
                        storeService.getCertificate(TestSigningUtils.needCertUri).getPublicKey());
        pubKeysMap.put(TestSigningUtils.ownerCertUri,
                        storeService.getCertificate(TestSigningUtils.ownerCertUri).getPublicKey());
        pubKeysMap.put(TestSigningUtils.nodeCertUri,
                        storeService.getCertificate(TestSigningUtils.nodeCertUri).getPublicKey());
        this.needKey = (ECPrivateKey) storeService.getPrivateKey(TestSigningUtils.needCertUri);
        // do we need owner key for some messages? e.g. when we send an error occurred
        // message not generated by owner client but by owner server?
        this.ownerKey = (ECPrivateKey) storeService.getPrivateKey(TestSigningUtils.ownerCertUri);
        this.nodeKey = (ECPrivateKey) storeService.getPrivateKey(TestSigningUtils.nodeCertUri);
    }

    @Test
    public void testVerify() throws Exception {
        // create signed dataset
        Dataset testDataset = TestSigningUtils.prepareTestDataset(RESOURCE_FILE_SIG);
        WonMessage testMsg = new WonMessage(testDataset);
        // verify
        SignatureVerificationState result = WonMessageSignerVerifier.verify(pubKeysMap, testMsg);
        Assert.assertTrue(result.getMessage(), result.isVerificationPassed());
        Assert.assertEquals(3, result.getSignatureGraphNames().size());
        Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));
        Assert.assertEquals(EVENT_ENV1_URI, result.getSignedGraphName(EVENT_ENV1_SIG_URI));
        Assert.assertEquals(EVENT_ENV2_URI, result.getSignedGraphName(EVENT_ENV2_SIG_URI));
    }

    @Test
    @Ignore
    public void signAndVerifyUnsignedMessage() throws Exception {
        // create signed dataset
        Dataset testDataset = TestSigningUtils.prepareTestDataset(RESOURCE_OWNER_FILE_NOSIG);
        WonMessage testMsg = new WonMessage(testDataset);
        // sign
        testMsg = WonMessageSignerVerifier.sign(needKey, pubKeysMap.get(TestSigningUtils.nodeCertUri),
                        TestSigningUtils.needCertUri, testMsg);
        // pretend msg was serialized and deserialized in between
        // pretend it was serialized and deserialized
        String datasetString = RdfUtils.writeDatasetToString(testMsg.getCompleteDataset(), Lang.TRIG);
        testMsg = new WonMessage(RdfUtils.readDatasetFromString(datasetString, Lang.TRIG));
        // verify
        SignatureVerificationState result = WonMessageSignerVerifier.verify(pubKeysMap, testMsg);
        Assert.assertTrue(result.isVerificationPassed());
        Assert.assertEquals(2, result.getSignatureGraphNames().size());
        Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));
        Assert.assertEquals(EVENT_ENV1_URI, result.getSignedGraphName(EVENT_ENV1_SIG_URI));
        // write for debugging
        TestSigningUtils.writeToTempFile(testMsg.getCompleteDataset());
    }

    @Test
    public void signAndVerifySignedMessageNode() throws Exception {
        // create signed dataset
        Dataset testDataset = TestSigningUtils.prepareTestDataset(RESOURCE_NODE_FILE_NOSIG);
        WonMessage testMsg = new WonMessage(testDataset);
        // sign
        testMsg = WonMessageSignerVerifier.sign(nodeKey, pubKeysMap.get(TestSigningUtils.nodeCertUri),
                        TestSigningUtils.nodeCertUri, testMsg);
        // pretend msg was serialized and deserialized in between
        // pretend it was serialized and deserialized
        String datasetString = RdfUtils.writeDatasetToString(testMsg.getCompleteDataset(), Lang.TRIG);
        testMsg = new WonMessage(RdfUtils.readDatasetFromString(datasetString, Lang.TRIG));
        // verify
        SignatureVerificationState result = WonMessageSignerVerifier.verify(pubKeysMap, testMsg);
        Assert.assertTrue(result.getMessage(), result.isVerificationPassed());
        Assert.assertEquals(3, result.getSignatureGraphNames().size());
        Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));
        Assert.assertEquals(EVENT_ENV1_URI, result.getSignedGraphName(EVENT_ENV1_SIG_URI));
        Assert.assertEquals(EVENT_ENV2_URI, result.getSignedGraphName(EVENT_ENV2_SIG_URI));
        // write for debugging
        TestSigningUtils.writeToTempFile(testMsg.getCompleteDataset());
    }
    // TODO test more versions of not valid signatures, e.g. signatures missing,
    // graphs missing,
    // wrong signature value, references signature values are wrong, etc.
}
