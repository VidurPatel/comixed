/*
 * ComiXed - A digital comic book library management application.
 * Copyright (C) 2019, The ComiXed Project
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

package org.comixedproject.rest.library;

import static org.comixedproject.batch.comicbooks.ConsolidationConfiguration.PARAM_CONSOLIDATION_JOB_STARTED;
import static org.comixedproject.batch.comicbooks.ConsolidationConfiguration.PARAM_DELETE_REMOVED_COMIC_FILES;
import static org.comixedproject.batch.comicbooks.ConsolidationConfiguration.PARAM_RENAMING_RULE;
import static org.comixedproject.batch.comicbooks.ConsolidationConfiguration.PARAM_TARGET_DIRECTORY;
import static org.comixedproject.batch.comicbooks.PurgeLibraryConfiguration.JOB_PURGE_LIBRARY_START;
import static org.comixedproject.batch.comicbooks.RecreateComicFilesConfiguration.JOB_DELETE_MARKED_PAGES;
import static org.comixedproject.batch.comicbooks.RecreateComicFilesConfiguration.JOB_RECREATE_COMICS_STARTED;
import static org.comixedproject.batch.comicbooks.RecreateComicFilesConfiguration.JOB_TARGET_ARCHIVE;
import static org.comixedproject.batch.comicbooks.UpdateComicBooksConfiguration.*;
import static org.comixedproject.rest.comicbooks.ComicBookSelectionController.LIBRARY_SELECTIONS;

import com.fasterxml.jackson.annotation.JsonView;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.comixedproject.batch.comicbooks.ProcessComicsConfiguration;
import org.comixedproject.batch.comicbooks.UpdateMetadataConfiguration;
import org.comixedproject.model.archives.ArchiveType;
import org.comixedproject.model.comicbooks.ComicBook;
import org.comixedproject.model.comicbooks.ComicDetail;
import org.comixedproject.model.net.admin.ClearImageCacheResponse;
import org.comixedproject.model.net.comicbooks.ConvertComicsRequest;
import org.comixedproject.model.net.comicbooks.EditMultipleComicsRequest;
import org.comixedproject.model.net.library.ConsolidateLibraryRequest;
import org.comixedproject.model.net.library.LoadLibraryRequest;
import org.comixedproject.model.net.library.LoadLibraryResponse;
import org.comixedproject.model.net.library.PurgeLibraryRequest;
import org.comixedproject.model.net.library.RemoteLibraryState;
import org.comixedproject.service.admin.ConfigurationService;
import org.comixedproject.service.comicbooks.*;
import org.comixedproject.service.library.LibraryException;
import org.comixedproject.service.library.LibraryService;
import org.comixedproject.service.library.RemoteLibraryStateService;
import org.comixedproject.views.View;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <code>LibraryController</code> provides REST APIs for working with groups of {@link ComicBook}
 * objects.
 *
 * @author Darryl L. Pierce
 */
@RestController
@Log4j2
public class LibraryController {
  @Autowired private LibraryService libraryService;
  @Autowired private RemoteLibraryStateService remoteLibraryStateService;
  @Autowired private ComicBookService comicBookService;
  @Autowired private ComicDetailService comicDetailService;
  @Autowired private ConfigurationService configurationService;
  @Autowired private ComicBookSelectionService comicBookSelectionService;

  @Autowired
  @Qualifier("batchJobLauncher")
  private JobLauncher jobLauncher;

  @Autowired
  @Qualifier("processComicsJob")
  private Job processComicsJob;

  @Autowired
  @Qualifier("updateMetadataJob")
  private Job updateMetadataJob;

  @Autowired
  @Qualifier("consolidateLibraryJob")
  private Job consolidateLibraryJob;

  @Autowired
  @Qualifier("recreateComicFilesJob")
  private Job recreateComicFilesJob;

  @Autowired
  @Qualifier("purgeLibraryJob")
  private Job purgeLibraryJob;

  @Autowired
  @Qualifier("updateComicBooksJob")
  private Job updateComicBooksJob;

  /**
   * Retrieves the current state of the library.
   *
   * @return the library state
   */
  @GetMapping(value = "/api/library/state", produces = MediaType.APPLICATION_JSON_VALUE)
  @Timed(value = "comixed.library.get-state")
  @JsonView(View.RemoteLibraryState.class)
  public RemoteLibraryState getLibraryState() {
    log.info("Loading the current library state");
    return this.remoteLibraryStateService.getLibraryState();
  }

