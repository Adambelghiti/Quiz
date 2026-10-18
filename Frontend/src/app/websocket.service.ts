import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { AuthService } from './auth.service';
import { environment } from '../environments/environment';

type MessageHandler = (payload: any) => void;

/**
 * Thin wrapper around a STOMP-over-WebSocket connection.
 * <p>
 * Components call {@link watch} with a topic and a handler; the service keeps
 * one shared connection, re-subscribes automatically after a reconnect, and
 * returns an unsubscribe function to call from {@code ngOnDestroy}.
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private readonly url = environment.wsUrl || WebSocketService.deriveUrl();

  /** Build a same-origin WebSocket URL when none is configured explicitly. */
  private static deriveUrl(): string {
    if (typeof location === 'undefined') {
      return 'ws://localhost:8080/ws';
    }
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    return `${scheme}://${location.host}/ws`;
  }

  private client: Client | null = null;
  private readonly handlers = new Map<string, Set<MessageHandler>>();
  private readonly subscriptions = new Map<string, StompSubscription>();
  private readonly connected$ = new BehaviorSubject<boolean>(false);

  constructor(private auth: AuthService) {}

  /** Emits whether the socket is currently connected. */
  get connected(): Observable<boolean> {
    return this.connected$.asObservable();
  }

  /**
   * Subscribe to a topic. Returns a function that removes this handler again.
   */
  watch<T>(topic: string, handler: (payload: T) => void): () => void {
    this.ensureConnected();
    if (!this.handlers.has(topic)) {
      this.handlers.set(topic, new Set());
    }
    this.handlers.get(topic)!.add(handler as MessageHandler);
    this.subscribeIfConnected(topic);

    return () => this.unwatch(topic, handler as MessageHandler);
  }

  private unwatch(topic: string, handler: MessageHandler): void {
    const set = this.handlers.get(topic);
    if (!set) {
      return;
    }
    set.delete(handler);
    if (set.size === 0) {
      this.handlers.delete(topic);
      this.subscriptions.get(topic)?.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  /**
   * Tear the connection down and forget all subscriptions. Call on logout so
   * the next user connects with their own token.
   */
  disconnect(): void {
    this.handlers.clear();
    this.subscriptions.clear();
    this.connected$.next(false);
    const client = this.client;
    this.client = null;
    client?.deactivate();
  }

  private ensureConnected(): void {
    if (this.client) {
      return;
    }

    // The server authenticates the CONNECT frame with this bearer token and
    // scopes game-topic subscriptions to participants.
    const token = this.auth.getToken();
    const client = new Client({
      brokerURL: this.url,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    });

    client.onConnect = () => {
      this.connected$.next(true);
      // A fresh connection loses all subscriptions, so re-create them.
      this.subscriptions.clear();
      for (const topic of this.handlers.keys()) {
        this.subscribeIfConnected(topic);
      }
    };
    client.onWebSocketClose = () => this.connected$.next(false);
    client.onStompError = () => this.connected$.next(false);

    this.client = client;
    client.activate();
  }

  private subscribeIfConnected(topic: string): void {
    const client = this.client;
    if (!client || !client.connected || this.subscriptions.has(topic)) {
      return;
    }
    const subscription = client.subscribe(topic, (message: IMessage) => {
      let payload: any = null;
      try {
        payload = message.body ? JSON.parse(message.body) : null;
      } catch {
        payload = null;
      }
      this.handlers.get(topic)?.forEach((handler) => handler(payload));
    });
    this.subscriptions.set(topic, subscription);
  }
}
