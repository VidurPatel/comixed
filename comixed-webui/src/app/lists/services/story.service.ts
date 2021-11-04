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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { LoggerService } from '@angular-ru/cdk/logger';
import {
  LOAD_ALL_STORY_NAMES_URL,
  LOAD_STORIES_FOR_NAME_URL
} from '@app/lists/lists.constants';
import { interpolate } from '@app/core';

@Injectable({
  providedIn: 'root'
})
export class StoryService {
  constructor(private logger: LoggerService, private http: HttpClient) {}

  loadAllNames(): Observable<any> {
    this.logger.trace('Loading all story names');
    return this.http.get(interpolate(LOAD_ALL_STORY_NAMES_URL));
  }

  loadForName(args: { name: string }): Observable<any> {
    this.logger.trace('Loading all stories for name:', args);
    return this.http.get(
      interpolate(LOAD_STORIES_FOR_NAME_URL, { name: args.name })
    );
  }
}
