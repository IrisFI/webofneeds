package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.message.Utils;
import won.protocol.model.*;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by hfriedrich on 16.03.2017.
 */
public class NeedModelWarpperTest {
    private final String NEED_URI = "https://node.matchat.org/won/resource/need/3030440624813201400";

    @Test
    public void loadModels() throws IOException {

        // load dataset and if the need and sysinfo models are there
        Dataset ds = Utils.createTestDataset("/needmodel/need1.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
        Assert.assertEquals(NEED_URI, needModelWrapper.getNeedNode(NeedGraphType.NEED).getURI());
        Assert.assertEquals(NEED_URI, needModelWrapper.getNeedNode(NeedGraphType.SYSINFO).getURI());

        // load the need and sysinfo models individually
        Model needModel = needModelWrapper.getNeedModel(NeedGraphType.NEED);
        Model sysInfoModel = needModelWrapper.getNeedModel(NeedGraphType.SYSINFO);
        NeedModelWrapper needModelWrapperNew = new NeedModelWrapper(needModel, sysInfoModel);
        Assert.assertEquals(needModelWrapperNew.getNeedModel(NeedGraphType.NEED), needModelWrapper.getNeedModel(NeedGraphType.NEED));
        Assert.assertEquals(needModelWrapperNew.getNeedModel(NeedGraphType.SYSINFO), needModelWrapper.getNeedModel(NeedGraphType.SYSINFO));

        // load only the need model, the other one is created
        needModelWrapperNew = new NeedModelWrapper(needModel, null);
        Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.NEED).getURI());
        Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.SYSINFO).getURI());

        // load only the sysinfo model, the other one is created
        needModelWrapperNew = new NeedModelWrapper(null, sysInfoModel);
        Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.NEED).getURI());
        Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.SYSINFO).getURI());

        // query sysinfo model values
        Assert.assertEquals(NeedState.ACTIVE, needModelWrapper.getNeedState());
        ZonedDateTime date = ZonedDateTime.parse("2017-02-07T08:46:32.917Z", DateTimeFormatter.ISO_DATE_TIME);
        Assert.assertEquals(date, needModelWrapper.getCreationDate());
        Assert.assertEquals("https://node.matchat.org/won/resource", needModelWrapper.getWonNodeUri());
        Assert.assertEquals("https://node.matchat.org/won/resource/need/3030440624813201400/connections", needModelWrapper.getConnectionContainerUri());

        // query the need model values
        Assert.assertTrue(needModelWrapper.hasFlag(WON.USED_FOR_TESTING));
        Assert.assertEquals(1, needModelWrapper.getFacetUris().size());
        Assert.assertEquals("http://purl.org/webofneeds/model#OwnerFacet", needModelWrapper.getFacetUris().iterator().next());
        Assert.assertTrue(needModelWrapper.hasFlag(WON.SUPPRESS_HINTS_FOR_ME));

        // query the content nodes
        Assert.assertEquals(2, needModelWrapper.getContentNodes(NeedContentPropertyType.ALL).size());
        Assert.assertEquals(1, needModelWrapper.getContentNodes(NeedContentPropertyType.IS).size());
        Assert.assertEquals(1, needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS).size());
        Assert.assertEquals(0, needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).size());
        Assert.assertEquals("Offering tennis lessons", needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS, DC.title));
        Assert.assertEquals("tennis students", needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.SEEKS, DC.title));
        Assert.assertEquals(2, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.ALL, DC.title).size());
        Assert.assertEquals(3, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS, WON.HAS_TAG).size());
        Assert.assertEquals(2, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.SEEKS, WON.HAS_TAG).size());
        Assert.assertEquals(5, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.ALL, WON.HAS_TAG).size());
        Assert.assertEquals("16.358398", needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS, "won:hasLocation/<s:geo>/<s:longitude>"));
    }

    @Test
    public void loadIsAndSeeksModel() throws IOException {

        Dataset ds = Utils.createTestDataset("/needmodel/need2.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);

        Assert.assertEquals(1, needModelWrapper.getContentNodes(NeedContentPropertyType.IS).size());
        Assert.assertEquals(1, needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).size());
        Assert.assertEquals(2, needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS).size());
        Assert.assertEquals("title1", needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS, DC.title));
        Assert.assertEquals("title1", needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS_AND_SEEKS, DC.title));
        Assert.assertEquals(2, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.SEEKS, DC.title).size());
        Assert.assertEquals(2, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.ALL, DC.title).size());
    }

    @Test
    public void createSysInfoModel() {

        // create a empty wrapper with a need uri, check that the need and sysinfo models are there
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(NEED_URI);
        Assert.assertNotNull(needModelWrapper.getNeedModel(NeedGraphType.SYSINFO));
        Assert.assertEquals(NEED_URI, needModelWrapper.getNeedUri());

        // check that wrapper is empty
        Assert.assertFalse(needModelWrapper.hasFlag(WON.USED_FOR_TESTING));
        Assert.assertEquals(0, needModelWrapper.getContentNodes(NeedContentPropertyType.ALL).size());
        Assert.assertEquals(0, needModelWrapper.getContentNodes(NeedContentPropertyType.ALL).size());
        Assert.assertEquals(0, needModelWrapper.getFacetUris().size());

        // set some values to the sysinfo model and check them
        needModelWrapper.setNeedState(NeedState.INACTIVE);
        needModelWrapper.setNeedState(NeedState.ACTIVE);
        Assert.assertEquals(NeedState.ACTIVE, needModelWrapper.getNeedState());
        needModelWrapper.addFlag(WON.USED_FOR_TESTING);
        needModelWrapper.addFlag(WON.GOOD);
        Assert.assertTrue(needModelWrapper.hasFlag(WON.USED_FOR_TESTING));
        Assert.assertTrue(needModelWrapper.hasFlag(WON.GOOD));
        needModelWrapper.setConnectionContainerUri("https://connnection1");
        needModelWrapper.setConnectionContainerUri("https://connnection2");
        Assert.assertEquals("https://connnection2", needModelWrapper.getConnectionContainerUri());
        needModelWrapper.addFacetUri("https://facet1");
        needModelWrapper.addFacetUri("https://facet2");
        Assert.assertEquals(2, needModelWrapper.getFacetUris().size());
        needModelWrapper.setWonNodeUri("https://wonnode1");
        needModelWrapper.setWonNodeUri("https://wonnode2");
        Assert.assertEquals("https://wonnode2", needModelWrapper.getWonNodeUri());
    }

    @Test
    public void createNeedModel() {

        // create a empty wrapper with a need uri, check that the need and sysinfo models are there
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(NEED_URI);
        Assert.assertNotNull(needModelWrapper.getNeedModel(NeedGraphType.NEED));
        Assert.assertEquals(NEED_URI, needModelWrapper.getNeedUri());

        // adding content without creating content nodes doesnt work
        needModelWrapper.setContentPropertyStringValue(NeedContentPropertyType.IS, WON.HAS_TEXT_DESCRIPTION, "description");
        needModelWrapper.addContentPropertyStringValue(NeedContentPropertyType.IS, WON.HAS_TAG, "tag");
        Assert.assertEquals(needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS, WON.HAS_TEXT_DESCRIPTION).size(), 0);
        Assert.assertEquals(needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS, WON.HAS_TAG).size(), 0);

        // add different content nodes now and check that they are there
        needModelWrapper.createContentNode(NeedContentPropertyType.IS, "https://is_uri");
        needModelWrapper.createContentNode(NeedContentPropertyType.SEEKS, "https://seeks_uri1");
        needModelWrapper.createContentNode(NeedContentPropertyType.SEEKS, "https://seeks_uri2");
        Assert.assertEquals(1, needModelWrapper.getContentNodes(NeedContentPropertyType.IS).size());
        Assert.assertEquals("https://is_uri", needModelWrapper.getContentNodes(NeedContentPropertyType.IS).iterator().next().getURI());
        Assert.assertEquals(2, needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS).size());
        needModelWrapper.createContentNode(NeedContentPropertyType.IS_AND_SEEKS, "https://is_and_seeks_uri");
        Assert.assertEquals(4, needModelWrapper.getContentNodes(NeedContentPropertyType.ALL).size());
        Assert.assertEquals(2, needModelWrapper.getContentNodes(NeedContentPropertyType.IS).size());
        Assert.assertEquals(3, needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS).size());
        Assert.assertEquals(1, needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).size());
        Assert.assertEquals("https://is_and_seeks_uri", needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).iterator().next().getURI());

        // add content now and check if it can be queried correctly
        needModelWrapper.setContentPropertyStringValue(NeedContentPropertyType.IS, WON.HAS_TEXT_DESCRIPTION, "description");
        needModelWrapper.setContentPropertyStringValue(NeedContentPropertyType.SEEKS, WON.HAS_TEXT_DESCRIPTION, "description1");
        needModelWrapper.setContentPropertyStringValue(NeedContentPropertyType.SEEKS, WON.HAS_TEXT_DESCRIPTION, "description2");
        needModelWrapper.addContentPropertyStringValue(NeedContentPropertyType.IS, WON.HAS_TAG, "tag1");
        needModelWrapper.addContentPropertyStringValue(NeedContentPropertyType.SEEKS, WON.HAS_TAG, "tag2");
        needModelWrapper.addContentPropertyStringValue(NeedContentPropertyType.IS_AND_SEEKS, WON.HAS_TAG, "tag3");
        Assert.assertEquals(2, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS, WON.HAS_TEXT_DESCRIPTION).size());
        Assert.assertEquals(1, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS_AND_SEEKS, WON.HAS_TEXT_DESCRIPTION).size());
        Assert.assertEquals("description2", needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS_AND_SEEKS, WON.HAS_TEXT_DESCRIPTION));
        Assert.assertEquals(4, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.ALL, WON.HAS_TEXT_DESCRIPTION).size());
        Assert.assertEquals(3, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS_AND_SEEKS, WON.HAS_TAG).size());
        Assert.assertEquals(6, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.ALL, WON.HAS_TAG).size());
        Assert.assertEquals(4, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS, WON.HAS_TAG).size());
        Assert.assertEquals(5, needModelWrapper.getContentPropertyStringValues(NeedContentPropertyType.SEEKS, WON.HAS_TAG).size());
    }

    @Test
    public void normalizeModel_Tree() throws IOException {

        // compare model that is not changed by normalization
        Dataset ds = Utils.createTestDataset("/needmodel/need1.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
        Model originalModel = needModelWrapper.getNeedModel(NeedGraphType.NEED);
        Model normalizedModel = needModelWrapper.normalizeNeedModel();
        Assert.assertTrue(originalModel.isIsomorphicWith(normalizedModel));


    }

    @Test
    public void normalizeNeedModel_Cycle1() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/needmodel/need2.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
        Model originalModel = needModelWrapper.getNeedModel(NeedGraphType.NEED);
        Model normalizedModel = needModelWrapper.normalizeNeedModel();
        NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
        Assert.assertEquals(needModelWrapper.getContentNodes(NeedContentPropertyType.IS),
                needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS));
        Assert.assertEquals(normalizedWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).size(), 0);
        String isSeeksTitle = needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS_AND_SEEKS, DC.title);
        Assert.assertEquals("title1", isSeeksTitle);
        Assert.assertEquals(isSeeksTitle, normalizedWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS, DC.title));
        Assert.assertTrue(normalizedWrapper.getContentPropertyStringValues(NeedContentPropertyType.SEEKS, DC.title).contains(isSeeksTitle));
    }

    @Test
    public void normalizeNeedModel_Cycle2() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/needmodel/need3.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
        Model originalModel = needModelWrapper.getNeedModel(NeedGraphType.NEED);
        Model normalizedModel = needModelWrapper.normalizeNeedModel();
        NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
        Assert.assertEquals(needModelWrapper.getContentNodes(NeedContentPropertyType.IS),
                needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS));
        Assert.assertEquals(normalizedWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).size(), 0);
        String isSeeksTitle = needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS_AND_SEEKS, DC.title);
        Assert.assertEquals("title1", isSeeksTitle);
        Assert.assertEquals(isSeeksTitle, normalizedWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS, DC.title));
        Assert.assertTrue(normalizedWrapper.getContentPropertyStringValues(NeedContentPropertyType.SEEKS, DC.title).contains(isSeeksTitle));
    }

    @Test
    public void normalizeNeedModel_Cycle3() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/needmodel/need4.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
        Model originalModel = needModelWrapper.getNeedModel(NeedGraphType.NEED);
        Model normalizedModel = needModelWrapper.normalizeNeedModel();
        NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
        Assert.assertEquals(needModelWrapper.getContentNodes(NeedContentPropertyType.IS),
                needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS));
        Assert.assertEquals(normalizedWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).size(), 0);
        String isSeeksTitle = needModelWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS_AND_SEEKS, DC.title);
        Assert.assertEquals("title1", isSeeksTitle);
        Assert.assertEquals(isSeeksTitle, normalizedWrapper.getContentPropertyStringValue(NeedContentPropertyType.IS, DC.title));
        Assert.assertTrue(normalizedWrapper.getContentPropertyStringValues(NeedContentPropertyType.SEEKS, DC.title).contains(isSeeksTitle));
    }

    @Test
    public void normalizeNeedModel_Cycle4() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/needmodel/need5.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
        Model originalModel = needModelWrapper.getNeedModel(NeedGraphType.NEED);
        Model normalizedModel = needModelWrapper.normalizeNeedModel();
        NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
        Assert.assertEquals(needModelWrapper.getContentNodes(NeedContentPropertyType.IS),
                needModelWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS));
        Assert.assertEquals(normalizedWrapper.getContentNodes(NeedContentPropertyType.IS_AND_SEEKS).size(), 0);
        Assert.assertTrue(normalizedWrapper.getContentPropertyStringValues(NeedContentPropertyType.SEEKS, DC.title).contains("title3"));
        Assert.assertTrue(normalizedWrapper.getContentPropertyStringValues(NeedContentPropertyType.IS, DC.title).contains("title3"));
    }

    @Test
    public void normalizeNeedModel_Cycle5() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/needmodel/need6.trig");
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
        Model originalModel = needModelWrapper.getNeedModel(NeedGraphType.NEED);
        Model normalizedModel = needModelWrapper.normalizeNeedModel();
        NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
    }
}
