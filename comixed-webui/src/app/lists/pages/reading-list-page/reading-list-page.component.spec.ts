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

import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ReadingListPageComponent } from './reading-list-page.component';
import { LoggerModule } from '@angular-ru/cdk/logger';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  initialState as initialReadingListDetailsState,
  READING_LIST_DETAIL_FEATURE_KEY
} from '@app/lists/reducers/reading-list-detail.reducer';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import {
  createReadingList,
  loadReadingList,
  readingListLoaded,
  saveReadingList
} from '@app/lists/actions/reading-list-detail.actions';
import { READING_LIST_3 } from '@app/lists/lists.fixtures';
import { ComicListViewComponent } from '@app/lists/components/comic-list-view/comic-list-view.component';
import {
  COMIC_DETAIL_1,
  COMIC_DETAIL_3,
  COMIC_DETAIL_5
} from '@app/comic-books/comic-books.fixtures';
import { removeComicsFromReadingList } from '@app/lists/actions/reading-list-entries.actions';
import {
  initialState as initialMessagingState,
  MESSAGING_FEATURE_KEY
} from '@app/messaging/reducers/messaging.reducer';
import { WebSocketService } from '@app/messaging';
import {
  READING_LIST_REMOVAL_TOPIC,
  READING_LIST_UPDATES_TOPIC
} from '@app/lists/lists.constants';
import { interpolate } from '@app/core';
import {
  DOWNLOAD_READING_LIST_FEATURE_KEY,
  initialState as initialDownloadReadingListState
} from '@app/lists/reducers/download-reading-list.reducer';
import { Subscription as WebstompSubscription } from 'webstomp-client';
import { downloadReadingList } from '@app/lists/actions/download-reading-list.actions';
import { deleteReadingLists } from '@app/lists/actions/reading-lists.actions';
import { TitleService } from '@app/core/services/title.service';
import {
  Confirmation,
  ConfirmationService
} from '@tragically-slick/confirmation';

