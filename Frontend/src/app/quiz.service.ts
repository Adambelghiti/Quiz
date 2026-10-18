import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/** The filter values that actually have questions. */
export interface QuizFacets {
  categories: string[];
  difficulties: number[];
  languages: string[];
}

/** Question metadata endpoints used by the game setup screen. */
@Injectable({ providedIn: 'root' })
export class QuizService {
  private apiUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  /** Available categories, difficulties and languages that have questions. */
  getFacets(): Observable<QuizFacets> {
    return this.http.get<QuizFacets>(`${this.apiUrl}/questions/facets`);
  }

  /** Available categories. */
  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/categories`);
  }

  /** Supported difficulty levels. */
  getDifficulties(): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/difficulties`);
  }
}
