import { browser, protractor } from 'protractor/globals';

export function changeURL(url: string) {
    return browser.get(url).then(() => {
        return browser.getCurrentUrl().then((newURL) => {
            return newURL;
        })
    })
}

export function waitForElement ( _element ) {
    var EC = protractor.ExpectedConditions;
    return browser.wait(EC.visibilityOf(_element));
}