describe('ReadingListPageComponent', () => {
  const READING_LIST = READING_LIST_3;
  const COMICS = [COMIC_DETAIL_1, COMIC_DETAIL_3, COMIC_DETAIL_5];
  const initialState = {
    [READING_LIST_DETAIL_FEATURE_KEY]: initialReadingListDetailsState,
    [MESSAGING_FEATURE_KEY]: initialMessagingState,
    [DOWNLOAD_READING_LIST_FEATURE_KEY]: initialDownloadReadingListState
  };

  let component: ReadingListPageComponent;
  let fixture: ComponentFixture<ReadingListPageComponent>;
  let store: MockStore<any>;
  let activatedRoute: ActivatedRoute;
  let router: Router;
  let confirmationService: ConfirmationService;
  let webSocketService: jasmine.SpyObj<WebSocketService>;
  const updateSubscription = jasmine.createSpyObj(['unsubscribe']);
  updateSubscription.unsubscribe = jasmine.createSpy(
    'Subscription.unsubscribe(updates)'
  );
  const removalSubscription = jasmine.createSpyObj(['unsubscribe']);
  removalSubscription.unsubscribe = jasmine.createSpy(
    'Subscription.unsubscribe(removals)'
  );
  let titleService: TitleService;
  let translateService: TranslateService;

  beforeEach(
    waitForAsync(() => {
      TestBed.configureTestingModule({
        declarations: [ReadingListPageComponent, ComicListViewComponent],
        imports: [
          RouterTestingModule.withRoutes([{ path: '**', redirectTo: '' }]),
          FormsModule,
          ReactiveFormsModule,
          LoggerModule.forRoot(),
          TranslateModule.forRoot(),
          MatDialogModule,
          MatToolbarModule,
          MatIconModule,
          MatTooltipModule
        ],
        providers: [
          provideMockStore({ initialState }),
          {
            provide: ActivatedRoute,
            useValue: {
              params: new BehaviorSubject<{}>({})
            }
          },
          ConfirmationService,
          {
            provide: WebSocketService,
            useValue: {
              subscribe: jasmine.createSpy('WebSocketService.subscribe()'),
              unsubscribe: jasmine.createSpy('WebSocketService.unsubscribe()')
            }
          },
          TitleService
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(ReadingListPageComponent);
      component = fixture.componentInstance;
      store = TestBed.inject(MockStore);
      spyOn(store, 'dispatch');
      activatedRoute = TestBed.inject(ActivatedRoute);
      router = TestBed.inject(Router);
      spyOn(router, 'navigateByUrl');
      spyOn(router, 'navigate');
      confirmationService = TestBed.inject(ConfirmationService);
      webSocketService = TestBed.inject(
        WebSocketService
      ) as jasmine.SpyObj<WebSocketService>;
      titleService = TestBed.inject(TitleService);
      spyOn(titleService, 'setTitle');
      translateService = TestBed.inject(TranslateService);
      fixture.detectChanges();
    })
  );

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('when the language changes', () => {
    beforeEach(() => {
      component.readingList = READING_LIST;
      translateService.use('fr');
    });

    it('loads the title', () => {
      expect(titleService.setTitle).toHaveBeenCalledWith(jasmine.any(String));
    });
  });

  describe('when creating a new reading list', () => {
    beforeEach(() => {
      component.readingListId = 1;
      (activatedRoute.params as BehaviorSubject<{}>).next({});
    });

    it('sets the reading list id', () => {
      expect(component.readingListId).toEqual(-1);
    });

    it('fires an action', () => {
      expect(store.dispatch).toHaveBeenCalledWith(createReadingList());
    });
  });

  describe('when loading an existing reading list', () => {
    beforeEach(() => {
      component.readingListId = -1;
      (activatedRoute.params as BehaviorSubject<{}>).next({
        id: `${READING_LIST.id}`
      });
    });

    it('sets the reading list id', () => {
      expect(component.readingListId).toEqual(READING_LIST.id);
    });

    it('fires an action', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        loadReadingList({ id: READING_LIST.id })
      );
    });
  });

  describe('when the reading list was not found', () => {
    beforeEach(() => {
      store.setState({
        ...initialState,
        [READING_LIST_DETAIL_FEATURE_KEY]: {
          ...initialReadingListDetailsState,
          notFound: true
        }
      });
    });

    it('redirects the browser', () => {
      expect(router.navigateByUrl).toHaveBeenCalledWith('/lists/reading/all');
    });
  });

  describe('receiving the reading list', () => {
    describe('after saving a new reading list', () => {
      beforeEach(() => {
        component.readingListId = -1;
        store.setState({
          ...initialState,
          [READING_LIST_DETAIL_FEATURE_KEY]: {
            ...initialReadingListDetailsState,
            notFound: false,
            list: READING_LIST
          }
        });
      });

      it('redirects the browser', () => {
        expect(router.navigate).toHaveBeenCalledWith([
          '/lists',
          'reading',
          READING_LIST.id
        ]);
      });
    });

    describe('when loading an existing reading list', () => {
      beforeEach(() => {
        component.readingListId = READING_LIST.id;
        store.setState({
          ...initialState,
          [READING_LIST_DETAIL_FEATURE_KEY]: {
            ...initialReadingListDetailsState,
            notFound: false,
            list: READING_LIST
          }
        });
      });

      it('sets the reading list', () => {
        expect(component.readingList).toEqual(READING_LIST);
      });

      it('loads the name', () => {
        expect(component.readingListForm.controls.name.value).toEqual(
          READING_LIST.name
        );
      });

      it('loads the summary', () => {
        expect(component.readingListForm.controls.summary.value).toEqual(
          READING_LIST.summary
        );
      });
    });
  });

  describe('saving a reading list', () => {
    beforeEach(() => {
      component.readingList = READING_LIST;
      spyOn(confirmationService, 'confirm').and.callFake(
        (confirmation: Confirmation) => confirmation.confirm()
      );
      component.onSave();
    });

    it('confirms with the user', () => {
      expect(confirmationService.confirm).toHaveBeenCalled();
    });

    it('fires an action', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        saveReadingList({ list: READING_LIST })
      );
    });
  });

  describe('undoing changes to a reading list', () => {
    beforeEach(() => {
      component.readingList = READING_LIST;
      component.readingListForm.controls.name.setValue(
        READING_LIST.name.substr(1)
      );
      component.readingListForm.controls.summary.setValue(
        READING_LIST.summary.substr(1)
      );
      spyOn(confirmationService, 'confirm').and.callFake(
        (confirmation: Confirmation) => confirmation.confirm()
      );
      component.onReset();
    });

    it('confirms with the user', () => {
      expect(confirmationService.confirm).toHaveBeenCalled();
    });

    it('resets the name field', () => {
      expect(component.readingListForm.controls.name.value).toEqual(
        READING_LIST.name
      );
    });

    it('resets the summary field', () => {
      expect(component.readingListForm.controls.summary.value).toEqual(
        READING_LIST.summary
      );
    });
  });

  describe('setting selected entries', () => {
    beforeEach(() => {
      component.onSelectionChanged(COMICS);
    });

    it('stores the selection', () => {
      expect(component.selectedEntries).toEqual(COMICS);
    });
  });

  describe('removing selected entries', () => {
    beforeEach(() => {
      component.readingList = READING_LIST;
      component.selectedEntries = COMICS;
      spyOn(confirmationService, 'confirm').and.callFake(
        (confirmation: Confirmation) => confirmation.confirm()
      );
      component.onRemoveEntries();
    });

    it('confirms with the user', () => {
      expect(confirmationService.confirm).toHaveBeenCalled();
    });

    it('fires an action', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        removeComicsFromReadingList({
          list: READING_LIST,
          comicBooks: COMICS
        })
      );
    });
  });

  describe('when messaging is started', () => {
    let readingListRemovalSubscription: any;

    beforeEach(() => {
      component.readingListId = READING_LIST.id;
      component.readingListUpdateSubscription = null;
      component.readingListRemovalSubscription = null;
      webSocketService.subscribe
        .withArgs(
          interpolate(READING_LIST_UPDATES_TOPIC, { id: READING_LIST.id }),
          jasmine.anything()
        )
        .and.callFake((topic, callback) => {
          callback(READING_LIST);
          return {} as WebstompSubscription;
        });
      webSocketService.subscribe
        .withArgs(READING_LIST_REMOVAL_TOPIC, jasmine.anything())
        .and.callFake((topic, callback) => {
          readingListRemovalSubscription = callback;
          return {} as WebstompSubscription;
        });
      store.setState({
        ...initialState,
        [MESSAGING_FEATURE_KEY]: { ...initialMessagingState, started: true }
      });
    });

    it('subscribes to reading list update topic', () => {
      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        interpolate(READING_LIST_UPDATES_TOPIC, { id: READING_LIST.id }),
        jasmine.anything()
      );
    });

    it('subscribes to reading list removal topic', () => {
      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        READING_LIST_REMOVAL_TOPIC,
        jasmine.anything()
      );
    });

    it('processes reading list updates', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        readingListLoaded({ list: READING_LIST })
      );
    });

    it('redirects the browser when the current list was removed', () => {
      readingListRemovalSubscription(READING_LIST);
      expect(router.navigateByUrl).toHaveBeenCalledWith('/lists/reading/all');
    });

    it('ignores when a different list was removed', () => {
      readingListRemovalSubscription({
        ...READING_LIST,
        id: READING_LIST.id + 1
      });
      expect(router.navigateByUrl).not.toHaveBeenCalled();
    });
  });

  describe('downloading the reading list', () => {
    beforeEach(() => {
      component.readingList = READING_LIST;
      component.onDownload();
    });

    it('fires an action', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        downloadReadingList({ list: READING_LIST })
      );
    });
  });

  describe('delete reading list', () => {
    beforeEach(() => {
      component.readingList = READING_LIST;
      spyOn(confirmationService, 'confirm').and.callFake(
        (confirmation: Confirmation) => confirmation.confirm()
      );
      component.onDeleteReadingList();
    });

    it('confirms with the user', () => {
      expect(confirmationService.confirm).toHaveBeenCalled();
    });

    it('fires an action', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        deleteReadingLists({ lists: [READING_LIST] })
      );
    });
  });
});
