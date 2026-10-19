import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/** A full question (including the answer); administrator view. */
export interface AdminQuestion {
  id?: number;
  text: string;
  category: string;
  difficulty: number;
  language: string;
  correctAnswer: string;
  options: string[];
  hint?: string;
  explanation?: string;
}

/** Admin CRUD for the question bank. */
@Injectable({ providedIn: 'root' })
export class QuestionAdminService {
  private apiUrl = `${environment.apiBaseUrl}/questions`;

  constructor(private http: HttpClient) {}

  list(): Observable<AdminQuestion[]> {
    return this.http.get<AdminQuestion[]>(`${this.apiUrl}/admin`);
  }

  create(question: AdminQuestion): Observable<AdminQuestion> {
    return this.http.post<AdminQuestion>(this.apiUrl, question);
  }

  update(id: number, question: AdminQuestion): Observable<AdminQuestion> {
    return this.http.put<AdminQuestion>(`${this.apiUrl}/${id}`, question);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
