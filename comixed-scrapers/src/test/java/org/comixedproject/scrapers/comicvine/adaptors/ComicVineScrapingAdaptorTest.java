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

package org.comixedproject.scrapers.comicvine.adaptors;

import static junit.framework.TestCase.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.*;
import org.comixedproject.model.metadata.MetadataSource;
import org.comixedproject.model.metadata.MetadataSourceProperty;
import org.comixedproject.scrapers.ScrapingException;
import org.comixedproject.scrapers.comicvine.actions.ComicVineGetIssueAction;
import org.comixedproject.scrapers.comicvine.actions.ComicVineGetIssueDetailsAction;
import org.comixedproject.scrapers.comicvine.actions.ComicVineGetVolumesAction;
import org.comixedproject.scrapers.model.ScrapingIssue;
import org.comixedproject.scrapers.model.ScrapingIssueDetails;
import org.comixedproject.scrapers.model.ScrapingVolume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.ObjectFactory;

@RunWith(MockitoJUnitRunner.class)
public class ComicVineScrapingAdaptorTest {
  private static final Random RANDOM = new Random();
  private static final String TEST_API_KEY = "TEST.API.KEY";
  private static final String TEST_SERIES_NAME = "Super Awesome Comic";
  private static final Integer TEST_MAX_RECORDS = RANDOM.nextInt();
  private static final String TEST_ENCODED_VALUE = "This is the encoded value";
  private static final Integer TEST_VOLUME_ID = RANDOM.nextInt();
  private static final String TEST_ISSUE_NUMBER = "17";
  private static final Integer TEST_ISSUE_ID = 327;

  @InjectMocks private ComicVineScrapingAdaptor scrapingAdaptor;
  @Mock private ObjectFactory<ComicVineGetVolumesAction> getVolumesActionObjectFactory;
  @Mock private ComicVineGetVolumesAction getVolumesAction;
  @Mock private ObjectFactory<ComicVineGetIssueAction> getIssueActionObjectFactory;
  @Mock private ComicVineGetIssueAction getIssueAction;
  @Mock private ObjectFactory<ComicVineGetIssueDetailsAction> getIssueDetailsActionObjectFactory;
  @Mock private ComicVineGetIssueDetailsAction getIssueDetailsAction;
  @Mock private ScrapingVolume scrapingVolume;
  @Mock private ScrapingIssue scrapingIssue;
  @Mock private ScrapingIssueDetails scrapingIssueDetails;
  @Mock private MetadataSource metadataSource;

  private List<ScrapingVolume> scrapingVolumeList = new ArrayList<>();
  private List<ScrapingIssue> scrapingIssueList = new ArrayList<>();
  private List<String> entries = new ArrayList<>();
  private List<ScrapingIssueDetails> comicWithDetailsList = new ArrayList<>();
  private Set<MetadataSourceProperty> sourceProperties = new HashSet<>();

  @Before
  public void setUp() {
    sourceProperties.add(
        new MetadataSourceProperty(metadataSource, ComicVineScrapingAdaptor.API_KEY, TEST_API_KEY));
  }

  @Test(expected = ScrapingException.class)
  public void testGetVolumesMissingApiKey() throws ScrapingException {
    sourceProperties.clear();
    Mockito.when(getVolumesActionObjectFactory.getObject()).thenReturn(getVolumesAction);

    try {
      scrapingAdaptor.getVolumes(TEST_SERIES_NAME, TEST_MAX_RECORDS, sourceProperties);
    } finally {
      Mockito.verify(getVolumesAction, Mockito.times(1))
          .setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    }
  }

  @Test(expected = ScrapingException.class)
  public void testGetVolumesUnsetApiKey() throws ScrapingException {
    sourceProperties.stream()
        .filter(property -> property.getName().equals(ComicVineScrapingAdaptor.API_KEY))
        .findFirst()
        .get()
        .setValue("");
    Mockito.when(getVolumesActionObjectFactory.getObject()).thenReturn(getVolumesAction);

    try {
      scrapingAdaptor.getVolumes(TEST_SERIES_NAME, TEST_MAX_RECORDS, sourceProperties);
    } finally {
      Mockito.verify(getVolumesAction, Mockito.times(1))
          .setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    }
  }

  @Test
  public void testGetVolumesNoResults() throws ScrapingException, JsonProcessingException {
    Mockito.when(getVolumesActionObjectFactory.getObject()).thenReturn(getVolumesAction);
    Mockito.when(getVolumesAction.execute()).thenReturn(scrapingVolumeList);

    final List<ScrapingVolume> result =
        scrapingAdaptor.getVolumes(TEST_SERIES_NAME, TEST_MAX_RECORDS, sourceProperties);

    assertNotNull(result);
    assertTrue(result.isEmpty());

    Mockito.verify(getVolumesAction, Mockito.times(1))
        .setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    Mockito.verify(getVolumesAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getVolumesAction, Mockito.times(1)).setSeries(TEST_SERIES_NAME);
    Mockito.verify(getVolumesAction, Mockito.times(1)).setMaxRecords(TEST_MAX_RECORDS);
  }

