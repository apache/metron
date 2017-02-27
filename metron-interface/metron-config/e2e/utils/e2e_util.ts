import { browser, protractor } from 'protractor/globals';

export function changeURL(url: string) {
    return browser.get(url).then(() => {
        return browser.getCurrentUrl().then((newURL) => {
            return newURL;
        })
    })
}

export function waitForElementInVisibility (_element ) {
    var EC = protractor.ExpectedConditions;
    return browser.wait(EC.invisibilityOf(_element));
}

export function waitForElementPresence (_element ) {
    var EC = protractor.ExpectedConditions;
    return browser.wait(EC.presenceOf(_element));
}

export function waitForElementVisibility (_element ) {
    var EC = protractor.ExpectedConditions;
    return browser.wait(EC.visibilityOf(_element));
}

export function waitForStalenessOf (_element ) {
    var EC = protractor.ExpectedConditions;
    return browser.wait(EC.stalenessOf(_element));
}

