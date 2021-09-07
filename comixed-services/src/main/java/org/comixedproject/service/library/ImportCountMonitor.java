/*
 * ComiXed - A digital comic book library management application.
 * Copyright (C) 2021, The ComiXed Project
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

package org.comixedproject.service.library;

import lombok.extern.log4j.Log4j2;
import org.comixedproject.messaging.PublishingException;
import org.comixedproject.messaging.library.PublishImportCountAction;
import org.comixedproject.model.comic.ComicState;
import org.comixedproject.model.state.messaging.ImportCountMessage;
import org.comixedproject.service.comic.ComicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Log4j2
public class ImportCountMonitor {
  @Autowired private ComicService comicService;
  @Autowired private PublishImportCountAction publishImportCountAction;

  private int addedCount = -1;
  private int importCount = -1;

  @Scheduled(fixedDelay = 1000)
  public void runMonitor() {
    log.trace("Getting added count");
    final int added = this.comicService.getCountForState(ComicState.ADDED);
    final int importing = this.comicService.getCountForState(ComicState.UNPROCESSED);

    if (added != this.addedCount || importing != this.importCount) {
      log.trace("Updating monitor values");
      this.addedCount = added;
      this.importCount = importing;
      log.trace("Publishing update: addedCount={} importCount={}", addedCount, importCount);
      try {
        this.publishImportCountAction.publish(
            new ImportCountMessage(this.addedCount, this.importCount));
      } catch (PublishingException error) {
        log.error("Failed to publish import update", error);
      }
    }
  }
}
