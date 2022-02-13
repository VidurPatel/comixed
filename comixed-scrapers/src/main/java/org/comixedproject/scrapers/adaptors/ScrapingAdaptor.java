/*
 * ComiXed - A digital comic book library management application.
 * Copyright (C) 2020, The ComiXed Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses>
 */

package org.comixedproject.scrapers.adaptors;

import java.util.List;
import java.util.Set;
import org.comixedproject.model.metadata.MetadataSourceProperty;
import org.comixedproject.scrapers.ScrapingException;
import org.comixedproject.scrapers.model.ScrapingIssue;
import org.comixedproject.scrapers.model.ScrapingIssueDetails;
import org.comixedproject.scrapers.model.ScrapingVolume;

/**
 * A <code>ScrapingAdaptor</code> manages running the scraping actions for a given scraping source
 * and caching data retrieved.
 *
 * @author Darryl L. Pierce
 */
public interface ScrapingAdaptor {
  /**
   * Returns the source identifier for the adaptor.
   *
   * @return the source
   */
  String getSource();

  /**
   * Returns the version identifier for this scraper.
   *
   * @return the identifier
   */
  String getIdentifier();

  /**
   * Returns a list of volumes for the given series name.
   *
   * <p>If <code>maxRecords</code> is a non-positive value then all records are returned.
   *
   * @param seriesName the series name
   * @param maxRecords the maximum records to fetch
   * @param properties the source's properties
   * @return the list of volumes
   * @throws ScrapingException if an error occurs
   */
  List<ScrapingVolume> getVolumes(
      String seriesName, Integer maxRecords, Set<MetadataSourceProperty> properties)
      throws ScrapingException;

  /**
   * Generates a consistent key for storing a fetching details for a single comic.
   *
   * @param issueId the issue number
   * @return the key value
   */
  String getIssueDetailsKey(Integer issueId);

  /**
   * Returns a single issue.
   *
   * @param volume the volume
   * @param issueNumber the issue number within the volume
   * @param properties the source's properties
   * @return the issue or null
   * @throws ScrapingException if an error occurs
   */
  ScrapingIssue getIssue(Integer volume, String issueNumber, Set<MetadataSourceProperty> properties)
      throws ScrapingException;

  /**
   * Returns ta single issue with details.
   *
   * @param issueId the issue id
   * @param properties the source's properties
   * @return the issue details
   * @throws ScrapingException if an error occurs
   */
  ScrapingIssueDetails getIssueDetails(Integer issueId, Set<MetadataSourceProperty> properties)
      throws ScrapingException;

  /**
   * Generates a consistent key for storing and fetching volume data.
   *
   * @param seriesName the series name
   * @return the key value
   */
  String getVolumeKey(String seriesName);

  String getIssueKey(Integer volume, String issueNumber);
}