  @Test
  public void testGetVolumes() throws ScrapingException, JsonProcessingException {
    for (int index = 0; index < 200; index++) scrapingVolumeList.add(scrapingVolume);

    Mockito.when(getVolumesActionObjectFactory.getObject()).thenReturn(getVolumesAction);
    Mockito.when(getVolumesAction.execute()).thenReturn(scrapingVolumeList);

    final List<ScrapingVolume> result =
        scrapingAdaptor.getVolumes(TEST_SERIES_NAME, TEST_MAX_RECORDS, sourceProperties);

    assertNotNull(result);
    assertEquals(scrapingVolumeList.size(), result.size());

    Mockito.verify(getVolumesAction, Mockito.times(1))
        .setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    Mockito.verify(getVolumesAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getVolumesAction, Mockito.times(1)).setSeries(TEST_SERIES_NAME);
    Mockito.verify(getVolumesAction, Mockito.times(1)).setMaxRecords(TEST_MAX_RECORDS);
  }

  @Test
  public void testGetIssueNoResults() throws ScrapingException, JsonProcessingException {
    Mockito.when(getIssueActionObjectFactory.getObject()).thenReturn(getIssueAction);
    Mockito.when(getIssueAction.execute()).thenReturn(scrapingIssueList);

    final ScrapingIssue result =
        scrapingAdaptor.getIssue(TEST_VOLUME_ID, TEST_ISSUE_NUMBER, sourceProperties);

    assertNull(result);

    Mockito.verify(getIssueAction, Mockito.times(1)).setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    Mockito.verify(getIssueAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getIssueAction, Mockito.times(1)).setVolumeId(TEST_VOLUME_ID);
  }

  @Test
  public void testGetIssue() throws ScrapingException, JsonProcessingException {
    scrapingIssueList.add(scrapingIssue);

    Mockito.when(getIssueActionObjectFactory.getObject()).thenReturn(getIssueAction);
    Mockito.when(getIssueAction.execute()).thenReturn(scrapingIssueList);

    final ScrapingIssue result =
        scrapingAdaptor.getIssue(TEST_VOLUME_ID, TEST_ISSUE_NUMBER, sourceProperties);

    assertNotNull(result);
    assertSame(scrapingIssue, result);

    Mockito.verify(getIssueAction, Mockito.times(1)).setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    Mockito.verify(getIssueAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getIssueAction, Mockito.times(1)).setVolumeId(TEST_VOLUME_ID);
    Mockito.verify(getIssueAction, Mockito.times(1)).setIssueNumber(TEST_ISSUE_NUMBER);
  }

  @Test
  public void testGetIssueWhenIssueNumberHasLeadingZeroes()
      throws ScrapingException, JsonProcessingException {
    scrapingIssueList.add(scrapingIssue);

    Mockito.when(getIssueActionObjectFactory.getObject()).thenReturn(getIssueAction);
    Mockito.when(getIssueAction.execute()).thenReturn(scrapingIssueList);

    final ScrapingIssue result =
        scrapingAdaptor.getIssue(TEST_VOLUME_ID, "0000" + TEST_ISSUE_NUMBER, sourceProperties);

    assertNotNull(result);
    assertSame(scrapingIssue, result);

    Mockito.verify(getIssueAction, Mockito.times(1)).setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    Mockito.verify(getIssueAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getIssueAction, Mockito.times(1)).setVolumeId(TEST_VOLUME_ID);
    Mockito.verify(getIssueAction, Mockito.times(1)).setIssueNumber(TEST_ISSUE_NUMBER);
  }

  @Test
  public void testGetIssueWhenIssueNumberIsZero()
      throws ScrapingException, JsonProcessingException {
    scrapingIssueList.add(scrapingIssue);

    Mockito.when(getIssueActionObjectFactory.getObject()).thenReturn(getIssueAction);
    Mockito.when(getIssueAction.execute()).thenReturn(scrapingIssueList);

    final ScrapingIssue result = scrapingAdaptor.getIssue(TEST_VOLUME_ID, "000", sourceProperties);

    assertNotNull(result);
    assertSame(scrapingIssue, result);

    Mockito.verify(getIssueAction, Mockito.times(1)).setBaseUrl(ComicVineScrapingAdaptor.BASE_URL);
    Mockito.verify(getIssueAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getIssueAction, Mockito.times(1)).setVolumeId(TEST_VOLUME_ID);
    Mockito.verify(getIssueAction, Mockito.times(1)).setIssueNumber("0");
  }

  @Test
  public void testGetIssueDetailsNoResults() throws ScrapingException {
    Mockito.when(getIssueDetailsActionObjectFactory.getObject()).thenReturn(getIssueDetailsAction);
    Mockito.when(getIssueDetailsAction.execute()).thenReturn(scrapingIssueDetails);

    final ScrapingIssueDetails result =
        scrapingAdaptor.getIssueDetails(TEST_ISSUE_ID, sourceProperties);

    assertNotNull(result);
    assertSame(scrapingIssueDetails, result);

    Mockito.verify(getIssueDetailsAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getIssueDetailsAction, Mockito.times(1)).setIssueId(TEST_ISSUE_ID);
  }

  @Test
  public void testGetIssueDetails() throws ScrapingException {
    Mockito.when(getIssueDetailsActionObjectFactory.getObject()).thenReturn(getIssueDetailsAction);
    Mockito.when(getIssueDetailsAction.execute()).thenReturn(scrapingIssueDetails);

    final ScrapingIssueDetails result =
        scrapingAdaptor.getIssueDetails(TEST_ISSUE_ID, sourceProperties);

    assertNotNull(result);
    assertSame(scrapingIssueDetails, result);

    Mockito.verify(getIssueDetailsAction, Mockito.times(1)).setApiKey(TEST_API_KEY);
    Mockito.verify(getIssueDetailsAction, Mockito.times(1)).setIssueId(TEST_ISSUE_ID);
  }
}
