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

import { createFeature, createReducer, on } from '@ngrx/store';
import {
  markSelectedComicBooksRead,
  markSelectedComicBooksReadFailed,
  markSelectedComicBooksReadSuccess,
  markSingleComicBookRead
} from '../actions/comic-books-read.actions';

export const COMIC_BOOKS_READ_FEATURE_KEY = 'comic_books_read_state';

export interface SetComicsReadState {
  updating: boolean;
}

export const initialState: SetComicsReadState = {
  updating: false
};

export const reducer = createReducer(
  initialState,

  on(markSingleComicBookRead, state => ({ ...state, updating: true })),
  on(markSelectedComicBooksRead, state => ({ ...state, updating: true })),
  on(markSelectedComicBooksReadSuccess, state => ({
    ...state,
    updating: false
  })),
  on(markSelectedComicBooksReadFailed, state => ({ ...state, updating: false }))
);

export const comicBooksReadFeature = createFeature({
  name: COMIC_BOOKS_READ_FEATURE_KEY,
  reducer
});
