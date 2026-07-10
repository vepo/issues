import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ServerSideEventsClient } from './sse.client';

describe('ServerSideEventsClient', () => {
  let client: ServerSideEventsClient;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['getToken', 'refreshToken']);
    authService.getToken.and.returnValue('access-token');
    authService.refreshToken.and.returnValue(of({ token: 'new', refreshToken: 'r', expiresIn: 900 }));

    TestBed.configureTestingModule({
      providers: [
        ServerSideEventsClient,
        { provide: AuthService, useValue: authService },
      ],
    });
    client = TestBed.inject(ServerSideEventsClient);
  });

  afterEach(() => {
    client.close();
  });

  it('should refresh token when SSE returns 401 then schedule reconnect path', async () => {
    const fetchSpy = spyOn(window, 'fetch').and.returnValue(
      Promise.resolve(new Response(null, { status: 401 })),
    );
    authService.getToken.and.returnValues('stale', 'stale', 'refreshed');

    client.connect('/api/notifications/register').subscribe();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(fetchSpy).toHaveBeenCalled();
    expect(authService.refreshToken).toHaveBeenCalled();
  });

  it('should return EMPTY when no token is available', () => {
    authService.getToken.and.returnValue(null);
    authService.refreshToken.and.returnValue(throwError(() => new Error('no refresh')));
    let emitted = false;
    client.connect('/api/notifications/register').subscribe({
      next: () => {
        emitted = true;
      },
    });
    expect(emitted).toBeFalse();
  });
});
