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

import { TestBed } from '@angular/core/testing';

import { LastReadService } from './last-read.service';
import {
  COMIC_DETAIL_4,
  LAST_READ_4
} from '@app/comic-books/comic-books.fixtures';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { interpolate } from '@app/core';
import {
  LAST_READ_REMOVED_TOPIC,
  LAST_READ_UPDATED_TOPIC,
  LOAD_UNREAD_COMIC_BOOK_COUNT_URL,
  SET_COMIC_BOOK_READ_STATE_URL,
  SET_SELECTED_COMIC_BOOKS_READ_STATE_URL
} from '@app/comic-books/comic-books.constants';
import { LoggerModule } from '@angular-ru/cdk/logger';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { WebSocketService } from '@app/messaging';
import {
  initialState as initialMessagingState,
  MESSAGING_FEATURE_KEY
} from '@app/messaging/reducers/messaging.reducer';
import {
  lastReadDateRemoved,
  lastReadDateUpdated,
  loadUnreadComicBookCount
} from '@app/comic-books/actions/last-read-list.actions';
import {
  initialState as initialLastReadListState,
  LAST_READ_LIST_FEATURE_KEY
} from '@app/comic-books/reducers/last-read-list.reducer';
import { HttpResponse } from '@angular/common/http';
import { LoadUnreadComicBookCountResponse } from '@app/comic-books/models/net/load-unread-comic-book-count-response';

describe('LastReadService', () => {
  const COMIC = COMIC_DETAIL_4;
  const READ_COUNT = Math.floor(Math.random() * 30000);
  const UNREAD_COUNT = Math.floor(Math.random() * 30000);
  const initialState = {
    [MESSAGING_FEATURE_KEY]: initialMessagingState,
    [LAST_READ_LIST_FEATURE_KEY]: initialLastReadListState
  };

  let service: LastReadService;
  let httpMock: HttpTestingController;
  let store: MockStore<any>;
  let webSocketService: jasmine.SpyObj<WebSocketService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, LoggerModule.forRoot()],
      providers: [
        provideMockStore({ initialState }),
        {
          provide: WebSocketService,
          useValue: {
            subscribe: jasmine.createSpy('WebSocketService.subscribe()')
          }
        }
      ]
    });

    service = TestBed.inject(LastReadService);
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(MockStore);
    spyOn(store, 'dispatch');
    webSocketService = TestBed.inject(
      WebSocketService
    ) as jasmine.SpyObj<WebSocketService>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('loads the unread comic book count', () => {
    const serverResponse = {
      readCount: READ_COUNT,
      unreadCount: UNREAD_COUNT
    } as LoadUnreadComicBookCountResponse;

    service
      .loadUnreadComicBookCount()
      .subscribe(response => expect(response).toEqual(serverResponse));

    const req = httpMock.expectOne(
      interpolate(LOAD_UNREAD_COMIC_BOOK_COUNT_URL)
    );
    expect(req.request.method).toEqual('GET');
    req.flush(serverResponse);
  });

  describe('marking a single comic book', () => {
    it('marks them as read', () => {
      service
        .setSingleReadState({ comicBookId: COMIC.comicId, read: true })
        .subscribe(response => expect(response.status).toEqual(200));

      const req = httpMock.expectOne(
        interpolate(SET_COMIC_BOOK_READ_STATE_URL, {
          comicBookId: COMIC.comicId
        })
      );
      expect(req.request.method).toEqual('PUT');
      req.flush(new HttpResponse({ status: 200 }));
    });

    it('marks them as unread', () => {
      service
        .setSingleReadState({ comicBookId: COMIC.comicId, read: false })
        .subscribe(response => expect(response.status).toEqual(200));

      const req = httpMock.expectOne(
        interpolate(SET_COMIC_BOOK_READ_STATE_URL, {
          comicBookId: COMIC.comicId
        })
      );
      expect(req.request.method).toEqual('DELETE');
      req.flush(new HttpResponse({ status: 200 }));
    });
  });

  describe('marking selected comic books', () => {
    it('marks them as read', () => {
      service
        .setSelectedReadState({ read: true })
        .subscribe(response => expect(response.status).toEqual(200));

      const req = httpMock.expectOne(
        interpolate(SET_SELECTED_COMIC_BOOKS_READ_STATE_URL)
      );
      expect(req.request.method).toEqual('PUT');
      req.flush(new HttpResponse({ status: 200 }));
    });

    it('marks them as unread', () => {
      service
        .setSelectedReadState({ read: false })
        .subscribe(response => expect(response.status).toEqual(200));

      const req = httpMock.expectOne(
        interpolate(SET_SELECTED_COMIC_BOOKS_READ_STATE_URL)
      );
      expect(req.request.method).toEqual('DELETE');
      req.flush(new HttpResponse({ status: 200 }));
    });
  });

  describe('when messaging starts', () => {
    const ENTRY = LAST_READ_4;

    beforeEach(() => {
      service.updateSubscription = null;
      service.removeSubscription = null;
      webSocketService.subscribe
        .withArgs(LAST_READ_UPDATED_TOPIC, jasmine.anything())
        .and.callFake((topic, callback) => {
          callback(ENTRY);
          return {
            unsubscribe: jasmine.createSpy('Subscription.unsubscribe()')
          } as any;
        });
      webSocketService.subscribe
        .withArgs(LAST_READ_REMOVED_TOPIC, jasmine.anything())
        .and.callFake((topic, callback) => {
          callback(ENTRY);
          return {
            unsubscribe: jasmine.createSpy('Subscription.unsubscribe()')
          } as any;
        });
      store.setState({
        ...initialState,
        [MESSAGING_FEATURE_KEY]: {
          ...initialMessagingState,
          started: true
        },
        [LAST_READ_LIST_FEATURE_KEY]: {
          ...initialLastReadListState,
          loading: false
        }
      });
    });

    it('requests the unread comic book count', () => {
      expect(store.dispatch).toHaveBeenCalledWith(loadUnreadComicBookCount());
    });

    it('subscribes to updates', () => {
      expect(service.updateSubscription).not.toBeNull();
    });

    it('processes updates', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        lastReadDateUpdated({ entry: ENTRY })
      );
    });

    it('subscribes to removals', () => {
      expect(service.removeSubscription).not.toBeNull();
    });

    it('processes removals', () => {
      expect(store.dispatch).toHaveBeenCalledWith(
        lastReadDateRemoved({ entry: ENTRY })
      );
    });

    describe('when messaging stops', () => {
      beforeEach(() => {
        store.setState({
          ...initialState,
          [MESSAGING_FEATURE_KEY]: {
            ...initialMessagingState,
            started: false
          },
          [LAST_READ_LIST_FEATURE_KEY]: {
            ...initialLastReadListState,
            loading: false
          }
        });
      });

      it('unsubscribes from updates', () => {
        expect(service.updateSubscription).toBeNull();
      });

      it('unsubscribes from removals', () => {
        expect(service.removeSubscription).toBeNull();
      });
    });
  });
});
