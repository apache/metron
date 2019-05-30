import { ShowHideAlertEntriesComponent } from "./show-hide-alert-entries.component";
import { ComponentFixture, async, TestBed } from "@angular/core/testing";
import { SwitchComponent } from "app/shared/switch/switch.component";
import { ImplicitFilteringService } from "../service/ImplicitFilteringService";

describe('show/hide RESOLVE and/or DISMISSED alert entries', () => {

  let component: ShowHideAlertEntriesComponent;
  let fixture: ComponentFixture<ShowHideAlertEntriesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ShowHideAlertEntriesComponent,
        SwitchComponent
      ],
      providers: [
        ImplicitFilteringService,
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ShowHideAlertEntriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should invoke ImplicitFilterService.addFilter() on user switch to hide', () => {
    // expect(component.implicitFiltersService).toBeDefined();
  })
});
