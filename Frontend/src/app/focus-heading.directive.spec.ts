import { ElementRef } from '@angular/core';
import { FocusHeadingDirective } from './focus-heading.directive';

describe('FocusHeadingDirective', () => {
  it('moves focus to the heading after the view initialises', () => {
    const focus = jasmine.createSpy('focus');
    const element = { nativeElement: { focus } } as unknown as ElementRef<HTMLElement>;
    const directive = new FocusHeadingDirective(element);

    directive.ngAfterViewInit();

    expect(focus).toHaveBeenCalled();
  });

  it('does not fail when the browser cannot focus the element', () => {
    const element = {
      nativeElement: {
        focus: () => {
          throw new Error('not focusable');
        },
      },
    } as unknown as ElementRef<HTMLElement>;
    const directive = new FocusHeadingDirective(element);

    expect(() => directive.ngAfterViewInit()).not.toThrow();
  });
});
