import { AfterViewInit, Directive, ElementRef } from '@angular/core';

/**
 * Moves keyboard focus to a page heading when its view appears. Used on the
 * first heading of each screen so a route change is announced and keyboard
 * users do not start at the top of the document every time.
 *
 * Apply with {@code tabindex="-1"} on the heading.
 */
@Directive({
  selector: '[appFocusOnInit]',
  standalone: true,
})
export class FocusHeadingDirective implements AfterViewInit {
  constructor(private readonly element: ElementRef<HTMLElement>) {}

  ngAfterViewInit(): void {
    try {
      this.element.nativeElement.focus();
    } catch {
      /* Older browsers may refuse focus on a heading; navigation still works. */
    }
  }
}
