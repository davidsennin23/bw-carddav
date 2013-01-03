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
package org.bedework.carddav.server.jmx;

import org.apache.activemq.broker.jmx.MBeanInfo;

import javax.management.openmbean.TabularData;

/** Run the carddav service
 *
 * @author douglm
 */
public interface CardDavMBean extends ConfBaseMBean {
  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /**
   * @return List of dir handler mappings
   */
  @MBeanInfo("Lists the dir handler mappings to the principal hierarchy " +
  		"(Not HTML friendy)")
  TabularData ListDirHandlersTable();

  /**
   * @return List of dir handler mappings
   */
  @MBeanInfo("Lists the dir handler mappings to the principal hierarchy ")
  String ListDirHandlers();

  /** (Re)load the configurations
   *
   * @return status
   */
  @MBeanInfo("(Re)load the configurations")
  String loadConfigs();
}
