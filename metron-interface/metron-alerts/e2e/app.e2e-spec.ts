import { MetronAlertsPage } from './app.po';

describe('metron-alerts App', function() {
  let page: MetronAlertsPage;

  beforeEach(() => {
    page = new MetronAlertsPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
