import { details, emptyDraft } from "../../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../../app/won-utils.js";
import {
  industryDetail,
  employmentTypesDetail,
  organizationNamesDetail,
} from "../../details/jobs.js";
import { jobLocation } from "../../details/location.js";
import {
  vicinityScoreSubQuery,
  tagOverlapScoreSubQuery,
  sparqlQuery,
} from "../../../app/sparql-builder-utils.js";

import won from "../../../app/won-es6.js";

import { getIn } from "../../../app/utils.js";

import { Generator } from "sparqljs";
window.SparqlGenerator4dbg = Generator;

export const jobSearch = {
  identifier: "jobSearch",
  label: "Search a Job",
  // icon: "#ico36_uc_find_people", TODO
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    is: {
      "@type": "s:Person",
      tags: ["search-job"],
    },
    seeks: {
      "@type": "s:JobPosting",
    },
    searchString: ["offer-job"],
  },
  isDetails: {
    title: { ...details.title },
    description: { ...details.description },
    // location: { ...details.location }, // why would your current location of residency matter?
    // person: { ...details.person }, // has current company fields, that are weird for a search
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
  seeksDetails: {
    description: { ...details.description },
    jobLocation: { ...jobLocation },
    industry: { ...industryDetail },
    employmentTypes: { ...employmentTypesDetail },
    organizationNames: { ...organizationNamesDetail },
  },

  /**
   *
   * e.g.: with just industries:
   * ```
   * # index for industries using binds
   * prefix s: <http://schema.org/>
   * prefix won:   <http://purl.org/webofneeds/model#>
   * select distinct * where {
   *   {
   *     select
   *       ${resultName}
   *       (sum(?var1) + sum(?var2) as ?targetOverlap)
   *       (count(${resultName}) as ?targetTotal)
   *     where {
   *       ${resultName} a won:Need;
   *             won:is ?is.
   *             ?is s:industry ?industry .
   *       bind(if(str(?industry) = "design",1,0) as ?var1)
   *       bind(if(str(?industry) = "computer science",1,0) as ?var2)
   *     } group by (${resultName})
   *   }
   *   bind (?targetOverlap / ( ?targetTotal + 2 - ?targetOverlap ) as ?jaccardIndex )
   * } order by desc(?jaccardIndex)
   * limit 100
   * ```
   */
  generateQuery: (draft, resultName) => {
    // skills
    const skillsSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?skills_jaccardIndex",
      pathToTags: "won:seeks/s:knowsAbout",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["is", "skills"]),
    });

    // hiringOrganizationName
    const organizationNameSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?organizationName_jaccardIndex",
      pathToTags: "won:is/s:hiringOrganization/s:name",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["seeks", "organizationNames"]),
    });

    // employmentType
    const employmentTypesSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?employmentTypes_jaccardIndex",
      pathToTags: "won:is/s:employmentType",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["seeks", "employmentTypes"]),
    });

    // industry:
    const industryScoreSQ = tagOverlapScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?industry_jaccardIndex",
      pathToTags: "won:is/s:industry",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      tagLikes: getIn(draft, ["seeks", "industry"]),
    });

    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?jobLocation_geoScore",
      pathToGeoCoords: "won:is/s:jobLocation/s:geo",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      geoCoordinates: getIn(draft, ["seeks", "jobLocation"]),
    });

    const subQueries = [
      industryScoreSQ,
      vicinityScoreSQ,
      employmentTypesSQ,
      organizationNameSQ,
      skillsSQ,
    ]
      .filter(sq => sq) // filter out non-existing details (the SQs should be `undefined` for them)
      .map(sq => ({
        query: sq,
        optional: true, // so counterparts without that detail don't get filtered out (just assigned a score of 0 via `coalesce`)
      }));

    const query = sparqlQuery({
      prefixes: {
        won: won.defaultContext["won"],
        rdfs: won.defaultContext["rdfs"],
      },
      distinct: true,
      variables: [resultName],
      subQueries: subQueries,
      where: [
        `${resultName} a won:Need.`,

        // calculate average of scores; can be weighed if necessary
        `BIND( ( 
          COALESCE(?industry_jaccardIndex, 0) + 
          COALESCE(?skills_jaccardIndex, 0) + 
          COALESCE(?organizationName_jaccardIndex, 0) + 
          COALESCE(?employmentTypes_jaccardIndex, 0) + 
          COALESCE(?jobLocation_geoScore, 0) 
        ) / 5  as ?aggregatedScore)`,
        `FILTER(?aggregatedScore > 0)`,
      ],
      orderBy: [{ order: "DESC", variable: "?aggregatedScore" }],
      limit: 20,
    });

    return query;
  },
};
