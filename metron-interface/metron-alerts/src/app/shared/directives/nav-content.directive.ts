import { Directive, ElementRef, OnInit } from '@angular/core';

@Directive({
  selector: '[appNavContent]'
})
export class NavContentDirective implements OnInit {

  constructor(private element: ElementRef) {
  }

  ngOnInit() {
    this.applyStyles();
  }
  private applyStyles() {
    this.element.nativeElement.classList = 'nav-content';
  }

}
