/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.carddav.server.filter;

import org.bedework.carddav.server.CarddavNode;
import org.bedework.carddav.server.SysIntf.GetLimits;
import org.bedework.carddav.server.SysIntf.GetResult;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsNode;

import org.w3c.dom.Node;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

/** Class to represent a calendar-query filter
 *  <pre>
10.5.  CARDDAV:filter XML Element
   Name:  filter
   Namespace:  urn:ietf:params:xml:ns:carddav
   Purpose:  Determines which matching objects are returned.

   Description:  The "filter" element specifies the search filter used
      to match address objects that should be returned by a report.  The
      "test" attribute specifies whether any (logical OR) or all
      (logical AND) of the prop-filter tests needs to match in order for
      the overall filter to match.

   Definition:
       <!ELEMENT filter (prop-filter*)>

       <!ATTLIST filter test (anyof | allof) "anyof">
       <!-- test value:
                 anyof  logical OR for prop-filter matches
                 allof  logical AND for prop-filter matches -->

10.5.1.  CARDDAV:prop-filter XML Element
   Name:  prop-filter
   Namespace:  urn:ietf:params:xml:ns:carddav
   Purpose:  Limits the search to specific properties.

   Description:  The CARDDAV:prop-filter XML element specifies a search
      criteria on a specific vCard property (e.g., NICKNAME).  An
      address object is said to match a CARDDAV:prop-filter if:

      *  A property of the type specified by the "name" attribute
         exists, and the CARDDAV:prop-filter is empty, or it matches the
         CARDDAV:text-match conditions if specified, and that CARDDAV:
         param-filter child elements also match.  The "test" attribute
         specifies whether any (logical OR) or all (logical AND) of the
         text-filter and param-filter tests needs to match in order for
         the overall filter to match.

      or:

      *  A property of the type specified by the "name" attribute does
         not exist, and the CARDAV:is-not-defined element is specified.

   Definition:
       <!ELEMENT prop-filter (is-not-defined |
                              (text-match?, param-filter*))>

       <!ATTLIST prop-filter name CDATA #REQUIRED
                             test (anyof | allof) "anyof">
       <!-- name value: a vCard property name (e.g., "NICKNAME")
         test value:
             anyof  logical OR for text-match/param-filter matches
             allof  logical AND for text-match/param-filter matches -->

10.5.2.  CARDDAV:param-filter XML Element
   Name:  param-filter
   Namespace:  urn:ietf:params:xml:ns:carddav
   Purpose:  Limits the search to specific parameter values.

   Description:  The CARDDAV:param-filter XML element specifies a search
      criteria on a specific vCard property parameter (e.g., TYPE) in
      the scope of a given CARDDAV:prop-filter.  A vCard property is
      said to match a CARDDAV:param-filter if:

      *  A parameter of the type specified by the "name" attribute
         exists, and the CARDDAV:param-filter is empty, or it matches
         the CARDDAV:text-match conditions if specified.

      or:

      *  A parameter of the type specified by the "name" attribute does
         not exist, and the CARDDAV:is-not-defined element is specified.

   Definition:
       <!ELEMENT param-filter (is-not-defined | text-match)?>

       <!ATTLIST param-filter name CDATA #REQUIRED>
       <!-- name value: a property parameter name (e.g., "TYPE") -->

10.5.3.  CARDDAV:is-not-defined XML Element
   Name:  is-not-defined
   Namespace:  urn:ietf:params:xml:ns:carddav
   Purpose:  Specifies that a match should occur if the enclosing
      property or parameter does not exist.

   Description:  The CARDDAV:is-not-defined XML element specifies that a
      match occurs if the enclosing property or parameter value
      specified in an address book REPORT request does not exist in the
      address data being tested.

   Definition:
       <!ELEMENT is-not-defined EMPTY>

10.5.4.  CARDDAV:text-match XML Element
   Name:  text-match
   Namespace:  urn:ietf:params:xml:ns:carddav
   Purpose:  Specifies a substring match on a property or parameter
      value.

   Description:  The CARDDAV:text-match XML element specifies text used
      for a substring match against the property or parameter value
      specified in an address book REPORT request.

      The "collation" attribute is used to select the collation that the
      server MUST use for character string matching.  In the absence of
      this attribute the server MUST use the "i;unicode-casemap"
      collation.

      The "negate-condition" attribute is used to indicate that this
      test returns a match if the text matches, when the attribute value
      is set to "no", or return a match if the text does not match, if
      the attribute value is set to "yes".  For example, this can be
      used to match components with a CATEGORIES property not set to
      PERSON.

      The "match-type" attribute is used to indicate the type of match
      operation to use.  Possible choices are:

         "equals" - an exact match to the target string

         "contains" - a substring match, matching anywhere within the
         target string

         "starts-with" - a substring match, matching only at the start
         of the target string

         "ends-with" - a substring match, matching only at the end of
         the target string

   Definition:
       <!ELEMENT text-match (#PCDATA)>
       <!-- PCDATA value: string -->

       <!ATTLIST text-match
          collation        CDATA "i;unicode-casemap"
          negate-condition (yes | no) "no"
          match-type (equals|contains|starts-with|ends-with) "contains">

 * </pre>
 *
 *   @author Mike Douglass   douglm @ bedework.edu
 */
public class Filter extends CarddavFilter {
  /** Constructor
   *
   * @param debug
   */
  public Filter(boolean debug) {
    super(debug);
  }

  /** Parse for the carddav server.
   *
   * @param nd
   * @param tz
   * @throws WebdavException
   */
  public void carddavParse(Node nd) throws WebdavException {
    try {
      super.parse(nd);
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      error(t);
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /** Use the given query to return a collection of nodes. An exception will
   * be raised if the entire query fails for some reason (access, etc). An
   * empty collection will be returned if no objects match.
   *
   * @param wdnode    WebdavNsNode defining root of search
   * @param limits    to limit result size
   * @return GetResult with collection of result nodes (empty for no result)
   * @throws WebdavException
   */
  public GetResult query(CarddavNode wdnode,
                         GetLimits limits) throws WebdavException {
    try {
      /*if (debug) {
      if (eventq.trange == null) {
        trace("No time-range specified for uri " + wdnode.getUri());
      } else {
        trace("time-range specified for uri " + wdnode.getUri() +
              " with start=" + eventq.trange.getStart() +
              " end=" + eventq.trange.getEnd());
      }
    }*/

      GetResult  res = wdnode.getSysi().getCards(wdnode.getWdCollection(),
                                                 this, limits);

      if (debug) {
        trace("Query returned " + res.cards.size());
      }

      return res;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      error(t);
      throw new WebdavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /** Carry out any postfiltering on the collection of initialised nodes,
   *
   * @param nodes     Collection of initialised CaldavBwNode
   * @return Collection of filtered nodes (empty for no result)
   * @throws WebdavException
   */
  public Collection<WebdavNsNode> postFilter(
                   Collection<WebdavNsNode> nodes) throws WebdavException {
//    if (!eventq.postFilter) {
      return nodes;
  /*  }

    if (debug) {
      trace("post filtering needed");
    }

    ArrayList<WebdavNsNode> filtered = new ArrayList<WebdavNsNode>();

    for (WebdavNsNode node: nodes) {
      CarddavCardNode curnode = null;

      if (!(node instanceof CarddavCardNode)) {
        // Cannot match to anything - don't pass it?
      } else {
        curnode = (CarddavCardNode)node;

        for (PropFilter pf: getPropFilters()) {
          if (pf.filter(curnode.getCard())) {
            filtered.add(curnode);
            break;
          }
        }
      }
    }

    return filtered;*/
  }
}
