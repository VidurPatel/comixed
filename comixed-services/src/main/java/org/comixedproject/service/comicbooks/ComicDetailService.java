/*
 * ComiXed - A digital comic book library management application.
 * Copyright (C) 2023, The ComiXed Project.
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

package org.comixedproject.service.comicbooks;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.comixedproject.model.comicbooks.ComicDetail;
import org.comixedproject.repositories.comicbooks.ComicDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * <code>ComicDetailService</code> provides methods for working with instances of {@link
 * ComicDetail}.
 *
 * @author Darryl L. Pierce
 */
@Service
@Log4j2
public class ComicDetailService {
  @Autowired private ComicDetailRepository comicDetailRepository;

  /**
   * Loads a set of comic details with ids greater than the last id.
   *
   * @param lastId the last id
   * @param maximum the maximum record
   * @return the list of comic details
   */
  public List<ComicDetail> loadById(final long lastId, final int maximum) {
    log.debug("Loading comic detail records: last id={} maximum={}", lastId, maximum);
    return this.comicDetailRepository.getWithIdGreaterThan(lastId, PageRequest.of(0, maximum));
  }
}
