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
package org.bedework.carddav.server;

import org.bedework.carddav.server.SysIntf.GetLimits;
import org.bedework.carddav.server.SysIntf.GetResult;
import org.bedework.carddav.server.filter.Filter;
import org.bedework.carddav.server.filter.PropFilter;
import org.bedework.carddav.server.filter.TextMatch;
import org.bedework.carddav.util.CardDAVConfig;
import org.bedework.carddav.vcard.Card;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.Util;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Implement the special uri(s).
 *
 */
public class SpecialUri {
  /** */
  public static String formatVcard = "vcard";

  /** */
  public static String formatJson = "json";

  /**
   * @param req
   * @param resp
   * @param resourceUri
   * @param sysi
   * @param config
   * @param fromGetAccept - true if this is GET with ACCEPT targeted at address book
   * @param accept - desired format
   * @param debug
   * @return true for a special uri
   * @throws WebdavException
   */
  public static boolean process(final HttpServletRequest req,
                                final HttpServletResponse resp,
                                final String resourceUri,
                                final SysIntf sysi,
                                final CardDAVConfig config,
                                final boolean fromGetAccept,
                                final String accept,
                                final boolean debug) throws WebdavException {
    String addrbook = null;
    String format;

    if (fromGetAccept) {
      addrbook = resourceUri;
      format = accept;
    } else {
      if (config.getWebaddrServiceURI() == null) {
        return false;
      }

      if (!config.getWebaddrServiceURI().equals(resourceUri)) {
        return false;
      }

      addrbook = req.getParameter("addrbook");

      if (addrbook == null) {
        addrbook = config.getWebaddrPublicAddrbook();
      }

      format = Util.checkNull(req.getParameter("format"));

      if (format == null) {
        format = formatVcard;
      }
    }

    if (format.equals(formatVcard)) {
      resp.setContentType("text/vcard"); // TODO - add version parameter
    } else if (format.equals(formatJson)) {
      resp.setContentType("application/json");
    }

    startResponse(resp, format);
    GetResult res = null;

    buildResponse: {
      CarddavCollection col = sysi.getCollection(addrbook);

      if (col == null) {
        setStatus(resp, HttpServletResponse.SC_NOT_FOUND, format);
        break buildResponse;
      }

      GetLimits limits = null;

      String limitStr = Util.checkNull(req.getParameter("limit"));

      if (limitStr != null) {
        limits = new GetLimits();

        try {
          limits.limit = Integer.parseInt(limitStr);
        } catch (Throwable t) {
          setStatus(resp, HttpServletResponse.SC_BAD_REQUEST, limitStr);
          break buildResponse;
        }
      }

      if (req.getParameter("list") != null) {
        if (addrbook == null) {
          throw new WebdavBadRequest("Must specify addressbook");
        }

        res = doList(req, col, limits, config, sysi, debug);
      } else {
        res = doSearch(req, col, limits, config, sysi, debug);
      }

      if (Util.isEmpty(res.cards)) {
        setStatus(resp, HttpServletResponse.SC_NOT_FOUND, format);
        break buildResponse;
      }

      doOutput(resp, res.cards, format);
    } // buildResponse

    if ((res != null) && ((res.overLimit) || (res.serverTruncated))) {
      String msg;
      if (res.overLimit) {
        msg = "User limit exceeded";
      } else {
        msg = "Server limit exceeded";
      }
      setStatus(resp, /*HttpServletResponse.SC_INSUFFICIENT_STORAGE*/ 507, msg);
    }

    endResponse(resp, format);

    return true;
  }

  private static void startResponse(final HttpServletResponse resp,
                                    final String format) throws WebdavException {
    if (format.equals(formatVcard)) {
      return;
    }

    // vcard
    try {
      resp.getWriter().write("{\n");
    } catch (IOException ie) {
      throw new WebdavException(ie);
    }
  }

  private static GetResult doSearch(final HttpServletRequest req,
                                    final CarddavCollection col,
                                    final GetLimits limits,
                                    final CardDAVConfig config,
                                    final SysIntf sysi,
                                    final boolean debug) throws WebdavException {
    String text = req.getParameter("q");

    if (text == null) {
      text = req.getParameter("term");
    }

    boolean orThem = true;

    Filter fltr = new Filter(debug);

    if (orThem) {
      fltr.setTestAllAny(Filter.testAnyOf);
    } else {
      fltr.setTestAllAny(Filter.testAllOf);
    }

    boolean hadTerm = false;

    for (String wsp: config.getWebaddrServiceProperties()) {
      String val = req.getParameter(wsp);

      if (val != null) {
        if (text != null) {
          throw new WebdavBadRequest("Conflicting request parameters");
        }
      } else if (text != null) {
        val = text;
      }

      if (val != null) {
        hadTerm = true;
        fltr.addPropFilter(new PropFilter(wsp,
                                          //new TextMatch(val)));
                                          new TextMatch(true,
                                                        TextMatch.matchTypeContains,
                                                        false,
                                                        val)));

      }
    }

    if (!hadTerm) {
      throw new WebdavBadRequest("No search terms provided");
    }

    return sysi.getCards(col, fltr, limits);
  }

  private static GetResult doList(final HttpServletRequest req,
                                  final CarddavCollection col,
                                  final GetLimits limits,
                                  final CardDAVConfig config,
                                  final SysIntf sysi,
                                  final boolean debug) throws WebdavException {
    return sysi.getCards(col, null, limits);
  }

  private static void doOutput(final HttpServletResponse resp,
                               final Collection<Card> cards,
                               final String format) throws WebdavException {
    try {
      Writer wtr = resp.getWriter();

      if (format.equals(formatVcard)) {
        for (Card card: cards) {
          wtr.write(card.output());
        }
        resp.setStatus(HttpServletResponse.SC_OK);

        return;
      }

      // vcard
      wtr.write("  \"microformats\": {");
      wtr.write("\n");

      wtr.write("    \"vcard\": [");
      wtr.write("\n");

      boolean first = true;

      for (Card card: cards) {
        if (first) {
          first = false;
        } else {
          wtr.write(",\n");
        }
        wtr.write(card.outputJson("      "));
      }

      wtr.write("\n    ]\n");
      wtr.write("  }\n");

      resp.setStatus(HttpServletResponse.SC_OK);
    } catch (IOException ie) {
      throw new WebdavException(ie);
    }
  }

  private static void setStatus(final HttpServletResponse resp,
                                final int status,
                                final String format) throws WebdavException {
    try {
      if (format.equals(formatVcard)) {
        resp.setStatus(status);
        return;
      }

      // vcard
      Writer wtr = resp.getWriter();

      wtr.write("    \"errors\" : [ \"");
      wtr.write(String.valueOf(status));
      wtr.write("\" ]\n");
    } catch (IOException ie) {
      throw new WebdavException(ie);
    }
  }

  private static void endResponse(final HttpServletResponse resp,
                                    final String format) throws WebdavException {
    if (format.equals(formatVcard)) {
      return;
    }

    // vcard
    try {
      resp.getWriter().write("}\n");
    } catch (IOException ie) {
      throw new WebdavException(ie);
    }
  }
}
