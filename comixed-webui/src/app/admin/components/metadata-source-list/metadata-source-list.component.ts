/*
 * ComiXed - A digital comic book library management application.
 * Copyright (C) 2022, The ComiXed Project
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

import {
  AfterViewInit,
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { Subscription } from 'rxjs';
import { MetadataSource } from '@app/comic-metadata/models/metadata-source';
import { LoggerService } from '@angular-ru/cdk/logger';
import { Store } from '@ngrx/store';
import {
  loadMetadataSources,
  preferMetadataSource
} from '@app/comic-metadata/actions/metadata-source-list.actions';
import { selectMetadataSourceList } from '@app/comic-metadata/selectors/metadata-source-list.selectors';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { loadMetadataSource } from '@app/comic-metadata/actions/metadata-source.actions';
import { ConfirmationService } from '@tragically-slick/confirmation';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { saveConfigurationOptions } from '@app/admin/actions/save-configuration-options.actions';
import { METADATA_IGNORE_EMPTY_VALUES } from '@app/admin/admin.constants';
import {
  selectConfigurationOptionListState,
  selectConfigurationOptions
} from '@app/admin/selectors/configuration-option-list.selectors';
import { setBusyState } from '@app/core/actions/busy.actions';
import { getConfigurationOption } from '@app/admin';
import { ConfigurationOption } from '@app/admin/models/configuration-option';

@Component({
  selector: 'cx-metadata-source-list',
  templateUrl: './metadata-source-list.component.html',
  styleUrls: ['./metadata-source-list.component.scss']
})
export class MetadataSourceListComponent
  implements OnInit, OnDestroy, AfterViewInit
{
  @ViewChild(MatSort) sort: MatSort;

  @Output() createSource = new EventEmitter<void>();

  sourcesSubscription: Subscription;
  dataSource = new MatTableDataSource<MetadataSource>([]);
  readonly displayedColumns = [
    'preferred',
    'name',
    'bean-name',
    'property-count',
    'actions'
  ];
  metadataForm: FormGroup;
  configurationStateSubscription: Subscription;
  configurationOptionListSubscription: Subscription;
  showConfigPopup = false;
  configurationOptionList: ConfigurationOption[];

  constructor(
    private logger: LoggerService,
    private store: Store<any>,
    private confirmationService: ConfirmationService,
    private translateService: TranslateService,
    private formBuilder: FormBuilder
  ) {
    this.sourcesSubscription = this.store
      .select(selectMetadataSourceList)
      .subscribe(sources => (this.dataSource.data = sources));
    this.logger.debug('Creating metadata form');
    this.metadataForm = this.formBuilder.group({
      ignoreEmptyValues: ['']
    });
    this.configurationStateSubscription = this.store
      .select(selectConfigurationOptionListState)
      .subscribe(state => {
        this.store.dispatch(
          setBusyState({ enabled: state.loading || state.saving })
        );
      });
    this.configurationOptionListSubscription = this.store
      .select(selectConfigurationOptions)
      .subscribe(list => {
        this.configurationOptionList = list;
        this.loadMetadataForm();
      });
  }

  ngOnDestroy(): void {
    this.logger.debug('Unsubscribing from source list updates');
    this.sourcesSubscription.unsubscribe();
    this.logger.debug('Unsubscribing from configuration option state updates');
    this.configurationStateSubscription.unsubscribe();
    this.logger.debug('Unsubscribing from configuration option updates');
    this.configurationOptionListSubscription.unsubscribe();
  }

  ngOnInit(): void {
    this.logger.debug('Loading metadata source list');
    this.store.dispatch(loadMetadataSources());
  }

  ngAfterViewInit(): void {
    this.logger.debug('Adding table sorting');
    this.dataSource.sort = this.sort;
    this.dataSource.sortingDataAccessor = (data, sortHeaderId) => {
      switch (sortHeaderId) {
        case 'preferred':
          return data.preferred ? 1 : 0;
        case 'name':
          return data.name;
        case 'bean-name':
          return data.beanName;
      }
    };
  }

  onSelectSource(source: MetadataSource): void {
    this.logger.debug('Loading metadata source');
    this.store.dispatch(loadMetadataSource({ id: source.id }));
  }

  onMarkPreferred(id: number): void {
    this.confirmationService.confirm({
      title: this.translateService.instant(
        'metadata-source-list.mark-preferred.confirmation-title'
      ),
      message: this.translateService.instant(
        'metadata-source-list.mark-preferred.confirmation-message'
      ),
      confirm: () => {
        this.logger.debug('Marking metadata source as preferred:', id);
        this.store.dispatch(preferMetadataSource({ id }));
      }
    });
  }

  onSaveConfig(): void {
    this.logger.debug('Saving metadata configuration');
    const ignoreEmptyValues =
      this.metadataForm.controls.ignoreEmptyValues.value;
    this.store.dispatch(
      saveConfigurationOptions({
        options: [
          {
            name: METADATA_IGNORE_EMPTY_VALUES,
            value: `${ignoreEmptyValues}`
          }
        ]
      })
    );
    this.showConfigPopup = false;
  }

  onCancelConfig() {
    this.logger.debug('Canceling configuration changes');
    this.loadMetadataForm();
    this.showConfigPopup = false;
  }

  private loadMetadataForm(): void {
    this.metadataForm.controls.ignoreEmptyValues.setValue(
      getConfigurationOption(
        this.configurationOptionList,
        METADATA_IGNORE_EMPTY_VALUES,
        `${false}`
      ) === `${true}`
    );
  }
}
