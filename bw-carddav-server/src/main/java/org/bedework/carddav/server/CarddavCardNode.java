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

import org.bedework.access.AccessPrincipal;
import org.bedework.access.CurrentAccess;
import org.bedework.access.PrivilegeDefs;
import org.bedework.carddav.common.CarddavCollection;
import org.bedework.carddav.common.vcard.Card;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.tagdefs.CarddavTags;
import org.bedework.webdav.servlet.shared.WebdavException;
import org.bedework.webdav.servlet.shared.WebdavNsIntf;

import org.w3c.dom.Element;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.namespace.QName;

/** Class to represent an entity such as events in caldav.
 *
 *   @author Mike Douglass   douglm  rpi.edu
 */
public class CarddavCardNode extends CarddavNode {
  private Card card;

  private String vcardVersion;

  private String entityName;

  private CarddavCollection col;

  private final static HashMap<QName, PropertyTagEntry> propertyNames =
    new HashMap<QName, PropertyTagEntry>();

  static {
    addPropEntry(propertyNames, CarddavTags.addressData);

  }

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   */
  public CarddavCardNode(final SysIntf sysi,
                         final int status,
                         final String uri) {
    super(true, sysi, uri);
    setStatus(status);
    this.uri = uri;
  }

  /** Constructor
   *
   * @param cdURI
   * @param sysi
   * @throws WebdavException
   */
  public CarddavCardNode(final CarddavURI cdURI,
                         final SysIntf sysi) throws WebdavException {
    super(cdURI, sysi);

    col = cdURI.getCol();
    collection = false;
    allowsGet = true;
    entityName = cdURI.getEntityName();

    exists = cdURI.getExists();

    card = cdURI.getEntity();
  }

  @Override
  public void init(final boolean content) {
    //if (!content) {
    //  return;
    //}
  }

  /* (non-Javadoc)
   * @see org.bedework.carddav.server.CarddavNode#getWdCollection()
   */
  @Override
  public CarddavCollection getWdCollection() throws WebdavException {
    return col;
  }

  @Override
  public AccessPrincipal getOwner() throws WebdavException {
    if (col == null) {
      return null;
    }

    if (col.getOwner() != null) {
      return col.getOwner();
    }

    return null;
  }

  /**
   * @param val
   */
  public void setVcardVersion(final String val) {
    vcardVersion = val;
  }

  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) throws WebdavException {
    warn("Unimplemented - removeProperty");

    return false;
  }

  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) throws WebdavException {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  @Override
  public void update() throws WebdavException {
    getSysi().updateCard(col.getPath(), card);
  }

  /**
   * @return String
   */
  public String getEntityName() {
    return entityName;
  }

  @Override
  public boolean trailSlash() {
    return false;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  @Override
  public boolean knownProperty(final QName tag) {
    if (propertyNames.get(tag) != null) {
      return true;
    }

    // Not ours
    return super.knownProperty(tag);
  }

@Override
public boolean generatePropertyValue(final QName tag,
                                      final WebdavNsIntf intf,
                                      final boolean allProp) throws WebdavException {
    //PropVal pv = new PropVal();
    //XmlEmit xml = intf.getXmlEmit();

    if (propertyNames.get(tag) == null) {
      // Not ours
      return super.generatePropertyValue(tag, intf, allProp);
    }

    try {

      return false;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public Collection<PropertyTagEntry> getPropertyNames() throws WebdavException {
    Collection<PropertyTagEntry> res = new ArrayList<PropertyTagEntry>();

    res.addAll(super.getPropertyNames());
    res.addAll(propertyNames.values());

    return res;
  }

  /* UNUSED
  @Override
  public Collection<WebdavProperty> getProperties(final String ns) throws WebdavException {
    init(true);
    ArrayList<WebdavProperty> al = new ArrayList<WebdavProperty>();

    /* Default property calendar-data returns all of the object * /
    al.add(new AddressData(CarddavTags.addressData, debug));

    return al;
  }
  */

  @Override
  public String getContentString(final String contentType) throws WebdavException {
    return card.output(vcardVersion);
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  @Override
  public String writeContent(final XmlEmit xml,
                             final Writer wtr,
                             final String contentType) throws WebdavException {
    try {
      if ("application/vcard+json".equals(contentType)) {
        if (xml == null) {
          wtr.write(card.outputJson(debug(), vcardVersion));
        } else {
          xml.cdataValue(card.outputJson(debug(), vcardVersion));
        }
        return contentType;
      }

      if (xml == null) {
        wtr.write(card.output(vcardVersion));
        return "text/vcard";
      }

      xml.cdataValue(card.output(vcardVersion));
      return "application/vcard+xml";
    } catch (final WebdavException we) {
      throw we;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  @Override
  public CurrentAccess getCurrentAccess() throws WebdavException {
    if (col == null) {
      return null;
    }

    return getSysi().checkAccess(col, PrivilegeDefs.privAny, true);
  }

  @Override
  public String getEtagValue(final boolean strong) throws WebdavException {
    init(true);

    if (card == null) {
      return null;
    }

    return makeEtag(card.getLastmod());
  }

  /**
   * @param strong
   * @return etag before changes
   * @throws WebdavException
   */
  public String getPrevEtagValue(final boolean strong) throws WebdavException {
    init(true);

    if (card == null) {
      return null;
    }

    return makeEtag(card.getPrevLastmod());
  }

  private String makeEtag(final String lastmod) {
    StringBuilder val = new StringBuilder();
    val.append("\"");
    val.append(lastmod);
    val.append("\"");

    return val.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("CaldavComponentNode{");
    sb.append("path=");
    sb.append(getPath());
    sb.append(", entityName=");
    sb.append(String.valueOf(entityName));
    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  @Override
  public String getContentLang() throws WebdavException {
    return "en";
  }

  @Override
  public long getContentLen() throws WebdavException {
    if (card != null) {
      return card.output(vcardVersion).length();
    }

    return 0;
  }

  @Override
  public String getContentType() throws WebdavException {
    return "text/vcard; version=\"4.0\"; charset=UTF-8";
  }

  @Override
  public String getCreDate() throws WebdavException {
    init(false);

    if (card == null) {
      return null;
    }

    //return card.getCreated();
    return null;
  }

  @Override
  public String getDisplayname() throws WebdavException {
    return getEntityName();
  }

  @Override
  public String getLastmodDate() throws WebdavException {
    init(false);

    if (card == null) {
      return null;
    }

    try {
      // return DateTimeUtil.fromISODateTimeUTCtoRfc822(card.getLastmod());
      return card.getLastmod();
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /**
   * @param val
   * @throws WebdavException
   */
  public void setCard(final Card val) throws WebdavException {
    card = val;
  }

  /**
   * @return VCard
   * @throws WebdavException
   */
  public Card getCard() throws WebdavException {
    return card;
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  //private void addProp(Collection<WebdavProperty> c, QName tag, Object val) {
  //  if (val != null) {
  //    c.add(new WebdavProperty(tag, String.valueOf(val)));
  //  }
  //}
}
