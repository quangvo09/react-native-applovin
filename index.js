
module.exports = {
  get AppLovinRewarded() {
    return require('./RNAppLovinRewarded').default;
  },
  get AppLovinInterstitial() {
    return require('./RNAppLovinInterstitial').default;
  }
}