  /**
   * Prepares comics to have their underlying file recreated.
   *
   * @param request the request body
   * @param comicBookId the comic book id
   * @throws LibraryException if an error occurs
   */
  @PutMapping(
      value = "/api/library/conversion/{comicBookId}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Timed(value = "comixed.library.convert-single-comic-book")
  @PreAuthorize("hasRole('ADMIN')")
  public void convertSingleComicBooks(
      @RequestBody() final ConvertComicsRequest request,
      @PathVariable("comicBookId") final long comicBookId)
      throws LibraryException {
    if (this.configurationService.isFeatureEnabled(
        ConfigurationService.CFG_LIBRARY_NO_RECREATE_COMICS)) {
      throw new LibraryException("Recreating comic files is currently disabled");
    }

    final ArchiveType archiveType = request.getArchiveType();
    final boolean renamePages = request.isRenamePages();
    final boolean deletePages = request.isDeletePages();

    log.info(
        "Converting single comic book: target={} delete pages={} rename pages={}",
        comicBookId,
        archiveType,
        renamePages,
        deletePages);

    log.trace("Preparing to recreate comic book file");
    this.libraryService.prepareToRecreateComicBook(comicBookId);

    this.doStartConversionBatchProcess(archiveType, deletePages);
  }

  /**
   * Prepares comics to have their underlying file recreated.
   *
   * @param session the session
   * @param request the request body
   * @throws LibraryException if an error occurs
   */
  @PutMapping(
      value = "/api/library/conversion/selected",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Timed(value = "comixed.library.convert-selected-comic-books")
  @PreAuthorize("hasRole('ADMIN')")
  public void convertSelectedComicBooks(
      final HttpSession session, @RequestBody() final ConvertComicsRequest request)
      throws LibraryException {
    if (this.configurationService.isFeatureEnabled(
        ConfigurationService.CFG_LIBRARY_NO_RECREATE_COMICS)) {
      throw new LibraryException("Recreating comic files is currently disabled");
    }

    log.trace("Loading comic book selections");
    try {
      final List<Long> idList =
          this.comicBookSelectionService.decodeSelections(session.getAttribute(LIBRARY_SELECTIONS));
      final ArchiveType archiveType = request.getArchiveType();
      final boolean renamePages = request.isRenamePages();
      final boolean deletePages = request.isDeletePages();

      log.info(
          "Converting comic{}: target={} delete pages={} rename pages={}",
          idList.size() == 1 ? "" : "s",
          archiveType,
          renamePages,
          deletePages);

      log.trace("Preparing to recreate comic files");
      this.libraryService.prepareToRecreateComicBooks(idList);

      this.doStartConversionBatchProcess(archiveType, deletePages);

      log.trace("Clearing comic book selections");
      this.comicBookSelectionService.clearSelectedComicBooks(idList);
      log.trace("Saving comic book selections");
      session.setAttribute(
          LIBRARY_SELECTIONS, this.comicBookSelectionService.encodeSelections(idList));
    } catch (ComicBookSelectionException error) {
      throw new LibraryException("Failed to start converting selected comic books", error);
    }
  }

  private void doStartConversionBatchProcess(
      final ArchiveType archiveType, final boolean deletePages) throws LibraryException {
    log.trace("Starting comic book conversion batch process");
    try {
      this.jobLauncher.run(
          recreateComicFilesJob,
          new JobParametersBuilder()
              .addLong(JOB_RECREATE_COMICS_STARTED, System.currentTimeMillis())
              .addString(JOB_TARGET_ARCHIVE, archiveType.getName())
              .addString(JOB_DELETE_MARKED_PAGES, String.valueOf(deletePages))
              .toJobParameters());
    } catch (JobExecutionAlreadyRunningException
        | JobRestartException
        | JobInstanceAlreadyCompleteException
        | JobParametersInvalidException error) {
      throw new LibraryException("Failed to start comic conversion batch process", error);
    }
  }

  /**
   * Initiates the library consolidation process.
   *
   * @param session the session
   * @param request the request body
   * @throws Exception if an error occurs
   */
  @PostMapping(
      value = "/api/library/consolidate",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Timed(value = "comixed.library.consolidate")
  public void consolidateLibrary(
      final HttpSession session, @RequestBody() ConsolidateLibraryRequest request)
      throws Exception {
    final boolean deleteRemovedComicFiles = request.getDeletePhysicalFiles();
    final List<Long> selectedIds =
        this.comicBookSelectionService.decodeSelections(session.getAttribute(LIBRARY_SELECTIONS));
    log.info(
        "Consolidating library: count={} delete physic files={}",
        selectedIds.size(),
        deleteRemovedComicFiles);
    log.trace("Loading target directory");
    final String targetDirectory =
        this.configurationService.getOptionValue(ConfigurationService.CFG_LIBRARY_ROOT_DIRECTORY);
    log.trace("Loading renaming rule");
    final String renamingRule =
        this.configurationService.getOptionValue(
            ConfigurationService.CFG_LIBRARY_COMIC_RENAMING_RULE);
    this.libraryService.prepareForConsolidation(selectedIds, targetDirectory);
    log.trace("Launch consolidation batch process");
    this.jobLauncher.run(
        this.consolidateLibraryJob,
        new JobParametersBuilder()
            .addLong(PARAM_CONSOLIDATION_JOB_STARTED, System.currentTimeMillis())
            .addString(PARAM_DELETE_REMOVED_COMIC_FILES, String.valueOf(deleteRemovedComicFiles))
            .addString(PARAM_TARGET_DIRECTORY, targetDirectory)
            .addString(PARAM_RENAMING_RULE, renamingRule)
            .toJobParameters());
    log.debug("Clearing comic book selections");
    this.comicBookSelectionService.clearSelectedComicBooks(selectedIds);
    log.debug("Deleting selections from session");
    session.setAttribute(
        LIBRARY_SELECTIONS, this.comicBookSelectionService.encodeSelections(selectedIds));
  }

  /**
   * Clears the library image cache.
   *
   * @return the response
   */
  @DeleteMapping(value = "/api/library/cache/images")
  @Timed(value = "comixed.library.image-cache.clear")
  public ClearImageCacheResponse clearImageCache() {
    log.info("Clearing the image cache");

    try {
      this.libraryService.clearImageCache();
    } catch (LibraryException error) {
      log.error("failed to clear image cache", error);
      return new ClearImageCacheResponse(false);
    }

    return new ClearImageCacheResponse(true);
  }

  /**
   * Loads a batch of comics during the initial startup process.
   *
   * @param request the request
   * @return the response
   */
  @PostMapping(
      value = "/api/library",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('READER')")
  @Timed(value = "comixed.library.load")
  @JsonView(View.ComicListView.class)
  public LoadLibraryResponse loadLibrary(@RequestBody() final LoadLibraryRequest request) {
    final Integer maxRecords = request.getMaxRecords();
    final Long lastId = request.getLastId();
    log.info("Loading library content: max records={} last id was {}", maxRecords, lastId);

    List<ComicDetail> comicBooks = this.comicDetailService.loadById(lastId, maxRecords + 1);
    boolean lastPayload = true;
    if (comicBooks.size() > maxRecords) {
      comicBooks = comicBooks.subList(0, maxRecords);
      lastPayload = false;
    }

    return new LoadLibraryResponse(
        comicBooks,
        comicBooks.isEmpty()
            ? 0
            : comicBooks.stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList()
                .get(comicBooks.size() - 1)
                .getId(),
        lastPayload);
  }

  /**
   * Initiates the rescan process for a single comic book.
   *
   * @param comicBookId the comic book id
   * @throws Exception if an error occurs
   */
  @PutMapping(
      value = "/api/library/rescan/{comicBookId}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @Timed(value = "comixed.library.batch.rescan-single")
  public void rescanSingleComicBook(@PathVariable("comicBookId") final long comicBookId)
      throws Exception {
    log.info("Rescanning single comic book: id={}", comicBookId);
    this.comicBookService.prepareForRescan(comicBookId);
    log.trace("Initiating a rescan batch process");
    this.jobLauncher.run(
        processComicsJob,
        new JobParametersBuilder()
            .addLong(ProcessComicsConfiguration.JOB_RESCAN_COMICS_START, System.currentTimeMillis())
            .toJobParameters());
  }

  /**
   * Initiates the rescan process for the selected comic books.
   *
   * @param session the session
   * @throws Exception if an error occurs
   */
  @PutMapping(value = "/api/library/rescan/selected", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @Timed(value = "comixed.library.batch.rescan-selected")
  public void rescanSelectedComicBooks(final HttpSession session) throws Exception {
    final List selectedIdList =
        this.comicBookSelectionService.decodeSelections(session.getAttribute(LIBRARY_SELECTIONS));
    log.info("Rescanning selected comic books");
    this.comicBookService.prepareForRescan(selectedIdList);
    this.comicBookSelectionService.clearSelectedComicBooks(selectedIdList);
    session.setAttribute(
        LIBRARY_SELECTIONS, this.comicBookSelectionService.encodeSelections(selectedIdList));
    log.trace("Launching rescan batch process");
    this.jobLauncher.run(
        processComicsJob,
        new JobParametersBuilder()
            .addLong(ProcessComicsConfiguration.JOB_RESCAN_COMICS_START, System.currentTimeMillis())
            .toJobParameters());
  }

  /**
   * Starts the metadata update process for a single comic book.
   *
   * @param comicBookId the comic book id
   * @throws Exception if an error occurs
   */
  @PutMapping(value = "/api/library/metadata/update/{comicBookId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Timed(value = "comixed.library.batch.metadata-update-selected-comic-books")
  public void updateSingleComicBookMetadata(@PathVariable("comicBookId") final long comicBookId)
      throws Exception {
    log.info("Updating the metadata a single comic book: id={}", comicBookId);
    this.comicBookService.prepareForMetadataUpdate(comicBookId);
    log.trace("Launching rescan batch process");
    this.jobLauncher.run(
        this.updateMetadataJob,
        new JobParametersBuilder()
            .addLong(
                UpdateMetadataConfiguration.JOB_UPDATE_METADATA_STARTED, System.currentTimeMillis())
            .toJobParameters());
  }

  /**
   * Starts the metadata update process for the selected comic books.
   *
   * @param session the session
   * @throws Exception if an error occurs
   */
  @PutMapping(value = "/api/library/metadata/update/selected")
  @PreAuthorize("hasRole('ADMIN')")
  @Timed(value = "comixed.library.batch.metadata-update-selected-comic-books")
  public void updateSelectedComicBooksMetadata(final HttpSession session) throws Exception {
    final List<Long> selectedComicBookIds =
        this.comicBookSelectionService.decodeSelections(session.getAttribute(LIBRARY_SELECTIONS));
    log.info(
        "Updating the metadata for {} comic{}",
        selectedComicBookIds.size(),
        selectedComicBookIds.size() == 1 ? "" : "s");
    this.libraryService.updateMetadata(selectedComicBookIds);
    this.comicBookSelectionService.clearSelectedComicBooks(selectedComicBookIds);
    session.setAttribute(
        LIBRARY_SELECTIONS, this.comicBookSelectionService.encodeSelections(selectedComicBookIds));
    log.trace("Launching batch process");
    this.jobLauncher.run(
        this.updateMetadataJob,
        new JobParametersBuilder()
            .addLong(
                UpdateMetadataConfiguration.JOB_UPDATE_METADATA_STARTED, System.currentTimeMillis())
            .toJobParameters());
  }

  /**
   * Purges deleted comics from the library.
   *
   * @param request the request body
   * @throws Exception if an error occurs
   */
  @PostMapping(value = "/api/library/purge", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @Timed(value = "comixed.library.batch.purge")
  public void purgeLibrary(@RequestBody() final PurgeLibraryRequest request) throws Exception {
    log.info("Purging comics marked for deletion");
    this.libraryService.prepareForPurging();
    log.trace("Launching batch process");
    this.jobLauncher.run(
        this.purgeLibraryJob,
        new JobParametersBuilder()
            .addLong(JOB_PURGE_LIBRARY_START, System.currentTimeMillis())
            .toJobParameters());
  }

  /**
   * Updates a set of comics with the provided details.
   *
   * @param request the request body
   * @throws ComicBookException if an error occurs
   */
  @PostMapping(value = "/api/library/comics/edit", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @Timed(value = "comixed.library.edit-comics")
  public void editMultipleComics(@RequestBody() final EditMultipleComicsRequest request)
      throws Exception {
    final List<Long> ids = request.getIds();
    log.info("Preparing to update details for {} comic{}", ids.size(), ids.size() == 1 ? "" : "s");
    this.comicBookService.updateMultipleComics(ids);
    log.trace("Launching update comics batch process");
    this.jobLauncher.run(
        this.updateComicBooksJob,
        new JobParametersBuilder()
            .addLong(JOB_UPDATE_COMICBOOKS_STARTED, System.currentTimeMillis())
            .addString(JOB_UPDATE_COMICBOOKS_PUBLISHER, request.getPublisher())
            .addString(JOB_UPDATE_COMICBOOKS_SERIES, request.getSeries())
            .addString(JOB_UPDATE_COMICBOOKS_VOLUME, request.getVolume())
            .addString(JOB_UPDATE_COMICBOOKS_ISSUENO, request.getIssueNumber())
            .addString(JOB_UPDATE_COMICBOOKS_IMPRINT, request.getImprint())
            .addString(
                JOB_UPDATE_COMICBOOKS_COMIC_TYPE,
                request.getComicType() != null ? request.getComicType().name() : "")
            .toJobParameters());
  }
}
