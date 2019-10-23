package won.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import won.node.service.linkeddata.lookup.SocketLookupFromLinkedData;
import won.node.service.persistence.AtomService;
import won.node.service.persistence.ConnectionService;
import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Atom;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.vocabulary.WONMSG;

@ContextConfiguration(locations = { "classpath:/won/node/common-test-context.xml",
                "classpath:/spring/component/storage/jdbc-storage.xml",
                "classpath:/spring/component/storage/jpabased-rdf-storage.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource
@Transactional
public class PersistenceTest {
    @Autowired
    AtomRepository atomRepository;
    @Autowired
    AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    AtomService atomService;
    @Autowired
    MessageService messageService;
    @Autowired
    ConnectionService connectionService;
    @MockBean
    SocketLookupFromLinkedData socketLookup;
    @MockBean
    WonNodeInformationService wonNodeInformationService;

    @Test(expected = DataIntegrityViolationException.class)
    public void test_Atom_missing_message_container() {
        Atom atom = new Atom();
        atom.setAtomURI(URI.create("uri:atom"));
        atom.setCreationDate(new Date());
        atomRepository.save(atom);
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void test_Atom_missing_state() {
        Atom atom = new Atom();
        URI atomUri = URI.create("uri:atom");
        atom.setAtomURI(atomUri);
        atom.setCreationDate(new Date());
        AtomMessageContainer mc = new AtomMessageContainer(atom, atom.getAtomURI());
        atomMessageContainerRepository.save(mc);
        mc.setParentUri(atomUri);
        atom.setMessageContainer(mc);
        atomRepository.save(atom);
    }

    @Test
    public void test_Atom_ok() {
        URI atomUri = URI.create("uri:atom");
        Atom atom = new Atom();
        atom.setState(AtomState.ACTIVE);
        atom.setAtomURI(atomUri);
        atom.setCreationDate(new Date());
        AtomMessageContainer mc = new AtomMessageContainer(atom, atom.getAtomURI());
        atomMessageContainerRepository.save(mc);
        mc.setParentUri(atomUri);
        atom.setMessageContainer(mc);
        atomRepository.save(atom);
    }

    @Test
    public void test_create_Atom_from_message() throws Exception {
        Dataset ds = createTestDataset("/won/node/test-messages/create-atom.trig");
        WonMessage msg = new WonMessage(ds);
        Atom atom = atomService.createAtom(msg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(msg, atom.getAtomURI());
        URI newMessageURI = URI.create("uri:successResponse");
        WonMessage responseMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(msg, true, newMessageURI).build();
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom atom2 = atomService.getAtomRequired(atomURI);
        assertEquals(2, atom2.getMessageContainer().getEvents().size());
    }

    @Test
    public void test_create_two_atoms_and_connect() throws Exception {
        // create an atom, as before
        Dataset ds = createTestDataset("/won/node/test-messages/create-atom.trig");
        WonMessage createMsg = new WonMessage(ds);
        Atom atom = atomService.createAtom(createMsg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(createMsg, atom.getAtomURI());
        URI newMessageURI = URI.create("uri:successResponse");
        WonMessage responseMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(createMsg, true, newMessageURI).build();
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom checkAtom = atomService.getAtomRequired(atomURI);
        assertEquals(2, checkAtom.getMessageContainer().getEvents().size());
        //
        // create another atom
        ds = createTestDataset("/won/node/test-messages/create-atom2.trig");
        WonMessage createMsg2 = new WonMessage(ds);
        Atom atom2 = atomService.createAtom(createMsg2);
        URI atomURI2 = atom2.getAtomURI();
        messageService.saveMessage(createMsg2, atom2.getAtomURI());
        URI successResponse2 = URI.create("uri:successResponse2");
        WonMessage responseMessage2 = WonMessageBuilder
                        .setPropertiesForNodeResponse(createMsg2, true, successResponse2).build();
        messageService.saveMessage(responseMessage2, atom2.getAtomURI());
        checkAtom = atomService.getAtomRequired(atomURI2);
        assertEquals(2, checkAtom.getMessageContainer().getEvents().size());
        //
        // simulate a connect from atom to atom2
        // we'll need a connection uri from the mocked service
        Mockito.when(wonNodeInformationService
                        .generateConnectionURI(URI.create("https://node.matchat.org/won/resource")))
                        .thenReturn(URI.create("uri:newConnection"));
        URI senderSocket = URI.create(atom.getAtomURI().toString() + "#chatSocket");
        URI targetSocket = URI.create(atom2.getAtomURI().toString() + "#chatSocket");
        Mockito.when(socketLookup.getCapacity(senderSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.getCapacity(targetSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.isCompatible(senderSocket, targetSocket)).thenReturn(true);
        Mockito.when(socketLookup.isCompatible(targetSocket, senderSocket)).thenReturn(true);
        URI connectMessageUri = URI.create("uri:connectMessage");
        WonMessage connectMessage = WonMessageBuilder.setMessagePropertiesForConnect(connectMessageUri,
                        senderSocket,
                        atom.getAtomURI(), atom.getWonNodeURI(),
                        targetSocket, atom2.getAtomURI(),
                        atom2.getWonNodeURI(), "Hey there!").build();
        // processing the message would lead to this call:
        Mockito.when(wonNodeInformationService.generateConnectionURI())
                        .thenReturn(URI.create("uri:newconnection1"));
        Connection con = connectionService.connectFromOwner(connectMessage);
        // then it would be stored:
        messageService.saveMessage(connectMessage, con.getConnectionURI());
        // we'd create a response
        URI successForConnect = URI.create("uri:successForConnect");
        WonMessage responseForConnectMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(connectMessage, true, successForConnect).build();
        // and store the response
        messageService.saveMessage(responseForConnectMessage, con.getConnectionURI());
        // let's check:
        assertEquals(2, con.getMessageContainer().getEvents().size());
        Set<URI> messages = con.getMessageContainer().getEvents().stream()
                        .map(mic -> mic.getMessageURI()).collect(Collectors.toSet());
        assertTrue(messages.contains(connectMessageUri));
        assertTrue(messages.contains(successForConnect));
    }

    @Test
    public void test_create_two_atoms_and_connect_use_same_message_in_both_connections() throws Exception {
        // create an atom, as before
        Dataset ds = createTestDataset("/won/node/test-messages/create-atom.trig");
        WonMessage createMsg = new WonMessage(ds);
        Atom atom = atomService.createAtom(createMsg);
        URI atomURI = atom.getAtomURI();
        messageService.saveMessage(createMsg, atom.getAtomURI());
        URI newMessageURI = URI.create("uri:successResponse");
        WonMessage responseMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(createMsg, true, newMessageURI).build();
        messageService.saveMessage(responseMessage, atom.getAtomURI());
        Atom checkAtom = atomService.getAtomRequired(atomURI);
        assertEquals(2, checkAtom.getMessageContainer().getEvents().size());
        //
        // create another atom
        ds = createTestDataset("/won/node/test-messages/create-atom2.trig");
        WonMessage createMsg2 = new WonMessage(ds);
        Atom atom2 = atomService.createAtom(createMsg2);
        URI atomURI2 = atom2.getAtomURI();
        messageService.saveMessage(createMsg2, atom2.getAtomURI());
        URI successResponse2 = URI.create("uri:successResponse2");
        WonMessage responseMessage2 = WonMessageBuilder
                        .setPropertiesForNodeResponse(createMsg2, true, successResponse2).build();
        messageService.saveMessage(responseMessage2, atom2.getAtomURI());
        checkAtom = atomService.getAtomRequired(atomURI2);
        assertEquals(2, checkAtom.getMessageContainer().getEvents().size());
        //
        // simulate a connect from atom to atom2
        // we'll need a connection uri from the mocked service
        Mockito.when(wonNodeInformationService
                        .generateConnectionURI(URI.create("https://node.matchat.org/won/resource")))
                        .thenReturn(URI.create("uri:newConnection"));
        URI senderSocket = URI.create(atom.getAtomURI().toString() + "#chatSocket");
        URI targetSocket = URI.create(atom2.getAtomURI().toString() + "#chatSocket");
        Mockito.when(socketLookup.getCapacity(senderSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.getCapacity(targetSocket)).thenReturn(Optional.of(10));
        Mockito.when(socketLookup.isCompatible(senderSocket, targetSocket)).thenReturn(true);
        Mockito.when(socketLookup.isCompatible(targetSocket, senderSocket)).thenReturn(true);
        URI connectMessageUri = URI.create("uri:connectMessage");
        WonMessage connectMessage = WonMessageBuilder.setMessagePropertiesForConnect(connectMessageUri,
                        senderSocket,
                        atom.getAtomURI(), atom.getWonNodeURI(),
                        targetSocket, atom2.getAtomURI(),
                        atom2.getWonNodeURI(), "Hey there!").build();
        // processing the message would lead to this call:
        Mockito.when(wonNodeInformationService.generateConnectionURI())
                        .thenReturn(URI.create("uri:newconnection1"), URI.create("uri:newconnection2"));
        Connection con = connectionService.connectFromOwner(connectMessage);
        // then it would be stored:
        messageService.saveMessage(connectMessage, con.getConnectionURI());
        // we'd create a response
        URI successForConnect = URI.create("uri:successForConnect");
        WonMessage responseForConnectMessage = WonMessageBuilder
                        .setPropertiesForNodeResponse(connectMessage, true, successForConnect).build();
        // and store the response
        messageService.saveMessage(responseForConnectMessage, con.getConnectionURI());
        // let's check:
        assertEquals(2, con.getMessageContainer().getEvents().size());
        Set<URI> messages = con.getMessageContainer().getEvents().stream()
                        .map(mic -> mic.getMessageURI()).collect(Collectors.toSet());
        assertTrue(messages.contains(connectMessageUri));
        assertTrue(messages.contains(successForConnect));
        // if we want to use the existing logic, we have to set the sender URI on the
        // connectMessage (TODO: remove this step eventually)
        connectMessage.addMessageProperty(WONMSG.sender, con.getConnectionURI());
        // now simulate the other side receives the connect
        Connection remoteCon = connectionService.connectFromNode(connectMessage);
        connectMessage.addMessageProperty(WONMSG.recipient, remoteCon.getConnectionURI());
        messageService.saveMessage(connectMessage, remoteCon.getConnectionURI());
        URI successForConnect2 = URI.create("uri:successForConnect2");
        WonMessage responseForConnectMessage2 = WonMessageBuilder
                        .setPropertiesForNodeResponse(connectMessage, true, successForConnect2).build();
        messageService.saveMessage(responseForConnectMessage2, remoteCon.getConnectionURI());
        assertEquals(2, con.getMessageContainer().getEvents().size());
        Set<URI> messages2 = remoteCon.getMessageContainer().getEvents().stream()
                        .map(mic -> mic.getMessageURI()).collect(Collectors.toSet());
        assertTrue("expecting the connect message in the connection's message container",
                        messages2.contains(connectMessageUri));
        assertTrue("expecting the success response in the connection's message container",
                        messages2.contains(successForConnect2));
    }

    private Dataset createTestDataset(String resourceName) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        Dataset dataset = DatasetFactory.createGeneral();
        dataset.begin(ReadWrite.WRITE);
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        dataset.commit();
        return dataset;
    }
}
